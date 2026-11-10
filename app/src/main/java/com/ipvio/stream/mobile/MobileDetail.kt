package com.ipvio.stream.mobile

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ipvio.stream.AIcon
import com.ipvio.stream.Accent1
import com.ipvio.stream.AppIcon
import com.ipvio.stream.Bg
import com.ipvio.stream.Credits
import com.ipvio.stream.CreditsRepo
import com.ipvio.stream.EpMeta
import com.ipvio.stream.EpProgress
import com.ipvio.stream.Episode
import com.ipvio.stream.History
import com.ipvio.stream.Item
import com.ipvio.stream.Kind
import com.ipvio.stream.Lang
import com.ipvio.stream.LazySeries
import com.ipvio.stream.LibMatch
import com.ipvio.stream.Library
import com.ipvio.stream.MetaRepo
import com.ipvio.stream.PersonRef
import com.ipvio.stream.PlaybackPrefs
import com.ipvio.stream.ResumeLogic
import com.ipvio.stream.Surf2
import com.ipvio.stream.Text
import com.ipvio.stream.Tok
import com.ipvio.stream.VariantPref
import com.ipvio.stream.WatchStore
import com.ipvio.stream.openTrailer
import com.ipvio.stream.pickVariant
import com.ipvio.stream.qualityOf
import com.ipvio.stream.seriesCountText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Movie / series page on phones and tablets (fork): scene image, title, Play/Resume, My list, Favorite, Trailer,
 * description; series: season chips and the episode list (tap = play, long press = watched / not watched);
 * cast and similar titles. Data and actions are the TV page's ([com.ipvio.stream.DetailScreen]).
 */
