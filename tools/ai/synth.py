#!/usr/bin/env python3
"""Generate SYNTHETIC recordings to test the AI pipeline end to end.

    python3 tools/ai/synth.py <out folder> [--players 12] [--minutes 3]

Legit players: noisy hand-like aim that lags behind a strafing target, jittery clicking with
misses. Cheaters: smooth aim that locks onto the target, regular clicking without misses.
A model trained on this data only proves the pipeline works. It is NOT a usable anticheat
model; train the real one on real recordings.
"""
from __future__ import annotations

import argparse
import gzip
import math
import random
from pathlib import Path

HEADER = "tick,yaw,pitch,dyaw,dpitch,x,y,z,ground,swings,attack,target,tdx,tdy,tdz,ping,sens"


def target_angles(tdx: float, tdy: float, tdz: float) -> tuple[float, float]:
    ty = tdy + 0.9 - 1.62
    yaw = math.degrees(math.atan2(-tdx, tdz))
    pitch = -math.degrees(math.atan2(ty, math.hypot(tdx, tdz)))
    return yaw, pitch


def simulate(cheat: bool, ticks: int, rng: random.Random) -> list[str]:
    rows = []
    yaw, pitch = 0.0, 0.0
    angle = rng.uniform(0, 2 * math.pi)
    ping = rng.randint(20, 150)
    next_click = 0
    for t in range(ticks):
        angle += rng.uniform(0.02, 0.08)  # target strafes around the player
        dist = 2.5 + math.sin(t / 30) * 0.5
        tdx, tdy, tdz = math.cos(angle) * dist, rng.uniform(-0.1, 0.1), math.sin(angle) * dist
        want_yaw, want_pitch = target_angles(tdx, tdy, tdz)
        diff = (want_yaw - yaw + 180) % 360 - 180
        if cheat:
            step_yaw = diff * 0.6
            step_pitch = (want_pitch - pitch) * 0.6
        else:
            step_yaw = diff * rng.uniform(0.15, 0.6) + rng.gauss(0, 2.5)
            step_pitch = (want_pitch - pitch) * rng.uniform(0.1, 0.5) + rng.gauss(0, 1.2)
        yaw += step_yaw
        pitch = max(-90, min(90, pitch + step_pitch))

        swings = attack = 0
        if t >= next_click:
            swings = 1
            err = abs((want_yaw - yaw + 180) % 360 - 180)
            attack = 1 if (cheat or err < 15 and rng.random() < 0.7) else 0
            next_click = t + (2 if cheat else rng.choice([1, 2, 2, 3, 3, 4, 5]))
        rows.append(f"{t},{yaw:.5f},{pitch:.5f},{step_yaw:.5f},{step_pitch:.5f},0.00000,64.00000,0.00000,1,"
                    f"{swings},{attack},7,{tdx:.5f},{tdy:.5f},{tdz:.5f},{ping},0.50000")
    return rows


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("out")
    ap.add_argument("--players", type=int, default=12)
    ap.add_argument("--minutes", type=float, default=3)
    ap.add_argument("--seed", type=int, default=7)
    args = ap.parse_args()
    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    rng = random.Random(args.seed)
    ticks = int(args.minutes * 60 * 20)
    for p in range(args.players):
        player = f"synth{p:011d}"
        for cheat in (False, True):
            label = ("cheat-synthetic" if cheat else "legit-synthetic")
            text = (f"# nsac-recording v1\n# label={label}\n# player={player}\n# client_protocol=0\n"
                    f"# started_at=0\n# synthetic=true\n{HEADER}\n" + "\n".join(simulate(cheat, ticks, rng)) + "\n")
            (out / f"synthetic-{label}-{player}.csv.gz").write_bytes(gzip.compress(text.encode()))
    print(f"wrote {args.players * 2} synthetic recordings to {out}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
