package com.kycis.demo

import android.content.Context
import com.kycis.sdk.core.TriggerStartMode

/**
 * Demo-persisted SDK RuntimePolicy overrides + catalog metadata for Backend Settings UI.
 * Handholding preference stays in [BackendUrlStore] (also applied via AI.setHandholdingPreference).
 */
object DemoSdkSettings {
    private const val PREFS = "kycis_demo_sdk_settings"

    data class BoolSetting(
        val key: String,
        val title: String,
        val meaning: String,
        val supported: String,
        val default: Boolean,
    )

    data class EnumSetting(
        val key: String,
        val title: String,
        val meaning: String,
        val options: List<Pair<String, String>>, // value to short label
        val default: String,
    )

    /** Editable SDK-owned knobs (app → RuntimePolicy). Grouped for the settings UI. */
    object Catalog {
        val triggerStartMode = EnumSetting(
            key = "trigger_start_mode",
            title = "Trigger start mode",
            meaning = "What happens when the SDK decides the user may need voice help.",
            options = listOf(
                TriggerStartMode.CONFIRM_UI.name to "Confirm UI",
                TriggerStartMode.IMMEDIATE.name to "Immediate",
                TriggerStartMode.COOLDOWN.name to "Cooldown",
            ),
            default = TriggerStartMode.CONFIRM_UI.name,
        )

        val autoTrigger = BoolSetting(
            key = "auto_trigger",
            title = "Auto trigger",
            meaning = "Evaluate trigger conditions in the background and offer voice help.",
            supported = "true | false",
            default = true,
        )
        val passiveEval = BoolSetting(
            key = "passive_eval",
            title = "Passive evaluation",
            meaning = "Periodic background loop that checks trigger signals on an interval.",
            supported = "true | false",
            default = true,
        )
        val includeErrors = BoolSetting(
            key = "include_error_signals",
            title = "Error signals",
            meaning = "Count validation failures toward offering the assistant.",
            supported = "true | false",
            default = true,
        )
        val includeTimeSpent = BoolSetting(
            key = "include_time_spent",
            title = "Time-on-screen signals",
            meaning = "Offer help after the user spends long enough on a step.",
            supported = "true | false",
            default = true,
        )
        val includeIdle = BoolSetting(
            key = "include_idle",
            title = "Idle signals",
            meaning = "Offer help when the user appears idle on a step.",
            supported = "true | false",
            default = true,
        )
        val includeStepHints = BoolSetting(
            key = "include_step_hints",
            title = "Step-hint signals",
            meaning = "Use KYC step / schema hints when scoring triggers.",
            supported = "true | false",
            default = true,
        )
        val reportHints = BoolSetting(
            key = "report_hints",
            title = "Report component hints",
            meaning = "Send component_input hint strings to the backend for voice context.",
            supported = "true | false",
            default = true,
        )
        val maskHints = BoolSetting(
            key = "mask_hints",
            title = "Mask hints (PII)",
            meaning = "Mark hints as masked so the agent treats values as sensitive / not for echo.",
            supported = "true | false (demo default false for debugging)",
            default = false,
        )
        val autoCapture = BoolSetting(
            key = "auto_capture",
            title = "Auto-capture UI",
            meaning = "Automatically scrape screen components; demo keeps this off and reports manually.",
            supported = "true | false",
            default = false,
        )
        val debugLogging = BoolSetting(
            key = "debug_logging",
            title = "SDK debug logging",
            meaning = "Verbose KYCIS Logcat (requests, WS, voice). Dev only.",
            supported = "true | false",
            default = true,
        )

        val triggerGroup = listOf(autoTrigger, passiveEval, includeErrors, includeTimeSpent, includeIdle, includeStepHints)
        val privacyGroup = listOf(reportHints, maskHints)
        val captureGroup = listOf(autoCapture, debugLogging)
    }

    /** Backend-owned VOICE_* / speech knobs — display only in the demo (not editable here). */
    data class BackendRef(
        val name: String,
        val meaning: String,
        val supported: String,
    )

