package com.ipvio.stream.mobile

import android.content.pm.ActivityInfo
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

    @Test fun rotationLockWinsThenPhonesTurnWithinTheirOrientation() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_LOCKED, PlayerTouchLogic.orientation(isPhone = true, portrait = false, locked = true))
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_LOCKED, PlayerTouchLogic.orientation(isPhone = false, portrait = false, locked = true))
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, PlayerTouchLogic.orientation(isPhone = true, portrait = false, locked = false))
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT, PlayerTouchLogic.orientation(isPhone = true, portrait = true, locked = false))
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, PlayerTouchLogic.orientation(isPhone = false, portrait = true, locked = false))
        // TV guide open: a phone follows how it is held; the rotation lock still wins
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_USER, PlayerTouchLogic.orientation(isPhone = true, portrait = false, locked = false, guide = true))
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_LOCKED, PlayerTouchLogic.orientation(isPhone = true, portrait = false, locked = true, guide = true))
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, PlayerTouchLogic.orientation(isPhone = false, portrait = false, locked = false, guide = true))
    }

    @Test fun swipeUpRaisesSwipeDownLowersAndStaysInRange() {
        assertEquals(0.75f, PlayerTouchLogic.swipeLevel(0.5f, -250f, 1333.33f), 0.01f)
        assertEquals(0.25f, PlayerTouchLogic.swipeLevel(0.5f, 250f, 1333.33f), 0.01f)
        assertEquals(1f, PlayerTouchLogic.swipeLevel(0.9f, -5000f, 1000f), 0.0001f)
        assertEquals(0f, PlayerTouchLogic.swipeLevel(0.1f, 5000f, 1000f), 0.0001f)
        assertEquals(0.4f, PlayerTouchLogic.swipeLevel(0.4f, -100f, 0f), 0.0001f)
        assertEquals(15, PlayerTouchLogic.volumeIndex(1f, 15))
        assertEquals(8, PlayerTouchLogic.volumeIndex(0.5f, 15))
        assertEquals(0, PlayerTouchLogic.volumeIndex(-1f, 15))
    }

    @Test fun pipAspectIsClampedToTheAndroidLimits() {
        assertEquals(1920 to 1080, PlayerTouchLogic.pipAspect(1920, 1080))
        assertEquals(239 to 100, PlayerTouchLogic.pipAspect(3000, 1000))
        assertEquals(100 to 239, PlayerTouchLogic.pipAspect(400, 1000))
        assertEquals(16 to 9, PlayerTouchLogic.pipAspect(0, 0))
    }

    @Test fun onlyPhonesAreLockedToLandscape() {
        assertTrue(PlayerTouchLogic.lockLandscape(isPhone = true, portrait = false))
        assertFalse(PlayerTouchLogic.lockLandscape(isPhone = true, portrait = true))
        assertFalse(PlayerTouchLogic.lockLandscape(isPhone = false, portrait = false))
    }
}

class TouchHintsTest {
    @Test fun remoteHintsBecomeTouchHintsAndOtherTextsStay() {
        assertEquals("Zurück schließt das Menü", TouchHints.of("Links schließt das Menü"))
        assertEquals("Zurück mit der Zurück-Geste.", TouchHints.of("Zurück mit der Zurück-Taste."))
        assertEquals("Abspielen", TouchHints.of("Abspielen"))
    }
}
