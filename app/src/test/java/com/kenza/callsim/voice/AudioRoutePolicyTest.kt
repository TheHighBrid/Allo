package com.kenza.callsim.voice

import android.media.AudioDeviceInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioRoutePolicyTest {
    @Test fun builtInRoutesDefaultToHalfDuplexWhileHeadsetsAllowBargeIn() {
        AudioRoutePolicy.earpieceFullDuplexEnabled = false

        assertFalse(AudioRoutePolicy.allowsFullDuplex(AudioDeviceInfo.TYPE_BUILTIN_MIC, false))
        assertFalse(AudioRoutePolicy.allowsFullDuplex(AudioDeviceInfo.TYPE_BUILTIN_MIC, true))
        assertTrue(AudioRoutePolicy.allowsFullDuplex(AudioDeviceInfo.TYPE_WIRED_HEADSET, false))
        assertTrue(AudioRoutePolicy.allowsFullDuplex(AudioDeviceInfo.TYPE_USB_HEADSET, false))
        assertTrue(AudioRoutePolicy.allowsFullDuplex(AudioDeviceInfo.TYPE_BLUETOOTH_SCO, false))
        assertFalse(AudioRoutePolicy.allowsFullDuplex(null, false))
    }

    @Test fun diagnosticsCanExplicitlyOptBuiltInEarpieceIntoFullDuplex() {
        try {
            AudioRoutePolicy.earpieceFullDuplexEnabled = true
            assertTrue(AudioRoutePolicy.allowsFullDuplex(AudioDeviceInfo.TYPE_BUILTIN_MIC, false))
        } finally {
            AudioRoutePolicy.earpieceFullDuplexEnabled = false
        }
    }
}
