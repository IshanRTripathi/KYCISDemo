package com.kycis.demo.harness

import android.content.Context
import com.kycis.demo.kycis.KycisScreenSchemas
import com.kycis.demo.kycis.KycisWorkflow
import com.kycis.sdk.core.ComponentSchema
import com.kycis.sdk.core.ScreenSchema
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

enum class CheckStatus { PASS, FAIL, SKIPPED }

data class ComponentCheckResult(
    val componentId: String,
    val componentType: String,
    val status: CheckStatus,
    val detail: String,
)

data class FlowStepResult(
    val screenId: String,
    val displayName: String?,
    val status: CheckStatus,
    val componentResults: List<ComponentCheckResult>,
    val notes: List<String>,
)

data class FlowTestReport(
    val running: Boolean = false,
    val steps: List<FlowStepResult> = emptyList(),
    val currentScreenId: String? = null,
    val fatalError: String? = null,
) {
    val overallStatus: CheckStatus
        get() = when {
            fatalError != null -> CheckStatus.FAIL
            steps.any { it.status == CheckStatus.FAIL } -> CheckStatus.FAIL
            steps.isEmpty() -> CheckStatus.SKIPPED
            else -> CheckStatus.PASS
        }

    fun toClipboardText(): String = buildString {
        appendLine("Flow E2E Harness report — overall: $overallStatus")
        fatalError?.let { appendLine("Fatal error: $it") }
        steps.forEach { step ->
            appendLine()
            appendLine("[${step.status}] ${step.screenId}${step.displayName?.let { " – $it" } ?: ""}")
            step.componentResults.forEach { c ->
                appendLine("  - [${c.status}] ${c.componentId} (${c.componentType}): ${c.detail}")
            }
            step.notes.forEach { appendLine("  note: $it") }
        }
    }
}

/**
 * Drives the app's own [KycisScreenSchemas.all] + [KycisWorkflow.model] through the backend using
 * a disposable [HarnessBackendClient] session, then validates the backend's response.
 *
 * Generic by design: this class never hardcodes a screen or component id. Swapping
 * [KycisScreenSchemas] / [KycisWorkflow] for a different tenant's screens/flow makes this exact
 * runner test that tenant's flow, unmodified — see [FlowTestData] for the one small piece of
 * tenant-specific config (sample values) it also reads.
 */
class FlowTestRunner(context: Context) {

    private val client = HarnessBackendClient(context)
    private val _report = MutableStateFlow(FlowTestReport())
    val report: StateFlow<FlowTestReport> = _report.asStateFlow()

    suspend fun run() {
        _report.value = FlowTestReport(running = true)
        withContext(Dispatchers.IO) {
            try {
                runInternal()
            } catch (e: Exception) {
                _report.value = _report.value.copy(
                    running = false,
                    currentScreenId = null,
                    fatalError = "Unexpected error: ${e.message ?: e.toString()}",
                )
            }
        }
    }

    private fun runInternal() {
        for (schema in KycisScreenSchemas.all) {
            val resp = client.pushSchema(schema)
            if (!resp.ok) {
                _report.value = _report.value.copy(
                    running = false,
                    fatalError = "Schema push failed for '${schema.screenId}': HTTP ${resp.statusCode} ${resp.error ?: resp.rawBody}",
                )
                return
            }
        }

        val workflowResp = client.pushWorkflow(KycisWorkflow.model)
        if (!workflowResp.ok) {
            _report.value = _report.value.copy(
                running = false,
                fatalError = "Workflow push failed: HTTP ${workflowResp.statusCode} ${workflowResp.error ?: workflowResp.rawBody}",
            )
            return
        }

        val schemasById = KycisScreenSchemas.all.associateBy { it.screenId }
        val stages = KycisWorkflow.model.stages
        val stageIds = stages.map { it.id }
        val steps = mutableListOf<FlowStepResult>()

        for ((index, stage) in stages.withIndex()) {
            _report.value = _report.value.copy(currentScreenId = stage.id, steps = steps.toList())

            val schema = schemasById[stage.id]
            steps += if (schema == null) {
                FlowStepResult(
                    screenId = stage.id,
                    displayName = stage.displayName,
                    status = CheckStatus.SKIPPED,
                    componentResults = emptyList(),
                    notes = listOf("No matching ScreenSchema registered for this workflow stage"),
                )
            } else {
                runScreenStep(schema, stage.id, stageIds, index)
            }
            _report.value = _report.value.copy(steps = steps.toList())
        }

        _report.value = _report.value.copy(running = false, currentScreenId = null, steps = steps.toList())
    }

