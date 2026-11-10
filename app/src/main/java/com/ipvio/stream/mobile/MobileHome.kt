package com.ipvio.stream.mobile

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipvio.stream.AIcon
import com.ipvio.stream.AppSettings
import com.ipvio.stream.AreaCache
import com.ipvio.stream.AreaSource
import com.ipvio.stream.BrowseReq
import com.ipvio.stream.CatReq
import com.ipvio.stream.Classifier
import com.ipvio.stream.EMPTY_HOME
import com.ipvio.stream.Entry
import com.ipvio.stream.History
import com.ipvio.stream.HomeCache
import com.ipvio.stream.HomeLayout
import com.ipvio.stream.ImportedLists
import com.ipvio.stream.Item
import com.ipvio.stream.Kind
import com.ipvio.stream.LangFilter
import com.ipvio.stream.Library
import com.ipvio.stream.MENU_LABELS
import com.ipvio.stream.MenuLogic
import com.ipvio.stream.NavState
import com.ipvio.stream.PlaylistLoadingOverlay
import com.ipvio.stream.PlaylistStore
import com.ipvio.stream.Profiles
import com.ipvio.stream.SettingsHub
import com.ipvio.stream.SettingsNav
import com.ipvio.stream.Trending
import com.ipvio.stream.WatchStore
import com.ipvio.stream.buildHome
import com.ipvio.stream.ipvioGroups
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Live TV area (MenuLogic has no constant for it). */
private const val LIVE_TAB = 3
private const val SORT_NEW = 0
private const val SORT_RATING = 2
private const val SORT_PLAYLIST = 8

/** Trend titles survive leaving the home screen (as on the TV), so they are not fetched again on every visit. */
private object MHomeMem { var trend: Set<String> = emptySet() }

/**
 * Main areas on phones and tablets (fork): Home, Movies, Series as vertical lists of horizontal rows, Live TV,
 * Favorites / My list / History as poster grids, Settings. Same data as the TV home screen ([buildHome]),
 * same parameters as [com.ipvio.stream.HomeScreen], which hands over here when [MobileUi.active].
 */
