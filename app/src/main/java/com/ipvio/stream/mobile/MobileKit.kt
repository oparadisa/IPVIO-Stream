package com.ipvio.stream.mobile

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ipvio.stream.AIcon
import com.ipvio.stream.Accent1
import com.ipvio.stream.AppIcon
import com.ipvio.stream.DS
import com.ipvio.stream.Entry
import com.ipvio.stream.Epg
import com.ipvio.stream.EpgEntry
import com.ipvio.stream.Item
import com.ipvio.stream.Lang
import com.ipvio.stream.Library
import com.ipvio.stream.MenuIcon
import com.ipvio.stream.OnAccent
import com.ipvio.stream.PosterTile
import com.ipvio.stream.Surf2
import com.ipvio.stream.Text
import com.ipvio.stream.Tok
import com.ipvio.stream.hhmm

/*
 * Building blocks of the phone/tablet screens (fork). Laid out in device dp inside [NativeDensity];
 * every tap target is at least 48 dp. Colors come from the app's design tokens, so themes apply here too.
 */

internal val PadX = 16.dp
internal val PosterW = 112.dp
/** Poster grid: as many columns of at least this width as fit (3 on a phone held upright). */
internal val GridMin = 108.dp

/** Title bar of a phone screen: optional back arrow, title, actions on the right. */
@Composable
internal fun MTopBar(title: String, raw: Boolean = false, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = if (onBack != null) 4.dp else PadX, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) MIconButton(AIcon.Left, "Zurück", onClick = onBack)
        Text(
            title, raw = raw, color = Tok.TextHi, fontSize = 20.sp, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = if (onBack != null) 4.dp else 0.dp)
        )
        actions()
    }
}

/** Round 48 dp icon button. [menuId] ≥ 0 draws the menu icon of that area instead of [icon]. */
@Composable
internal fun MIconButton(icon: AIcon?, desc: String, menuId: Int = -1, tint: Color = Tok.TextHi, onClick: () -> Unit) {
    val label = Lang.tr(desc)
    Box(
        Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) AppIcon(icon, tint, size = 24.dp) else MenuIcon(menuId, tint, Modifier.size(24.dp))
    }
}

/** Filter/selection chip (one of a row). */
@Composable
internal fun MChip(label: String, selected: Boolean, raw: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        Modifier.heightIn(min = 40.dp).clip(shape)
            .background(if (selected) Accent1 else Tok.Card)
            .border(1.dp, if (selected) Color.Transparent else Tok.Line, shape)
            .selectable(selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, raw = raw, color = if (selected) OnAccent else Tok.TextHi, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** Horizontally scrolling chips. */
@Composable
internal fun <T> MChipRow(entries: List<T>, label: (T) -> String, selected: (T) -> Boolean, raw: (T) -> Boolean = { false }, onPick: (T) -> Unit) {
    LazyRow(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = PadX),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(entries.size) { i -> val e = entries[i]; MChip(label(e), selected(e), raw(e)) { onPick(e) } }
    }
}

/** Pill button (48 dp high); [primary] = accent color. */
@Composable
internal fun MButton(text: String, icon: AIcon? = null, primary: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val ink = if (primary) OnAccent else Tok.TextHi
    Row(
        modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(24.dp))
            .background(if (primary) Accent1 else Tok.Card)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        if (icon != null) AppIcon(icon, ink, size = 20.dp)
        Text(text, color = ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Section title with an optional "Show all" link on the right. */
@Composable
internal fun MSection(title: String, raw: Boolean = false, onAll: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(start = PadX, end = 4.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title, raw = raw, color = Tok.TextHi, fontSize = 18.sp, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(vertical = 12.dp)
        )
        if (onAll != null) Box(
            Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(24.dp)).clickable(role = Role.Button, onClick = onAll).padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) { Text("Alle anzeigen", color = Accent1, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
    }
}

/** Poster with the title below (two lines at most). */
@Composable
internal fun MPoster(item: Item, modifier: Modifier, progress: Float? = null, onOpen: (Item) -> Unit) {
    Column(modifier) {
        Box {
            // The tile's placeholder art and badges are sized for the TV scale
            TvDensity { PosterTile(item, Modifier.fillMaxWidth()) { onOpen(item) } }
            if (progress != null) Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(6.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0x66000000))) {
                Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight().background(Accent1))
            }
        }
        Text(item.title, raw = true, color = Tok.TextMid, fontSize = 12.sp, lineHeight = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
    }
}

/** Horizontal row of posters. */
@Composable
internal fun MPosterRow(list: List<Item>, onOpen: (Item) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = PadX), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(list, key = { it.id }) { MPoster(it, Modifier.width(PosterW), onOpen = onOpen) }
    }
}

