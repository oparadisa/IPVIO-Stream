package com.ipvio.stream.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ipvio.stream.Bg
import com.ipvio.stream.GuideSearchLogic
import com.ipvio.stream.Item
import com.ipvio.stream.Text
import com.ipvio.stream.Tok
import com.ipvio.stream.dayLabel
import com.ipvio.stream.hhmm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Search in the TV guide on phones and tablets: full screen, field with the system keyboard, hits below (channel or show
 * with time). A tap picks the channel; Back or the X closes. Same search as the TV dialog ([GuideSearchLogic]).
 */
@Composable
fun MobileGuideSearch(channels: List<Item>, onPick: (Item) -> Unit, onClose: () -> Unit) {
    var q by remember { mutableStateOf("") }
    val hits by produceState(emptyList<GuideSearchLogic.Hit>(), q) {
        if (q.isBlank()) { value = emptyList(); return@produceState }
        delay(250)
        value = withContext(Dispatchers.Default) { GuideSearchLogic.search(channels, q, System.currentTimeMillis() / 1000) }
    }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        NativeDensity {
            Column(Modifier.fillMaxSize().background(Bg).windowInsetsPadding(WindowInsets.safeDrawing)) {
                MTopBar("Im TV-Guide suchen", onBack = onClose)
                SearchField(q, "Sender oder Sendung", { q = it.take(40) }) { if (q.isEmpty()) onClose() else q = "" }
                if (q.isNotBlank() && hits.isEmpty()) Text("Keine Treffer", color = Tok.TextLow, fontSize = 15.sp, modifier = Modifier.padding(PadX))
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(PadX), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(hits) { h ->
                        val e = h.entry
                        Column(
                            Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(12.dp)).background(Tok.Card)
                                .clickable { onPick(h.channel) }.padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(raw = true, text = e?.title ?: h.channel.title, color = Tok.TextHi, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                raw = true,
                                text = if (e == null) h.channel.group else h.channel.title + "  ·  " + dayLabel(e.start) + " " + hhmm(e.start) + " - " + hhmm(e.end),
                                color = Tok.TextLow, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
