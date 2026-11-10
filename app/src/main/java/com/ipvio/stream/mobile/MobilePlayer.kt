package com.ipvio.stream.mobile

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.ipvio.stream.AIcon
import com.ipvio.stream.Accent1
import com.ipvio.stream.AppIcon
import com.ipvio.stream.Lang
import com.ipvio.stream.PIcon
import com.ipvio.stream.PlayerIcon
import com.ipvio.stream.Text
import com.ipvio.stream.fmtTime

/**
 * Touch controls of the player on phones and tablets (fork). Replaces the TV control bar; all actions are the
 * player's own (passed in), so playback logic stays in [com.ipvio.stream.PlayerScreen]. Also makes the player full
 * screen: system bars hidden (swipe to show), phones in landscape unless the user switches to portrait.
 * [prev]/[next] = previous/next episode, or channel for live; null hides the button.
 */
@Composable
fun MobilePlayerBar(
    player: androidx.media3.common.Player, visible: Boolean, live: Boolean, title: String, subtitle: String?, playing: Boolean,
    pos: () -> Long, dur: () -> Long, buf: () -> Long, stepSec: Int,
    onInteract: () -> Unit, onBack: () -> Unit, onToggle: () -> Unit, onStep: (Int) -> Unit, onSeekTo: (Long) -> Unit,
    prev: (() -> Unit)?, next: (() -> Unit)?, onTracks: () -> Unit, onSettings: () -> Unit,
    onEpisodes: (() -> Unit)?, onChannels: (() -> Unit)?, onGuide: (() -> Unit)?, onPip: () -> Unit
) = NativeDensity {
    var portrait by rememberSaveable { mutableStateOf(false) }
    var locked by rememberSaveable { mutableStateOf(false) }
    val isPhone = MobileLogic.isPhone(LocalConfiguration.current.smallestScreenWidthDp)
    FullscreenPlayer(PlayerTouchLogic.orientation(isPhone, portrait, locked))
    MobilePip.Effect(player, playing, onPip)
    if (!MobilePip.inPip) SwipeIndicator()

    AnimatedVisibility(visible && !MobilePip.inPip, Modifier.fillMaxSize(), enter = fadeIn(tween(160)), exit = fadeOut(tween(120))) {
        val safe = WindowInsets.safeDrawing.asPaddingValues()
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().height(120.dp).align(Alignment.TopCenter).background(Brush.verticalGradient(listOf(Color(0xB3000000), Color.Transparent))))
            Box(Modifier.fillMaxWidth().height(180.dp).align(Alignment.BottomCenter).background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))))
            Box(Modifier.fillMaxSize().padding(safe).padding(horizontal = 8.dp, vertical = 4.dp)) {
                // Top: back, title, audio/subtitles, settings, orientation (phones)
                Row(Modifier.fillMaxWidth().align(Alignment.TopCenter), verticalAlignment = Alignment.CenterVertically) {
                    MIconButton(AIcon.Left, "Zurück", tint = Color.White) { onInteract(); onBack() }
                    Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                        Text(title, raw = true, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        subtitle?.let { Text(it, raw = true, color = Color(0xCCFFFFFF), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    }
                    PBtn(PIcon.AudioSubs, "Audio & Untertitel") { onInteract(); onTracks() }
                    PBtn(PIcon.Settings, "Einstellungen") { onInteract(); onSettings() }
                    if (isPhone && !locked) MIconButton(AIcon.Phone, if (portrait) "Querformat" else "Hochformat", tint = Color.White) { onInteract(); portrait = !portrait }
                    MIconButton(AIcon.Lock, if (locked) "Drehung entsperren" else "Drehung sperren", tint = if (locked) Accent1 else Color.White) { onInteract(); locked = !locked }
                }

                // Middle: previous, back/forward, play/pause, next
                Row(Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    prev?.let { p -> PBtn(PIcon.PrevEp, if (live) "Vorheriger Sender" else "Vorherige Folge") { onInteract(); p() } }
                    if (!live) StepBtn(PIcon.Replay, stepSec, "$stepSec Sekunden zurück") { onInteract(); onStep(-1) }
                    Box(
                        Modifier.size(72.dp).clip(CircleShape).background(Color(0x66000000))
                            .clickable(role = Role.Button) { onInteract(); onToggle() }
                            .semantics { contentDescription = Lang.tr(if (playing) "Pause" else "Abspielen") },
                        contentAlignment = Alignment.Center
                    ) { PlayerIcon(if (playing) PIcon.Pause else PIcon.Play, Color.White, size = 40.dp) }
                    if (!live) StepBtn(PIcon.Forward, stepSec, "$stepSec Sekunden vor") { onInteract(); onStep(1) }
                    next?.let { n -> PBtn(PIcon.NextEp, if (live) "Nächster Sender" else "Nächste Folge") { onInteract(); n() } }
                }

                // Bottom: seek bar (on demand) and the list buttons
                Column(Modifier.fillMaxWidth().align(Alignment.BottomCenter)) {
                    if (!live && dur() > 0) SeekBar(pos, dur, buf, onInteract, onSeekTo)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        onEpisodes?.let { e -> PBtn(PIcon.Episodes, "Folgen") { onInteract(); e() } }
                        onChannels?.let { c -> PBtn(PIcon.Channels, "Senderliste") { onInteract(); c() } }
                        onGuide?.let { g -> PBtn(PIcon.Guide, "TV-Guide") { onInteract(); g() } }
                    }
                }
            }
        }
    }
}

