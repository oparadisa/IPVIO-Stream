package com.ipvio.stream.mobile

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipvio.stream.AIcon
import com.ipvio.stream.DS
import com.ipvio.stream.GuideCat
import com.ipvio.stream.Muted
import com.ipvio.stream.Text

/*
 * TV guide on phones and tablets (fork): layout by window size, dragging along the time axis, category chips
 * and touch wording instead of the key legend. On the TV the guide keeps its layout ([MobileLogic.GuideDims.TV]).
 */

/** Layout of the guide for this window. */
@Composable
fun guideDims(): MobileLogic.GuideDims {
    if (!MobileUi.active) return MobileLogic.GuideDims.TV
    val c = LocalConfiguration.current
    return MobileLogic.guideDims(c.screenWidthDp, c.screenHeightDp)
}

/** Place and size of the running picture while the guide is open (phones/tablets). */
@Composable
fun BoxScope.guideVideoMod(): Modifier {
    val d = guideDims()
    return if (d.videoTop) Modifier.align(Alignment.TopCenter).size(d.videoW.dp, d.videoH.dp)
    else Modifier.align(Alignment.TopEnd).padding(top = 20.dp, end = 24.dp).size(d.videoW.dp, d.videoH.dp).clip(RoundedCornerShape(DS.RadiusM))
}

/** Dragging sideways moves the time window; [onMinutes] gets the minutes to move (positive = later). */
fun Modifier.guideDrag(dpPerMin: Float, onMinutes: (Float) -> Unit): Modifier =
    if (!MobileUi.active) this else this.then(Modifier.pointerInput(dpPerMin) {
        detectHorizontalDragGestures { ch, dx ->
            ch.consume()
            onMinutes(-dx / density / dpPerMin)
        }
    })

/** Categories as chips above the grid (phone portrait). */
@Composable
fun GuideCatChips(cats: List<GuideCat>, selected: Int, onPick: (Int) -> Unit) {
    MChipRow(cats.indices.toList(), { cats[it].name }, { it == selected }, { cats[it].icon == null || cats[it].icon == AIcon.Lists }, onPick)
}

/** One line instead of the TV key legend. */
@Composable
fun GuideTouchHint() {
    Text(
        "Zur Seite ziehen = Uhrzeit  ·  Tippen = ansehen, Erinnerung oder Archiv", color = Muted, fontSize = 13.sp,
        maxLines = 1, overflow = TextOverflow.Ellipsis
    )
}
