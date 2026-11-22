package it.mobile.bisax.ptzvision.ui.console

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ZoomLabelTest {
    @Test fun hidesUnavailableZoom() {
        assertNull(MainUiState().zoomLabel())
        assertNull(MainUiState(zoomLevel = 3.5, zoomFeedback = false).zoomLabel())
    }

    @Test fun displaysReportedZoom() {
        assertEquals("3.5x", MainUiState(zoomLevel = 3.5).zoomLabel())
    }
}
