package com.ipvio.stream

// Row data of the home screen (Home, Movies, Series), moved unchanged out of HomeScreen.kt so the phone/tablet
// home can reuse it (fork). Only the visibility changed (private -> internal). Covered by HomeRowsLogicTest.

internal fun ipvioGroups(list: List<Item>): Map<String, List<Item>> {
    val map = LinkedHashMap<String, ArrayList<Item>>()
    for (item in list) {
        val m = MetaRepo.cached(item)
        val cats = if (m != null) Genres.categoriesOf(m) else emptyList()
        if (cats.isEmpty()) map.getOrPut("Sonstige") { ArrayList() }.add(item)
        else for (c in cats) map.getOrPut(c) { ArrayList() }.add(item)
    }
    val prio = listOf("Anime", "Animation")
    val sorted = map.entries.sortedWith(
        compareBy<Map.Entry<String, ArrayList<Item>>>(
            { if (it.key in prio) prio.indexOf(it.key) else if (it.key == "Sonstige") 1000 else 10 },
            { -it.value.size }
        )
    )
    return sorted.associate { it.key to it.value }
}

internal fun providerGroups(list: List<Item>): Map<String, List<Item>> {
    val g = list.groupBy { it.group }
    val order = PlaylistStore.catOrder
    if (order.isEmpty()) return g
    return g.entries.sortedBy { order[it.key] ?: 1_000_000 }.associate { it.key to it.value }
}

/**
 * Everything the home screen derives from the playlist. Computed on a background thread (with 100,000+ titles
 * this takes noticeable time); the UI shows the previous state or the loading skeleton meanwhile.
 */
internal class HomeData(
    val live: List<Item>, val movies: List<Item>, val series: List<Item>,
    val liveP: Map<String, List<Item>>, val movieP: Map<String, List<Item>>, val seriesP: Map<String, List<Item>>,
    val movieG: Map<String, List<Item>>, val seriesG: Map<String, List<Item>>,
    val topM: List<Item>, val topS: List<Item>, val byId: Map<String, Item>, val trendItems: List<Item>,
    val genresOk: Boolean
)

internal fun buildHome(all: List<Item>, ipvioWanted: Boolean, trend: Set<String>): HomeData {
    val live = ArrayList<Item>(); val movies0 = ArrayList<Item>(); val series = ArrayList<Item>()
    for (i in all) when (i.kind) { Kind.LIVE -> live.add(i); Kind.MOVIE -> movies0.add(i); Kind.SERIES -> series.add(i) }
    // Show the same movie in several versions (HD, 4K, …) only once; the version is chosen on the detail page (source)
    val movies = PlaylistLogic.firstPerKey(movies0) { it.mk + "|" + it.lang }
    val genresOk = MetaRepo.enabled() && Classifier.supported(movies.size + series.size)
    val ipvio = ipvioWanted && genresOk
    val movieP = providerGroups(movies)
    val seriesP = providerGroups(series)
    val trendItems = if (trend.isEmpty()) emptyList() else (movies.asSequence() + series.asSequence()).filter { Trending.norm(it.title) in trend }.toList()
    return HomeData(
        live, movies, series, providerGroups(live), movieP, seriesP,
        if (ipvio) ipvioGroups(movies) else movieP, if (ipvio) ipvioGroups(series) else seriesP,
        topRated(movies), topRated(series), all.associateBy { it.id }, trendItems, genresOk
    )
}

/** Filtered lists per area, so the same list object comes back and [HomeCache] can recognize it (no full recompute on every tab switch). */
internal object AreaCache {
    private val m = HashMap<String, Pair<List<Item>, List<Item>>>()
    @Synchronized fun get(all: List<Item>, tab: Int, k: Int): List<Item> {
        val key = "$tab|$k"
        m[key]?.let { if (it.first === all) return it.second }
        return AreaSource.filter(all, tab, k).also { m[key] = all to it }
    }
}

/** Last result: when returning to the home screen (e.g. from the detail page) nothing has to be recomputed. */
internal object HomeCache {
    private var all: List<Item>? = null
    private var order: Map<String, Int>? = null
    private var want = false
    private var ver = -1
    private var trend: Set<String>? = null
    private var data: HomeData? = null

    @Synchronized fun get(all: List<Item>, order: Map<String, Int>, want: Boolean, ver: Int, trend: Set<String>): HomeData? =
        if (all === this.all && order === this.order && want == this.want && ver == this.ver && trend === this.trend) data else null

    @Synchronized fun put(all: List<Item>, order: Map<String, Int>, want: Boolean, ver: Int, trend: Set<String>, d: HomeData) {
        this.all = all; this.order = order; this.want = want; this.ver = ver; this.trend = trend; data = d
    }

    /** For the wait after a change: better show the previous state than an empty skeleton. */
    @Synchronized fun last(): HomeData? = data
}

internal val EMPTY_HOME = HomeData(
    emptyList(), emptyList(), emptyList(), emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap(),
    emptyList(), emptyList(), emptyMap(), emptyList(), false
)

internal fun topRated(list: List<Item>): List<Item> =
    list.mapNotNull { i ->
        val m = MetaRepo.cached(i)
        val sc = m?.score()
        if (m != null && (m.votes >= 300 || m.tmdbId <= 0) && sc != null) i to sc else null
    }.sortedByDescending { it.second }.map { it.first }
