package com.ipvio.stream.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipvio.stream.Accent1
import com.ipvio.stream.AppIcon
import com.ipvio.stream.CATS
import com.ipvio.stream.SettingsNav
import com.ipvio.stream.SettingsPageContent
import com.ipvio.stream.Text
import com.ipvio.stream.Tok

/**
 * Settings on wide tablets (fork): the areas as a list on the left, the chosen page on the right.
 * Each side scrolls on its own. The pages are the TV's ([SettingsPageContent]); phones keep [com.ipvio.stream.SettingsHub].
 */
@Composable
internal fun MobileSettingsPanes(onAdd: () -> Unit, reload: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    LaunchedEffect(SettingsNav.openPage) {
        val p = SettingsNav.openPage
        if (p >= 0) { SettingsNav.openPage = -1; page = p }
    }
    val pageState = rememberLazyListState()
    LaunchedEffect(page) { pageState.scrollToItem(0) }
    Row(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.width(listPaneWidth()).fillMaxHeight(), contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 24.dp)) {
            item { Text("Einstellungen", color = Tok.TextHi, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 12.dp)) }
            itemsIndexed(CATS) { i, cat ->
                val sel = i == page
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(12.dp))
                        .background(if (sel) Tok.CardFocus else Color.Transparent)
                        .clickable { page = i }.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppIcon(cat.icon, if (sel) Accent1 else Tok.TextLow, size = 22.dp)
                    Column(Modifier.weight(1f)) {
                        Text(cat.title, color = Tok.TextHi, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(cat.sub, color = Tok.TextLow, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        LazyColumn(Modifier.weight(1f).fillMaxHeight(), state = pageState, contentPadding = PaddingValues(start = 8.dp, end = PadX, bottom = 24.dp)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(CATS[page].title, color = Tok.TextHi, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 16.dp))
                    // Pages that end with "back to the list" (profiles, parental control, Boomer mode) go back to the first area
                    SettingsPageContent(page, { page = it.coerceAtLeast(0) }, onAdd, reload)
                }
            }
        }
    }
}