    val backendOwnedRefs: List<BackendRef> = listOf(
        BackendRef("VOICE_HANDHOLDING_MODE", "Server default when session preference is inherit.", "passive | hybrid | active"),
        BackendRef("VOICE_STT_PROVIDER / LANGUAGE", "Speech-to-text provider and language.", "e.g. cartesia + en"),
        BackendRef("VOICE_TTS_PROVIDER / LANGUAGE / VOICE", "Text-to-speech provider, language, voice id.", "e.g. cartesia + en + voice UUID"),
        BackendRef("VOICE_LLM_PROVIDER / MODEL", "LLM for the voice agent.", "e.g. cerebras + gpt-oss-120b"),
        BackendRef("VOICE_LLM_REASONING_EFFORT", "Reasoning depth for gpt-oss (latency vs quality).", "low | medium | high | off"),
        BackendRef("VOICE_LLM_PROMPT_CACHE_KEY", "Prompt-cache routing stickiness.", "session | off"),
        BackendRef("Handholding settle / cooldown / caps", "Proactive speak budgets.", "backend YAML / env only"),
    )

    fun getBool(context: Context, setting: BoolSetting): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return if (prefs.contains(setting.key)) prefs.getBoolean(setting.key, setting.default) else setting.default
    }

    fun setBool(context: Context, setting: BoolSetting, value: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(setting.key, value)
            .apply()
    }

    fun getTriggerStartMode(context: Context): TriggerStartMode {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(Catalog.triggerStartMode.key, Catalog.triggerStartMode.default)
        return runCatching { TriggerStartMode.valueOf(raw ?: Catalog.triggerStartMode.default) }
            .getOrDefault(TriggerStartMode.CONFIRM_UI)
    }

    fun setTriggerStartMode(context: Context, mode: TriggerStartMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(Catalog.triggerStartMode.key, mode.name)
            .apply()
    }

    /** Immutable snapshot of editable SDK knobs — used by [toRuntimePolicyFields] and unit tests. */
    data class Snapshot(
        val triggerStartMode: TriggerStartMode = TriggerStartMode.CONFIRM_UI,
        val autoTrigger: Boolean = Catalog.autoTrigger.default,
        val passiveEval: Boolean = Catalog.passiveEval.default,
        val includeErrors: Boolean = Catalog.includeErrors.default,
        val includeTimeSpent: Boolean = Catalog.includeTimeSpent.default,
        val includeIdle: Boolean = Catalog.includeIdle.default,
        val includeStepHints: Boolean = Catalog.includeStepHints.default,
        val reportHints: Boolean = Catalog.reportHints.default,
        val maskHints: Boolean = Catalog.maskHints.default,
        val autoCapture: Boolean = Catalog.autoCapture.default,
        val debugLogging: Boolean = Catalog.debugLogging.default,
    )

    fun loadSnapshot(context: Context): Snapshot = Snapshot(
        triggerStartMode = getTriggerStartMode(context),
        autoTrigger = getBool(context, Catalog.autoTrigger),
        passiveEval = getBool(context, Catalog.passiveEval),
        includeErrors = getBool(context, Catalog.includeErrors),
        includeTimeSpent = getBool(context, Catalog.includeTimeSpent),
        includeIdle = getBool(context, Catalog.includeIdle),
        includeStepHints = getBool(context, Catalog.includeStepHints),
        reportHints = getBool(context, Catalog.reportHints),
        maskHints = getBool(context, Catalog.maskHints),
        autoCapture = getBool(context, Catalog.autoCapture),
        debugLogging = getBool(context, Catalog.debugLogging),
    )

    /** Persist a full snapshot (used by tests and bulk apply). */
    fun saveSnapshot(context: Context, snapshot: Snapshot) {
        setTriggerStartMode(context, snapshot.triggerStartMode)
        setBool(context, Catalog.autoTrigger, snapshot.autoTrigger)
        setBool(context, Catalog.passiveEval, snapshot.passiveEval)
        setBool(context, Catalog.includeErrors, snapshot.includeErrors)
        setBool(context, Catalog.includeTimeSpent, snapshot.includeTimeSpent)
        setBool(context, Catalog.includeIdle, snapshot.includeIdle)
        setBool(context, Catalog.includeStepHints, snapshot.includeStepHints)
        setBool(context, Catalog.reportHints, snapshot.reportHints)
        setBool(context, Catalog.maskHints, snapshot.maskHints)
        setBool(context, Catalog.autoCapture, snapshot.autoCapture)
        setBool(context, Catalog.debugLogging, snapshot.debugLogging)
    }
}
