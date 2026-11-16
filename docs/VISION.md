# Etapa 1: Visão do Negócio (versão simplificada)

## Objetivo, problema, público e benefícios
- **Objetivo:** antecipar problemas territoriais (sobrecarga de energia e pressão no abastecimento de água) e apoiar a decisão humana com alertas explicáveis.
- **Problema:** dados dispersos, análise só depois do problema, alertas sem justificativa e dificuldade de priorizar ações.
- **Público-alvo:** gestor territorial (usuário principal), moradores e lideranças comunitárias; administrador/curador de dados.
- **Benefícios:** problemas antecipados em até 2 dias, transparência (fonte, confiança, data), decisões registradas e auditáveis.

## As-Is e To-Be
| As-Is | To-Be |
|---|---|
| Dados em fontes e formatos diferentes; problema percebido depois de ocorrer; decisão por relato informal | Dados num app único, offline, com indicadores, gráficos e alertas com justificativa; decisão humana registrada com data e responsável |

## Papéis
| Papel | Responsabilidades |
|---|---|
| Morador | Consulta indicadores e alertas do território e entende a justificativa |
| Gestor Territorial | Analisa gráficos e alertas, decide (acatar, adiar, descartar) e registra |
| Administrador / Curador | Gera e atualiza dados (`prepare_data.py`), define limiares e fontes |

## Princípios de decisão humana (e onde o app os cumpre)
| Princípio | Atendimento |
|---|---|
| Justificativa obrigatória | `JustificationPanel`: variáveis, regra, período, fonte |
| Transparência da incerteza | Confiança em %, "atualizado há X", origem (pública/sintética) |
| Humano no circuito | O app recomenda e avisa; racionamento/interrupção nunca são executados |
| Rastreabilidade | Tabela `decisions`: alerta, responsável, ação, observação, data |
| Privacidade e ética | Dados agregados/sintéticos, sem dados pessoais; apagar histórico em Configurações |

## Contexto, restrições, riscos e jornada
- **Recorte:** energia + água, 3 bairros fictícios. **Usuário:** gestor, em pressa, às vezes sem internet.
- **Restrições:** Android nativo (minSdk 26, target 34), prazo do semestre, dados públicos limitados (sem série diária por bairro).
- **Riscos:** dados indisponíveis ou vencidos (bloqueia alerta e avisa), alerta falso (confiança e limiares explícitos), permissão negada (plano B na tela), iOS fora do escopo.
- **Jornada:** abrir o app → escolher território → ler KPI/gráfico → abrir o alerta → ler a justificativa → registrar a decisão. Sem dados: o painel explica o motivo e não emite alerta.

## Requisitos funcionais (6)
| ID | Descrição | Prioridade |
|---|---|---|
| RF01 | Exibir indicadores de energia e água com valor, tendência, status, fonte e data, e gráfico com histórico, previsão e limiares | Alta |
| RF02 | Gerar alertas (informativo, atenção, crítico) com justificativa, confiança e período, e não emitir alerta sem dados suficientes, atualizados e confiáveis | Alta |
| RF03 | Carregar os dados locais (CSV em assets) para o banco na primeira execução e recarregar quando a versão mudar | Alta |
| RF04 | Persistir leituras, alertas, decisões e preferências (território, modo, notificações) no aparelho | Alta |
| RF05 | Registrar a decisão humana (acatar, adiar, descartar) com responsável e observação, e listar o histórico | Alta |
| RF06 | Notificar localmente alertas críticos (sem duplicar), tratando permissão concedida, negada e negada para sempre, com plano B na tela | Média |

## Requisitos não funcionais (4)
| ID | Descrição | Atributo | Meta, medida e prova |
|---|---|---|---|
| RNF01 | Painel abre rápido em aparelho fraco | Desempenho | ≤ 2 s até o painel renderizado; Profiler (release, emulador fraco); gráfico antes/depois |
| RNF02 | Uso contido de memória e bateria na navegação | Eficiência | Pico de memória e energia comparados antes/depois; Profiler |
| RNF03 | Acessível | Acessibilidade | Contraste ≥ 4,5:1, alvos ≥ 48 dp, texto a 200%, TalkBack; vídeo de navegação |
| RNF04 | Estável no fluxo principal | Confiabilidade | 0 falhas no fluxo painel → alerta → decisão → histórico; Logcat e relatório de erros corrigidos |
