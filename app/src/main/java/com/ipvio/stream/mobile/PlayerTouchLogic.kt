package com.ipvio.stream.mobile

/** Pure math for the touch seek bar on phones and tablets (fork). */
object PlayerTouchLogic {
    /** Position as 0..1 of the duration (0 while the duration is unknown). */
    fun fraction(pos: Long, dur: Long): Float = if (dur <= 0) 0f else (pos.toFloat() / dur).coerceIn(0f, 1f)

    /** Seek target for a touch at [x] on a bar [width] px wide. */
    fun target(x: Float, width: Float, dur: Long): Long =
        if (dur <= 0 || width <= 0f) 0L else ((x / width).coerceIn(0f, 1f) * dur).toLong()

    /** Phones play in landscape unless the user turned the screen to portrait; tablets follow the sensor. */
    fun lockLandscape(isPhone: Boolean, portrait: Boolean): Boolean = isPhone && !portrait
}