/** Seek bar with times: tap or drag; the video jumps when the finger is lifted. */
@Composable
private fun SeekBar(pos: () -> Long, dur: () -> Long, buf: () -> Long, onInteract: () -> Unit, onSeekTo: (Long) -> Unit) {
    var drag by remember { mutableStateOf<Long?>(null) }
    val d = dur()
    val shown = drag ?: pos()
    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Box(
            Modifier.fillMaxWidth().height(32.dp)
                .pointerInput(d) {
                    detectTapGestures { o -> onInteract(); onSeekTo(PlayerTouchLogic.target(o.x, size.width.toFloat(), d)) }
                }
                .pointerInput(d) {
                    detectHorizontalDragGestures(
                        onDragStart = { o -> onInteract(); drag = PlayerTouchLogic.target(o.x, size.width.toFloat(), d) },
                        onDragEnd = { drag?.let(onSeekTo); drag = null },
                        onDragCancel = { drag = null }
                    ) { change, _ -> onInteract(); drag = PlayerTouchLogic.target(change.position.x, size.width.toFloat(), d) }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            val f = PlayerTouchLogic.fraction(shown, d)
            Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0x40FFFFFF)))
            Box(Modifier.fillMaxWidth(PlayerTouchLogic.fraction(buf(), d)).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0x66FFFFFF)))
            Box(Modifier.fillMaxWidth(f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Accent1))
            Thumb(f, if (drag != null) 20.dp else 14.dp)
        }
        Row(Modifier.fillMaxWidth()) {
            Text(fmtTime(shown), raw = true, color = Color.White, fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            Text(fmtTime(d), raw = true, color = Color(0xCCFFFFFF), fontSize = 13.sp)
        }
    }
}

@Composable
private fun Thumb(f: Float, size: Dp) {
    // Placed with a weight row so it follows the fraction without measuring the bar
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (f > 0f) Spacer(Modifier.weight(f))
        Box(Modifier.offset(x = -size / 2).size(size).clip(CircleShape).background(Accent1))
        if (f < 1f) Spacer(Modifier.weight(1f - f))
    }
}

@Composable
private fun PBtn(icon: PIcon, desc: String, onClick: () -> Unit) {
    val label = Lang.tr(desc)
    Box(
        Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) { PlayerIcon(icon, Color.White, size = 26.dp) }
}

@Composable
private fun StepBtn(icon: PIcon, sec: Int, desc: String, onClick: () -> Unit) {
    val label = Lang.tr(desc)
    Box(
        Modifier.size(56.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        PlayerIcon(icon, Color.White, size = 32.dp)
        Text("$sec", raw = true, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.offset(y = 2.dp))
    }
}

/** Full screen while the player is open: no frame padding, system bars hidden, [orientation] requested. */
@Composable
private fun FullscreenPlayer(orientation: Int) {
    val view = LocalView.current
    val act = LocalContext.current.activity()
    DisposableEffect(Unit) {
        MobileNav.fullscreen = true
        val win = act?.window
        val ctl = win?.let { WindowCompat.getInsetsController(it, view) }
        ctl?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        ctl?.hide(WindowInsetsCompat.Type.systemBars())
        val before = act?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        onDispose {
            MobileNav.fullscreen = false
            ctl?.show(WindowInsetsCompat.Type.systemBars())
            act?.requestedOrientation = before
        }
    }
    LaunchedEffect(orientation) { act?.requestedOrientation = orientation }
}

internal tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

/** Moves the player's status message below the center buttons while the touch controls show (used by PlayerScreen). */
@Composable
fun belowCenterControls(controls: Boolean): Modifier {
    val nd = LocalNativeDensity.current
    if (nd == null || !MobileUi.active || !controls) return Modifier
    val y = with(nd) { 84.dp.roundToPx() }
    return Modifier.offset { androidx.compose.ui.unit.IntOffset(0, y) }
}