@Composable
fun MobileHome(
    tab: Int, onTab: (Int) -> Unit, states: List<LazyListState>,
    onOpen: (Item) -> Unit, onPlayNow: (Item) -> Unit,
    onResume: (Entry, Item) -> Unit, onBrowse: (BrowseReq) -> Unit,
    onCategories: (CatReq) -> Unit, onAdd: () -> Unit
) = NativeDensity {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val activity = LocalActivity.current
    val allRaw = PlaylistStore.channels
    val areaK = if (tab in 1..2) AreaSource.get(tab) else -1
    val all = remember(allRaw, areaK, tab) { if (areaK < 0) allRaw else AreaCache.get(allRaw, tab, areaK) }
    val order = PlaylistStore.catOrder
    // Genre progress is adopted at most every 45 s (every adoption rebuilds the rows), as on the TV
    val ver by produceState(Classifier.version) { while (true) { delay(45_000); value = Classifier.version } }
    var trend by remember { mutableStateOf(MHomeMem.trend) }
    LaunchedEffect(all.isNotEmpty()) { if (all.isNotEmpty()) { trend = Trending.titles(); MHomeMem.trend = trend } }
    val want = AppSettings.ipvioFilter
    val data = produceState(HomeCache.get(all, order, want, ver, trend), all, order, want, ver, trend) {
        value = HomeCache.get(all, order, want, ver, trend)
            ?: withContext(Dispatchers.Default) { buildHome(all, want, trend) }.also { HomeCache.put(all, order, want, ver, trend, it) }
    }.value ?: HomeCache.last()?.takeIf { it.byId.isNotEmpty() && all.isNotEmpty() } ?: EMPTY_HOME
    val byId = data.byId
    val hist by remember(byId) { derivedStateOf { History.ids.mapNotNull { byId[it] } } }
    val watchList by remember(byId) { derivedStateOf { Library.watch.mapNotNull { byId[it] } } }
    val favs by remember(byId) { derivedStateOf { Library.favs.mapNotNull { byId[it] } } }
    val resume by remember(byId) { derivedStateOf { WatchStore.entries.mapNotNull { e -> byId[e.itemId]?.let { e to it } } } }
    val favLive = remember(favs) { favs.filter { it.kind == Kind.LIVE } }
    androidx.compose.runtime.SideEffect { NavState.tab = tab }

    val listState = states[tab.coerceIn(0, states.lastIndex)]
    BackHandler {
        when (MobileLogic.back(tab, listState.firstVisibleItemIndex > 0)) {
            MobileLogic.Back.GO_HOME -> onTab(MenuLogic.HOME)
            MobileLogic.Back.SCROLL_TOP -> scope.launch { runCatching { listState.animateScrollToItem(0) } }
            MobileLogic.Back.EXIT -> activity?.finishAffinity()
        }
    }

    val title = MENU_LABELS.getOrElse(tab) { "" }
    val profile: () -> Unit = { SettingsNav.openPage = SettingsNav.PROFILES; onTab(MenuLogic.SETTINGS) }
    Column(Modifier.fillMaxSize()) {
        // Settings draws its own title and back arrow
        if (tab != MenuLogic.SETTINGS) MTopBar(if (tab == MenuLogic.HOME) "Couchless" else title, raw = tab == MenuLogic.HOME) {
            if (tab != MenuLogic.SETTINGS && tab != LIVE_TAB) MIconButton(null, "Suche", menuId = MenuLogic.SEARCH) { onTab(MenuLogic.SEARCH) }
            if (tab == MenuLogic.HOME && Profiles.list.size > 1) MIconButton(AIcon.Person, "Profil", onClick = profile)
        }
        Box(Modifier.weight(1f)) {
            when {
                tab == MenuLogic.SETTINGS -> {
                    val reload: () -> Unit = { PlaylistStore.savedUrl(ctx)?.let { u -> scope.launch { PlaylistStore.load(ctx, u, false) } } }
                    if (twoPane()) MobileSettingsPanes(onAdd, reload)
                    else LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = 24.dp)) { item { SettingsHub(onAdd, reload) } }
                }
                PlaylistStore.loading && all.isEmpty() && PlaylistStore.heavy -> PlaylistLoadingOverlay()
                PlaylistStore.loading && all.isEmpty() -> MLoading()
                all.isEmpty() -> NoContent(onAdd)
                tab == LIVE_TAB -> MobileLive(data.live, data.liveP, favLive, onOpen)
                tab == MenuLogic.FAVS -> {
                    val m = favs.filter { it.kind == Kind.MOVIE }; val s = favs.filter { it.kind == Kind.SERIES }
                    if (favs.isEmpty()) MEmpty(AIcon.Heart, "Noch keine Favoriten", "Sender lange drücken, bei Filmen und Serien auf der Detailseite „Zu Favoriten“ wählen.", "Zu Live TV") { onTab(LIVE_TAB) }
                    else PosterGrid { channels("Favoriten · Sender", favLive, onOpen); posters("Favoriten · Filme", m, onOpen); posters("Favoriten · Serien", s, onOpen) }
                }
                tab == MenuLogic.MYLIST -> {
                    val m = watchList.filter { it.kind == Kind.MOVIE }; val s = watchList.filter { it.kind == Kind.SERIES }
                    if (m.isEmpty() && s.isEmpty()) MEmpty(AIcon.Plus, "Noch nichts in deiner Liste", "Auf der Seite eines Films oder einer Serie „Meine Liste“ wählen.", "Filme entdecken") { onTab(1) }
                    else PosterGrid { posters("Meine Liste · Filme", m, onOpen); posters("Meine Liste · Serien", s, onOpen) }
                }
                tab == MenuLogic.VERLAUF -> {
                    val m = hist.filter { it.kind == Kind.MOVIE }; val s = hist.filter { it.kind == Kind.SERIES }
                    if (m.isEmpty() && s.isEmpty()) MEmpty(AIcon.History, "Noch nichts angesehen", "Hier erscheinen die zuletzt gestarteten Filme und Serien.", "Filme entdecken") { onTab(1) }
                    else PosterGrid { posters("Verlauf · Filme", m, onOpen); posters("Verlauf · Serien", s, onOpen) }
                }
                tab == 1 || tab == 2 -> {
                    val isM = tab == 1
                    val kind = if (isM) Kind.MOVIE else Kind.SERIES
                    val base = if (isM) data.movies else data.series
                    val top = if (isM) data.topM else data.topS
                    val noun = if (isM) "Filme" else "Serien"
                    val groups = if (isM) data.movieG else data.seriesG
                    val trendK = remember(data.trendItems, kind) { data.trendItems.filter { it.kind == kind } }
                    val pop = if (trendK.size >= 5) trendK else top
                    val newest = remember(base) { base.takeLast(40).reversed() }
                    val resK = remember(resume, kind) { resume.filter { it.second.kind == kind } }
                    val wlK = remember(watchList, kind) { watchList.filter { it.kind == kind } }
                    val histK = remember(hist, kind) { hist.filter { it.kind == kind } }
                    val openCats: () -> Unit = {
                        scope.launch {
                            val g = if (data.genresOk) withContext(Dispatchers.Default) { ipvioGroups(base) } else emptyMap()
                            val imported = ImportedLists.groups(ctx, kind, byId)
                            onCategories(CatReq("$noun · Kategorien", imported + g, imported + (if (isM) data.movieP else data.seriesP), SORT_NEW))
                        }
                    }
                    val chips = listOf("Kategorien", "Top bewertet", "Neu hinzugefügt", "Alle $noun")
                    LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = 24.dp)) {
                        item(key = "chips") {
                            MChipRow(chips, { it }, { false }) { c ->
                                when (c) {
                                    "Kategorien" -> openCats()
                                    "Top bewertet" -> onBrowse(BrowseReq("Top bewertet · $noun", top, SORT_RATING))
                                    "Neu hinzugefügt" -> onBrowse(BrowseReq("Neu hinzugefügt · $noun", base, SORT_NEW))
                                    else -> onBrowse(BrowseReq("Alle $noun", base, SORT_NEW))
                                }
                            }
                        }
                        if (base.isEmpty()) item { MEmpty(AIcon.Info, "Keine $noun in dieser Playlist") }
                        rows(if (isM) "Beliebte Filme" else "Beliebte Serien", pop, 3, onOpen) { onBrowse(BrowseReq(if (isM) "Beliebte Filme" else "Beliebte Serien", pop, SORT_PLAYLIST)) }
                        if (resK.isNotEmpty()) { item(key = "resh") { MSection("Weiterschauen") }; item(key = "res") { MResumeRow(resK, onResume) } }
                        rows("Meine Liste", wlK, 1, onOpen) { onBrowse(BrowseReq("Meine Liste · $noun", wlK, SORT_PLAYLIST)) }
                        rows("Zuletzt geschaut", histK, 1, onOpen) { onBrowse(BrowseReq("Zuletzt geschaut · $noun", histK, SORT_PLAYLIST)) }
                        rows("Neu hinzugefügt", newest, 1, onOpen) { onBrowse(BrowseReq("Neu hinzugefügt · $noun", base, SORT_NEW)) }
                        rows("Top bewertet", top, 8, onOpen) { onBrowse(BrowseReq("Top bewertet · $noun", top, SORT_RATING)) }
                        groups.entries.take(12).forEach { (g, l) -> rows(g, l, 1, onOpen, raw = true) { onBrowse(BrowseReq(g, l, SORT_NEW)) } }
                    }
                }
                else -> LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = 24.dp)) {
                    // Home: the blocks of Settings > Customize home, in that order (hero, tiles and clock are TV-only)
                    val newM = data.movies.takeLast(40).reversed()
                    val newS = data.series.takeLast(40).reversed()
                    val wl = watchList.filter { it.kind != Kind.LIVE }
                    val nowCh = (favLive + hist.filter { it.kind == Kind.LIVE }).distinctBy { it.id }.ifEmpty { data.live.take(20) }
                    for (id in HomeLayout.order) {
                        if (!HomeLayout.shown(id)) continue
                        when (id) {
                            "resume" -> if (resume.isNotEmpty()) { item(key = "resh") { MSection("Weiterschauen") }; item(key = "res") { MResumeRow(resume, onResume) } }
                            "favch" -> if (favLive.isNotEmpty()) { item(key = "fch") { MSection("Meine Sender") { onTab(LIVE_TAB) } }; item(key = "fc") { MChannelTiles(favLive, onOpen) } }
                            "chnow" -> if (nowCh.isNotEmpty()) { item(key = "chh") { MSection("Live TV") { onTab(LIVE_TAB) } }; item(key = "ch") { MChannelTiles(nowCh, onOpen) } }
                            "trend" -> rows("Top 10 heute", data.trendItems.take(10), 3, onOpen) { onBrowse(BrowseReq("Im Trend", data.trendItems, SORT_PLAYLIST)) }
                            "wl" -> rows("Meine Liste", wl, 1, onOpen) { onTab(MenuLogic.MYLIST) }
                            "hist" -> rows("Zuletzt geschaut", hist.take(30), 1, onOpen) { onBrowse(BrowseReq("Zuletzt geschaut", hist, SORT_PLAYLIST)) }
                            "newm" -> rows("Neu hinzugefügt · Filme", newM, 1, onOpen) { onBrowse(BrowseReq("Neu hinzugefügt · Filme", data.movies, SORT_NEW)) }
                            "topm" -> rows("Top bewertet · Filme", data.topM, 8, onOpen) { onBrowse(BrowseReq("Top bewertet · Filme", data.topM, SORT_RATING)) }
                            "news" -> rows("Neu hinzugefügt · Serien", newS, 1, onOpen) { onBrowse(BrowseReq("Neu hinzugefügt · Serien", data.series, SORT_NEW)) }
                            "tops" -> rows("Top bewertet · Serien", data.topS, 8, onOpen) { onBrowse(BrowseReq("Top bewertet · Serien", data.topS, SORT_RATING)) }
                            "sg" -> data.seriesG.entries.take(3).forEach { (g, l) -> rows("Serien · $g", l, 1, onOpen) { onBrowse(BrowseReq("Serien · $g", l, SORT_NEW)) } }
                            "mg" -> data.movieG.entries.take(3).forEach { (g, l) -> rows("Filme · $g", l, 1, onOpen) { onBrowse(BrowseReq("Filme · $g", l, SORT_NEW)) } }
                        }
                    }
                    if (data.movies.isEmpty() && data.series.isEmpty()) item { MEmpty(AIcon.Info, "Diese Playlist enthält keine Filme oder Serien.") }
                }
            }
        }
    }
}

