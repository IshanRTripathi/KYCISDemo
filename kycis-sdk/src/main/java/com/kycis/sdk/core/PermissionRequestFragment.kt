package com.kycis.sdk.core

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment

/**
 * Headless fragment that requests RECORD_AUDIO and delivers the result.
 * Receives the callback directly (no host Activity override needed).
 */
internal const val REQUEST_CODE_RECORD_AUDIO = 0x5F17A001

internal class PermissionRequestFragment : Fragment() {

    @Suppress("Deprecation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_CODE_RECORD_AUDIO)
    }

    @Suppress("Deprecation")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode != REQUEST_CODE_RECORD_AUDIO) return
        val idx = permissions.indexOf(Manifest.permission.RECORD_AUDIO)
        val granted = idx >= 0 && grantResults.getOrElse(idx) { PackageManager.PERMISSION_DENIED } == PackageManager.PERMISSION_GRANTED
        VoicePermissionCallbackHolder.invoke(granted)
        parentFragmentManager.beginTransaction().remove(this).commit()
    }

    companion object {
        const val TAG = "kycis_voice_permission"
    }
}

internal object VoicePermissionCallbackHolder {
    var callback: ((Boolean) -> Unit)? = null

    fun invoke(granted: Boolean) {
        callback?.invoke(granted)
        callback = null
    }
}
