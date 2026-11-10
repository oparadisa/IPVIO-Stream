package com.ipvio.stream.mobile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ipvio.stream.Lang
import com.ipvio.stream.PIcon
import com.ipvio.stream.PlayerIcon

/**
 * Player panels and menus on phones and tablets (fork): a close (X) button in the header and a tap outside the panel
 * closes it; system Back keeps its TV behaviour (one level back). Nothing changes on the TV.
 */
object MobilePanels {
    /** Closes the open player panel completely (set while a [TapOutside] layer is shown). */
    internal var closeAll: (() -> Unit)? = null
}

/** Full-screen layer behind a panel: a tap on it closes the panel. Put it before the panel in the same Box. */
@Composable
fun BoxScope.TapOutside(onClose: () -> Unit) {
    if (!MobileUi.active) return
    val close by rememberUpdatedState(onClose)
    DisposableEffect(Unit) {
        MobilePanels.closeAll = { close() }
        onDispose { MobilePanels.closeAll = null }
    }
    Box(Modifier.matchParentSize().pointerInput(Unit) { detectTapGestures { close() } })
}

/** Keeps taps on a panel's empty parts from reaching the [TapOutside] layer below. */
fun Modifier.panelTaps(): Modifier =
    if (!MobileUi.active) this else pointerInput(Unit) { detectTapGestures { } }

/** Header row with a close (X) at the end on phones/tablets; on the TV just [content]. [onClose] null = no button. */
@Composable
fun WithClose(onClose: (() -> Unit)?, content: @Composable () -> Unit) {
    if (!MobileUi.active || onClose == null) { content(); return }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) { content() }
        CloseX(MobilePanels.closeAll ?: onClose)
    }
}

/** Close (X) for a full-screen player view (the TV guide), top right inside the safe area. */
@Composable
fun BoxScope.CloseCorner(onClose: () -> Unit) {
    if (!MobileUi.active) return
    Box(Modifier.align(Alignment.TopEnd).windowInsetsPadding(WindowInsets.safeDrawing).padding(8.dp)) {
        CloseX(onClose, background = Color(0x99000000))
    }
}

@Composable
private fun CloseX(onClose: () -> Unit, size: Dp = 44.dp, background: Color = Color(0x1FFFFFFF)) {
    val label = Lang.tr("Schließen")
    Box(
        Modifier.size(size).clip(CircleShape).background(background)
            .clickable(role = Role.Button, onClick = onClose).semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) { PlayerIcon(PIcon.Close, Color.White, size = size * 0.5f) }
}
