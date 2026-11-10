package com.ipvio.stream.mobile

import com.ipvio.stream.MenuLogic
import com.ipvio.stream.mobile.MobileLogic.Back
import com.ipvio.stream.mobile.MobileLogic.MORE
import com.ipvio.stream.mobile.MobileLogic.SizeClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileLogicTest {
    private val defaultVisible = MenuLogic.visible(MenuLogic.DEFAULT_ORDER, MenuLogic.parseHidden(MenuLogic.DEFAULT_HIDDEN))

    @Test fun sizeClassThresholds() {
        assertEquals(SizeClass.COMPACT, MobileLogic.sizeClass(411))
        assertEquals(SizeClass.COMPACT, MobileLogic.sizeClass(599))
        assertEquals(SizeClass.MEDIUM, MobileLogic.sizeClass(600))
        assertEquals(SizeClass.MEDIUM, MobileLogic.sizeClass(839))
        assertEquals(SizeClass.EXPANDED, MobileLogic.sizeClass(840))
        assertEquals(SizeClass.EXPANDED, MobileLogic.sizeClass(1280))
    }

    @Test fun phoneByShortestSide() {
        assertTrue(MobileLogic.isPhone(411))    // Pixel 9, either orientation
        assertFalse(MobileLogic.isPhone(600))
        assertFalse(MobileLogic.isPhone(800))   // Pixel Tablet
    }

    @Test fun defaultMenuGivesFourAreasPlusMore() {
        // Default order: Home, Live TV, Movies, Series, Sport, EPG*, Favorites, My list, History, Search*, Settings (* hidden)
        assertEquals(listOf(MenuLogic.HOME, 3, 1, 2, MORE), MobileLogic.barItems(defaultVisible))
        assertEquals(listOf(MenuLogic.SPORT, MenuLogic.FAVS, MenuLogic.MYLIST, MenuLogic.VERLAUF, MenuLogic.SETTINGS), MobileLogic.moreItems(defaultVisible))
    }

    @Test fun barFollowsUserOrderAndSkipsSearchAndSettings() {
        val visible = listOf(MenuLogic.SEARCH, MenuLogic.HOME, MenuLogic.SETTINGS, 2, MenuLogic.FAVS, 3, 1)
        assertEquals(listOf(MenuLogic.HOME, 2, MenuLogic.FAVS, 3, MORE), MobileLogic.barItems(visible))
        assertEquals(listOf(MenuLogic.SEARCH, 1, MenuLogic.SETTINGS), MobileLogic.moreItems(visible))
    }

    @Test fun moreAlwaysHoldsSettings() {
        val visible = listOf(MenuLogic.HOME, 3, MenuLogic.SETTINGS)
        assertEquals(listOf(MenuLogic.HOME, 3, MORE), MobileLogic.barItems(visible))
        assertEquals(listOf(MenuLogic.SETTINGS), MobileLogic.moreItems(visible))
    }

    @Test fun everyVisibleAreaIsReachableExactlyOnce() {
        val all = MenuLogic.parseOrder(null)
        for (hidden in listOf(emptyList(), MenuLogic.parseHidden(MenuLogic.DEFAULT_HIDDEN), listOf(1, 2, 3, 6, 7, 8, 9, 10))) {
            val visible = MenuLogic.visible(all, hidden)
            val reachable = MobileLogic.barItems(visible).filter { it != MORE } + MobileLogic.moreItems(visible)
            assertEquals(visible.sorted(), reachable.sorted())
            assertEquals(visible.sorted(), MobileLogic.railItems(visible).sorted())
        }
    }

    @Test fun selectedEntry() {
        assertEquals(3, MobileLogic.barSelected(defaultVisible, 3))
        assertEquals(MORE, MobileLogic.barSelected(defaultVisible, MenuLogic.SETTINGS))
        assertEquals(MORE, MobileLogic.barSelected(defaultVisible, MenuLogic.SPORT))
    }

    @Test fun railPinsSettingsLast() {
        assertEquals(MenuLogic.SETTINGS, MobileLogic.railItems(defaultVisible).last())
        assertEquals(MenuLogic.HOME, MobileLogic.railItems(defaultVisible).first())
    }

    @Test fun backOnMainScreen() {
        assertEquals(Back.GO_HOME, MobileLogic.back(3, scrolled = true))
        assertEquals(Back.GO_HOME, MobileLogic.back(MenuLogic.SETTINGS, scrolled = false))
        assertEquals(Back.SCROLL_TOP, MobileLogic.back(MenuLogic.HOME, scrolled = true))
        assertEquals(Back.EXIT, MobileLogic.back(MenuLogic.HOME, scrolled = false))
    }

    @Test fun twoPaneFromExpandedWidth() {
        assertEquals(false, MobileLogic.twoPane(411))     // phone
        assertEquals(false, MobileLogic.twoPane(800))     // tablet in portrait
        assertEquals(true, MobileLogic.twoPane(840))
        assertEquals(true, MobileLogic.twoPane(1280))     // tablet in landscape
    }

    @Test fun listPaneWidth() {
        assertEquals(280, MobileLogic.listPaneDp(840))
        assertEquals(300, MobileLogic.listPaneDp(900))
        assertEquals(400, MobileLogic.listPaneDp(1280))
    }

    @Test fun guideLayoutBySize() {
        val portrait = MobileLogic.guideDims(412, 915)
        assertEquals(true, portrait.videoTop)
        assertEquals(412, portrait.videoW)
        assertEquals(231, portrait.videoH)
        assertEquals(0, portrait.catW)                                   // categories as chips
        assertEquals(false, MobileLogic.guideDims(915, 412).videoTop)   // phone landscape: small picture top right
        assertEquals(126, MobileLogic.guideDims(915, 412).videoH)
        assertEquals(MobileLogic.GuideDims.TV, MobileLogic.guideDims(1280, 800))   // tablet
        assertEquals(MobileLogic.GuideDims.TV, MobileLogic.guideDims(800, 1280))   // tablet portrait
    }

    @Test fun designWidthOnlyForPhoneSport() {
        assertEquals(1658f, MobileLogic.designWidth(1658f, false))
        assertEquals(MobileLogic.PHONE_DESIGN_W, MobileLogic.designWidth(1658f, true))
    }
}
