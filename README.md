# Territory Intelligence Dashboard

**Global Solution FIAP 2026.2 · Cross-Platform Application Development · CitizenTech**

Aplicativo Android nativo (Kotlin + Jetpack Compose) que funciona como painel de inteligência territorial. Ele mostra indicadores de **energia** e **água** de bairros, emite **alertas preditivos sempre acompanhados de justificativa** e registra a **decisão humana** (acatar, adiar ou descartar). O app recomenda e avisa; **nunca executa ações de alto impacto**.

## Equipe
| Integrante | RM |
|---|---|
| João Pedro Tomas Dominguito | 562166 |
| Luiz Gustavo Lima da Silva | 563554 |
| Vicente Casellato Rodriguez | 563865 |

Repositório: https://github.com/LuizGustavoLimaDaSilva/GS2---Territory-Intelligence-Dashboard

## Funcionalidades implementadas
- **Painel** com seletor de território, alertas ativos e cartões de indicadores (valor, unidade, status com ícone e texto, mini gráfico, tendência, projeção, fonte e data da última leitura).
- **Detalhe do indicador** com gráfico de histórico, previsão tracejada com faixa de incerteza e linhas de limiar, resumo da previsão e alerta do indicador.
- **Detalhe do alerta** com justificativa (variáveis, regra aplicada, período, fonte, índice de confiança) e registro da decisão humana (ação, responsável e observação).
- **Histórico de decisões** persistido no aparelho.
- **Configurações:** território, modo demonstração, notificações e estado da permissão, limiares, fontes de dados, privacidade (LGPD) e apagar histórico.
- **Alertas preditivos** (informativo, atenção e crítico) calculados por média móvel de 3 dias, regressão linear de 14 dias e ajuste por temperatura prevista. Não há alerta quando faltam dados, os dados estão vencidos ou o índice de confiança é baixo.
- **Notificação local** de alerta crítico (sem duplicar), com tratamento dos três estados de permissão (concedida, negada e negada para sempre) e plano B na tela.
- **Offline-first:** dados em Room, preferências em DataStore, sem acesso à internet.
- **Estados de tela:** carregando, sucesso, vazio, erro (com "Tentar novamente") e dados desatualizados.
- **Tema claro e escuro**, componentes reutilizáveis e acessibilidade (alvos de 48 dp, descrições para TalkBack, cor sempre acompanhada de ícone e rótulo).

## Tecnologias
Kotlin 2.0.21 · Jetpack Compose (BOM 2024.09.03) e Material 3 · MVVM com `StateFlow` · Room 2.6.1 · DataStore 1.1.1 · Hilt 2.52 · Navigation Compose 2.8.2 · JUnit, Turbine e Coroutines Test · minSdk 26, targetSdk 34, compileSdk 34.

## Arquitetura
`ui` (Compose + ViewModel + `UiState`) → `domain` (Kotlin puro: modelos, casos de uso, `PredictionEngine`, contratos) ← `data` (Room, DataStore, assets, notificação). O domínio não importa classes do Android; as camadas se comunicam por interfaces.
Detalhes em `docs/ARCHITECTURE.md`; diagramas em `docs/diagrams/` (contexto e fluxo de dados, camadas e módulos, sequência do cenário crítico).

## Dados
Todos os dados são **sintéticos (fictícios)**, gerados por `tools/prepare_data.py` com semente fixa (`--seed 20262`): 3 bairros fictícios (Jardim Aurora, Vila Esperança, Parque Central), 120 dias de leituras de energia (horárias), água (diárias) e temperatura (diária, com 3 dias de previsão). Nenhum dado pessoal. A origem de cada dado aparece na tela. Detalhes em `DATA_SOURCES.md`.

Arquivos carregados pelo app (`app/src/main/assets/seed/`): `territories.csv`, `indicators.csv` (inclui limiares e nota de fonte), `readings.csv` e `seed_version.txt`.

