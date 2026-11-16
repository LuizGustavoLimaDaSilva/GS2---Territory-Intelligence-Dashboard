#!/usr/bin/env python3
"""
Prepara os arquivos de seed do app (app/src/main/assets/seed/).

MODOS
  1) Demonstração (100% sintético, roda sem baixar nada):
       python tools/prepare_data.py --demo
  2) Dados públicos reais (você baixa os arquivos manualmente):
       python tools/prepare_data.py --energy tools/raw/ons.csv \
           --water tools/raw/sinisa.csv --municipio Osasco [--subsystem SE]

O script NÃO acessa a internet, NÃO inventa datas e falha com mensagem clara
quando não reconhece o esquema do arquivo (mostra as colunas encontradas).
Saídas: territories.csv, indicators.csv, readings.csv, seed_version.txt
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import math
import random
import sys
import unicodedata
from datetime import date, datetime, timedelta, timezone
from pathlib import Path

BRT = timezone(timedelta(hours=-3))  # Brasil sem horário de verão desde 2019

# (id, nome, tipo, pico de energia em MW, fator de água, rampa energia %/dia, rampa água %/dia)
TERRITORIES = [
    ("bairro-a", "Jardim Aurora", "bairro fictício", 7.0, 0.95, 0.000, 0.000),
    ("bairro-b", "Vila Esperança", "bairro fictício", 8.2, 1.00, 0.013, 0.009),
    ("bairro-c", "Parque Central", "bairro fictício", 9.0, 1.04, 0.017, 0.000),
]
ENERGY_WARNING_MW, ENERGY_CRITICAL_MW = 10.0, 11.5
WATER_WARNING_FACTOR, WATER_CRITICAL_FACTOR = 1.12, 1.25
FORECAST_TEMPERATURES = [31.0, 33.5, 34.5]  # sintético: onda de calor nos próximos 3 dias
RAMP_DAYS_ENERGY, RAMP_DAYS_WATER = 21, 14


# ----------------------------- utilidades --------------------------------
def norm(text: str) -> str:
    text = unicodedata.normalize("NFKD", text.strip().lower())
    text = "".join(c for c in text if not unicodedata.combining(c))
    return text.replace(" ", "_").replace("-", "_")


def epoch_ms(day: date, hour: int = 0) -> int:
    return int(datetime(day.year, day.month, day.day, hour, tzinfo=BRT).timestamp() * 1000)


def gauss_bump(x: float, mu: float, sigma: float) -> float:
    return math.exp(-0.5 * ((x - mu) / sigma) ** 2)


def parse_number(text: str) -> float:
    t = text.strip()
    if "," in t and "." in t:
        t = t.replace(".", "").replace(",", ".")
    elif "," in t:
        t = t.replace(",", ".")
    return float(t)


TIME_FORMATS = ("%Y-%m-%d %H:%M:%S", "%Y-%m-%dT%H:%M:%S", "%Y-%m-%d %H:%M",
                "%d/%m/%Y %H:%M:%S", "%d/%m/%Y %H:%M", "%Y-%m-%d", "%d/%m/%Y")


def parse_time(text: str) -> datetime:
    raw = text.strip()
    for fmt in TIME_FORMATS:
        try:
            return datetime.strptime(raw, fmt).replace(tzinfo=BRT)
        except ValueError:
            continue
    raise ValueError(f"data/hora não reconhecida: {raw!r}")


def read_table(path: Path) -> tuple[list[str], list[dict[str, str]]]:
    with path.open(newline="", encoding="utf-8-sig") as f:
        head = f.readline()
        f.seek(0)
        delimiter = ";" if head.count(";") > head.count(",") else ","
        reader = csv.DictReader(f, delimiter=delimiter)
        headers = reader.fieldnames or []
        return headers, list(reader)


def find_column(headers: list[str], aliases: list[str], contains: list[str] | None = None) -> str | None:
    index = {norm(h): h for h in headers}
    for alias in aliases:
        if alias in index:
            return index[alias]
    for key, original in index.items():
        if contains and all(token in key for token in contains):
            return original
    return None


def fail_schema(path: Path, headers: list[str], missing: str) -> None:
    raise ValueError(
        f"Esquema não reconhecido em {path.name}: não achei a coluna de {missing}.\n"
        f"Colunas encontradas: {headers}\n"
        "Ajuste os aliases em tools/prepare_data.py (ou me envie as 3 primeiras linhas do arquivo)."
    )


# ----------------------------- temperatura -------------------------------
def temperature_for(day: date, rng: random.Random) -> float:
    doy = day.timetuple().tm_yday
    seasonal = 25.0 + 5.0 * math.cos(2 * math.pi * (doy - 30) / 365.0)  # pico no verão (jan/fev)
    return round(seasonal + rng.gauss(0, 1.1), 1)


# ----------------------------- energia -----------------------------------
def synthetic_energy(days: list[date], temps: dict[date, float], rng: random.Random) -> dict[str, list[tuple[int, float]]]:
    out: dict[str, list[tuple[int, float]]] = {}
    total = len(days)
    for tid, _n, _t, peak, _wf, ramp, _wr in TERRITORIES:
        rows = []
        for i, day in enumerate(days):
            weekend = 0.93 if day.weekday() >= 5 else 1.0
            heat = 1.0 + 0.012 * max(0.0, temps[day] - 27.0)
            since = i - (total - RAMP_DAYS_ENERGY)
            trend = 1.0 + ramp * max(0, since)
            for hour in range(24):
                profile = 0.60 + 0.22 * gauss_bump(hour, 8.5, 2.2) + 0.40 * gauss_bump(hour, 19.0, 2.0)
                value = peak * profile * weekend * heat * trend * (1.0 + rng.gauss(0, 0.010))
                rows.append((epoch_ms(day, hour), round(value, 4)))
        out[tid] = rows
    return out


def real_energy(path: Path, days_wanted: int, subsystem: str | None) -> tuple[dict[str, list[tuple[int, float]]], list[date]]:
    headers, rows = read_table(path)
    tcol = find_column(headers, ["din_instante", "instante", "data_hora", "datahora", "timestamp", "datetime", "data", "date"])
    vcol = find_column(headers, ["val_cargaenergiahomwmed", "val_cargaenergiamwmed", "val_carga", "carga_mw", "carga", "value", "valor"],
                       contains=["carga"])
    scol = find_column(headers, ["id_subsistema", "subsistema", "nom_subsistema"])
    if not tcol:
        fail_schema(path, headers, "data/hora")
    if not vcol:
        fail_schema(path, headers, "carga de energia")
    if subsystem and scol:
        wanted = norm(subsystem)
        rows = [r for r in rows if norm(r.get(scol, "")) == wanted or wanted in norm(r.get(scol, ""))]
        if not rows:
            raise ValueError(f"Nenhuma linha do subsistema {subsystem!r} (coluna {scol}).")
    values, skipped = [], 0
    for r in rows:
        try:
            values.append((parse_time(r[tcol]), parse_number(r[vcol])))
        except (ValueError, KeyError, TypeError):
            skipped += 1
    if not values or skipped > len(rows) * 0.5:
        raise ValueError(f"Linhas ilegíveis demais em {path.name} ({skipped} de {len(rows)}). Confira data e valor.")
    values.sort()
    last_day = values[-1][0].date()
    first_day = last_day - timedelta(days=days_wanted - 1)
    values = [(t, v) for t, v in values if t.date() >= first_day]
    peak_real = max(v for _, v in values)
    if peak_real <= 0:
        raise ValueError("Pico de carga igual a zero.")
    days = sorted({t.date() for t, _ in values})
    out = {}
    for tid, _n, _t, target_peak, *_ in TERRITORIES:
        scale = target_peak / peak_real  # preserva o formato real da curva; muda só a escala
        out[tid] = [(int(t.timestamp() * 1000), round(v * scale, 4)) for t, v in values]
    print(f"Energia: {len(values)} leituras reais de {days[0]} a {days[-1]} (pulei {skipped}).")
    return out, days


# ----------------------------- água --------------------------------------
def real_water_reference(path: Path, municipio: str | None) -> tuple[float, str]:
    headers, rows = read_table(path)
    vcol = find_column(headers, ["in022"], contains=["in022"])
    mcol = find_column(headers, ["municipio", "nome_do_municipio", "nome_municipio", "cidade"], contains=["municipio"])
    if not vcol:
        fail_schema(path, headers, "IN022 (consumo médio per capita de água)")
    if municipio:
        if not mcol:
            fail_schema(path, headers, "município")
        wanted = norm(municipio)
        rows = [r for r in rows if wanted in norm(r.get(mcol, ""))]
        if not rows:
            raise ValueError(f"Nenhuma linha do município {municipio!r} em {path.name}.")
    vals = []
    for r in rows:
        try:
            vals.append(parse_number(r[vcol]))
        except (ValueError, KeyError, TypeError):
            continue
    if not vals:
        raise ValueError("Nenhum valor numérico de IN022 encontrado.")
    ref = sum(vals) / len(vals)
    print(f"Água: IN022 médio = {ref:.1f} L/hab/dia ({len(vals)} linhas{', ' + municipio if municipio else ''}).")
    return ref, (municipio or "todas as linhas do arquivo")


def synthetic_water(days: list[date], temps: dict[date, float], ref: float, rng: random.Random) -> dict[str, list[tuple[int, float]]]:
    out = {}
    total = len(days)
    for tid, _n, _t, _p, factor, _er, ramp in TERRITORIES:
        rows = []
        for i, day in enumerate(days):
            heat = 1.0 + 0.010 * max(0.0, temps[day] - 26.0)
            weekend = 1.03 if day.weekday() >= 5 else 1.0
            trend = 1.0 + ramp * max(0, i - (total - RAMP_DAYS_WATER))
            value = ref * factor * heat * weekend * trend * (1.0 + rng.gauss(0, 0.012))
            rows.append((epoch_ms(day, 12), round(value, 3)))
        out[tid] = rows
    return out


# ----------------------------- escrita -----------------------------------
def write_csv(path: Path, header: list[str], rows: list[list[object]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", newline="", encoding="utf-8") as f:
        w = csv.writer(f, lineterminator="\n")
        w.writerow(header)
        w.writerows(rows)


def validate(readings: list[list[object]], territory_ids: set[str]) -> None:
    """Falha (ValueError) se houver duplicidade, valor inválido, referência quebrada ou série curta."""
    ids = [r[0] for r in readings]
    if len(ids) != len(set(ids)):
        raise ValueError("ids de leitura duplicados")
    keys = [(r[1], r[2], r[3]) for r in readings]
    if len(keys) != len(set(keys)):
        raise ValueError("série (território, indicador, instante) duplicada")
    counts: dict[tuple[str, str], int] = {}
    for rid, tid, ind, ts, value, origin in readings:
        if tid not in territory_ids:
            raise ValueError(f"território inexistente na leitura {rid}: {tid}")
        if ind not in ("ENERGY", "WATER", "CLIMATE"):
            raise ValueError(f"indicador inválido na leitura {rid}: {ind}")
        if origin not in ("PUBLICA", "SINTETICA", "AUTORIZADA"):
            raise ValueError(f"origem inválida na leitura {rid}: {origin}")
        if not isinstance(value, (int, float)) or not math.isfinite(value) or value < 0:
            raise ValueError(f"valor inválido na leitura {rid}: {value!r}")
        if not isinstance(ts, int) or ts <= 0:
            raise ValueError(f"timestamp inválido na leitura {rid}: {ts!r}")
        counts[(tid, ind)] = counts.get((tid, ind), 0) + 1
    for tid in territory_ids:
        for ind in ("ENERGY", "WATER", "CLIMATE"):
            if counts.get((tid, ind), 0) < 14:
                raise ValueError(f"poucas leituras de {ind} em {tid}: {counts.get((tid, ind), 0)} (mínimo 14)")


def main() -> int:
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--demo", action="store_true", help="gera tudo sintético (não precisa de arquivos)")
    p.add_argument("--energy", type=Path, help="CSV do ONS (carga horária), baixado manualmente")
    p.add_argument("--water", type=Path, help="CSV do SINISA/SNIS com IN022, baixado manualmente")
    p.add_argument("--municipio", default=None, help="filtra o município no arquivo do SINISA (ex.: Osasco)")
    p.add_argument("--subsystem", default=None, help="filtra o subsistema do ONS (ex.: SE)")
    p.add_argument("--days", type=int, default=120)
    p.add_argument("--end-date", default=None, help="YYYY-MM-DD (demo); padrão: hoje")
    p.add_argument("--water-ref", type=float, default=150.0, help="L/hab/dia hipotético (demo)")
    p.add_argument("--out", type=Path, default=Path("app/src/main/assets/seed"))
    p.add_argument("--seed", type=int, default=20262)
    a = p.parse_args()
    rng = random.Random(a.seed)

    try:
        if a.demo:
            end = date.fromisoformat(a.end_date) if a.end_date else datetime.now(BRT).date()
            days = [end - timedelta(days=a.days - 1 - i) for i in range(a.days)]
            temps = {d: temperature_for(d, rng) for d in days}
            energy = synthetic_energy(days, temps, rng)
            water_ref, water_note = a.water_ref, f"referência hipotética de {a.water_ref:.0f} L/hab/dia"
            energy_origin = "SINTETICA"
            energy_note = "Sintética: curva horária fictícia de carga elétrica (pico no fim da tarde), gerada por script com seed fixa"
            water_source = "Sintética: série diária fictícia de consumo por habitante, com " + water_note + ", gerada por script com seed fixa"
            mode = "demo"
        elif a.energy and a.water:
            energy, days = real_energy(a.energy, a.days, a.subsystem)
            ref, where = real_water_reference(a.water, a.municipio)
            water_ref = ref
            temps = {d: temperature_for(d, rng) for d in days}
            energy_origin = "PUBLICA"
            energy_note = "Pública (ONS): curva de carga horária escalada para bairro fictício (formato real, escala ajustada)"
            water_source = f"Sintética calibrada com IN022 do SINISA/SNIS ({where}); série diária gerada por script"
            mode = "real"
        else:
            p.print_usage(sys.stderr)
            print("Use --demo ou informe --energy e --water.", file=sys.stderr)
            return 2
        water = synthetic_water(days, temps, water_ref, rng)
    except (OSError, ValueError) as e:
        print(f"ERRO: {e}", file=sys.stderr)
        return 2

    readings: list[list[object]] = []
    for tid, *_ in TERRITORIES:
        for ts, v in energy[tid]:
            readings.append([f"ENERGY-{tid}-{ts}", tid, "ENERGY", ts, v, energy_origin])
        for ts, v in water[tid]:
            readings.append([f"WATER-{tid}-{ts}", tid, "WATER", ts, v, "SINTETICA"])
        for d in days:
            readings.append([f"CLIMATE-{tid}-{epoch_ms(d, 12)}", tid, "CLIMATE", epoch_ms(d, 12), temps[d], "SINTETICA"])
        for k, t in enumerate(FORECAST_TEMPERATURES, start=1):
            ts = epoch_ms(days[-1] + timedelta(days=k), 12)
            readings.append([f"CLIMATE-{tid}-{ts}", tid, "CLIMATE", ts, t, "SINTETICA"])

    try:
        validate(readings, {t[0] for t in TERRITORIES})
    except ValueError as e:
        print(f"ERRO de validação: {e}", file=sys.stderr)
        return 2

    out = a.out
    write_csv(out / "territories.csv", ["id", "name", "type"], [[t[0], t[1], t[2]] for t in TERRITORIES])
    write_csv(out / "indicators.csv", ["type", "unit", "warning", "critical", "source_note"], [
        ["ENERGY", "MW", ENERGY_WARNING_MW, ENERGY_CRITICAL_MW, energy_note],
        ["WATER", "L/hab/dia", round(water_ref * WATER_WARNING_FACTOR, 1), round(water_ref * WATER_CRITICAL_FACTOR, 1), water_source],
        ["CLIMATE", "°C", 0, 0, "Sintética: temperatura diária e previsão fictícias, geradas por script com seed fixa"],
    ])
    write_csv(out / "readings.csv", ["id", "territory_id", "indicator", "timestamp", "value", "origin"], readings)
    digest = hashlib.sha1(b"".join((out / n).read_bytes() for n in ("territories.csv", "indicators.csv", "readings.csv"))).hexdigest()[:10]
    (out / "seed_version.txt").write_text(f"{mode}-{days[-1]}-{digest}\n", encoding="utf-8")
    print(f"OK ({mode}): {len(readings)} leituras, {days[0]} a {days[-1]} -> {out}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
