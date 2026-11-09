package eu.bfss.puzzlebubbles

import eu.bfss.puzzlebubbles.domain.RecordingDeadline
import eu.bfss.puzzlebubbles.presentation.library.soundTime
import org.junit.Assert.*
import org.junit.Test

class RecordingDeadlineTest {
    @Test fun rotationAndDelayedTicksDoNotRestartOrExtendTheDeadline() {
        val deadline = RecordingDeadline(12_000)
        assertEquals(3_000L, deadline.elapsed(15_000))
        assertFalse(deadline.expired(16_999))
        assertTrue(deadline.expired(17_000))
        assertTrue(deadline.expired(40_000))
        assertEquals(5_000L, deadline.elapsed(40_000))
        assertEquals(0L, deadline.elapsed(11_000))
    }
    @Test fun durationTextHandlesInitialProgressAndMinuteBoundaries() {
        assertEquals("0:00", soundTime(0))
        assertEquals("0:05", soundTime(5_000))
        assertEquals("1:01", soundTime(61_000))
        assertEquals("0:00", soundTime(-1))
    }
}