    private fun runScreenStep(
        schema: ScreenSchema,
        screenId: String,
        stepSequence: List<String>,
        stageIndex: Int,
    ): FlowStepResult {
        val notes = mutableListOf<String>()

        val screenStateResp = client.setScreenState(screenId)
        if (!screenStateResp.ok) {
            notes += "screen_state event rejected: HTTP ${screenStateResp.statusCode}"
        }

        val flowResp = client.setFlow(flowKey = "kyc", stepSequence = stepSequence, currentStep = screenId)
        if (!flowResp.ok) {
            notes += "flows/set rejected: HTTP ${flowResp.statusCode}"
        } else {
            val position = flowResp.json?.optJSONObject("progress")?.optInt("position", -1) ?: -1
            val expected = stageIndex + 1
            if (position in 0..Int.MAX_VALUE && position != expected) {
                notes += "flow position mismatch: expected $expected, got $position"
            }
        }

        val componentResults = schema.components.map { checkComponent(screenId, it) }

        val contextResp = client.getAssistantContext()
        var contextOk = contextResp.ok
        if (contextResp.ok) {
            val currentScreen = contextResp.json?.optString("current_screen")
            if (currentScreen != screenId) {
                contextOk = false
                notes += "assistant context current_screen mismatch: expected '$screenId', got '$currentScreen'"
            }
        } else {
            notes += "assistant/context fetch failed: HTTP ${contextResp.statusCode} ${contextResp.error ?: ""}"
        }

        val overall = when {
            !contextOk -> CheckStatus.FAIL
            componentResults.any { it.status == CheckStatus.FAIL } -> CheckStatus.FAIL
            componentResults.isNotEmpty() && componentResults.all { it.status == CheckStatus.SKIPPED } -> CheckStatus.SKIPPED
            else -> CheckStatus.PASS
        }

        return FlowStepResult(
            screenId = screenId,
            displayName = schema.displayName,
            status = overall,
            componentResults = componentResults,
            notes = notes,
        )
    }

    private fun checkComponent(screenId: String, component: ComponentSchema): ComponentCheckResult {
        if (!FlowTestData.isSimulatable(component)) {
            return ComponentCheckResult(
                componentId = component.id,
                componentType = component.type.value,
                status = CheckStatus.SKIPPED,
                detail = "Binary capture component - not simulated by this harness",
            )
        }

        val sample = FlowTestData.sampleFor(component)
        val rule = component.validations.firstOrNull()

        if (sample.invalid != null) {
            val invalidResp = client.sendValidationFailed(
                code = rule?.errorCodes?.firstOrNull() ?: "harness_invalid_value",
                componentId = component.id,
                componentType = component.type.value,
                expectedPattern = rule?.pattern,
                validationRuleId = rule?.ruleId,
                hint = sample.invalid,
                screenId = screenId,
            )
            if (!invalidResp.ok) {
                return ComponentCheckResult(
                    componentId = component.id,
                    componentType = component.type.value,
                    status = CheckStatus.FAIL,
                    detail = "validation_failed event rejected: HTTP ${invalidResp.statusCode} ${invalidResp.error ?: ""}",
                )
            }
        }

        val validResp = client.sendComponentInput(
            componentId = component.id,
            hint = sample.valid,
            screenId = screenId,
            componentType = component.type.value,
        )
        if (!validResp.ok) {
            return ComponentCheckResult(
                componentId = component.id,
                componentType = component.type.value,
                status = CheckStatus.FAIL,
                detail = "component_input event rejected: HTTP ${validResp.statusCode} ${validResp.error ?: ""}",
            )
        }

        return ComponentCheckResult(
            componentId = component.id,
            componentType = component.type.value,
            status = CheckStatus.PASS,
            detail = "component_input accepted (value='${sample.valid}')",
        )
    }
}
