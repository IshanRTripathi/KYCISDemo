package com.kycis.sdk

import com.kycis.sdk.core.RuntimeContext
import com.kycis.sdk.core.RuntimePolicy
import com.kycis.sdk.core.SdkRuntime
import java.util.UUID

object AI {
    private var initialized = false
    private var apiKey: String? = null
    private var userId: String? = null
    private var phone: String? = null
    private var currentKycStep: String? = null
    private val runtime = SdkRuntime()

    fun init(apiKey: String, userId: String, policy: RuntimePolicy = RuntimePolicy()) {
        require(apiKey.isNotBlank()) { "apiKey cannot be blank" }
        require(userId.isNotBlank()) { "userId cannot be blank" }
        this.apiKey = apiKey
        this.userId = userId
        initialized = true
        runtime.initialize(
            RuntimeContext(
                apiKey = apiKey,
                userId = userId,
                sessionId = UUID.randomUUID().toString(),
            ),
            policy = policy,
        )
        // TODO: bind lifecycle/navigation observers and passive behavior tracking.
    }

    fun setUser(id: String, phone: String? = null) {
        requireInitialized()
        require(id.isNotBlank()) { "id cannot be blank" }
        this.userId = id
        this.phone = phone
        runtime.setUser(id = id, phone = phone)
    }

    fun setKycStep(step: String) {
        requireInitialized()
        require(step.isNotBlank()) { "step cannot be blank" }
        currentKycStep = step
        runtime.setKycStep(step)
    }

    fun trackError(code: String, properties: Map<String, Any?> = emptyMap()) {
        requireInitialized()
        require(code.isNotBlank()) { "code cannot be blank" }
        runtime.onUserInteractionObserved()
        if (properties.isNotEmpty()) {
            // TODO: attach optional error properties to event payload.
        }
        runtime.trackError(code = code, properties = properties)
    }

    internal fun onScreenObservedInternal(screen: String) {
        requireInitialized()
        runtime.onScreenObserved(screen)
    }

    internal fun startAssistantInternal() {
        requireInitialized()
        runtime.startAssistantSession()
    }

    internal fun stopAssistantInternal() {
        requireInitialized()
        runtime.stopAssistantSession()
    }

    private fun requireInitialized() {
        check(initialized) { "AI SDK not initialized. Call AI.init first." }
    }
}
