package com.ipvio.stream.mobile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipvio.stream.AIcon
import com.ipvio.stream.Accent1
import com.ipvio.stream.CatFavs
import com.ipvio.stream.ChannelSearch
import com.ipvio.stream.FAV_CAT
import com.ipvio.stream.HiddenCats
import com.ipvio.stream.Item
import com.ipvio.stream.Lang
import com.ipvio.stream.Library
import com.ipvio.stream.LiveMemory
import com.ipvio.stream.LiveNowPanel
import com.ipvio.stream.MenuLogic
import com.ipvio.stream.Text
import com.ipvio.stream.Tok
import com.ipvio.stream.ZapContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val ALL = "ALL"

/**
 * Live TV on phones and tablets (fork): category chips (favorites, all channels, the provider's categories)
 * and a channel list with what is on now. Tap = play (zapping stays inside the chosen category),
 * long press = add to / remove from favorites. Search filters by channel name; the TV guide opens in the player.
 */
@Composable
internal fun MobileLive(live: List<Item>, groups: Map<String, List<Item>>, favs: List<Item>, onOpen: (Item) -> Unit) {
    val ctx = LocalContext.current
    CatFavs.init(ctx)
    HiddenCats.init(ctx)
    val cats = remember(groups, favs.isNotEmpty(), CatFavs.list.size, HiddenCats.list.size) {
        val favCats = CatFavs.list.filter { it in groups && it !in HiddenCats.list }
        (if (favs.isNotEmpty()) listOf(FAV_CAT) else emptyList()) + ALL + favCats + groups.keys.filter { it !in favCats && it !in HiddenCats.list }
    }
    var sel by remember { mutableStateOf(LiveMemory.sel(ctx)?.takeIf { it in cats } ?: if (favs.isNotEmpty()) FAV_CAT else ALL) }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val base = when (sel) { FAV_CAT -> favs; ALL -> live; else -> groups[sel].orEmpty() }
    val shown by produceState(base, base, query, searching) {
        value = if (!searching || query.isBlank()) base else withContext(Dispatchers.Default) { ChannelSearch.filter(live, query) }
    }
    val listState = rememberLazyListState()
    LaunchedEffect(sel) { listState.scrollToItem(0) }
    // Wide tablets: list │ preview. The first tap selects a channel, a second tap (or Play) starts it.
    val wide = twoPane()
    var picked by remember { mutableStateOf<Item?>(null) }
    val cur = picked?.takeIf { p -> shown.any { it.id == p.id } } ?: shown.firstOrNull()
    fun play(ch: Item) {
        // Zapping in the player stays inside the list the channel was picked from
        ZapContext.list = shown; ZapContext.key = if (searching) null else sel
        onOpen(ch)
    }

    Row(Modifier.fillMaxSize()) {
    Column(if (wide) Modifier.width(listPaneWidth()).fillMaxHeight() else Modifier.fillMaxSize()) {
        if (searching) SearchField(query, "Sender suchen", { query = it }) { searching = false; query = "" }
        else Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                MChipRow(cats, { label(it) }, { it == sel }, raw = { it != FAV_CAT && it != ALL }) { k -> sel = k; LiveMemory.setSel(ctx, k) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = PadX, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (searching && query.isNotBlank()) "${shown.size} Treffer" else "${shown.size} Sender",
                color = Tok.TextLow, fontSize = 13.sp, modifier = Modifier.weight(1f)
            )
            if (!searching) MIconButton(null, "Suche", menuId = MenuLogic.SEARCH) { searching = true }
            MIconButton(null, "TV-Guide", menuId = MenuLogic.EPG) { MobileNav.onTab(MenuLogic.EPG) }
        }
        LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = 24.dp)) {
            if (shown.isEmpty()) item { MEmpty(AIcon.Live, if (searching) "Keine Treffer" else "Noch keine Sender") }
            items(shown, key = { it.id }) { ch ->
                MChannelRow(ch, selected = wide && ch.id == cur?.id, onLong = {
                    Library.toggleFav(ctx, ch.id)
                    Toast.makeText(ctx, Lang.tr(if (Library.isFav(ch.id)) "Zu Favoriten hinzugefügt" else "Aus Favoriten entfernt"), Toast.LENGTH_SHORT).show()
                }) { if (wide && ch.id != cur?.id) picked = ch else play(ch) }
            }
        }
    }
    if (wide && cur != null) LivePreview(cur) { play(cur) }
    }
}

/** Right pane on wide tablets: logo, what is on now and next (the TV's [LiveNowPanel]), Play and TV guide. */
@Composable
private fun RowScope.LivePreview(ch: Item, onPlay: () -> Unit) {
    Column(
        Modifier.weight(1f).fillMaxHeight().background(Tok.Card.copy(alpha = 0.35f)).verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        MLogo(ch, 160.dp, 110.dp)
        TvDensity { LiveNowPanel(ch) }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MButton("Abspielen", AIcon.Play, primary = true, onClick = onPlay)
            MButton("TV-Guide", AIcon.Live) { MobileNav.onTab(MenuLogic.EPG) }
        }
    }
}

private fun label(k: String): String = when (k) { FAV_CAT -> "Favoriten"; ALL -> "Alle"; else -> k }

/** Search field in the bar with the system keyboard (opens right away); the X closes the search. */
@Composable
internal fun SearchField(text: String, hint: String, onChange: (String) -> Unit, onDone: (() -> Unit)? = null, onClose: () -> Unit) {
    val req = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { req.requestFocus() } }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = PadX, vertical = 4.dp).heightIn(min = 48.dp)
            .clip(RoundedCornerShape(24.dp)).background(Tok.Card).padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f)) {
            if (text.isEmpty()) Text(hint, color = Tok.TextLow, fontSize = 16.sp)
            BasicTextField(
                text, onChange, Modifier.fillMaxWidth().focusRequester(req), singleLine = true,
                textStyle = TextStyle(color = Tok.TextHi, fontSize = 16.sp), cursorBrush = SolidColor(Accent1),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { onDone?.invoke() })
            )
        }
        MIconButton(AIcon.Close, "Schließen", onClick = onClose)
    }
}

/** Overload without a search action. */
@Composable
internal fun SearchField(text: String, hint: String, onChange: (String) -> Unit, onClose: () -> Unit) = SearchField(text, hint, onChange, null, onClose)
