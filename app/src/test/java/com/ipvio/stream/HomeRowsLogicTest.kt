package com.ipvio.stream

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Row data of the home screen (moved out of HomeScreen.kt unchanged). No metadata is cached in these tests. */
class HomeRowsLogicTest {
    private fun item(id: String, kind: Kind, title: String, group: String, year: Int? = null, lang: String = LangTagLogic.UNKNOWN) =
        Item(id, kind, title, year, "http://x/$id", null, group, lang = lang)

    private val list = listOf(
        item("l1", Kind.LIVE, "Das Erste", "DE News"),
        item("l2", Kind.LIVE, "BBC One", "UK"),
        item("m1", Kind.MOVIE, "Heat", "Action", 1995),
        item("m2", Kind.MOVIE, "Heat", "Action 4K", 1995),          // same movie in another version: shown once
        item("m3", Kind.MOVIE, "Heat", "Action", 1995, lang = "EN"), // other language: kept
        item("m4", Kind.MOVIE, "Alien", "Horror", 1979),
        item("s1", Kind.SERIES, "Dark", "Drama")
    )

    @After fun reset() { PlaylistStore.catOrder = emptyMap() }

    @Test fun splitsByKindAndShowsEachMovieVersionOnce() {
        val d = buildHome(list, false, emptySet())
        assertEquals(listOf("l1", "l2"), d.live.map { it.id })
        assertEquals(listOf("m1", "m3", "m4"), d.movies.map { it.id })
        assertEquals(listOf("s1"), d.series.map { it.id })
        assertEquals(list.size, d.byId.size)
        assertTrue(d.trendItems.isEmpty())
    }

    @Test fun providerGroupsKeepPlaylistOrderOrFollowCategoryOrder() {
        assertEquals(listOf("Action", "Horror"), buildHome(list, false, emptySet()).movieP.keys.toList())
        PlaylistStore.catOrder = mapOf("Horror" to 0, "Action" to 1)
        val d = buildHome(list, false, emptySet())
        assertEquals(listOf("Horror", "Action"), d.movieP.keys.toList())
        assertEquals(listOf("DE News", "UK"), d.liveP.keys.toList())   // groups missing from the order keep their place
        // Without genre grouping the genre rows are the provider groups
        assertEquals(d.movieP, d.movieG)
        assertEquals(d.seriesP, d.seriesG)
    }

    @Test fun trendMatchesNormalizedTitlesOfMoviesAndSeries() {
        val d = buildHome(list, false, setOf(Trending.norm("ALIEN"), Trending.norm("Dark"), Trending.norm("Das Erste")))
        assertEquals(listOf("m4", "s1"), d.trendItems.map { it.id })   // live channels never trend
    }

    @Test fun topRatedNeedsMetadata() {
        assertTrue(topRated(list).isEmpty())
    }

    @Test fun cacheHitsOnlyForTheSameInputs() {
        val order = PlaylistStore.catOrder
        val trend = emptySet<String>()
        val d = buildHome(list, false, trend)
        HomeCache.put(list, order, false, 3, trend, d)
        assertSame(d, HomeCache.get(list, order, false, 3, trend))
        assertNull(HomeCache.get(list.toList(), order, false, 3, trend))   // another list object
        assertNull(HomeCache.get(list, order, true, 3, trend))
        assertNull(HomeCache.get(list, order, false, 4, trend))
        assertSame(d, HomeCache.last())
    }
}
