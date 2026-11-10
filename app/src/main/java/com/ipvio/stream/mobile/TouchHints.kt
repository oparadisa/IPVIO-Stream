package com.ipvio.stream.mobile

/** Remote-key hints of the reused TV player panels, reworded for touch (fork). Keys are the German source texts. */
object TouchHints {
    private const val CLOSE = "Zurück schließt das Menü"
    private val map = mapOf(
        "Links schließt das Menü  ·  Rechts wechselt zu den Untertiteln" to CLOSE,
        "Rechts / Vorspulen = 50 weiter · Zurückspulen = zurück · Links schließt" to CLOSE,
        "Links schließt das Menü" to CLOSE,
        "Links = zurück" to "Zurück = vorheriges Menü",
        "OK = nächster Wert. Die Vorschau zeigt das Ergebnis." to "Tippen = nächster Wert. Die Vorschau zeigt das Ergebnis.",
        "Gilt für Links/Rechts, Halten und die Zeitleiste." to "Gilt für die Sprungtasten, Doppeltippen und die Zeitleiste.",
        "Zurück mit der Zurück-Taste." to "Zurück mit der Zurück-Geste.",
        "OK = ansehen  ·  halten = Favorit  ·  Links = Kategorien" to "Tippen = ansehen",
        "Links/Rechts = Spalte  ·  Zurück schließt" to CLOSE
    )

    fun of(s: String): String = map[s] ?: s
}
