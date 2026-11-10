#!/usr/bin/env python3
"""Train the NoSkillAntiCheat combat AI from session recordings.

    python3 tools/ai/train.py <recordings folder> [more folders] --out model.json

Labels come from the recording label: "legit..." = 0, "cheat..." = 1, anything else is skipped.
Train and test data are split BY PLAYER (pseudonym), so the model is judged on players it has
never seen. The decision threshold is chosen so that legit TRAINING windows stay under
--max-fp-rate; the report then shows how many false alarms that gives on unseen legit players.

Model: standardized logistic regression with L2 regularization, trained with batch gradient
descent in pure Python (no extra packages). Copy model.json to
plugins/NoSkillAntiCheat/ai/model.json and run /nsac reload; the AICombat check then runs in
shadow mode (experimental).
"""
from __future__ import annotations

import argparse
import csv
import gzip
import io
import json
import math
import random
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import features as F  # noqa: E402

WINDOWS_PER_HOUR = 3600 * 20 / F.WINDOW_TICKS


def load_recording(path: Path) -> tuple[dict[str, str], list[dict]]:
    with gzip.open(path, "rt", encoding="utf-8") as fh:
        text = fh.read()
    meta, lines = {}, text.splitlines()
    start = 0
    for i, line in enumerate(lines):
        if line.startswith("# "):
            if "=" in line:
                k, v = line[2:].split("=", 1)
                meta[k.strip()] = v.strip()
            continue
        start = i
        break
    rows = [F.parse_row(r) for r in csv.DictReader(io.StringIO("\n".join(lines[start:])))]
    return meta, rows


def label_of(label: str) -> int | None:
    if label.startswith("legit"):
        return 0
    if label.startswith("cheat"):
        return 1
    return None


def sigmoid(z: float) -> float:
    if z < -40:
        return 0.0
    if z > 40:
        return 1.0
    return 1.0 / (1.0 + math.exp(-z))


def train_lr(xs: list[list[float]], ys: list[int], l2: float, epochs: int, lr: float) -> tuple[list[float], float]:
    d = len(xs[0])
    w, b = [0.0] * d, 0.0
    pos = sum(ys)
    neg = len(ys) - pos
    # class weights so a few cheat windows are not drowned by many legit windows
    cw = {0: len(ys) / (2 * max(neg, 1)), 1: len(ys) / (2 * max(pos, 1))}
    for _ in range(epochs):
        gw, gb = [0.0] * d, 0.0
        for x, y in zip(xs, ys):
            p = sigmoid(sum(wi * xi for wi, xi in zip(w, x)) + b)
            g = (p - y) * cw[y]
            for j in range(d):
                gw[j] += g * x[j]
            gb += g
        n = len(xs)
        for j in range(d):
            w[j] -= lr * (gw[j] / n + l2 * w[j])
        b -= lr * gb / n
    return w, b


def standardize(xs: list[list[float]]) -> tuple[list[float], list[float]]:
    d = len(xs[0])
    mean = [sum(x[j] for x in xs) / len(xs) for j in range(d)]
    std = [math.sqrt(sum((x[j] - mean[j]) ** 2 for x in xs) / len(xs)) or 1.0 for j in range(d)]
    return mean, std


def apply(xs, mean, std):
    return [[(x[j] - mean[j]) / std[j] for j in range(len(x))] for x in xs]


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("folders", nargs="+")
    ap.add_argument("--out", default="model.json")
    ap.add_argument("--test-share", type=float, default=0.3, help="share of players held out for testing")
    ap.add_argument("--max-fp-rate", type=float, default=0.002, help="max share of legit training windows over threshold")
    ap.add_argument("--l2", type=float, default=0.01)
    ap.add_argument("--epochs", type=int, default=400)
    ap.add_argument("--seed", type=int, default=1)
    args = ap.parse_args()

    by_player: dict[str, list[tuple[list[float], int]]] = {}
    for folder in args.folders:
        for path in sorted(Path(folder).glob("*.csv.gz")):
            meta, rows = load_recording(path)
            y = label_of(meta.get("label", ""))
            if y is None:
                continue
            player = meta.get("player", path.name)
            for window in F.windows(rows):
                by_player.setdefault(player, []).append((F.window_features(window), y))
    if not by_player:
        print("No labelled combat windows found (labels must start with legit or cheat).")
        return 1

    players = sorted(by_player)
    random.Random(args.seed).shuffle(players)
    n_test = max(1, round(len(players) * args.test_share)) if len(players) > 1 else 0
    test_players, train_players = set(players[:n_test]), players[n_test:]
    train = [s for p in train_players for s in by_player[p]]
    test = [s for p in test_players for s in by_player[p]]
    if len(players) < 4:
        print(f"WARNING: only {len(players)} players. Results will not generalize; record many more players.")
    if len({y for _, y in train}) < 2:
        print("Need legit AND cheat windows in the training players.")
        return 1

    xs, ys = [x for x, _ in train], [y for _, y in train]
    mean, std = standardize(xs)
    w, b = train_lr(apply(xs, mean, std), ys, args.l2, args.epochs, lr=0.5)

    def score(x):
        z = sum(wj * (xj - mj) / sj for wj, xj, mj, sj in zip(w, x, mean, std)) + b
        return sigmoid(z)

    legit_train = sorted(score(x) for x, y in train if y == 0)
    k = int(len(legit_train) * (1 - args.max_fp_rate))
    threshold = max(0.5, legit_train[min(k, len(legit_train) - 1)] + 1e-6)
    threshold = min(threshold, 0.999)

    def report(name, samples):
        if not samples:
            print(f"{name}: no windows")
            return {}
        legit = [score(x) for x, y in samples if y == 0]
        cheat = [score(x) for x, y in samples if y == 1]
        fp = sum(1 for s in legit if s >= threshold)
        tp = sum(1 for s in cheat if s >= threshold)
        out = {
            "legit_windows": len(legit), "cheat_windows": len(cheat),
            "false_alarms": fp, "detected": tp,
            "recall": round(tp / len(cheat), 3) if cheat else None,
            "false_alarms_per_legit_hour": round(fp / (len(legit) / WINDOWS_PER_HOUR), 2) if legit else None,
        }
        print(f"{name}: {out}")
        return out

    print(f"players: train={len(train_players)} test={len(test_players)}  windows: train={len(train)} test={len(test)}")
    print(f"threshold={threshold:.4f}")
    train_report = report("train", train)
    test_report = report("test (unseen players)", test)

    model = {
        "format": 1,
        "type": "logistic_regression",
        "features": F.FEATURES,
        "window_ticks": F.WINDOW_TICKS,
        "min_combat_ticks": F.MIN_COMBAT_TICKS,
        "mean": mean, "std": std, "weights": w, "bias": b,
        "threshold": threshold,
        "trained_at": int(time.time()),
        "report": {"train": train_report, "test": test_report,
                   "players_train": len(train_players), "players_test": len(test_players)},
    }
    Path(args.out).write_text(json.dumps(model, indent=2) + "\n")
    print(f"model written to {args.out}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