/** "Continue watching": posters with progress. */
@Composable
internal fun MResumeRow(list: List<Pair<Entry, Item>>, onResume: (Entry, Item) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = PadX), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(list, key = { it.second.id }) { (e, i) ->
            MPoster(i, Modifier.width(PosterW), if (e.dur > 0) e.pos.toFloat() / e.dur else null) { onResume(e, i) }
        }
    }
}

/** Channel logo on a calm surface (initials when there is no logo). */
@Composable
internal fun MLogo(item: Item, w: Dp, h: Dp) {
    Box(Modifier.size(w, h).clip(RoundedCornerShape(8.dp)).background(Surf2), contentAlignment = Alignment.Center) {
        if (item.logo != null) AsyncImage(item.logo, item.title, Modifier.fillMaxSize().padding(4.dp), contentScale = ContentScale.Fit)
        else Text(item.title.take(3).uppercase(), raw = true, color = Tok.TextHi, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
    }
}

/** Channels as logo tiles in a horizontal row (home screen). */
@Composable
internal fun MChannelTiles(list: List<Item>, onOpen: (Item) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = PadX), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(list, key = { it.id }) { ch ->
            Column(Modifier.width(96.dp).clip(RoundedCornerShape(10.dp)).clickable { onOpen(ch) }, horizontalAlignment = Alignment.CenterHorizontally) {
                MLogo(ch, 96.dp, 64.dp)
                Text(ch.title, raw = true, color = Tok.TextMid, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

/** One channel in a list: logo, name, what is on now with its time and progress. Long press = [onLong]. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MChannelRow(item: Item, selected: Boolean = false, padX: Dp = PadX, onLong: (() -> Unit)? = null, onClick: () -> Unit) {
    val epg by produceState(emptyList<EpgEntry>(), item.id) { value = runCatching { Epg.now(item) }.getOrDefault(emptyList()) }
    val now = Epg.current(epg)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp)
            .background(if (selected) Tok.CardFocus else Color.Transparent)
            .combinedClickable(onLongClick = onLong, onClick = onClick)
            .padding(horizontal = padX, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MLogo(item, 64.dp, 44.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.title, raw = true, color = Tok.TextHi, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (now != null) {
                Text(hhmm(now.start) + "  " + now.title, raw = true, color = Tok.TextLow, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val frac = ((System.currentTimeMillis() / 1000 - now.start).toFloat() / (now.end - now.start).coerceAtLeast(1)).coerceIn(0f, 1f)
                Box(Modifier.padding(top = 4.dp).fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(Tok.Line)) {
                    Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(Accent1))
                }
            }
        }
        if (Library.isFav(item.id)) AppIcon(AIcon.Star, DS.Gold, size = 18.dp)
    }
}

/** Empty area: icon, title, explanation, optional button. */
@Composable
internal fun MEmpty(icon: AIcon, title: String, text: String? = null, action: String? = null, onAction: () -> Unit = {}) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AppIcon(icon, Tok.TextLow, size = 40.dp)
        Text(title, color = Tok.TextHi, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        if (text != null) Text(text, color = Tok.TextLow, fontSize = 14.sp, textAlign = TextAlign.Center)
        if (action != null) { Spacer(Modifier.height(4.dp)); MButton(action, primary = true, onClick = onAction) }
    }
}

/** Placeholder while something loads: spinner on the page background. */
@Composable
internal fun MLoading(text: String? = null) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)) {
        com.ipvio.stream.Spinner(Accent1, Modifier.size(40.dp))
        if (text != null) Text(text, color = Tok.TextLow, fontSize = 15.sp)
    }
}
