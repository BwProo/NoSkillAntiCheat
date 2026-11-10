#!/usr/bin/env python3
"""Read and summarize NoSkillAntiCheat session recordings (*.csv.gz).

Usage:
    python3 tools/ai/recordings.py <folder or files...>

Each recording is a gzip file (possibly several concatenated gzip members) holding
'# key=value' metadata lines followed by a CSV with one row per client tick.
Uses only the Python standard library.
"""
from __future__ import annotations

import csv
import gzip
import io
import sys
from dataclasses import dataclass, field
from pathlib import Path

EXPECTED_HEADER = "tick,yaw,pitch,dyaw,dpitch,x,y,z,ground,swings,attack,target,tdx,tdy,tdz,ping,sens"


@dataclass
class Recording:
    path: Path
    meta: dict[str, str] = field(default_factory=dict)
    rows: list[dict[str, str]] = field(default_factory=list)

    @property
    def label(self) -> str:
        return self.meta.get("label", "?")


def load(path: Path) -> Recording:
    with gzip.open(path, "rt", encoding="utf-8") as fh:  # handles concatenated gzip members
        text = fh.read()
    rec = Recording(path)
    lines = text.splitlines()
    data_start = 0
    for i, line in enumerate(lines):
        if line.startswith("# "):
            if "=" in line:
                key, value = line[2:].split("=", 1)
                rec.meta[key.strip()] = value.strip()
            continue
        data_start = i
        break
    header = lines[data_start] if lines else ""
    if header != EXPECTED_HEADER:
        raise ValueError(f"{path.name}: unexpected header {header!r}")
    rec.rows = list(csv.DictReader(io.StringIO("\n".join(lines[data_start:]))))
    return rec


def summarize(rec: Recording) -> dict[str, object]:
    rows = rec.rows
    attacks = sum(int(r["attack"]) for r in rows)
    swings = sum(int(r["swings"]) for r in rows)
    combat = sum(1 for r in rows if r["target"] != "")
    pings = [int(r["ping"]) for r in rows]
    return {
        "file": rec.path.name,
        "label": rec.label,
        "player": rec.meta.get("player", "?"),
        "ticks": len(rows),
        "minutes": round(len(rows) / 20 / 60, 2),
        "attacks": attacks,
        "swings": swings,
        "combat_ticks": combat,
        "avg_ping": round(sum(pings) / len(pings), 1) if pings else 0,
    }


def main(argv: list[str]) -> int:
    if not argv:
        print(__doc__)
        return 2
    paths: list[Path] = []
    for arg in argv:
        p = Path(arg)
        paths.extend(sorted(p.glob("*.csv.gz")) if p.is_dir() else [p])
    if not paths:
        print("no recordings found")
        return 1
    totals: dict[str, float] = {}
    print(f"{'label':<20} {'player':<17} {'min':>6} {'attacks':>8} {'swings':>7} {'combat':>7} {'ping':>6}  file")
    for path in paths:
        try:
            s = summarize(load(path))
        except Exception as e:  # report broken files and continue
            print(f"ERROR {path.name}: {e}")
            continue
        totals[s["label"]] = totals.get(s["label"], 0) + s["minutes"]
        print(f"{s['label']:<20} {s['player']:<17} {s['minutes']:>6} {s['attacks']:>8} {s['swings']:>7} "
              f"{s['combat_ticks']:>7} {s['avg_ping']:>6}  {s['file']}")
    print("\nminutes per label: " + ", ".join(f"{k}={v:.1f}" for k, v in sorted(totals.items())))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
