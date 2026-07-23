package com.kycis.demo.kycis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KycisFieldKbTest {
    @Test
    fun phoneKb_matchesNativeParityRules() {
        val kb = KycisFieldKb.forComponent("phone_field")
        assertNotNull(kb)
        assertEquals("Phone Number", kb!!.displayName)
        assertTrue(kb.validations.any { it.contains("10 digits") })
        assertTrue(kb.validations.any { it.contains("6, 7, 8, or 9") })
        assertTrue(kb.commonIssues.isNotEmpty())
        assertTrue(kb.faqs.isNotEmpty())
    }

    @Test
    fun allScreenFieldIds_haveKb() {
        val required = listOf(
            "phone_field", "phone_otp_field", "email_field", "email_otp_field",
            "pan_field", "dob_field", "terms_checkbox",
            "name_field", "father_name_field", "gender_field",
            "marital_status_field", "residency_status_field",
            "aadhaar_digilocker_field", "verify_documents_button",
            "aadhaar_front_field", "aadhaar_back_field",
            "selfie_capture", "signature_field",
        )
        required.forEach { id ->
            assertNotNull("missing KB for $id", KycisFieldKb.forComponent(id))
        }
    }

    @Test
    fun unknownComponent_returnsNull() {
        assertEquals(null, KycisFieldKb.forComponent("no_such_field"))
    }
}
