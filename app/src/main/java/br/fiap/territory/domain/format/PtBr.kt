package br.fiap.territory.domain.format

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Formatação em português do Brasil, sem depender de classes do Android. */
object PtBr {
    val zone: ZoneId = ZoneId.of("America/Sao_Paulo")
    private val locale: Locale = Locale.forLanguageTag("pt-BR")
    private val dateTimeFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", locale)
    private val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy", locale)
    private val shortDateFormat = DateTimeFormatter.ofPattern("dd/MM", locale)

    fun number(value: Double, decimals: Int = 1): String = String.format(locale, "%.${decimals}f", value)

    fun signed(value: Double, decimals: Int = 2): String = String.format(locale, "%+.${decimals}f", value)

    fun percent(fraction: Double): String = "${Math.round(fraction * 100)}%"

    fun dateTime(millis: Long): String = dateTimeFormat.format(Instant.ofEpochMilli(millis).atZone(zone))

    fun date(millis: Long): String = dateFormat.format(Instant.ofEpochMilli(millis).atZone(zone))

    fun shortDate(date: LocalDate): String = shortDateFormat.format(date)

    fun localDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    /** "há 5 min", "há 3 h", "há 2 dias". */
    fun ago(fromMillis: Long, nowMillis: Long): String {
        val minutes = ((nowMillis - fromMillis).coerceAtLeast(0)) / 60_000
        return when {
            minutes < 1 -> "agora mesmo"
            minutes < 60 -> "há $minutes min"
            minutes < 60 * 24 -> "há ${minutes / 60} h"
            else -> {
                val days = minutes / (60 * 24)
                if (days == 1L) "há 1 dia" else "há $days dias"
            }
        }
    }

    fun confidenceLabel(confidence: Double): String = when {
        confidence >= 0.75 -> "alta"
        confidence >= 0.5 -> "média"
        else -> "baixa"
    }
}
