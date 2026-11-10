package com.ipvio.stream.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ipvio.stream.Accent1
import com.ipvio.stream.Item
import com.ipvio.stream.PersonInfo
import com.ipvio.stream.PersonRef
import com.ipvio.stream.Surf2
import com.ipvio.stream.Text
import com.ipvio.stream.Tok

/**
 * Person page on phones and tablets (the data comes from [com.ipvio.stream.PersonScreen]): photo, name and life data at the top,
 * the biography folded to five lines, then the person's titles from the playlist as a poster grid. Works upright and sideways.
 */
@Composable
internal fun MobilePerson(ref: PersonRef, info: PersonInfo?, loaded: Boolean, owned: List<Item>, onOpen: (Item) -> Unit, onBack: () -> Unit) = NativeDensity {
    var expanded by remember(ref.id) { mutableStateOf(false) }
    var overflow by remember(ref.id) { mutableStateOf(false) }
    val full: androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }
    Column(Modifier.fillMaxSize()) {
        MTopBar(ref.name, raw = true, onBack = onBack)
        LazyVerticalGrid(
            GridCells.Adaptive(GridMin), Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = PadX, end = PadX, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "head", span = full) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(96.dp).clip(CircleShape).background(Surf2), contentAlignment = Alignment.Center) {
                        val ph = info?.photo ?: ref.photo
                        if (ph != null) AsyncImage(ph, ref.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        else Text(raw = true, text = ref.name.take(1), color = Tok.TextHi, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(raw = true, text = ref.name, color = Tok.TextHi, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        val i = info
                        val line = listOfNotNull(
                            i?.dept?.let { when (it) { "Directing" -> "Regie"; "Acting" -> "Schauspiel"; "Writing" -> "Drehbuch"; else -> it } },
                            i?.born?.let { "geboren $it" }, i?.place
                        ).joinToString("  ·  ")
                        if (line.isNotBlank()) Text(line, color = Tok.TextMid, fontSize = 14.sp)
                    }
                }
            }
            val bio = info?.bio
            if (bio != null) item(key = "bio", span = full) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(raw = true, text = bio, color = Tok.TextMid, fontSize = 15.sp, lineHeight = 22.sp,
                        maxLines = if (expanded) Int.MAX_VALUE else 5, overflow = TextOverflow.Ellipsis,
                        onTextLayout = { if (!expanded) overflow = it.hasVisualOverflow })
                    if (overflow || expanded) Box(
                        Modifier.heightIn(min = 48.dp).clip(CircleShape).clickable(role = Role.Button) { expanded = !expanded },
                        contentAlignment = Alignment.CenterStart
                    ) { Text(if (expanded) "Weniger" else "Mehr lesen", color = Accent1, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
                }
            } else if (loaded && info == null) item(key = "err", span = full) { Text("Infos konnten nicht geladen werden.", color = Tok.TextMid, fontSize = 15.sp) }
            val i = info
            if (i != null) {
                item(key = "sec", span = full) {
                    Text(
                        if (owned.isEmpty()) "Keine Titel in deiner Playlist" else "In deiner Playlist  ·  ${owned.size}",
                        color = Tok.TextHi, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(owned, key = { it.id }) { MPoster(it, Modifier.fillMaxWidth(), onOpen = onOpen) }
                val rest = (i.titles.size - owned.size).coerceAtLeast(0)
                if (rest > 0) item(key = "rest", span = full) { Text("$rest weitere Titel dieser Person sind nicht in deiner Playlist.", color = Tok.TextMid, fontSize = 14.sp) }
            } else if (!loaded) item(key = "load", span = full) { Text("Lade …", color = Tok.TextMid, fontSize = 15.sp) }
        }
    }
}
