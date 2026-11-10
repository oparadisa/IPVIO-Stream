package com.ipvio.stream.mobile

import android.content.pm.ActivityInfo

/** Pure math for the touch seek bar on phones and tablets (fork). */
object PlayerTouchLogic {
    /** Position as 0..1 of the duration (0 while the duration is unknown). */
    fun fraction(pos: Long, dur: Long): Float = if (dur <= 0) 0f else (pos.toFloat() / dur).coerceIn(0f, 1f)

    /** Seek target for a touch at [x] on a bar [width] px wide. */
    fun target(x: Float, width: Float, dur: Long): Long =
        if (dur <= 0 || width <= 0f) 0L else ((x / width).coerceIn(0f, 1f) * dur).toLong()

    /** Phones play in landscape unless the user turned the screen to portrait; tablets follow the sensor. */
    fun lockLandscape(isPhone: Boolean, portrait: Boolean): Boolean = isPhone && !portrait

    /**
     * Requested orientation: rotation lock keeps the current one; otherwise phones turn within landscape/portrait, tablets follow the system.
     * While the TV guide is open ([guide]) a phone follows how it is held (portrait guide when upright).
     */
    fun orientation(isPhone: Boolean, portrait: Boolean, locked: Boolean, guide: Boolean = false): Int = when {
        locked -> ActivityInfo.SCREEN_ORIENTATION_LOCKED
        !isPhone -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        guide -> ActivityInfo.SCREEN_ORIENTATION_USER
        lockLandscape(isPhone, portrait) -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        else -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
    }

    /** Level 0..1 after a vertical swipe: [dragged] px since the start (negative = up); 3/4 of the [height] is the full range. */
    fun swipeLevel(start: Float, dragged: Float, height: Float): Float =
        if (height <= 0f) start.coerceIn(0f, 1f) else (start - dragged / (height * 0.75f)).coerceIn(0f, 1f)

    /** Volume step for a level, at least 0 and at most [max]. */
    fun volumeIndex(level: Float, max: Int): Int = Math.round(level.coerceIn(0f, 1f) * max).coerceIn(0, max.coerceAtLeast(0))

    /** PiP aspect ratio of a video, clamped to what Android allows (1:2.39 ... 2.39:1); 16:9 while the size is unknown. */
    fun pipAspect(w: Int, h: Int): Pair<Int, Int> {
        if (w <= 0 || h <= 0) return 16 to 9
        val r = w.toDouble() / h
        return when { r > 2.39 -> 239 to 100; r < 1 / 2.39 -> 100 to 239; else -> w to h }
    }
}
