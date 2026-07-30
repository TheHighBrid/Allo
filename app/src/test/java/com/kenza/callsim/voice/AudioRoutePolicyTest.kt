package com.kenza.callsim.voice

import android.media.AudioDeviceInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioRoutePolicyTest {
    @Test fun earpieceAndHeadsetsAllowFullDuplex() {
        assertTrue(AudioRoutePolicy.allowsFullDuplex(AudioDeviceInfo.TYPE_BUILTIN_MIC, false))
        assertTrue(AudioRoutePolicy.allowsFullDuplex(AudioDeviceInfo.TYPE_WIRED_HEADSET, false))
        assertTrue(AudioRoutePolicy.allowsFullDuplex(AudioDeviceInfo.TYPE_USB_HEADSET, false))
        assertTrue(AudioRoutePolicy.allowsFullDuplex(AudioDeviceInfo.TYPE_BLUETOOTH_SCO, false))
    }

    @Test fun speakerAndUnknownRoutesRemainConservative() {
        assertFalse(AudioRoutePolicy.allowsFullDuplex(AudioDeviceInfo.TYPE_BUILTIN_MIC, true))
        assertFalse(AudioRoutePolicy.allowsFullDuplex(null, false))
    }
}