/** Title + poster row, shown when the list has at least [min] titles. */
private fun androidx.compose.foundation.lazy.LazyListScope.rows(
    title: String, list: List<Item>, min: Int, onOpen: (Item) -> Unit, raw: Boolean = false, onAll: () -> Unit
) {
    if (list.size < min || list.isEmpty()) return
    item(key = "h|$title") { MSection(title, raw = raw, onAll = onAll) }
    item(key = "r|$title") { MPosterRow(list.take(40), onOpen) }
}

@Composable
private fun PosterGrid(content: LazyGridScope.() -> Unit) {
    LazyVerticalGrid(
        GridCells.Adaptive(GridMin), Modifier.fillMaxSize(), rememberLazyGridState(),
        contentPadding = PaddingValues(start = PadX, end = PadX, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

private fun LazyGridScope.posters(title: String, list: List<Item>, onOpen: (Item) -> Unit) {
    if (list.isEmpty()) return
    item(key = "h|$title", span = { GridItemSpan(maxLineSpan) }) { MSectionFlush(title) }
    items(list, key = { "$title|" + it.id }) { MPoster(it, Modifier.fillMaxWidth(), onOpen = onOpen) }
}

private fun LazyGridScope.channels(title: String, list: List<Item>, onOpen: (Item) -> Unit) {
    if (list.isEmpty()) return
    item(key = "h|$title", span = { GridItemSpan(maxLineSpan) }) { MSectionFlush(title) }
    items(list, key = { "$title|" + it.id }, span = { GridItemSpan(maxLineSpan) }) { ch -> MChannelRow(ch, padX = 0.dp) { onOpen(ch) } }
}

/** Section title inside a padded grid (the grid already has the side margin). */
@Composable
private fun MSectionFlush(title: String) {
    Box(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        com.ipvio.stream.Text(title, color = com.ipvio.stream.Tok.TextHi, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 12.dp))
    }
}

@Composable
private fun NoContent(onAdd: () -> Unit) {
    val ctx = LocalContext.current
    val err = PlaylistStore.error
    val allHidden = err == null && PlaylistStore.langCounts.isNotEmpty() && PlaylistStore.langCounts.keys.all { it in LangFilter.hidden }
    Column {
        MEmpty(
            AIcon.Warning,
            when { err != null -> "Die Playlist konnte nicht geladen werden"; allHidden -> "Alle Sprachen dieser Playlist sind ausgeblendet"; else -> "Noch keine Sender" },
            err,
            if (allHidden) "Alle Sprachen zeigen" else "Playlist hinzufügen"
        ) { if (allHidden) LangFilter.setAndApply(ctx, emptySet()) else onAdd() }
    }
}