@Composable
fun MobileDetail(
    item: Item, all: List<Item>, onPlay: (String, String, Long) -> Unit,
    onPerson: (PersonRef) -> Unit, onOpenItem: (Item) -> Unit, onBack: () -> Unit
) = NativeDensity {
    val ctx = LocalContext.current
    BackHandler(onBack = onBack)
    // Series with a placeholder: load the episodes first
    var tryNo by remember(item.id) { mutableIntStateOf(0) }
    var full by remember(item.id) { mutableStateOf(LazySeries.full(item).takeIf { it !== item || !LazySeries.isStub(item) }) }
    var failed by remember(item.id) { mutableStateOf(false) }
    if (full == null) {
        LaunchedEffect(item.id, tryNo) { failed = false; LazySeries.load(ctx, item)?.let { full = it } ?: run { failed = true } }
        Column(Modifier.fillMaxSize()) {
            MTopBar(item.title, raw = true, onBack = onBack)
            if (!failed) MLoading("Folgen werden geladen …")
            else MEmpty(AIcon.Warning, "Die Folgen konnten nicht geladen werden. Verbindung oder Zugang prüfen.", action = "Erneut versuchen") { tryNo++ }
        }
        return@NativeDensity
    }
    DetailBody(full ?: item, all, onPlay, onPerson, onOpenItem, onBack)
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun DetailBody(
    item: Item, all: List<Item>, onPlay: (String, String, Long) -> Unit,
    onPerson: (PersonRef) -> Unit, onOpenItem: (Item) -> Unit, onBack: () -> Unit
) {
    val ctx = LocalContext.current
    var meta by remember(item.id) { mutableStateOf(MetaRepo.cached(item)) }
    var metaDone by remember(item.id) { mutableStateOf(!MetaRepo.usable(item)) }
    LaunchedEffect(item.id) { MetaRepo.extended(ctx, item)?.let { meta = it }; metaDone = true }
    val credits by produceState<Credits?>(null, meta?.tmdbId) {
        val m = meta
        value = if (m != null && m.tmdbId > 0) CreditsRepo.credits(ctx, m) else null
    }
    val similar by produceState(emptyList<Item>(), credits, all) {
        val c = credits ?: return@produceState
        value = withContext(Dispatchers.Default) { LibMatch.matches(all, c.similar, 30).filter { it.id != item.id } }
    }
    val isSeries = item.kind == Kind.SERIES
    val seasons = remember(item) { item.episodes.map { it.season }.distinct().sorted() }
    val resume = WatchStore.entries.firstOrNull { it.itemId == item.id }
    val resumeSE = remember(resume) { resume?.let { r -> if (r.season > 0 || r.ep > 0) r.season to r.ep else ResumeLogic.seasonEp(r.title) } }
    var season by remember(item.id) { mutableIntStateOf(resumeSE?.first?.takeIf { it in seasons } ?: seasons.firstOrNull() ?: 1) }
    val eps = remember(item, season) { item.episodes.filter { it.season == season }.sortedBy { it.num } }
    var progVer by remember(item.id) { mutableIntStateOf(0) }
    val progress = remember(item.id, progVer) { EpProgress.of(ctx, item.id).toMap() }
    val pref = remember(item.id) { VariantPref.get(ctx, item.id) }
    val cap = remember { PlaybackPrefs.cap(ctx) }
    fun pick(ep: Episode) = pickVariant(ep, pref.first, pref.second, pref.third, cap)
    fun playEp(ep: Episode, pos: Long = 0L) = onPlay(pick(ep).url, item.title + " S" + ep.season + "E" + ep.num, pos)
    val m = meta
    val tmdbId = m?.tmdbId ?: 0
    val epMetas by produceState(emptyList<EpMeta>(), tmdbId, season) {
        value = emptyList()
        if (isSeries && m?.isTv == true && tmdbId > 0) value = runCatching { MetaRepo.season(tmdbId, season, m.imdbId) }.getOrDefault(emptyList())
    }
    var descOpen by remember(item.id) { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item(key = "hero") {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Surf2)) {
                (m?.backdrop ?: m?.poster)?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color(0x66000000), 0.35f to Color.Transparent, 1f to Bg)))
                Box(Modifier.padding(4.dp)) { MIconButton(AIcon.Left, "Zurück", onClick = onBack) }
            }
        }
        item(key = "head") {
            Column(Modifier.padding(horizontal = PadX), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(item.title, raw = true, color = Tok.TextHi, fontSize = 26.sp, lineHeight = 30.sp, fontWeight = FontWeight.ExtraBold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                val line = listOfNotNull(
                    m?.year ?: item.year?.toString(),
                    m?.runtime?.takeIf { it > 0 && !isSeries }?.let { if (it >= 60) "${it / 60}h ${it % 60}min" else "$it min" },
                    if (isSeries && seasons.isNotEmpty()) Lang.tr(seriesCountText(item)) else null,
                    m?.score()?.let { "★ " + String.format(java.util.Locale.US, "%.1f", it) },
                    qualityOf(item)
                ).joinToString("  ·  ")
                if (line.isNotEmpty()) Text(line, raw = true, color = Tok.TextLow, fontSize = 14.sp)
                if (m != null && m.genres.isNotEmpty()) Text(m.genres.take(3).joinToString(", "), raw = true, color = Tok.TextLow, fontSize = 14.sp)
                // Movie started: progress with the remaining time
                if (!isSeries && resume != null && resume.dur > 0 && !ResumeLogic.finished(resume.pos, resume.dur)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.width(140.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Tok.Line)) {
                            Box(Modifier.fillMaxWidth((resume.pos.toFloat() / resume.dur).coerceIn(0f, 1f)).fillMaxHeight().background(Accent1))
                        }
                        Text("noch " + ((resume.dur - resume.pos) / 60000).coerceAtLeast(1) + " Min", color = Tok.TextLow, fontSize = 13.sp)
                    }
                }
                // Play / Resume across the full width, From start next to it when there is a resume point
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (resume != null) {
                        val label = when {
                            !isSeries -> "Fortsetzen"
                            resumeSE == null -> "Fortsetzen"
                            else -> (if (resume.next) "Weiter mit" else "Fortsetzen") + " · S${resumeSE.first} E${resumeSE.second}"
                        }
                        MButton(label, AIcon.Play, primary = true, modifier = Modifier.weight(1f)) {
                            val t = WatchStore.target(ctx, item, resume); onPlay(t.second, t.first, resume.pos)
                        }
                        MButton("Von vorn", AIcon.Rewind) { if (isSeries) eps.firstOrNull()?.let { playEp(it) } else onPlay(item.url, item.title, 0L) }
                    } else {
                        val seen = !isSeries && progress[ResumeLogic.key(0, 0)]?.done == true
                        MButton(if (seen) "Erneut abspielen" else "Abspielen", AIcon.Play, primary = true, modifier = Modifier.weight(1f)) {
                            if (isSeries) eps.firstOrNull()?.let { playEp(it) } else onPlay(item.url, item.title, 0L)
                        }
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val inList = Library.isWatch(item.id)
                    MButton(if (inList) "In Meiner Liste" else "Meine Liste", if (inList) AIcon.Check else AIcon.Plus) { Library.toggleWatch(ctx, item.id) }
                    val fav = Library.isFav(item.id)
                    MButton(if (fav) "Favorit" else "Zu Favoriten", if (fav) AIcon.HeartFilled else AIcon.Heart) { Library.toggleFav(ctx, item.id) }
                    if (m?.trailerKey != null) MButton("Trailer", AIcon.Play) { openTrailer(ctx, m.trailerKey, item.title) }
                    if (!isSeries) {
                        val k = ResumeLogic.key(0, 0)
                        val seen = progress[k]?.done == true
                        MButton(if (seen) "Als ungesehen markieren" else "Als gesehen markieren", if (seen) AIcon.Rewind else AIcon.Check) {
                            EpProgress.mark(ctx, item.id, listOf(k), !seen)
                            if (!seen && resume != null) WatchStore.remove(ctx, item.id)
                            progVer++
                        }
                    }
                    if (item.id in History.ids) MButton("Aus „Zuletzt geschaut“ entfernen", AIcon.Close) { History.remove(ctx, item.id) }
                }
                val ov = m?.overview
                when {
                    ov != null -> Text(
                        ov, raw = true, color = Tok.TextMid, fontSize = 15.sp, lineHeight = 22.sp,
                        maxLines = if (descOpen) Int.MAX_VALUE else 5, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { descOpen = !descOpen }
                    )
                    metaDone -> Text(if (MetaRepo.usable(item)) "Keine Beschreibung gefunden." else "Tipp: Unter Einstellungen > API-Schlüssel einen eigenen TMDB-Schlüssel eintragen, dann erscheinen hier Beschreibung, Cover und Bewertungen.", color = Tok.TextLow, fontSize = 14.sp)
                }
                credits?.directors?.takeIf { it.isNotEmpty() }?.let { ds ->
                    Row(Modifier.clickable { ds.first().let { d -> onPerson(PersonRef(d.id, d.name, d.photo, item.id)) } }, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Regie", color = Tok.TextLow, fontSize = 14.sp)
                        Text(ds.take(3).joinToString(", ") { it.name }, raw = true, color = Tok.TextMid, fontSize = 14.sp)
                    }
                }
            }
        }
        if (isSeries) {
            if (seasons.size > 1) item(key = "seasons") {
                Column(Modifier.padding(top = 16.dp)) {
                    MChipRow(seasons, { "Staffel $it" }, { it == season }) { season = it }
                }
            } else item(key = "eph") { MSection("Folgen") }
            items(eps, key = { "e${it.season}_${it.num}_${it.url.hashCode()}" }) { ep ->
                val k = ResumeLogic.key(ep.season, ep.num)
                val p = progress[k]
                val em = epMetas.firstOrNull { it.num == ep.num }
                EpisodeRowM(ep, em, p, onLong = {
                    val seen = p?.done == true
                    EpProgress.mark(ctx, item.id, listOf(k), !seen)
                    if (!seen && resumeSE?.let { ResumeLogic.key(it.first, it.second) } == k) WatchStore.remove(ctx, item.id)
                    progVer++
                    Toast.makeText(ctx, Lang.tr(if (seen) "Als ungesehen markieren" else "Als gesehen markieren"), Toast.LENGTH_SHORT).show()
                }) { playEp(ep) }
            }
        }
        credits?.cast?.takeIf { it.isNotEmpty() }?.let { cast ->
            item(key = "casth") { MSection("Besetzung") }
            item(key = "cast") {
                LazyRow(contentPadding = PaddingValues(horizontal = PadX), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(cast.take(30), key = { it.id }) { p ->
                        Column(
                            Modifier.width(84.dp).clip(RoundedCornerShape(8.dp)).clickable { onPerson(PersonRef(p.id, p.name, p.photo, item.id)) },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(Modifier.size(72.dp).clip(CircleShape).background(Surf2), contentAlignment = Alignment.Center) {
                                if (p.photo != null) AsyncImage(p.photo, p.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                else AppIcon(AIcon.Person, Tok.TextLow, size = 28.dp)
                            }
                            Text(p.name, raw = true, color = Tok.TextHi, fontSize = 12.sp, maxLines = 2, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                            p.role?.let { Text(it, raw = true, color = Tok.TextLow, fontSize = 11.sp, maxLines = 1, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis) }
                        }
                    }
                }
            }
        }
        if (similar.isNotEmpty()) {
            item(key = "simh") { MSection("Ähnliche Titel in deiner Playlist") }
            item(key = "sim") { MPosterRow(similar, onOpenItem) }
        }
        if (MetaRepo.enabled()) item(key = "tmdb") {
            Text("Dieses Produkt verwendet die TMDB-API, wird aber nicht von TMDB unterstützt oder zertifiziert.", color = Tok.TextLow, fontSize = 12.sp, modifier = Modifier.padding(horizontal = PadX, vertical = 24.dp))
        }
    }
}

