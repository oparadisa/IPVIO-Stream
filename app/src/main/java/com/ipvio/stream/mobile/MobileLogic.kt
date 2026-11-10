package com.ipvio.stream.mobile

import com.ipvio.stream.MenuLogic

/**
 * Pure rules for the phone/tablet UI (no Android), covered by MobileLogicTest.
 * Window size classes follow the Material thresholds (compact < 600 dp, medium < 840 dp, expanded).
 */
object MobileLogic {
    enum class SizeClass { COMPACT, MEDIUM, EXPANDED }

    /** Pseudo menu id for the "More" entry of the bottom bar (not a real area). */
    const val MORE = -1

    /** Entries shown directly in the bottom bar; the rest goes into "More". */
    const val BAR_SLOTS = 4

    fun sizeClass(widthDp: Int): SizeClass = when {
        widthDp < 600 -> SizeClass.COMPACT
        widthDp < 840 -> SizeClass.MEDIUM
        else -> SizeClass.EXPANDED
    }

    /** Phone (not tablet) by the smallest side, so a phone stays a phone in landscape. */
    fun isPhone(smallestWidthDp: Int): Boolean = smallestWidthDp < 600

    /**
     * Bottom bar (compact width): the first [BAR_SLOTS] visible areas in the user's menu order, then "More".
     * Settings and Search never take a bar slot (Search sits in the header, Settings goes into "More").
     * No "More" when nothing is left for it.
     */
    fun barItems(visible: List<Int>): List<Int> {
        val direct = visible.filter { it != MenuLogic.SETTINGS && it != MenuLogic.SEARCH }.take(BAR_SLOTS)
        return if (moreItems(visible).isEmpty()) direct else direct + MORE
    }

    /** Areas behind "More": everything visible that has no bar slot, Settings always last. */
    fun moreItems(visible: List<Int>): List<Int> {
        val direct = visible.filter { it != MenuLogic.SETTINGS && it != MenuLogic.SEARCH }.take(BAR_SLOTS)
        val rest = visible.filter { it !in direct && it != MenuLogic.SETTINGS }
        return if (MenuLogic.SETTINGS in visible) rest + MenuLogic.SETTINGS else rest
    }

    /** "More" is highlighted when the current area lives behind it. */
    fun barSelected(visible: List<Int>, tab: Int): Int =
        if (tab in barItems(visible)) tab else if (tab in moreItems(visible)) MORE else -2

    /** Navigation rail (medium/expanded width): all visible areas in menu order, Settings pinned to the end. */
    fun railItems(visible: List<Int>): List<Int> =
        visible.filter { it != MenuLogic.SETTINGS } + listOfNotNull(MenuLogic.SETTINGS.takeIf { it in visible })

    enum class Back { GO_HOME, SCROLL_TOP, EXIT }

    /** System back on the main screen of a phone/tablet: other area → Home, Home scrolled → top, Home at the top → leave. */
    fun back(tab: Int, scrolled: Boolean): Back = when {
        tab != MenuLogic.HOME -> Back.GO_HOME
        scrolled -> Back.SCROLL_TOP
        else -> Back.EXIT
    }
}
