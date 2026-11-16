# Arquitetura, estado, persistência, visual e alertas (Etapas 2 a 5)

## Abordagem escolhida
☑ **Arquitetura em camadas com MVVM e fluxo unidirecional, no estilo Clean** (estado desce, eventos sobem).

| Item | Justificativa |
|---|---|
| Motivação | Separar interface, regra e dados permite testar o domínio sem Android e trocar a fonte de dados (assets → API) sem mexer na regra. Cabe no prazo e na equipe. |
| Benefícios | Testabilidade (72 testes unitários rodam na JVM), manutenção, contratos claros, estado previsível. |
| Limitações | Mais arquivos e interfaces do que um app simples; Hilt e Room adicionam configuração; o domínio usa `javax.inject` (biblioteca JVM, não Android). |
| Alternativas descartadas | MVVM sem camada de domínio (regra vazaria para ViewModels); Feature-First (módulos demais para 2 indicadores); Flutter/React Native (ver ADR-001). |

## Blueprint: onde cada decisão mora
| Requisito | Interface (Compose) | Domínio (Kotlin puro) | Dados e nativo |
|---|---|---|---|
| Indicadores e gráficos | `KpiCard`, `TrendChart` | `TerritoryEvaluator` monta status a partir das leituras | `RoomReadingRepository` |
| Alerta preditivo | `AlertCard`, `JustificationPanel` | `PredictionEngine` + regras de nível, confiança e justificativa | Leituras no Room |
| Dados ausentes ou vencidos | `StateView` com data da última leitura | Validade de 7 dias, mínimo de 7 dias de amostras, confiança ≥ 50% | `readings` com timestamp |
| Falha ao ler | `StateView` com "Tentar novamente" | `UiState.Error` no ViewModel (`catch`) | Exceção do Room/seed vira estado |
| Notificar crítico | Banner e botão se permissão negada | `NotifyCriticalAlertsUseCase` (só crítico, sem duplicar, corpo contextual) + `NotificationPermissionResolver` (regra pura) | `AndroidAlertNotifier` + permissão |
| Registrar decisão | `DecisionSheet` (estado de envio, feedback) | `RegisterDecisionUseCase` valida, barra duplicidade (30 s) e cria o registro | `decisions` no Room |

Diagramas: `docs/diagrams/context-flow.mmd` e `docs/diagrams/layers-modules.mmd` (renderize em mermaid.live ou no Android Studio/IDE com plugin Mermaid e exporte PNG para o PDF).

## Estado (Etapa 3)
- **Solução:** `ViewModel` + `StateFlow` + `sealed interface UiState` (Loading, Success, Empty, Error, Stale), coletado com `collectAsStateWithLifecycle`.
- **Tipos de estado:** *local* (formulário da decisão, menu aberto, `rememberSaveable`); *compartilhado* (território e modo demonstração, em DataStore); *de dados* (leituras no Room, com validade).
- **Fluxo:** evento do Composable → ViewModel → caso de uso → repositório → Flow do Room → `UiState` → recomposição.
- **Recarga e retry:** `retry()` reexecuta o seed se ele falhou e reassina toda a coleta (um toque por vez); `refresh()` roda em `ON_RESUME` (no máximo 1 vez por minuto) para reavaliar validade e alertas.
- **Ciclo de vida:** `ViewModel` sobrevive à rotação; `flatMapLatest` mantém **um único coletor** (sem carregamento duplicado); argumentos de rota (ids) vão no `SavedStateHandle`; formulário usa `rememberSaveable`.
- **Estado derivado:** indicadores e alertas são **calculados** das leituras (`TerritoryEvaluator`), não duplicados.

## Persistência
Room (territórios, indicadores, leituras, alertas, decisões, notificações já feitas) e DataStore (território, modo demonstração, notificações, último responsável, versão do seed). Offline é o regime normal. Privacidade: só dados agregados/sintéticos; "Apagar histórico de decisões" nas Configurações (LGPD).

Modelo: Territory, Indicator (limiares), Reading (origem pública/sintética/autorizada), Alert (nível, confiança, justificativa, período, validade), Decision (alerta, responsável, ação, observação, data).

## Recursos nativos
| Recurso | Risco | Plano B |
|---|---|---|
| Notificação local (POST_NOTIFICATIONS, Android 13+) | Negada ou negada para sempre | Banner no painel + botão "Permitir" ou "Abrir configurações do sistema"; alerta continua visível |
| Armazenamento local | Falha de leitura | `UiState.Error` com "Tentar novamente" |

O pedido de permissão acontece no toque do usuário (Configurações ou banner), depois de explicar o motivo. O adaptador distingue concedida, negada e negada para sempre (`AndroidAlertNotifier`).