/** One episode: still image (when known), number and title, runtime, progress / watched mark. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EpisodeRowM(ep: Episode, em: EpMeta?, p: ResumeLogic.EpPos?, onLong: () -> Unit, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp).combinedClickable(onLongClick = onLong, onClick = onClick).padding(horizontal = PadX, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.width(112.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp)).background(Surf2), contentAlignment = Alignment.Center) {
            if (em?.still != null) AsyncImage(em.still, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else AppIcon(AIcon.Play, Tok.TextLow, size = 22.dp)
            if (p != null && !p.done && p.fraction > 0f) Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp).background(Color(0x66000000))) {
                Box(Modifier.fillMaxWidth(p.fraction).fillMaxHeight().background(Accent1))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val name = em?.name?.takeIf { it.isNotBlank() } ?: ep.title.takeIf { it.isNotBlank() }
            Text("${ep.num}. " + (name ?: ""), raw = true, color = Tok.TextHi, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val sub = listOfNotNull(em?.runtime?.takeIf { it > 0 }?.let { "$it Min" }, em?.air).joinToString("  ·  ")
            if (sub.isNotEmpty()) Text(sub, raw = true, color = Tok.TextLow, fontSize = 13.sp, maxLines = 1)
        }
        if (p?.done == true) AppIcon(AIcon.Check, Accent1, size = 20.dp)
    }
}
