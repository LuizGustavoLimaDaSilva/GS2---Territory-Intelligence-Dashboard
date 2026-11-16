# Roteiro de defesa (10 minutos): problema → decisão → prova

| Tempo | Bloco | O que mostrar |
|---|---|---|
| 2 min | Problema e valor | Comece pelo território: bairros sem aviso de sobrecarga de energia e de pressão na água; gestores descobrem tarde e sem justificativa. O app antecipa em até 2 dias, explica o porquê e registra quem decidiu. |
| 3 min | Decisão de arquitetura | Diagrama de camadas; ADR-001 (nativo vs. Flutter/RN: prazo, equipe, experiência; custo: sem iOS); ADR-002 (Room offline-first; custo: dados envelhecem, por isso validade de 7 dias). Mostre que `domain` não importa Android. |
| 3 min | Demonstração real (aparelho) | Painel → alerta crítico do Parque Central → justificativa → registrar decisão → histórico (reabrir o app). Depois a falha prevista: permissão de notificação negada (banner e atalho) e dados desatualizados (aviso + modo demonstração). |
| 2 min | Riscos e limites do escopo | Três riscos assumidos: alerta falso (índice de confiança e limiares visíveis), dados 100% sintéticos (identificados em cada tela) e ausência de execução em segundo plano (os avisos são gerados ao abrir o app). Fora do escopo: versão iOS. |

## Perguntas que a banca costuma fazer
- *Por que não Flutter?* Prazo, equipe e experiência no Android; custo assumido: sem iOS; o domínio e os contratos sobrevivem à troca.
- *A confiança é a probabilidade do alerta?* Não: é um índice heurístico (cobertura da janela e ruído). O modelo é simples, não foi validado com dados reais e a regra de temperatura é uma hipótese; por isso o app mostra a justificativa e a decisão é humana.
- *E em segundo plano?* Não há WorkManager: os avisos são gerados quando o app é aberto ou retomado, e isso está declarado em Configurações.
- *Como o alerta é calculado?* Média móvel de 3 dias + regressão linear de 14 dias × 2 dias, com ajuste por temperatura; confiança pela cobertura e ruído. Mostre o `PredictionEngineTest`.
- *E se faltar dado?* Não emite alerta e explica (insuficiente, vencido ou confiança baixa). Mostre `StateView`.
- *Onde a IA ajudou e como você validou?* Abra o `AI_USAGE_LOG.md` e explique um trecho em voz alta.
- *O app decide sozinho?* Nunca: só recomenda e avisa; a decisão exige responsável e é registrada.
