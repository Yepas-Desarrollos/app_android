package mx.checklist.util

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Formatea una fecha ISO8601 como tiempo relativo
 * Ejemplos: "Hace 2 min", "Hace 1 hora", "Hace 3 días"
 */
fun formatRelativeTime(dateString: String): String {
    return try {
        val instant = Instant.parse(dateString)
        val now = Instant.now()
        
        val minutes = ChronoUnit.MINUTES.between(instant, now)
        val hours = ChronoUnit.HOURS.between(instant, now)
        val days = ChronoUnit.DAYS.between(instant, now)
        
        when {
            minutes < 1 -> "Ahora"
            minutes < 60 -> "Hace $minutes min"
            hours < 24 -> "Hace $hours hora${if (hours > 1) "s" else ""}"
            days < 7 -> "Hace $days día${if (days > 1) "s" else ""}"
            days < 30 -> "Hace ${days / 7} semana${if (days / 7 > 1) "s" else ""}"
            else -> formatShortDate(dateString)
        }
    } catch (e: Exception) {
        dateString // Retornar original si falla el parseo
    }
}

/**
 * Formatea fecha corta: "9 Dic 2025"
 */
fun formatShortDate(dateString: String): String {
    return try {
        val instant = Instant.parse(dateString)
        val date = instant.atZone(ZoneId.systemDefault()).toLocalDate()
        val months = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", 
                           "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
        "${date.dayOfMonth} ${months[date.monthValue - 1]} ${date.year}"
    } catch (e: Exception) {
        dateString
    }
}
