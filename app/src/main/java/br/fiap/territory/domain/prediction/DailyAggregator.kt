package br.fiap.territory.domain.prediction

import br.fiap.territory.domain.format.PtBr
import br.fiap.territory.domain.model.Aggregation
import br.fiap.territory.domain.model.Reading
import java.time.LocalDate

data class DailyValue(val date: LocalDate, val value: Double, val lastTimestamp: Long)

/** Resume leituras (horárias ou diárias) em um valor por dia, no fuso de São Paulo. */
object DailyAggregator {
    fun aggregate(readings: List<Reading>, aggregation: Aggregation): List<DailyValue> =
        readings
            .groupBy { PtBr.localDate(it.timestamp) }
            .map { (date, items) ->
                val values = items.map { it.value }
                val value = when (aggregation) {
                    Aggregation.DAILY_MAX -> values.max()
                    Aggregation.DAILY_MEAN -> values.average()
                }
                DailyValue(date, value, items.maxOf { it.timestamp })
            }
            .sortedBy { it.date }
}
