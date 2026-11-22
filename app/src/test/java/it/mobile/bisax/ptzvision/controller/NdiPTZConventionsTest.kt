package it.mobile.bisax.ptzvision.controller

import org.junit.Assert.assertEquals
import org.junit.Test

/** The NDI PTZ API uses different sign conventions than the app (see Processing.NDI.Recv.ex.h). */
class NdiPTZConventionsTest {

    @Test
    fun `app pan right maps to negative NDI pan speed`() {
        assertEquals(-0.5f, NdiPTZController.ndiPanSpeed(0.5f))
        assertEquals(1f, NdiPTZController.ndiPanSpeed(-1f))
    }

    @Test
    fun `speeds are clamped to the NDI range`() {
        assertEquals(-1f, NdiPTZController.ndiPanSpeed(3f))
        assertEquals(1f, NdiPTZController.ndiSpeed(1.7f))
        assertEquals(-1f, NdiPTZController.ndiSpeed(-1.7f))
    }

    @Test
    fun `negative zero and NaN are sent as plain zero`() {
        // -0f would be serialised by the SDK as "-0.000000", which some cameras may reject.
        assertEquals(0f.toRawBits(), NdiPTZController.ndiSpeed(-0f).toRawBits())
        assertEquals(0f.toRawBits(), NdiPTZController.ndiPanSpeed(0f).toRawBits())
        assertEquals(0f.toRawBits(), NdiPTZController.ndiSpeed(Float.NaN).toRawBits())
    }
}
