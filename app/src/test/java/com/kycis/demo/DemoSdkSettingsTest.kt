package com.kycis.demo

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.kycis.demo.kycis.KycisIntegration
import com.kycis.sdk.core.TriggerStartMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DemoSdkSettingsTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Isolate prefs between tests
        context.getSharedPreferences("kycis_demo_sdk_settings", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("kycis_demo_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun defaults_mapIntoRuntimePolicy() {
        val policy = KycisIntegration.policyFromSnapshot(
            backendBaseUrl = "https://example.com/v1",
            snapshot = DemoSdkSettings.Snapshot(),
        )
        assertEquals(TriggerStartMode.CONFIRM_UI, policy.triggerStartMode)
        assertTrue(policy.triggerSettings.autoTriggerEnabled)
        assertTrue(policy.passiveEvalEnabled)
        assertTrue(policy.triggerSettings.includeErrorSignals)
        assertTrue(policy.triggerSettings.includeTimeSpentSignals)
        assertTrue(policy.triggerSettings.includeIdleSignals)
        assertTrue(policy.triggerSettings.includeStepHints)
        assertTrue(policy.reportComponentInputHints)
        assertFalse(policy.componentInputHintsMasked)
        assertFalse(policy.autoCaptureEnabled)
        assertTrue(policy.debugEnabled)
    }

    @Test
    fun eachBoolFlip_mapsIntoRuntimePolicy() {
        val cases = listOf(
            DemoSdkSettings.Snapshot(autoTrigger = false) to { p: com.kycis.sdk.core.RuntimePolicy ->
                assertFalse(p.triggerSettings.autoTriggerEnabled)
            },
            DemoSdkSettings.Snapshot(passiveEval = false) to { p ->
                assertFalse(p.passiveEvalEnabled)
            },
            DemoSdkSettings.Snapshot(includeErrors = false) to { p ->
                assertFalse(p.triggerSettings.includeErrorSignals)
            },
            DemoSdkSettings.Snapshot(includeTimeSpent = false) to { p ->
                assertFalse(p.triggerSettings.includeTimeSpentSignals)
            },
            DemoSdkSettings.Snapshot(includeIdle = false) to { p ->
                assertFalse(p.triggerSettings.includeIdleSignals)
            },
            DemoSdkSettings.Snapshot(includeStepHints = false) to { p ->
                assertFalse(p.triggerSettings.includeStepHints)
            },
            DemoSdkSettings.Snapshot(reportHints = false) to { p ->
                assertFalse(p.reportComponentInputHints)
            },
            DemoSdkSettings.Snapshot(maskHints = true) to { p ->
                assertTrue(p.componentInputHintsMasked)
            },
            DemoSdkSettings.Snapshot(autoCapture = true) to { p ->
                assertTrue(p.autoCaptureEnabled)
            },
            DemoSdkSettings.Snapshot(debugLogging = false) to { p ->
                assertFalse(p.debugEnabled)
            },
        )
        cases.forEach { (snapshot, assertFn) ->
            assertFn(KycisIntegration.policyFromSnapshot("https://example.com/v1", snapshot))
        }
    }

    @Test
    fun eachTriggerStartMode_mapsIntoRuntimePolicy() {
        TriggerStartMode.entries.forEach { mode ->
            val policy = KycisIntegration.policyFromSnapshot(
                "https://example.com/v1",
                DemoSdkSettings.Snapshot(triggerStartMode = mode),
            )
            assertEquals(mode, policy.triggerStartMode)
        }
    }

    @Test
    fun prefsRoundTrip_eachBoolAndMode() {
        DemoSdkSettings.Catalog.triggerGroup.forEach { setting ->
            DemoSdkSettings.setBool(context, setting, !setting.default)
            assertEquals(!setting.default, DemoSdkSettings.getBool(context, setting))
            DemoSdkSettings.setBool(context, setting, setting.default)
            assertEquals(setting.default, DemoSdkSettings.getBool(context, setting))
        }
        DemoSdkSettings.Catalog.privacyGroup.forEach { setting ->
            DemoSdkSettings.setBool(context, setting, !setting.default)
            assertEquals(!setting.default, DemoSdkSettings.getBool(context, setting))
        }
        DemoSdkSettings.Catalog.captureGroup.forEach { setting ->
            DemoSdkSettings.setBool(context, setting, !setting.default)
            assertEquals(!setting.default, DemoSdkSettings.getBool(context, setting))
        }
        TriggerStartMode.entries.forEach { mode ->
            DemoSdkSettings.setTriggerStartMode(context, mode)
            assertEquals(mode, DemoSdkSettings.getTriggerStartMode(context))
        }
    }

    @Test
    fun snapshotSaveLoad_matchesPolicyMapping() {
        val wanted = DemoSdkSettings.Snapshot(
            triggerStartMode = TriggerStartMode.IMMEDIATE,
            autoTrigger = false,
            passiveEval = false,
            includeErrors = false,
            includeTimeSpent = false,
            includeIdle = false,
            includeStepHints = false,
            reportHints = false,
            maskHints = true,
            autoCapture = true,
            debugLogging = false,
        )
        DemoSdkSettings.saveSnapshot(context, wanted)
        val loaded = DemoSdkSettings.loadSnapshot(context)
        assertEquals(wanted, loaded)
        val policy = KycisIntegration.policyFromSnapshot("https://api.example/v1", loaded)
        assertEquals(TriggerStartMode.IMMEDIATE, policy.triggerStartMode)
        assertFalse(policy.triggerSettings.autoTriggerEnabled)
        assertFalse(policy.passiveEvalEnabled)
        assertTrue(policy.componentInputHintsMasked)
        assertTrue(policy.autoCaptureEnabled)
        assertFalse(policy.debugEnabled)
    }

    @Test
    fun handholdingPreference_supportedValuesPersist() {
        listOf("passive", "hybrid", "active", "inherit").forEach { pref ->
            assertEquals(pref, BackendUrlStore.saveHandholdingPreference(context, pref))
            assertEquals(pref, BackendUrlStore.getHandholdingPreference(context))
        }
        assertEquals("hybrid", BackendUrlStore.saveHandholdingPreference(context, "bogus"))
        assertEquals("hybrid", BackendUrlStore.getHandholdingPreference(context))
    }

    @Test
    fun backendOwnedRefs_catalogIsNonEmptyAndDocumented() {
        assertTrue(DemoSdkSettings.backendOwnedRefs.isNotEmpty())
        DemoSdkSettings.backendOwnedRefs.forEach { ref ->
            assertTrue(ref.name.isNotBlank())
            assertTrue(ref.meaning.isNotBlank())
            assertTrue(ref.supported.isNotBlank())
        }
    }
}