**Validade dos dados:** os dados empacotados têm validade de 7 dias (regra de negócio). A última leitura é de 11/11/2026 23:00, então, sem o modo demonstração, o app gera alertas até 18/11/2026. Depois dessa data o painel mostra "Dados desatualizados" e não emite alertas; ligando **Configurações → Modo demonstração**, o app usa a última leitura como data de referência e volta a emitir alertas. Para gerar uma base que termine em outra data: `python tools/prepare_data.py --demo --end-date AAAA-MM-DD` e reinstale o app.

## Como rodar
**Requisitos:** Android Studio (Koala ou mais novo), Android SDK 34 ou superior e um JDK entre 17 e 21 para o Gradle (o Gradle 8.9 não funciona com JDK 25; no aviso "Select Gradle JVM", escolha o JDK 21).

1. Abra a pasta do projeto no Android Studio (File → Open) e aguarde o *Gradle sync*. O Gradle 8.9 é baixado automaticamente (`gradle/wrapper/gradle-wrapper.properties`).
2. Escolha um emulador (Pixel, API 34 ou superior) ou um aparelho Android com depuração USB.
3. Selecione a configuração **app** e clique em **Run** (▶).
4. **Testes unitários:** botão direito em `app/src/test` → *Run 'Tests in…'*.
5. **Testes instrumentados:** com emulador ou aparelho ligado, botão direito em `app/src/androidTest` → *Run*.
6. **APK de debug:** gerado pelo Gradle ao executar o app (ou em Build → Build Bundle(s)/APK(s) → Build APK(s)); fica em `app/build/outputs/apk/debug/app-debug.apk`.

## Roteiro de uso
1. No Painel, escolha **Parque Central**: aparece o alerta **Crítico** de energia, com os indicadores de energia e água.
2. Toque no alerta: justificativa completa. Toque em **Registrar decisão**, escolha a ação, informe o responsável e confirme.
3. Abra o **Histórico de decisões** (ícone de relógio): a decisão aparece e permanece salva no aparelho.
4. Toque no cartão de Energia: gráfico, previsão e limiares.
5. Escolha **Jardim Aurora**: sem alertas, indicadores normais. **Vila Esperança** mostra alertas de atenção.
6. Em **Configurações**: modo demonstração, notificações e estado da permissão, limiares, fontes e apagar histórico.

## Testes
**72 testes unitários** (JVM), por área:

| Área | Testes | O que verifica |
|---|---|---|
| `PredictionEngineTest` | 17 | série estável, tendência de alta e queda, ajuste por temperatura, leituras futuras ignoradas, dados insuficientes e vencidos, valores inválidos, NaN e infinito, confiança por cobertura e ruído |
| `TerritoryEvaluatorTest` | 13 | níveis de alerta, limiares inclusivos, justificativa e fonte, confiança baixa, dados vencidos, temperatura prevista |
| `UseCasesTest` | 9 | decisão registrada, decisão duplicada bloqueada, notificação sem duplicar, permissão negada e preferência desligada |
| `DashboardViewModelTest` | 9 | sucesso, retry real do carregamento, falhas de leitura, dados vencidos, modo demonstração, atualização limitada a 1 por minuto |
| `SettingsViewModelTest` | 6 | erro diferente de lista vazia, retry, preferências, apagar histórico |
| `NotificationPermissionResolverTest` | 6 | estados concedida, negada e negada para sempre |
| `SeedDataTest` | 7 | consistência dos CSVs (duplicidade, referências, valores, origem) |
| `EndToEndDomainFlowTest` | 4 | CSV → avaliação → alerta → justificativa → decisão → histórico, com os dados do seed |
| `HistoryViewModelTest` | 1 | estados vazio e sucesso do histórico |

Em `app/src/androidTest` há 8 testes instrumentados: Room (DAOs, decisões, notificação única), carga do seed no Room e formulário de decisão.

