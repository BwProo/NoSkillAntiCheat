#!/usr/bin/env python3
"""Feature extraction for the NoSkillAntiCheat combat AI.

The SAME formulas are implemented in Java (ac.grim.grimac.checks.impl.ai.CombatFeatures).
A parity test (CombatFeaturesParityTest) checks both against the fixture in
common/src/test/resources/ai/. Change both together, then run:

    python3 tools/ai/features.py --fixture

to regenerate the expected values.

A window is WINDOW_TICKS consecutive client ticks (non-overlapping blocks from the start of a
recording) with at least MIN_COMBAT_TICKS ticks that have a recent target.
Uses only the Python standard library.
"""
from __future__ import annotations

import json
import math
import sys
from pathlib import Path

WINDOW_TICKS = 100          # 5 seconds
MIN_COMBAT_TICKS = 50
ON_TARGET_DEGREES = 10.0
EYE_HEIGHT = 1.62
TARGET_CENTER = 0.9

FEATURES = [
    "mean_abs_dyaw", "std_dyaw", "mean_abs_dpitch", "std_dpitch",
    "yaw_smoothness", "yaw_jerk",
    "aim_err_mean", "aim_err_std", "on_target_frac", "aim_err_at_attack",
    "attacks_per_sec", "swings_per_sec", "miss_ratio", "click_gap_std",
    "ping_mean",
]


def _mean(values: list[float]) -> float:
    return sum(values) / len(values) if values else 0.0


def _std(values: list[float]) -> float:
    if not values:
        return 0.0
    m = _mean(values)
    return math.sqrt(sum((v - m) ** 2 for v in values) / len(values))


def aim_error_degrees(yaw: float, pitch: float, tdx: float, tdy: float, tdz: float) -> float | None:
    """Angle between the look vector and the vector from the eyes to the target's center."""
    yr, pr = math.radians(yaw), math.radians(pitch)
    lx, ly, lz = -math.sin(yr) * math.cos(pr), -math.sin(pr), math.cos(yr) * math.cos(pr)
    tx, ty, tz = tdx, tdy + TARGET_CENTER - EYE_HEIGHT, tdz
    length = math.sqrt(tx * tx + ty * ty + tz * tz)
    if length < 1e-6:
        return None
    dot = (lx * tx + ly * ty + lz * tz) / length
    return math.degrees(math.acos(max(-1.0, min(1.0, dot))))


def window_features(rows: list[dict]) -> list[float]:
    """rows: dicts with float/int values for the recording columns (target fields may be None)."""
    n = len(rows)
    dyaw = [r["dyaw"] for r in rows]
    dpitch = [r["dpitch"] for r in rows]
    abs_dyaw = [abs(v) for v in dyaw]
    mean_abs_dyaw = _mean(abs_dyaw)

    first = [abs(dyaw[i] - dyaw[i - 1]) for i in range(1, n)]
    second = [abs((dyaw[i] - dyaw[i - 1]) - (dyaw[i - 1] - dyaw[i - 2])) for i in range(2, n)]
    yaw_smoothness = _mean(first) / (mean_abs_dyaw + 1e-6)
    yaw_jerk = _mean(second) / (mean_abs_dyaw + 1e-6)

    errors, attack_errors = [], []
    for r in rows:
        if r["target"] is None:
            continue
        err = aim_error_degrees(r["yaw"], r["pitch"], r["tdx"], r["tdy"], r["tdz"])
        if err is None:
            continue
        errors.append(err)
        if r["attack"]:
            attack_errors.append(err)
    on_target = sum(1 for e in errors if e < ON_TARGET_DEGREES) / len(errors) if errors else 0.0

    seconds = n / 20.0
    attacks = sum(1 for r in rows if r["attack"])
    swings = sum(r["swings"] for r in rows)
    swing_ticks = [i for i, r in enumerate(rows) if r["swings"] > 0]
    missed = sum(1 for i in swing_ticks if not rows[i]["attack"])
    miss_ratio = missed / len(swing_ticks) if swing_ticks else 0.0
    gaps = [float(swing_ticks[i] - swing_ticks[i - 1]) for i in range(1, len(swing_ticks))]
    click_gap_std = _std(gaps) if len(gaps) >= 2 else 0.0

    return [
        mean_abs_dyaw, _std(dyaw), _mean([abs(v) for v in dpitch]), _std(dpitch),
        yaw_smoothness, yaw_jerk,
        _mean(errors), _std(errors), on_target, _mean(attack_errors),
        attacks / seconds, swings / seconds, miss_ratio, click_gap_std,
        _mean([r["ping"] for r in rows]) / 1000.0,
    ]


def parse_row(raw: dict[str, str]) -> dict:
    def opt(key: str):
        return float(raw[key]) if raw[key] != "" else None
    return {
        "yaw": float(raw["yaw"]), "pitch": float(raw["pitch"]),
        "dyaw": float(raw["dyaw"]), "dpitch": float(raw["dpitch"]),
        "swings": int(raw["swings"]), "attack": raw["attack"] == "1",
        "target": opt("target"), "tdx": opt("tdx"), "tdy": opt("tdy"), "tdz": opt("tdz"),
        "ping": int(raw["ping"]),
    }


def windows(rows: list[dict]) -> list[list[dict]]:
    out = []
    for start in range(0, len(rows) - WINDOW_TICKS + 1, WINDOW_TICKS):
        block = rows[start:start + WINDOW_TICKS]
        if sum(1 for r in block if r["target"] is not None) >= MIN_COMBAT_TICKS:
            out.append(block)
    return out


def _write_fixture() -> None:
    """Regenerates the parity fixture from the CSV in the Java test resources."""
    import csv
    folder = Path(__file__).resolve().parents[2] / "common/src/test/resources/ai"
    with open(folder / "parity-window.csv", newline="") as fh:
        rows = [parse_row(r) for r in csv.DictReader(fh)]
    assert len(rows) == WINDOW_TICKS, len(rows)
    values = window_features(rows)
    (folder / "parity-expected.json").write_text(json.dumps(dict(zip(FEATURES, values)), indent=2) + "\n")
    print(f"wrote {folder / 'parity-expected.json'}")


if __name__ == "__main__":
    if sys.argv[1:] == ["--fixture"]:
        _write_fixture()
    else:
        print(__doc__)
