package com.ipvio.stream.mobile

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.util.Rational
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.ipvio.stream.Accent1
import com.ipvio.stream.Lang
import com.ipvio.stream.Text
import kotlinx.coroutines.delay

/** Which level a vertical swipe on the video is changing right now (shown by [SwipeIndicator]). */
private object Swipe {
    var kind by mutableStateOf<Kind?>(null)
    var level by mutableFloatStateOf(0f)
    var tick by mutableStateOf(0)
    enum class Kind { BRIGHTNESS, VOLUME }
}

/**
 * Swipe up/down on the video: left half = screen brightness (this window only, reset when the player closes),
 * right half = media volume. Empty Modifier on the TV. Taps and double-taps on the same surface keep working.
 */
@Composable
fun swipeLevels(): Modifier {
    if (!MobileUi.active) return Modifier
    val act = LocalContext.current.activity() ?: return Modifier
    val audio = remember { act.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    DisposableEffect(act) {
        onDispose {
            act.window.attributes = act.window.attributes.apply { screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE }
        }
    }
    return Modifier.pointerInput(Unit) {
        var start = 0f
        var dragged = 0f
        detectVerticalDragGestures(
            onDragStart = { o ->
                dragged = 0f
                val left = o.x < size.width / 2f
                Swipe.kind = if (left) Swipe.Kind.BRIGHTNESS else Swipe.Kind.VOLUME
                start = if (left) brightnessOf(act) else audio?.let {
                    it.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / it.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                } ?: 0f
                Swipe.level = start
            },
            onDragEnd = { Swipe.tick++ },
            onDragCancel = { Swipe.tick++ }
        ) { change, dy ->
            change.consume()
            dragged += dy
            val l = PlayerTouchLogic.swipeLevel(start, dragged, size.height.toFloat())
            Swipe.level = l
            if (Swipe.kind == Swipe.Kind.BRIGHTNESS) {
                act.window.attributes = act.window.attributes.apply { screenBrightness = l.coerceAtLeast(0.01f) }
            } else audio?.let {
                val max = it.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                it.setStreamVolume(AudioManager.STREAM_MUSIC, PlayerTouchLogic.volumeIndex(l, max), 0)
            }
        }
    }
}

private fun brightnessOf(act: Activity): Float {
    val w = act.window.attributes.screenBrightness
    if (w >= 0f) return w
    // shortcut: the system value is 0..255 on most phones; curves of adaptive brightness are ignored
    val sys = runCatching { Settings.System.getInt(act.contentResolver, Settings.System.SCREEN_BRIGHTNESS) }.getOrDefault(128)
    return (sys / 255f).coerceIn(0f, 1f)
}

/** Level bar while swiping; fades a moment after the finger is lifted. */
@Composable
internal fun SwipeIndicator() {
    val kind = Swipe.kind ?: return
    LaunchedEffect(Swipe.tick) { if (Swipe.tick > 0) { delay(700); Swipe.kind = null } }
    // Top center, clear of the center buttons and the seek bar
    Box(Modifier.fillMaxSize().padding(top = 64.dp), contentAlignment = Alignment.TopCenter) {
        Row(
            Modifier.clip(RoundedCornerShape(24.dp)).background(Color(0xB3000000)).padding(horizontal = 16.dp, vertical = 10.dp)
                .semantics { contentDescription = Lang.tr(if (kind == Swipe.Kind.BRIGHTNESS) "Helligkeit" else "Lautstärke") },
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LevelIcon(kind)
            Box(Modifier.width(140.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0x40FFFFFF))) {
                Box(Modifier.fillMaxWidth(Swipe.level).height(4.dp).background(Accent1))
            }
            Text("${(Swipe.level * 100).toInt()} %", raw = true, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LevelIcon(kind: Swipe.Kind) = Canvas(Modifier.size(28.dp)) {
    val u = size.minDimension / 24f
    val s = Stroke(2f * u)
    if (kind == Swipe.Kind.BRIGHTNESS) {
        drawCircle(Color.White, 4.5f * u, center, style = s)
        for (i in 0 until 8) {
            val a = Math.toRadians(i * 45.0)
            val c = Math.cos(a).toFloat(); val n = Math.sin(a).toFloat()
            drawLine(Color.White, center + Offset(c * 8f * u, n * 8f * u), center + Offset(c * 10.5f * u, n * 10.5f * u), 2f * u)
        }
    } else {
        val p = Path().apply { moveTo(3f * u, 9f * u); lineTo(7f * u, 9f * u); lineTo(12f * u, 4.5f * u); lineTo(12f * u, 19.5f * u); lineTo(7f * u, 15f * u); lineTo(3f * u, 15f * u); close() }
        drawPath(p, Color.White)
        drawArc(Color.White, -50f, 100f, false, Offset(10f * u, 7f * u), androidx.compose.ui.geometry.Size(8f * u, 10f * u), style = s)
        drawArc(Color.White, -55f, 110f, false, Offset(10f * u, 3.5f * u), androidx.compose.ui.geometry.Size(12f * u, 17f * u), style = s)
    }
}

/**
 * Picture-in-picture for phones and tablets: while a video plays in the player, Home (or switching apps) shrinks it into
 * a PiP window instead of pausing. Android 12+ enters by itself (auto-enter); 8–11 via [onUserLeaveHint].
 */
object MobilePip {
    /** The player window is in PiP mode now. */
    var inPip by mutableStateOf(false)
        private set
    private var armed = false
    private var aspect = Rational(16, 9)

    private fun supported(act: Activity) =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && act.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

    private fun params(auto: Boolean): PictureInPictureParams? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
        val b = PictureInPictureParams.Builder().setAspectRatio(aspect)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) b.setAutoEnterEnabled(auto).setSeamlessResizeEnabled(true)
        return b.build()
    }

    private fun update(act: Activity) {
        if (!supported(act)) return
        params(armed)?.let { runCatching { act.setPictureInPictureParams(it) } }
    }

    /** Called from MainActivity.onUserLeaveHint; true = the player goes into PiP (so the app should not count it as "left"). */
    fun onUserLeaveHint(act: Activity): Boolean {
        if (!armed || !MobileUi.active || !supported(act)) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) params(false)?.let { runCatching { act.enterPictureInPictureMode(it) } }
        return true
    }

    /** The app is going to the background but keeps playing in PiP (the player must not pause). */
    fun keepsPlaying(ctx: Context): Boolean {
        val act = ctx.activity() ?: return false
        return MobileUi.active && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && act.isInPictureInPictureMode
    }

    /** Arms PiP while [playing]; keeps the window's aspect ratio in step with the video. [onEnter] tidies the screen. */
    @Composable
    internal fun Effect(player: Player, playing: Boolean, onEnter: () -> Unit) {
        val act = LocalContext.current.activity() as? ComponentActivity ?: return
        LaunchedEffect(playing) { armed = playing; update(act) }
        DisposableEffect(player, act) {
            fun size(v: VideoSize) {
                val (w, h) = PlayerTouchLogic.pipAspect((v.width * v.pixelWidthHeightRatio).toInt(), v.height)
                aspect = Rational(w, h); update(act)
            }
            val l = object : Player.Listener { override fun onVideoSizeChanged(videoSize: VideoSize) = size(videoSize) }
            player.addListener(l)
            size(player.videoSize)
            val pipL = androidx.core.util.Consumer<androidx.core.app.PictureInPictureModeChangedInfo> {
                inPip = it.isInPictureInPictureMode
                if (inPip) onEnter()
            }
            act.addOnPictureInPictureModeChangedListener(pipL)
            onDispose {
                player.removeListener(l)
                act.removeOnPictureInPictureModeChangedListener(pipL)
                armed = false; inPip = false; update(act)
            }
        }
    }
}
