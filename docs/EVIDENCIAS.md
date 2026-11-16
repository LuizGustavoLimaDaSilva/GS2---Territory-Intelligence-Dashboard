# Evidências de execução e qualidade

Ambiente: Android Studio, emulador **Pixel 8 (API 36.1)**, 12/11/2026. Capturas de tela em `docs/evidencias/`.

## Resultado da compilação e dos testes
| Item | Resultado | Evidência |
|---|---|---|
| Build (`app:testDebugUnitTest` e dependências) | **BUILD SUCCESSFUL** em 1 min 52 s (30 tarefas: 13 executadas, 17 atualizadas) | `08-testes-unitarios-72.png` |
| Testes unitários | **72 passaram, 0 falharam** (4 s 369 ms) | `08-testes-unitarios-72.png` |
| Execução do app no emulador | Aplicativo instalado e executado; fluxo principal percorrido | capturas 01 a 07 |

## Provas de qualidade (antes e depois)
O “antes” é a versão inicial do projeto (esqueleto gerado e revisado em 11/11/2026); o “depois” é a versão entregue.

### Prova 1: acessibilidade (contraste de cores)
- **Meta:** contraste mínimo de 4,5:1 em textos e de 3:1 em elementos gráficos (WCAG 2.1, critérios 1.4.3 e 1.4.11).
- **Como foi medido:** razão de contraste pela fórmula do WCAG, calculada a partir das cores definidas no código da versão inicial e da versão entregue (arquivo `ui/theme/Color.kt`). No “antes”, o texto do alerta usava a cor padrão do tema escuro (#E6E0E9) sobre o fundo fixo #FFF3E0 do cartão.

| Par texto / fundo | Antes | Depois |
|---|---|---|
| Texto do alerta de atenção sobre o cartão (tema escuro) | 1.18:1 | 9.71:1 |
| Texto de status “atenção” sobre a superfície (escuro) | 3.15:1 | 11.88:1 |
| Texto de status “normal” sobre a superfície (escuro) | 2.76:1 | 11.00:1 |
| Texto de status “crítico” sobre a superfície (escuro) | 2.51:1 | 9.88:1 |
| Linha do gráfico sobre a superfície (escuro; meta de gráficos ≥ 3:1) | 2.86:1 | 9.55:1 |
| Texto de status “atenção” sobre a superfície (claro) | 5.21:1 | 8.52:1 |

Todos os pares da versão entregue atendem à meta; na versão inicial, 5 dos 6 pares ficavam abaixo dela (o texto de atenção sobre fundo claro, 5,21:1, já atendia).

### Prova 2: estabilidade e qualidade de código
- **Meta:** o projeto compila, os testes automatizados passam e o fluxo principal roda sem falhas.
- **Como foi medido:** revisão do código inicial (antes) e build, testes e execução no emulador (depois).

| Indicador | Antes (versão inicial) | Depois (versão entregue) |
|---|---|---|
| Compilação | 5 bloqueadores identificados na revisão: compilador do Room ausente, `hilt-navigation-compose` ausente, `when` não exaustivo no `StateView`, import ausente de `AlertData` e `ui-test-manifest` ausente | **BUILD SUCCESSFUL** (12/11/2026) |
| Testes escritos | 8 (6 unitários e 2 instrumentados) | 80 (72 unitários e 8 instrumentados) |
| Testes unitários aprovados | nenhum executado | **72 de 72** (4 s 369 ms) |
| Fluxo principal | simulado (alerta fixo, decisão sem gravar) | executado no emulador: alerta calculado, justificativa, decisão gravada e histórico (capturas 01 a 07) |

## Revisão pela equipe
João Pedro Tomas Dominguito e Vicente Casellato Rodriguez receberam o projeto, analisaram o código e a estrutura, executaram o aplicativo e aprovaram o resultado.

## Capturas de tela
| # | Arquivo | O que mostra |
|---|---|---|
| 01 | `01-alerta-justificativa.png` | Detalhe do alerta do Parque Central: justificativa com variáveis, regra aplicada, período, fonte, atualização e índice de confiança de 85% (alta), identificado como heurístico |
| 02 | `02-historico-decisao.png` | Histórico de decisões: **Acatar**, alerta crítico do Parque Central, responsável e data/hora do registro |
| 03 | `03-painel-jardim-aurora.png` | Painel do Jardim Aurora: nenhum alerta ativo; energia 6,90 MW e água 141,0 L/hab/dia com status Normal, mini gráficos, tendência, projeção, fonte e data da última leitura |
| 04 | `04-configuracoes.png` | Configurações: território, modo demonstração, notificações, estado da permissão e limiares |
| 05 | `05-formulario-decisao-vazio.png` | Formulário de decisão vazio: botão "Confirmar decisão" desabilitado e orientação de preenchimento |
| 06 | `06-formulario-decisao-preenchido.png` | Formulário de decisão com responsável preenchido e botão habilitado |
| 07 | `07-painel-parque-central-critico.png` | Painel do Parque Central: alerta **Crítico** de energia (projeção de 13,06 MW em 2 dias, 114% do limiar crítico de 11,50 MW), indicador de energia em estado Crítico, gráfico com previsão e limiares |

## Conferência dos números com a regra do alerta
Valores da captura 01 (Parque Central, energia):
- Média móvel de 3 dias: 11,92 MW; tendência: +0,16 MW/dia.
- Projeção base em 2 dias: 11,92 + 0,16 × 2 ≈ 12,24 MW.
- Temperatura prevista 34,5 °C, 4,5 °C acima do limiar de 30 °C: ajuste de 1,5% por °C sobre a projeção base, ≈ +0,83 MW.
- Projeção final: ≈ 13,06 MW (a diferença de centésimos vem do arredondamento da tendência exibida), acima do limiar crítico de 11,50 MW, portanto nível **Crítico**.
- Período analisado: 29/10 a 11/11/2026, 14 de 14 dias.

Os valores exibidos pela tela coincidem com o cálculo documentado em `docs/ARCHITECTURE.md`.

## Comportamentos confirmados pelas capturas
- O app **não emite alerta falso** quando os indicadores estão dentro dos limiares (captura 03).
- Todo alerta mostra **justificativa, fonte, período, atualização e índice de confiança** (capturas 01 e 07).
- A **decisão humana** exige ação e responsável: o botão só é habilitado depois do preenchimento (capturas 05 e 06) e a decisão registrada aparece no histórico com responsável e data (captura 02).
- Cada dado exibido identifica a **origem sintética** (capturas 03 e 07).
- O status é comunicado por **ícone e texto**, não só por cor (capturas 03 e 07).

## Testes automatizados por área (72)
`PredictionEngineTest` 17 · `TerritoryEvaluatorTest` 13 · `UseCasesTest` 9 · `DashboardViewModelTest` 9 · `SettingsViewModelTest` 6 · `NotificationPermissionResolverTest` 6 · `SeedDataTest` 7 · `EndToEndDomainFlowTest` 4 · `HistoryViewModelTest` 1.

Cobrem, entre outros: leituras futuras fora do histórico, valores inválidos e NaN, dados insuficientes e vencidos, limites de alerta, confiança, retry real do carregamento, falhas de leitura, decisão duplicada, notificação sem duplicar, estados de permissão, consistência dos CSVs e o fluxo CSV → alerta → justificativa → decisão → histórico.
