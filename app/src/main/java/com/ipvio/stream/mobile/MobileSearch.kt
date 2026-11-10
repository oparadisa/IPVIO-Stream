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
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipvio.stream.AIcon
import com.ipvio.stream.AppIcon
import com.ipvio.stream.AppSettings
import com.ipvio.stream.BrowseReq
import com.ipvio.stream.Item
import com.ipvio.stream.MenuLogic
import com.ipvio.stream.PlaylistLogic
import com.ipvio.stream.PlaylistStore
import com.ipvio.stream.SearchCache
import com.ipvio.stream.SearchState
import com.ipvio.stream.Text
import com.ipvio.stream.Tok
import com.ipvio.stream.ZapContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Search on phones and tablets (fork): field with the system keyboard, quick filters, results as poster rows
 * (movies, series) and a channel list. Same index and matching as the TV search ([SearchCache]).
 */
@Composable
fun MobileSearch(onOpen: (Item) -> Unit, onBrowse: (BrowseReq) -> Unit, onTab: (Int) -> Unit) = NativeDensity {
    val ctx = LocalContext.current
    BackHandler { onTab(MenuLogic.HOME) }
    val all = PlaylistStore.channels
    val index by produceState(SearchCache.get(all), all) { value = withContext(Dispatchers.Default) { SearchCache.build(all) } }
    val q = SearchState.query.trim()
    val none = Triple(emptyList<Item>(), emptyList<Item>(), emptyList<Item>())
    val found by produceState("" to none, q, index) {
        val idx = index ?: return@produceState
        if (q.length < 2) { value = q to none; return@produceState }
        delay(250)
        value = q to withContext(Dispatchers.Default) { idx.find(q) }
    }
    val searching = q.length >= 2 && found.first != q
    val (rm0, rs0, rl0) = if (q.length < 2 || searching) none else found.second
    val rm = remember(rm0) { PlaylistLogic.firstPerKey(rm0) { it.mk + "|" + it.lang } }
    val fl = SearchState.filter
    val showM = fl == 0 || fl == 1; val showS = fl == 0 || fl == 2; val showL = fl == 0 || fl == 3
    fun saveQuery(text: String) = AppSettings.addSearch(ctx, text)

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(top = 8.dp)) {
            SearchField(SearchState.query, "Titel, Sender, Schauspieler …", { SearchState.query = it }, onDone = { saveQuery(SearchState.query) }) {
                if (SearchState.query.isEmpty()) onTab(MenuLogic.HOME) else SearchState.query = ""
            }
        }
        MChipRow(listOf(0, 1, 2, 3), { listOf("Alle", "Filme", "Serien", "Live TV")[it] }, { it == fl }) { SearchState.filter = it }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            when {
                q.length < 2 -> {
                    if (AppSettings.searchHistory.isNotEmpty()) item { MSection("Zuletzt gesucht") }
                    items(AppSettings.searchHistory.toList(), key = { "h|$it" }) { h ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { SearchState.query = h }.padding(horizontal = PadX),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AppIcon(AIcon.History, Tok.TextLow, size = 20.dp)
                            Text(h, raw = true, color = Tok.TextMid, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        }
                    }
                }
                searching -> item { MLoading() }
                (if (showM) rm.size else 0) + (if (showS) rs0.size else 0) + (if (showL) rl0.size else 0) == 0 -> item { MEmpty(AIcon.Info, "Keine Treffer") }
                else -> {
                    val open: (Item) -> Unit = { saveQuery(q); onOpen(it) }
                    if (showM && rm.isNotEmpty()) {
                        item(key = "mh") { MSection("Filme · ${rm.size}") { saveQuery(q); onBrowse(BrowseReq("Suche: $q · Filme", rm.asReversed(), 0)) } }
                        item(key = "m") { MPosterRow(rm.take(30), open) }
                    }
                    if (showS && rs0.isNotEmpty()) {
                        item(key = "sh") { MSection("Serien · ${rs0.size}") { saveQuery(q); onBrowse(BrowseReq("Suche: $q · Serien", rs0.asReversed(), 0)) } }
                        item(key = "s") { MPosterRow(rs0.take(30), open) }
                    }
                    if (showL && rl0.isNotEmpty()) {
                        item(key = "lh") { MSection("Live TV · ${rl0.size}") }
                        items(rl0.take(60), key = { "l|" + it.id }) { ch -> MChannelRow(ch) { ZapContext.list = rl0; ZapContext.key = null; open(ch) } }
                    }
                }
            }
        }
    }
}
