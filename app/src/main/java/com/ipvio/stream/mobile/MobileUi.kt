package com.ipvio.stream.mobile

import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipvio.stream.Accent1
import com.ipvio.stream.AppSettings
import com.ipvio.stream.Bg
import com.ipvio.stream.Look
import com.ipvio.stream.MENU_LABELS
import com.ipvio.stream.MenuIcon
import com.ipvio.stream.Text
import com.ipvio.stream.Tok
import com.ipvio.stream.mobile.MobileLogic.SizeClass

/**
 * Phone/tablet state. [active] is the device check (touch, no TV features) and never true on a TV.
 * Phone vs tablet is not stored: it is derived from [LocalConfiguration] wherever it is needed, so it follows
 * rotation, split screen and window resizing live (MainActivity handles those config changes itself).
 */
object MobileUi {
    var active by mutableStateOf(false)
}

/** Navigation of the main screen, published by AppContent (which owns the tab state) and drawn by [MobileFrame]. */
object MobileNav {
    /** Bar/rail visible: only on the main areas (not in the player, detail pages, setup). */
    var shown by mutableStateOf(false)
    var tab by mutableIntStateOf(0)
    var more by mutableStateOf(false)
    var onTab: (Int) -> Unit = {}

    fun publish(shown: Boolean, tab: Int, onTab: (Int) -> Unit) {
        this.shown = shown; this.tab = tab; this.onTab = onTab
        if (!shown) more = false
    }

    fun pick(id: Int) {
        more = false
        if (id == MobileLogic.MORE) more = true else onTab(id)
    }
}

/** Bar/rail entry height: icon pill + one 12 sp label line, so it grows with the system font size (64 dp at 100 %). */
private fun Density.itemH(): Dp = 48.dp + 16.sp.toDp()
private val RailW = 80.dp
private val BarBg: Color get() = lerp(Look.bg, Color.Black, 0.25f)
private val Ink = Color(0xFFCFCFD6)
private val Hairline = Color(0x1AFFFFFF)
private val SheetBg: Color get() = lerp(Look.bg, Color.White, 0.07f)

/** All visible areas in the user's menu order (Settings > Menu). */
private fun visibleAreas(): List<Int> = AppSettings.menuOrder.filter { it !in AppSettings.menuHidden }

/**
 * Frame around the whole app on phones and tablets: keeps the content clear of status bar, navigation bar,
 * cutout and keyboard, and draws the bottom bar (compact width) or the navigation rail (wider).
 * On the TV it only calls [content].
 *
 * The content always stays in the same slot (only its padding changes), so showing or hiding the bar never
 * recreates the app's screens and their state. [native] = the device density without the TV scaling:
 * the bar, rail and "More" sheet use it, so they have phone sizes and follow the system font size.
 */
@Composable
fun MobileFrame(native: Density, content: @Composable () -> Unit) {
    if (!MobileUi.active) { content(); return }
    val cfg = LocalConfiguration.current
    val rail = MobileLogic.sizeClass(cfg.screenWidthDp) != SizeClass.COMPACT
    val density = LocalDensity.current
    val dir = LocalLayoutDirection.current
    val safe = WindowInsets.safeDrawing.asPaddingValues()
    val imeOpen = WindowInsets.ime.getBottom(density) > 0
    val nav = MobileNav.shown
    val showBar = nav && !rail && !imeOpen
    // Bar and rail are measured in native dp; the content is laid out with the (TV-scaled) app density
    fun native(d: Dp): Dp = with(density) { with(native) { d.toPx() }.toDp() }
    val pad = PaddingValues(
        start = safe.calculateStartPadding(dir) + if (nav && rail) native(RailW) else 0.dp,
        top = safe.calculateTopPadding(),
        end = safe.calculateEndPadding(dir),
        bottom = safe.calculateBottomPadding() + if (showBar) native(native.itemH()) else 0.dp
    )
    Box(Modifier.fillMaxSize().background(Bg)) {
        Box(Modifier.fillMaxSize().padding(pad)) { content() }
        CompositionLocalProvider(LocalDensity provides native) {
            if (showBar) BottomBar(Modifier.align(Alignment.BottomCenter))
            if (nav && rail) Rail(Modifier.align(Alignment.CenterStart))
            if (MobileNav.more && nav) MoreSheet()
        }
    }
}

