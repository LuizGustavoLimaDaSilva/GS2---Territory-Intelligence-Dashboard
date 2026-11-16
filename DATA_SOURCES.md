# Fontes de dados (origem, licença e finalidade)

O enunciado permite dados públicos, sintéticos ou autorizados. Esta entrega usa **somente dados sintéticos**, gerados pela própria equipe. Nenhuma fonte externa foi usada, portanto não há licença de terceiros a transcrever.

| Indicador | Origem | Formato | Licença / termos | Finalidade | Como é gerado |
|---|---|---|---|---|---|
| Energia (curva horária por bairro) | Sintética, gerada por `tools/prepare_data.py` | CSV (`readings.csv`) | Uso livre (gerada pela equipe) | Indicador de carga e alerta de sobrecarga | Perfil diário com pico no fim da tarde, efeito de fim de semana, efeito do calor e ruído; picos-base de 7,0, 8,2 e 9,0 MW por bairro; rampas de crescimento nos bairros com alerta |
| Água (consumo diário por habitante) | Sintética, gerada por `tools/prepare_data.py` | CSV (`readings.csv`) | Uso livre (gerada pela equipe) | Indicador de consumo e alerta de pressão no abastecimento | Referência hipotética de 150 L/hab/dia por bairro (fatores 0,95, 1,00 e 1,04), efeito do calor e do fim de semana e ruído |
| Temperatura e previsão | Sintética, gerada por `tools/prepare_data.py` | CSV (`readings.csv`) | Uso livre (gerada pela equipe) | Ajuste da projeção por calor (regra acima de 30 °C) | Sazonalidade anual (pico no verão) com ruído; 3 dias de previsão fictícia (31,0, 33,5 e 34,5 °C) |
| Territórios | Fictícios (Jardim Aurora, Vila Esperança, Parque Central) | CSV (`territories.csv`) | Uso livre | Representar bairros | Definidos no script |
| Limiares de alerta | Definidos pela equipe | CSV (`indicators.csv`) | Uso livre | Níveis de atenção e crítico | Energia: 10,0 e 11,5 MW; água: 168,0 e 187,5 L/hab/dia (1,12 e 1,25 vezes a referência) |

## Regras
- **Nenhum dado pessoal** e nenhum dado real de bairro ou município.
- O app identifica a origem de cada dado na tela ("Sintética") e mostra a data da última leitura.
- Semente fixa: o mesmo comando gera exatamente os mesmos arquivos.
- Período dos dados entregues: 15/07/2026 a 11/11/2026 (mais 3 dias de previsão de temperatura).

## Como os dados foram gerados
```bash
python tools/prepare_data.py --demo --end-date 2026-11-11
```
O script **valida os dados antes de gravar** (ids e séries duplicados, valores NaN ou negativos, timestamps inválidos, origem ou indicador desconhecidos, territórios inexistentes, mínimo de 14 leituras por série). O teste `SeedDataTest` repete essas verificações sobre os CSVs versionados. O script também contém uma rotina de importação de CSV público, que não foi usada nesta entrega.

## Arquivos gerados (`app/src/main/assets/seed/`)
`territories.csv`, `indicators.csv` (limiares e nota de fonte de cada indicador), `readings.csv` (9.369 leituras) e `seed_version.txt` (identifica a versão da base; quando muda, o app recarrega as leituras sem apagar as decisões registradas).
