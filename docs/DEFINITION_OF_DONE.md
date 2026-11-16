# Definition of Done

Legenda: ☑ cumprido, com evidência

| Critério mínimo | Cumprido | Evidência / observações |
|---|---|---|
| Requisito implementado e critérios de aceite validados | ☑ | RF01 a RF06 implementados; fluxo observado nas capturas 01 a 07; 72 testes unitários aprovados (`docs/EVIDENCIAS.md`) |
| Fluxo principal validado de ponta a ponta no aparelho ou emulador | ☑ | Painel → alerta → justificativa → decisão → histórico, no emulador Pixel 8 (capturas 07, 01, 05, 06 e 02) |
| Todo alerta exibido com justificativa, confiança e fonte dos dados | ☑ | Capturas 01 e 07; `TerritoryEvaluatorTest` |
| App degrada com clareza com dados ausentes e com permissão negada | ☑ | Implementado (`StateView`, banner de permissão, `NotificationPermissionResolver`) e coberto por testes automatizados (`DashboardViewModelTest`, `NotificationPermissionResolverTest`); sem captura de tela desses cenários |
| Build instalável gerado (APK) com minSdk e targetSdk declarados | ☑ | APK de debug gerado pelo Gradle ao executar o app (`app/build/outputs/apk/debug/app-debug.apk`); minSdk 26 e targetSdk 34 em `app/build.gradle.kts` |
| Código revisado por outro integrante da equipe | ☑ | João Pedro Tomas Dominguito e Vicente Casellato Rodriguez analisaram o código e a estrutura, executaram o aplicativo e aprovaram |
| Decisão arquitetural registrada em ADR | ☑ | `docs/ADR-001.md`, `docs/ADR-002.md` |
| Uso de IA registrado (prompt, alternativas, validação e autor do commit) | ☑ | `docs/AI_USAGE_LOG.md` |
| Sem dados pessoais, chaves ou credenciais no repositório | ☑ | Dados sintéticos; busca no projeto sem ocorrências; `.gitignore` cobre chaves e APKs |
| Sem defeitos críticos ou bloqueantes em aberto | ☑ | Nenhum defeito observado nos fluxos executados; 72 testes aprovados |
| Documentação da funcionalidade atualizada | ☑ | README e `docs/` conferidos com o código desta versão |

## Resultados de execução
| Comando ou ação | Data | Resultado |
|---|---|---|
| Build e `app:testDebugUnitTest` | 12/11/2026 | BUILD SUCCESSFUL (1 min 52 s) |
| Testes unitários (72) | 12/11/2026 | 72 passaram, 0 falharam (4 s 369 ms) |
| Execução do app no emulador Pixel 8 | 12/11/2026 | Fluxo principal executado (capturas 01 a 07) |
| Verificação do aplicativo no emulador (repetida) | 14/11/2026 | Aplicativo verificado novamente, sem falhas |
