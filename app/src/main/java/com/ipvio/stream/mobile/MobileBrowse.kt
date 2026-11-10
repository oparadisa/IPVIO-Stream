package com.ipvio.stream.mobile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipvio.stream.AIcon
import com.ipvio.stream.AppIcon
import com.ipvio.stream.AppSettings
import com.ipvio.stream.BrowseKey
import com.ipvio.stream.BrowseState
import com.ipvio.stream.CatReq
import com.ipvio.stream.Item
import com.ipvio.stream.Kind
import com.ipvio.stream.SORTS
import com.ipvio.stream.Text
import com.ipvio.stream.Tok
import com.ipvio.stream.filterAndSort
import com.ipvio.stream.isTopEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Sorts offered on a phone (index into [SORTS]): newest, popular, best rated, newest year, A–Z, playlist order. */
private val PHONE_SORTS = listOf(0, 1, 2, 4, 6, 8)

/**
 * "Show all" on phones and tablets (fork): poster grid (as many columns as fit) with sort chips.
 * Channels (e.g. from the search) are shown as a list. Sorting is the TV's ([filterAndSort]), filters stay on the TV.
 */
@Composable
fun MobileBrowse(state: BrowseState, gridState: LazyGridState, onOpen: (Item) -> Unit, onBack: () -> Unit) = NativeDensity {
    BackHandler(onBack = onBack)
    val req = state.req
    val key = BrowseKey(state.sort, 0, "Alle", null, null, state.ver)
    val list by produceState<List<Item>?>(null, req, key) { value = withContext(Dispatchers.Default) { filterAndSort(req.items, key) } }
    val live = req.items.firstOrNull()?.kind == Kind.LIVE
    Column(Modifier.fillMaxSize()) {
        MTopBar(req.title, onBack = onBack)
        if (!live) MChipRow(PHONE_SORTS, { SORTS[it] }, { it == state.sort }) { state.sort = it }
        val l = list
        when {
            l == null -> MLoading()
            l.isEmpty() -> MEmpty(AIcon.Info, "Keine Treffer")
            live -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(l, key = { it.id }) { ch -> MChannelRow(ch) { onOpen(ch) } }
            }
            else -> LazyVerticalGrid(
                GridCells.Adaptive(GridMin), Modifier.fillMaxSize(), gridState,
                contentPadding = PaddingValues(start = PadX, end = PadX, top = 8.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
            ) { items(l, key = { it.id }) { MPoster(it, Modifier.fillMaxWidth(), onOpen = onOpen) } }
        }
    }
}

/**
 * Categories on phones and tablets (fork): provider categories or automatic genres as a list with the
 * number of titles; tap opens the grid. "Recently watched" and "My list" come first, as on the TV.
 */
@Composable
fun MobileCategories(req: CatReq, onPick: (String, List<Item>, Int) -> Unit, onBack: () -> Unit) = NativeDensity {
    val ctx = LocalContext.current
    BackHandler(onBack = onBack)
    var mode by remember { mutableIntStateOf(if (AppSettings.ipvioFilter && req.ipvio.isNotEmpty()) 0 else 1) }
    var az by remember { mutableStateOf(false) }
    val map = if (mode == 0) req.ipvio else req.provider
    val entries = remember(map, az, req.recent, req.watch) {
        val l = map.entries.map { it.key to it.value }
        val top = ArrayList<Pair<String, List<Item>>>()
        if (req.recent.isNotEmpty()) top.add(com.ipvio.stream.CAT_RECENT to req.recent)
        if (req.watch.isNotEmpty()) top.add(com.ipvio.stream.CAT_WATCH to req.watch)
        top + if (az) l.sortedBy { it.first.lowercase() } else l
    }
    // Auto genres: best rated first; provider categories keep the area's sorting
    val catSort = if (mode == 0) 2 else req.sort
    Column(Modifier.fillMaxSize()) {
        MTopBar(req.title, onBack = onBack)
        val chips = buildList { add(1); if (req.ipvio.isNotEmpty()) add(0); add(2) }
        MChipRow(chips, { when (it) { 0 -> "Auto-Genres"; 1 -> "Anbieter-Kategorien"; else -> "A–Z" } }, { if (it == 2) az else it == mode }) {
            if (it == 2) az = !az else { mode = it; AppSettings.set(ctx, mode == 0) }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            if (entries.isEmpty()) item { MEmpty(AIcon.Categories, "Keine Kategorien") }
            items(entries, key = { it.first }) { (name, items) ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp)
                        .clickable { onPick(name, items, if (isTopEntry(name)) 8 else catSort) }
                        .padding(horizontal = PadX),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(name, raw = !isTopEntry(name), color = Tok.TextHi, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text("${items.size}", raw = true, color = Tok.TextLow, fontSize = 14.sp)
                    AppIcon(AIcon.Right, Tok.TextLow, size = 18.dp)
                }
            }
        }
    }
}
