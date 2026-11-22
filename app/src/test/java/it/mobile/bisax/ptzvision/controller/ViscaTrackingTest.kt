package it.mobile.bisax.ptzvision.controller

import it.mobile.bisax.ptzvision.controller.utils.HexConverter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ViscaTrackingTest {
    private fun completed(hex: String) = ViscaTracking.completed(HexConverter.parseHex(hex).inputStream())

    @Test fun acceptsCompletionWithOrWithoutAcknowledgement() {
        assertTrue(completed("9041FF9051FF"))
        assertTrue(completed("9050FF"))
    }

    @Test fun rejectsErrorsIncompletePacketsAndMismatchedSockets() {
        for (hex in listOf("9041FF", "906002FF", "9051", "9041FF9052FF")) {
            assertFalse(hex, completed(hex))
        }
    }
}
