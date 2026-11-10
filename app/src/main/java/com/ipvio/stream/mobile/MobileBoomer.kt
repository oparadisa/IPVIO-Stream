package com.ipvio.stream.mobile

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ipvio.stream.AIcon
import com.ipvio.stream.BRow
import com.ipvio.stream.Boomer
import com.ipvio.stream.PinPad
import com.ipvio.stream.Text

/**
 * Boomer mode on phones and tablets: the same rows as on the TV ([com.ipvio.stream.BoomerHome] builds them), as big touch tiles
 * that scroll sideways, one row under the other (upright and sideways). Leaving needs the PIN: the lock button top right or a long
 * press on the title opens it. Tile and text size follow the Boomer settings.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MobileBoomer(
    rows: List<BRow>, loading: Boolean, askPin: Boolean, pinErr: String?,
    onAskPin: () -> Unit, onPin: (String) -> Unit, onCancel: () -> Unit
) {
    val tk = Boomer.TEXT_K[Boomer.textSize]
    val sk = Boomer.SIZE_K[Boomer.tileSize]
    if (askPin) {
        // The PIN pad is the TV's, laid out with the app scale like the other TV dialogs
        Box(Modifier.fillMaxSize().background(Color(0xFF0B0B10)).padding(16.dp), contentAlignment = Alignment.Center) {
            PinPad("Boomer Mode beenden", "Gib die PIN ein.", pinErr, onPin, onCancel)
        }
        return
    }
    NativeDensity {
        Column(Modifier.fillMaxSize().background(Color(0xFF0B0B10))) {
            Row(Modifier.fillMaxWidth().padding(start = PadX, end = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Was möchtest du sehen?", color = Color.White, fontSize = (26 * tk).sp, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f).combinedClickable(onClick = {}, onLongClick = onAskPin)
                )
                MIconButton(AIcon.Lock, "Boomer Mode beenden", onClick = onAskPin)
            }
            if (rows.isEmpty()) Text(
                if (loading) "Die Inhalte werden geladen …" else "Hier gibt es noch nichts zu sehen. Bitte lass den Bildschirm einrichten.",
                color = Color.White, fontSize = (20 * tk).sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(PadX)
            )
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                items(rows, key = { it.id }) { row ->
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(row.label, color = Color(0xFFD7DAE3), fontSize = (20 * tk).sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = PadX))
                        LazyRow(contentPadding = PaddingValues(horizontal = PadX), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            items(row.tiles.size) { i ->
                                val t = row.tiles[i]
                                BigTile(t.title, t.logo, sk, tk, t.onClick)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Big tile: logo (or the first letters) above the name in large type; at least 150 dp wide. */
@Composable
private fun BigTile(title: String, logo: String?, sk: Float, tk: Float, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier.width((170 * sk).dp).height((170 * sk).dp).clip(shape).background(Color(0xFF1A1B22))
            .clickable(role = Role.Button, onClick = onClick).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (Boomer.logos) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                if (logo != null) AsyncImage(logo, title, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                else Text(raw = true, text = title.take(3).uppercase(), color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.size(8.dp))
        }
        Text(
            raw = true, text = title, color = Color.White, fontSize = (18 * tk).sp, fontWeight = FontWeight.ExtraBold, lineHeight = (22 * tk).sp,
            textAlign = TextAlign.Center, maxLines = if (Boomer.logos) 2 else 5, overflow = TextOverflow.Ellipsis,
            modifier = if (Boomer.logos) Modifier else Modifier.weight(1f)
        )
    }
}
