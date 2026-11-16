# Registro de uso de IA

## Ferramenta
Claude (Anthropic), via chat. Modelo informado pela ferramenta na etapa final: Claude Sonnet 5.5 (a conversa pode ter usado outros modelos nas etapas anteriores; o modelo de cada mensagem não foi registrado).

## Responsabilidade
O código gerado por IA foi compilado, testado e executado no emulador pelos alunos (resultados em `docs/EVIDENCIAS.md`). Os integrantes da equipe (João Pedro Tomas Dominguito, Luiz Gustavo Lima da Silva e Vicente Casellato Rodriguez) respondem pelo código entregue; o código também foi analisado e aprovado por João Pedro Tomas Dominguito e Vicente Casellato Rodriguez, que executaram o aplicativo.

## Registro
| Data | Onde a IA ajudou | Prompt / especificação | Alternativas comparadas | Como foi validado |
|---|---|---|---|---|
| 11/11/2026 | Leitura do enunciado e resumo das 8 etapas, critérios e entregáveis | Pedido de resumo do PDF oficial | Não se aplica (sem código) | Resumo conferido com o PDF anexado |
| 11/11/2026 | Plano de desenvolvimento por fases (arquitetura, dados, telas, testes) | Prompt estruturado com decisões fechadas (Android nativo, Room, Hilt, MVVM, previsão explicável, dados sintéticos) | Dados 100% sintéticos × dados reais; modelo de aprendizado de máquina × regressão e regra de limiar (escolhidos: sintéticos e regra explicável) | Decisões registradas em `docs/ADR-001.md`, `docs/ADR-002.md` e `docs/ARCHITECTURE.md` |
| 11/11/2026 | Primeiro esqueleto do app, gerado por agente | Prompt de desenvolvimento (fases 0 a 7) | Não se aplica | Revisão rigorosa apontou que não compilava e que o fluxo principal era simulado; o esqueleto foi substituído |
| 11 e 12/11/2026 | Reescrita do app: motor de previsão, avaliador de alertas, decisão persistida, notificação com três estados de permissão, tema, telas, componentes, testes, script de dados e documentação | Lista de problemas da revisão e missão de correções (leituras futuras fora da previsão, retry real, erros em Configurações, formulário de decisão com estado de envio, reuso de componentes, validação de dados) | WorkManager para segundo plano × limitar o escopo à avaliação ao abrir o app (escolhido: limitar o escopo e declarar na tela); teste de interface de ponta a ponta × teste de integração do domínio com os dados reais (escolhido: integração do domínio) | Build bem-sucedido, 72 testes unitários aprovados e fluxo executado no emulador Pixel 8 em 12/11/2026 (`docs/EVIDENCIAS.md`) |

## O que a IA não decide
- Limiares de alerta e qualquer regra de alto impacto (racionamento, interrupção de serviço, evacuação): definidos e revisados por pessoas; o app só recomenda e avisa.
- Dados pessoais: nunca entram no repositório nem em prompts.
- Sessão, permissões e chaves: não são gerados por IA.
- Texto da defesa e respostas à banca: escritos e apresentados pela equipe.

## Como cada trecho gerado foi validado
Compilação no Android Studio, execução dos testes automatizados e execução do fluxo no emulador.