@Composable
private fun BottomBar(modifier: Modifier) {
    val visible = visibleAreas()
    val selected = MobileLogic.barSelected(visible, MobileNav.tab)
    Row(
        modifier.fillMaxWidth().background(BarBg)
            .drawBehind { drawLine(Hairline, Offset(0f, 0f), Offset(size.width, 0f), 1f) }
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
            .height(LocalDensity.current.itemH())
    ) {
        MobileLogic.barItems(visible).forEach { id ->
            NavItem(id, id == selected, Modifier.weight(1f).fillMaxHeight())
        }
    }
}

@Composable
private fun Rail(modifier: Modifier) {
    val visible = visibleAreas()
    val items = MobileLogic.railItems(visible)
    Column(
        modifier.fillMaxHeight().background(BarBg)
            .drawBehind { drawLine(Hairline, Offset(size.width - 1f, 0f), Offset(size.width - 1f, size.height), 1f) }
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical))
            .width(RailW).verticalScroll(rememberScrollState()).padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEach { id ->
            if (id == com.ipvio.stream.MenuLogic.SETTINGS) Spacer(Modifier.height(12.dp))
            NavItem(id, id == MobileNav.tab, Modifier.fillMaxWidth().height(LocalDensity.current.itemH()))
        }
    }
}

/** One entry of bar or rail: icon in a pill (highlighted when selected) above a short label. Tap target ≥ 48 dp. */
@Composable
private fun NavItem(id: Int, selected: Boolean, modifier: Modifier) {
    val ink = if (selected) Accent1 else Ink
    Column(
        modifier.selectable(selected, interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Tab) { MobileNav.pick(id) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(width = 56.dp, height = 30.dp).clip(RoundedCornerShape(15.dp))
                .background(if (selected) Accent1.copy(alpha = .18f) else Color.Transparent),
            contentAlignment = Alignment.Center
        ) { MenuIcon(id, ink, Modifier.size(22.dp)) }
        Text(
            label(id), color = if (selected) Tok.TextHi else Ink, fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp, start = 2.dp, end = 2.dp)
        )
    }
}

private fun label(id: Int): String = if (id == MobileLogic.MORE) "Mehr" else MENU_LABELS.getOrElse(id) { "" }

/** "More": the areas without a bar slot as a list rising from the bottom. Back or a tap outside closes it. */
@Composable
private fun BoxScope.MoreSheet() {
    BackHandler { MobileNav.more = false }
    val visible = visibleAreas()
    Box(
        Modifier.fillMaxSize().background(Color(0x99000000))
            .clickable(remember { MutableInteractionSource() }, indication = null) { MobileNav.more = false }
    )
    Column(
        Modifier.align(Alignment.BottomCenter).widthIn(max = 560.dp).fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)).background(SheetBg)
            .clickable(remember { MutableInteractionSource() }, indication = null) { }   // taps on the sheet itself stay here
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
            .padding(vertical = 10.dp)
    ) {
        Box(Modifier.align(Alignment.CenterHorizontally).padding(bottom = 6.dp).size(width = 36.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(Hairline.copy(alpha = .35f)))
        MobileLogic.moreItems(visible).forEach { id ->
            val sel = id == MobileNav.tab
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp)
                    .selectable(sel, role = Role.Tab) { MobileNav.pick(id) }
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                MenuIcon(id, if (sel) Accent1 else Ink, Modifier.size(24.dp))
                Text(label(id), color = if (sel) Tok.TextHi else Ink, fontSize = 16.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium)
            }
        }
    }
}

/**
 * Screens still in their TV layout (sport, boomer mode, person page) are too wide for a phone held upright:
 * while such a screen is open, a phone turns to landscape. Tablets and TVs are not touched.
 */
@Composable
fun LandscapeOnPhone() {
    if (!MobileUi.active || !MobileLogic.isPhone(LocalConfiguration.current.smallestScreenWidthDp)) return
    val act = LocalActivity.current ?: return
    DisposableEffect(act) {
        act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose { act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }
}
