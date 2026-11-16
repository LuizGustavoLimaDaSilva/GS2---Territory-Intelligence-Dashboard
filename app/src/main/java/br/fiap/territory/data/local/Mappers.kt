package br.fiap.territory.data.local

import br.fiap.territory.domain.model.Alert
import br.fiap.territory.domain.model.AlertJustification
import br.fiap.territory.domain.model.AlertLevel
import br.fiap.territory.domain.model.DataSource
import br.fiap.territory.domain.model.Decision
import br.fiap.territory.domain.model.DecisionAction
import br.fiap.territory.domain.model.Indicator
import br.fiap.territory.domain.model.IndicatorType
import br.fiap.territory.domain.model.JustificationItem
import br.fiap.territory.domain.model.Reading
import br.fiap.territory.domain.model.Territory

private const val ITEM_SEPARATOR = "\u001E"
private const val FIELD_SEPARATOR = "\u001F"

fun TerritoryEntity.toDomain() = Territory(id, name, type)

fun IndicatorEntity.toDomain() = Indicator(
    type = IndicatorType.valueOf(type),
    unit = unit,
    warningThreshold = warningThreshold,
    criticalThreshold = criticalThreshold,
    sourceNote = sourceNote,
)

fun ReadingEntity.toDomain() = Reading(
    id = id,
    territoryId = territoryId,
    indicator = IndicatorType.valueOf(indicator),
    timestamp = timestamp,
    value = value,
    origin = DataSource.valueOf(origin),
)

fun Alert.toEntity() = AlertEntity(
    id = id,
    territoryId = territoryId,
    territoryName = territoryName,
    indicator = indicator.name,
    level = level.name,
    title = title,
    summary = summary,
    confidence = confidence,
    variablesText = justification.variables.joinToString(ITEM_SEPARATOR) { "${it.label}$FIELD_SEPARATOR${it.value}" },
    rule = justification.rule,
    period = justification.period,
    source = justification.source,
    forecastPeriod = forecastPeriod,
    createdAt = createdAt,
    validUntil = validUntil,
    origin = origin.name,
)

fun AlertEntity.toDomain() = Alert(
    id = id,
    territoryId = territoryId,
    territoryName = territoryName,
    indicator = IndicatorType.valueOf(indicator),
    level = AlertLevel.valueOf(level),
    title = title,
    summary = summary,
    confidence = confidence,
    justification = AlertJustification(
        variables = variablesText.split(ITEM_SEPARATOR).filter { it.isNotEmpty() }.map {
            val parts = it.split(FIELD_SEPARATOR, limit = 2)
            JustificationItem(parts[0], parts.getOrElse(1) { "" })
        },
        rule = rule,
        period = period,
        source = source,
    ),
    forecastPeriod = forecastPeriod,
    createdAt = createdAt,
    validUntil = validUntil,
    origin = DataSource.valueOf(origin),
)

fun Decision.toEntity() = DecisionEntity(id, alertId, responsible, action.name, note, timestamp)

fun DecisionEntity.toDomain() = Decision(id, alertId, responsible, DecisionAction.valueOf(action), note, timestamp)
