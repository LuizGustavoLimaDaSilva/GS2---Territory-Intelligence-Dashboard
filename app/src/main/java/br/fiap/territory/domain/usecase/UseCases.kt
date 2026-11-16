package br.fiap.territory.domain.usecase

import br.fiap.territory.domain.model.Alert
import br.fiap.territory.domain.model.AlertDetail
import br.fiap.territory.domain.model.AlertLevel
import br.fiap.territory.domain.model.Decision
import br.fiap.territory.domain.model.DecisionAction
import br.fiap.territory.domain.model.HistoryItem
import br.fiap.territory.domain.model.IndicatorType
import br.fiap.territory.domain.model.TerritoryEvaluation
import br.fiap.territory.domain.notification.AlertNotifier
import br.fiap.territory.domain.repository.AlertRepository
import br.fiap.territory.domain.repository.DecisionRepository
import br.fiap.territory.domain.repository.IndicatorRepository
import br.fiap.territory.domain.repository.PreferencesRepository
import br.fiap.territory.domain.repository.ReadingRepository
import br.fiap.territory.domain.repository.TerritoryRepository
import br.fiap.territory.domain.repository.TimeProvider
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach

/**
 * Observa as leituras do território, avalia indicadores/alertas e guarda os alertas no banco.
 * No modo demonstração, o "agora" é a última leitura (e não o relógio do aparelho).
 */
class ObserveTerritoryEvaluationUseCase @Inject constructor(
    private val territories: TerritoryRepository,
    private val indicators: IndicatorRepository,
    private val readings: ReadingRepository,
    private val alerts: AlertRepository,
    private val evaluator: TerritoryEvaluator,
    private val time: TimeProvider,
) {
    operator fun invoke(territoryId: String, demoMode: Boolean): Flow<TerritoryEvaluation> =
        combine(
            territories.observeAll(),
            indicators.observeAll(),
            readings.observe(territoryId, IndicatorType.ENERGY),
            readings.observe(territoryId, IndicatorType.WATER),
            readings.observe(territoryId, IndicatorType.CLIMATE),
        ) { allTerritories, allIndicators, energy, water, climate ->
            val latest = (energy + water).maxOfOrNull { it.timestamp }
            val now = if (demoMode && latest != null) latest else time.now()
            evaluator.evaluate(
                territory = allTerritories.firstOrNull { it.id == territoryId },
                indicators = allIndicators,
                readings = mapOf(
                    IndicatorType.ENERGY to energy,
                    IndicatorType.WATER to water,
                    IndicatorType.CLIMATE to climate,
                ),
                nowMillis = now,
                demoMode = demoMode,
            )
        }.onEach { evaluation -> alerts.upsertAll(evaluation.alerts) }
}

/** Avisa alertas críticos uma única vez cada. A notificação só informa: nunca executa ação. */
class NotifyCriticalAlertsUseCase @Inject constructor(
    private val alerts: AlertRepository,
    private val notifier: AlertNotifier,
    private val preferences: PreferencesRepository,
) {
    suspend operator fun invoke(newAlerts: List<Alert>): Int {
        if (!preferences.notificationsEnabled.first()) return 0
        var delivered = 0
        for (alert in newAlerts.filter { it.level == AlertLevel.CRITICO }) {
            if (alerts.wasNotified(alert.id)) continue
            val shown = notifier.notifyAlert(
                alertId = alert.id,
                title = alert.title,
                body = "${alert.summary} Abra o app para ver a justificativa; a decisão é sua.",
            )
            if (shown) {
                alerts.markNotified(alert.id)
                delivered++
            }
        }
        return delivered
    }
}

/** Registra a decisão humana (acatar, adiar ou descartar) com responsável e data. */
/** Janela em que uma decisão idêntica é tratada como envio duplicado. */
private const val DUPLICATE_WINDOW_MILLIS = 30_000L

class RegisterDecisionUseCase @Inject constructor(
    private val decisions: DecisionRepository,
    private val preferences: PreferencesRepository,
    private val time: TimeProvider,
) {
    suspend operator fun invoke(
        alertId: String,
        action: DecisionAction?,
        responsible: String,
        note: String,
    ): Result<Decision> {
        val name = responsible.trim()
        if (action == null) {
            return Result.failure(IllegalArgumentException("Escolha uma ação: acatar, adiar ou descartar."))
        }
        if (name.length < 2) {
            return Result.failure(IllegalArgumentException("Informe o responsável pela decisão."))
        }
        val recent = decisions.observeForAlert(alertId).first().firstOrNull()
        val now = time.now()
        if (recent != null &&
            recent.action == action &&
            recent.responsible == name &&
            recent.note == note.trim() &&
            now - recent.timestamp in 0..DUPLICATE_WINDOW_MILLIS
        ) {
            return Result.failure(IllegalStateException("Esta mesma decisão acabou de ser registrada."))
        }
        val decision = Decision(
            id = UUID.randomUUID().toString(),
            alertId = alertId,
            responsible = name,
            action = action,
            note = note.trim(),
            timestamp = now,
        )
        decisions.insert(decision)
        preferences.setLastResponsible(name)
        return Result.success(decision)
    }
}

class ObserveAlertDetailUseCase @Inject constructor(
    private val alerts: AlertRepository,
    private val decisions: DecisionRepository,
) {
    operator fun invoke(alertId: String): Flow<AlertDetail?> =
        combine(alerts.observeById(alertId), decisions.observeForAlert(alertId)) { alert, list ->
            alert?.let { AlertDetail(it, list.sortedByDescending { d -> d.timestamp }) }
        }
}

class ObserveHistoryUseCase @Inject constructor(
    private val alerts: AlertRepository,
    private val decisions: DecisionRepository,
) {
    operator fun invoke(): Flow<List<HistoryItem>> =
        combine(decisions.observeAll(), alerts.observeAll()) { allDecisions, allAlerts ->
            val byId = allAlerts.associateBy { it.id }
            allDecisions.sortedByDescending { it.timestamp }.map { d ->
                HistoryItem(d, byId[d.alertId]?.title, byId[d.alertId]?.level)
            }
        }
}

class ClearDecisionsUseCase @Inject constructor(private val decisions: DecisionRepository) {
    suspend operator fun invoke() = decisions.clearAll()
}