## Verificação realizada (12/11/2026 e 14/11/2026)
Resultados registrados em `docs/EVIDENCIAS.md`, com as capturas de tela em `docs/evidencias/`:
- **Build:** `BUILD SUCCESSFUL` (1 min 52 s).
- **Testes unitários:** **72 passaram**, 0 falharam (4 s 369 ms).
- **Execução no emulador Pixel 8 (API 36.1), em 12/11/2026:** painel de dois bairros, alerta crítico com justificativa, registro da decisão e histórico. A verificação do aplicativo no emulador foi repetida em 14/11/2026, sem falhas.
- **Revisão pela equipe:** João Pedro Tomas Dominguito e Vicente Casellato Rodriguez analisaram o código e a estrutura do projeto, executaram o aplicativo e aprovaram o resultado.

## Atendimento ao enunciado (onde cada etapa está)
| Etapa | Onde |
|---|---|
| 1. Visão do negócio (objetivo, problema, público, As-Is/To-Be, papéis, princípios, 6 RF e 4 RNF) | `docs/VISION.md` |
| 2. Arquitetura (abordagem, justificativa, blueprint, diagramas) | `docs/ARCHITECTURE.md`, `docs/diagrams/` |
| 3. Estado e persistência (UiState, fluxo, ciclo de vida, Room, DataStore, fontes, recurso nativo e permissão) | `docs/ARCHITECTURE.md`, `DATA_SOURCES.md`, código em `ui/`, `data/` |
| 4. Identidade visual e componentes (tokens, paleta com contraste ≥ 4,5:1, 5 componentes em 2+ telas, 5 telas) | `docs/ARCHITECTURE.md`, `ui/theme`, `ui/components` |
| 5. Alertas preditivos e decisão humana (método, níveis, justificativa, confiança, limites, ética) | `docs/ARCHITECTURE.md` |
| 6. ADR | `docs/ADR-001.md`, `docs/ADR-002.md` |
| 7. Qualidade (Definition of Done, evidências) | `docs/DEFINITION_OF_DONE.md`, `docs/EVIDENCIAS.md` |
| 8. Uso de IA e roteiro de defesa | `docs/AI_USAGE_LOG.md`, `docs/DEFENSE_SCRIPT.md` |

## Decisões arquiteturais (resumo)
- **Android nativo com Kotlin e Compose** em vez de Flutter ou React Native, pelo prazo, pela equipe e pela experiência no Android (ADR-001).
- **Room offline-first** com carga inicial por CSV em assets e DataStore para preferências, em vez de API remota (ADR-002).
- **Previsão explicável** (média móvel + regressão linear + regra de temperatura) em vez de modelo de aprendizado de máquina.
- **Notificação isolada por adaptador** (`AlertNotifier`), sem chamar a API do Android a partir da tela.

## Limites do escopo
- Os alertas e as notificações são gerados quando o app é aberto ou retomado; não há execução em segundo plano.
- O **índice de confiança é heurístico** (cobertura dos dados e ruído em torno da reta). Não é probabilidade de o evento ocorrer, e o modelo não foi validado contra dados reais.
- Os dados são fictícios; não representam bairros ou municípios reais.
- Não há versão iOS.

## Uso responsável de IA
Registro completo em `docs/AI_USAGE_LOG.md`: ferramenta, finalidade, alternativas, validação e política do que a IA não decide.

## Referências
- Enunciado da Global Solution GS2026.2, Prof. Dr. Leandro Ramos da Silva (FIAP).
- Documentação oficial do Android (developer.android.com): Jetpack Compose, Material 3, Room, DataStore, Navigation, Notificações e permissão `POST_NOTIFICATIONS`, acessibilidade.
- Kotlin (kotlinlang.org), Dagger Hilt, Kotlin Coroutines e Flow, Turbine (testes de Flow), JUnit 4.
- WCAG 2.1, critério 1.4.3 (contraste mínimo de 4,5:1), usado no cálculo da paleta.
- Fontes de dados: nenhuma fonte externa; os dados são gerados por `tools/prepare_data.py` (ver `DATA_SOURCES.md`).
