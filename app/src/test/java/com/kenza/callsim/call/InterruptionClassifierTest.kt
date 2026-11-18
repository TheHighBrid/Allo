package com.kenza.callsim.call

import org.junit.Assert.assertEquals
import org.junit.Test

class InterruptionClassifierTest {
    @Test fun distinguishesAcknowledgmentCorrectionAndTopicShift() {
        assertEquals(InterruptionKind.ACKNOWLEDGMENT, InterruptionClassifier.classify("mm-hm"))
        assertEquals(InterruptionKind.CORRECTION, InterruptionClassifier.classify("No, I meant tomorrow"))
        assertEquals(InterruptionKind.TOPIC_CHANGE, InterruptionClassifier.classify("By the way, did you see that?"))
        assertEquals(InterruptionKind.EMOTIONAL, InterruptionClassifier.classify("I'm worried"))
        assertEquals(InterruptionKind.UNCLEAR, InterruptionClassifier.classify(""))
    }
}
