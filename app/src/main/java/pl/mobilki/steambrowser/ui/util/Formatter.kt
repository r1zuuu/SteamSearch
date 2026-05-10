package pl.mobilki.steambrowser.ui.util

import java.text.NumberFormat
import java.util.Locale

fun playersText(players: Int?): String {
    if (players == null || players == 0) return "Brak danych o graczach"
    return "${NumberFormat.getIntegerInstance(Locale("pl", "PL")).format(players)} graczy teraz"
}
