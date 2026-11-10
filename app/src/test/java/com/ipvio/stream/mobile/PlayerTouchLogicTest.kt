package com.ipvio.stream.mobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerTouchLogicTest {
    @Test fun fractionIsClampedAndZeroWithoutDuration() {
        assertEquals(0.25f, PlayerTouchLogic.fraction(250, 1000), 0.0001f)
        assertEquals(1f, PlayerTouchLogic.fraction(2000, 1000), 0.0001f)
        assertEquals(0f, PlayerTouchLogic.fraction(500, 0), 0.0001f)
    }

    @Test fun targetFollowsTheFingerAndStaysInsideTheBar() {
        assertEquals(30_000L, PlayerTouchLogic.target(300f, 1000f, 100_000L))
        assertEquals(0L, PlayerTouchLogic.target(-50f, 1000f, 100_000L))
        assertEquals(100_000L, PlayerTouchLogic.target(1200f, 1000f, 100_000L))
        assertEquals(0L, PlayerTouchLogic.target(300f, 0f, 100_000L))
        assertEquals(0L, PlayerTouchLogic.target(300f, 1000f, 0L))
    }

    @Test fun onlyPhonesAreLockedToLandscape() {
        assertTrue(PlayerTouchLogic.lockLandscape(isPhone = true, portrait = false))
        assertFalse(PlayerTouchLogic.lockLandscape(isPhone = true, portrait = true))
        assertFalse(PlayerTouchLogic.lockLandscape(isPhone = false, portrait = false))
    }
}
