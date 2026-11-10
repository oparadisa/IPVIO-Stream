package com.ipvio.stream.mobile

import com.ipvio.stream.DictA
import com.ipvio.stream.DictB
import com.ipvio.stream.DictC
import com.ipvio.stream.DictD
import com.ipvio.stream.DictE
import com.ipvio.stream.DictF
import com.ipvio.stream.DictG
import com.ipvio.stream.DictH
import com.ipvio.stream.DictI
import com.ipvio.stream.DictJ
import com.ipvio.stream.DictK
import com.ipvio.stream.I18nCore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every phone/tablet text has all 9 languages of DictMobile plus Dutch, and the texts the mobile screens build are translated. */
class DictMobileTest {
    private fun rows(block: String) = block.lineSequence().filter { it.contains('¦') }.map { it.split('¦') }.toList()

    @Test fun allLanguagesPresent() {
        val main = rows(DictMobile)
        assertTrue(main.isNotEmpty())
        for (r in main) assertEquals("columns: " + r[0], 9, r.count { it.isNotBlank() })
        val nl = rows(DictNlMobile).associate { it[0] to it[1] }
        for (r in main) assertTrue("Dutch missing: " + r[0], !nl[r[0]].isNullOrBlank())
    }

    /** Texts of the mobile screens (fixed ones and the composed ones with numbers / titles), as the app hands them to Lang.tr. */
    private val screenTexts = listOf(
        "Mehr", "Zurück", "Suche", "Profil", "Alle anzeigen", "Weiterschauen", "Meine Sender", "Kategorien", "Top bewertet",
        "Neu hinzugefügt", "Alle Filme", "Alle Serien", "Beliebte Filme", "Beliebte Serien", "Zuletzt geschaut", "Top 10 heute",
        "Neu hinzugefügt · Filme", "Top bewertet · Serien", "Favoriten · Sender", "Meine Liste · Filme", "Verlauf · Serien",
        "Noch keine Favoriten", "Sender lange drücken, bei Filmen und Serien auf der Detailseite „Zu Favoriten“ wählen.",
        "Noch nichts in deiner Liste", "Auf der Seite eines Films oder einer Serie „Meine Liste“ wählen.", "Filme entdecken",
        "Noch nichts angesehen", "Hier erscheinen die zuletzt gestarteten Filme und Serien.", "Zu Live TV",
        "Keine Filme in dieser Playlist", "Diese Playlist enthält keine Filme oder Serien.", "Noch keine Sender", "Playlist hinzufügen",
        "Sender suchen", "TV-Guide", "Schließen", "Alle", "Favoriten", "12 Sender", "3 Treffer", "Keine Treffer",
        "Zu Favoriten hinzugefügt", "Aus Favoriten entfernt",
        "Anbieter-Kategorien", "Auto-Genres", "Keine Kategorien",
        "Titel, Sender, Schauspieler …", "Zuletzt gesucht", "Filme · 12", "Serien · 3",
        "Abspielen", "Erneut abspielen", "Fortsetzen", "Von vorn", "Fortsetzen · S1 E2", "Weiter mit · S1 E2", "noch 5 Min",
        "Meine Liste", "In Meiner Liste", "Zu Favoriten", "Favorit", "Als gesehen markieren", "Als ungesehen markieren",
        "Aus „Zuletzt geschaut“ entfernen", "Regie", "Folgen", "Staffel 2", "Besetzung", "Ähnliche Titel in deiner Playlist",
        "Folgen werden geladen …", "Die Folgen konnten nicht geladen werden. Verbindung oder Zugang prüfen.", "Erneut versuchen",
        "Keine Beschreibung gefunden.", "1 Staffel", "3 Staffeln",
        "Audio & Untertitel", "Einstellungen", "Hochformat", "Querformat", "Vorheriger Sender", "Nächster Sender",
        "Vorherige Folge", "Nächste Folge", "10 Sekunden zurück", "10 Sekunden vor", "Senderliste",
        "Zurück schließt das Menü", "Zurück = vorheriges Menü", "Tippen = nächster Wert. Die Vorschau zeigt das Ergebnis.",
        "Gilt für die Sprungtasten, Doppeltippen und die Zeitleiste.", "Zurück mit der Zurück-Geste.", "Tippen = ansehen",
        "Helligkeit", "Lautstärke", "Drehung sperren", "Drehung entsperren"
    )

    @Test fun mobileScreenTextsAreTranslated() {
        I18nCore.load(DictA, DictB, DictC, DictD, DictE, DictF, DictG, DictH, DictI, DictJ, DictK, DictMobile)
        val missing = screenTexts.filter { I18nCore.translate(it, 1) == it }
        assertEquals("not translated to English", emptyList<String>(), missing)
    }
}
