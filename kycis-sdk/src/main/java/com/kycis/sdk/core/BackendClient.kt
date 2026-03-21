package com.kycis.sdk.core

internal interface BackendClient {
    fun setUser(id: String, phone: String?)
    fun trackErrorEvent(
        userId: String,
        sessionId: String,
        screen: String?,
        code: String,
    )
    fun evaluateTrigger(
        userId: String,
        sessionId: String,
        signals: TriggerSignals,
        onResult: (TriggerDecision) -> Unit,
    )
    fun startAssistant(userId: String, sessionId: String, screen: String?)
    fun stopAssistant(userId: String, sessionId: String)
}

internal class NoOpBackendClient : BackendClient {
    override fun setUser(id: String, phone: String?) = Unit

    override fun trackErrorEvent(
        userId: String,
        sessionId: String,
        screen: String?,
        code: String,
    ) = Unit

    override fun evaluateTrigger(
        userId: String,
        sessionId: String,
        signals: TriggerSignals,
        onResult: (TriggerDecision) -> Unit,
    ) {
        onResult(TriggerDecision(trigger = false, reason = "not_wired", action = "none"))
    }

    override fun startAssistant(userId: String, sessionId: String, screen: String?) = Unit

    override fun stopAssistant(userId: String, sessionId: String) = Unit
}