## Escopo: sem execução em segundo plano
O app não monitora nem notifica em segundo plano (não usa WorkManager). A notificação crítica é disparada quando o app avalia os dados, ao abrir ou voltar ao primeiro plano, e a tela de Configurações informa isso ao usuário.

## Identidade visual e componentes (Etapa 4)
- **Conceito:** "clareza territorial" (azul sereno, pulso de dados dentro de um anel). Ícone adaptativo e ícone de notificação próprios.
- **Tema único:** `MaterialTheme` com `ColorScheme` claro e escuro, `Typography` de **6 níveis**, `Shapes` e grade `Spacing`. Cores semânticas (normal, informativo, atenção, crítico) em `StatusColors`; todos os pares texto/fundo ≥ 4,5:1 (medido).
- **Acessibilidade:** alvos ≥ 48 dp, `contentDescription`/`semantics`, `sp` para texto, cor sempre com ícone e rótulo, listas roláveis para fonte a 200%, largura máxima e cartões lado a lado quando a largura permite (≥ 600 dp).
- **Componentes reutilizáveis:** `KpiCard` e `TrendChart` (Painel e Indicador), `AlertCard` (Painel, Indicador, Alerta), `TerritorySelector` (Painel e Configurações), `StateView` (todas as telas), além de `JustificationPanel` e `DecisionSheet` (Detalhe do Alerta) e `StatusChip`, `AppCard`, `AppScaffold`.
- **Telas (5):** Painel, Detalhe do Indicador, Detalhe do Alerta, Histórico de Decisões, Configurações. **Navegação:** só ids nas rotas.

## Alertas preditivos e decisão humana (Etapa 5)
0. **Só observações até o horário de referência** entram no histórico; leituras futuras (previsões, como a temperatura) nunca são tratadas como observadas.
1. Série diária (pico do dia na energia; média do dia na água).
2. **Média móvel de 3 dias** suaviza o nível atual.
3. **Regressão linear** nos últimos 14 dias estima a tendência por dia.
4. **Projeção** = média móvel + tendência × 2 dias, **ajustada pela temperatura** prevista (energia +1,5% e água +1,0% por °C acima de 30 °C).
5. **Níveis:** Crítico (projeção ou média ≥ limiar crítico), Atenção (≥ limiar de atenção), Informativo (tendência de alta com projeção ≥ 90% do limiar de atenção).
6. **Índice de confiança (heurístico)** = cobertura da janela × (1 − ruído relativo / 25%). Abaixo de 50%, **nenhum alerta é emitido** e a tela explica o motivo.
7. **Quando não alertar:** menos de 7 dias de dados, leitura mais velha que 7 dias, confiança baixa, sem dados.
8. **Ação humana:** o app sugere; a pessoa escolhe Acatar, Adiar ou Descartar e registra responsável e observação.
9. **Limitações matemáticas (leia antes de defender):**
   - O índice de confiança **não é probabilidade** de o evento ocorrer nem intervalo de confiança; mede só a cobertura da janela e o ruído em torno da reta.
   - A regressão é linear em 14 pontos: não modela sazonalidade semanal (fim de semana entra como ruído), nem eventos raros, nem mudanças de regime.
   - A regra de temperatura (+1,5% energia, +1,0% água por °C acima de 30 °C) é uma **aproximação linear sem calibração com dados reais**; os coeficientes são hipóteses da equipe.
   - Os limiares vêm do `indicators.csv` (sintéticos no modo demo); a precisão dos alertas **não foi validada** com dados reais.
   - Os dados da água são sintéticos (só a média de referência pode vir do SINISA).
10. **Riscos e ética:** falsos positivos/negativos (limiares e confiança visíveis), dados sintéticos sinalizados, LGPD (sem dados pessoais, histórico apagável), sem automação de decisões de alto impacto.

## Decisão → alternativa descartada → critério
| Decisão | Alternativa descartada | Critério |
|---|---|---|
| Plataforma: Android nativo | Flutter, React Native, 2 apps nativos | Prazo, equipe e experiência no Android |
| Estado: ViewModel + StateFlow | Estado local nas telas | Complexidade real do fluxo (várias telas, dados em Room) |
| Origem dos dados: Room + CSV | API remota | Escopo da disciplina e uso offline |
| Recurso nativo: adaptador | API do Android direto na tela | Testabilidade e risco de manutenção |
| Gráfico: Canvas próprio | Biblioteca de gráficos | Menos dependências e acessibilidade controlada |
| Previsão: regressão + média móvel + regra de temperatura | Modelo de ML | Explicabilidade e prazo |
| Entrega do build: Gradle `assembleRelease` | Build manual | Repetibilidade |
