package com.ipvio.stream.mobile

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
        "Zur Seite ziehen = Uhrzeit  ·  Tippen = Details", color = Muted, fontSize = 13.sp,
        maxLines = 1, overflow = TextOverflow.Ellipsis
    )
}

/**
 * Details of a tapped program (phones/tablets): a tap in the guide never acts directly. Buttons: watch the channel,
 * set/delete a reminder (later shows), play from the start (archive, also for the running show).
 */
@Composable
fun GuideProgramSheet(ch: com.ipvio.stream.Item, e: com.ipvio.stream.EpgEntry, onWatch: () -> Unit, onFromStart: () -> Unit, onDismiss: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val now = System.currentTimeMillis() / 1000
    var reminded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(com.ipvio.stream.Reminders.isSet(ch.id, e.start)) }
    val canRemind = com.ipvio.stream.ReminderLogic.canRemind(e.start, now)
    val fromStart = com.ipvio.stream.Catchup.canPlay(ch, e, now)
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        androidx.compose.foundation.layout.Box(
            Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { onDismiss() } },
            contentAlignment = Alignment.BottomCenter
        ) {
            androidx.compose.foundation.layout.Column(
                Modifier.widthIn(max = 600.dp).fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = DS.RadiusL, topEnd = DS.RadiusL)).background(com.ipvio.stream.Tok.GlassStrong)
                    .pointerInput(Unit) { detectTapGestures { } }   // taps on the sheet stay inside
                    .navigationBarsPadding().padding(20.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
            ) {
                Text(raw = true, text = ch.title + "  ·  " + com.ipvio.stream.dayLabel(e.start) + "  " + com.ipvio.stream.hhmm(e.start) + " – " + com.ipvio.stream.hhmm(e.end),
                    color = Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(raw = true, text = e.title, color = androidx.compose.ui.graphics.Color.White, fontSize = 20.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (e.desc.isNotBlank()) Text(raw = true, text = e.desc, color = com.ipvio.stream.DS.TextSoft, fontSize = 14.sp, lineHeight = 19.sp,
                    modifier = Modifier.heightIn(max = 160.dp).verticalScroll(rememberScrollState()))
                androidx.compose.foundation.layout.Column(Modifier.fillMaxWidth().padding(top = 6.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
                    MButton("Ansehen", AIcon.Play, primary = !fromStart, modifier = Modifier.fillMaxWidth(), onClick = onWatch)
                    if (fromStart) MButton("Von Anfang an", AIcon.Catchup, primary = true, modifier = Modifier.fillMaxWidth(), onClick = onFromStart)
                    else if (canRemind) MButton(if (reminded) "Erinnerung löschen" else "Erinnerung setzen", AIcon.Bell, modifier = Modifier.fillMaxWidth()) {
                        val on = com.ipvio.stream.Reminders.toggle(ctx, com.ipvio.stream.Reminder(ch.id, ch.title, e.title, e.start, e.end))
                        if (on) com.ipvio.stream.Reminders.askPermission(ctx.activity())
                        reminded = on
                        android.widget.Toast.makeText(ctx,
                            if (on) com.ipvio.stream.Lang.tr("Erinnerung gesetzt – ${com.ipvio.stream.Reminders.leadMin} Min vorher") else com.ipvio.stream.Lang.tr("Erinnerung gelöscht"),
                            android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
