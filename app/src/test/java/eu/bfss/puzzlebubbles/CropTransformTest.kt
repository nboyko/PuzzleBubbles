package eu.bfss.puzzlebubbles

import eu.bfss.puzzlebubbles.presentation.library.CropTransform
import org.junit.Assert.*
import org.junit.Test

class CropTransformTest {
    @Test fun initialCropIsCenteredForEveryAspectRatio() {
        for ((w, h) in listOf(300 to 100, 100 to 300, 1024 to 1024, 20 to 10)) {
            val crop = CropTransform(w, h).region
            assertEquals(minOf(w, h).toFloat(), crop.side, .001f)
            assertEquals(w / 2f, crop.left + crop.side / 2, .001f)
            assertEquals(h / 2f, crop.top + crop.side / 2, .001f)
        }
    }
    @Test fun panFollowsFingerAndStopsAtEverySourceEdge() {
        val state = CropTransform(300, 100)
        assertEquals(0f, state.gesture(1f, 500f, 500f, 0f, 0f, 100f).region.left, .001f)
        assertEquals(200f, state.gesture(1f, -500f, -500f, 0f, 0f, 100f).region.left, .001f)
        assertEquals(0f, state.gesture(1f, 0f, 500f, 0f, 0f, 100f).region.top, .001f)
        val portrait = CropTransform(100, 300)
        assertEquals(200f, portrait.gesture(1f, 500f, -500f, 0f, 0f, 100f).region.top, .001f)
    }
    @Test fun combinedZoomAndPanAlwaysKeepTheViewportCovered() {
        for ((w, h) in listOf(300 to 100, 100 to 300, 1024 to 1024, 20 to 10)) {
            var state = CropTransform(w, h)
            for (zoom in listOf(100f, .0001f, 2f, 3f, .5f)) {
                for (pan in listOf(-10000f, 10000f, 0f)) {
                    state = state.gesture(zoom, pan, -pan, 35f, -20f, 200f)
                    assertTrue(state.zoom in 1f..4f)
                    val crop = state.region
                    assertTrue(crop.left >= -.001f && crop.top >= -.001f)
                    assertTrue(crop.left + crop.side <= w + .001f)
                    assertTrue(crop.top + crop.side <= h + .001f)
                }
            }
        }
    }
    @Test fun pinchKeepsTheSourcePointUnderTheFingersWhenNotClamped() {
        val before = CropTransform(1000, 1000)
        val after = before.gesture(2f, 0f, 0f, 30f, -20f, 200f)
        assertEquals(before.centerX + 30 * before.side / 200, after.centerX + 30 * after.side / 200, .001f)
        assertEquals(before.centerY - 20 * before.side / 200, after.centerY - 20 * after.side / 200, .001f)
    }
    @Test fun viewportResizingDoesNotChangeTheSelectedRegion() {
        val source = CropTransform(2000, 1000)
        val small = source.gesture(2f, 20f, -10f, 30f, 10f, 100f)
        val large = source.gesture(2f, 80f, -40f, 120f, 40f, 400f)
        assertEquals(small, large)
        assertEquals(small, small.gesture(1f, 0f, 0f, 0f, 0f, 600f))
    }
    @Test fun returningToFullSquareIsUnchangedAndInvalidGesturesAreIgnored() {
        val initial = CropTransform(1024, 1024)
        val zoomed = initial.gesture(3f, 20f, 20f, 10f, 10f, 200f)
        assertFalse(zoomed.unchanged)
        assertTrue(zoomed.gesture(.01f, 0f, 0f, 0f, 0f, 200f).unchanged)
        assertEquals(initial, initial.gesture(Float.NaN, 0f, 0f, 0f, 0f, 200f))
        assertEquals(initial, initial.gesture(2f, 0f, 0f, 0f, 0f, 0f))
    }
}
