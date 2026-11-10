#!/usr/bin/env python3
"""Record labelled bot fights for the combat AI on a local offline test server.

    python3 tools/ai/botfights.py --server run/botserver --node-modules <dir> --minutes 6

Starts the server in --server (offline mode, NSAC installed), runs tools/ai/bots/arena.js with
legit-style and cheat-style mineflayer attackers, records every attacker with /nsac record and
copies the recordings to run/ai-data/<timestamp>/. Bot data only bootstraps the pipeline: bots are
not people, so a model trained only on it must stay in shadow mode.
Uses only the Python standard library (plus node + mineflayer for the bots).
"""
from __future__ import annotations

import argparse
import json
import os
import shutil
import subprocess
import sys
import threading
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

ROSTER = [  # (mode, label, how many attackers)
    ("legit", "legit_bot", 6),
    ("aura_snap", "cheat_aura_snap", 2),
    ("aura_smooth", "cheat_aura_smooth", 2),
    ("aimassist", "cheat_aimassist", 2),
]


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--server", default=str(ROOT / "run/botserver"))
    ap.add_argument("--node-modules", required=True, help="folder that contains node_modules/mineflayer")
    ap.add_argument("--java", default="/usr/lib/jvm/java-25-openjdk/bin/java")
    ap.add_argument("--minutes", type=float, default=6)
    ap.add_argument("--seed", type=int, default=1)
    args = ap.parse_args()
    server = Path(args.server)

    pairs, seed = [], args.seed * 100
    for mode, label, n in ROSTER:
        for i in range(n):
            seed += 7
            pairs.append({"name": f"{label.split('_')[0][:2]}{len(pairs)}s{args.seed}", "target": f"dum{len(pairs)}s{args.seed}",
                          "mode": mode, "label": label, "seed": seed})
    pairs_file = server / "pairs.json"
    pairs_file.write_text(json.dumps(pairs))

    log = open(server / "botfights.log", "w")
    proc = subprocess.Popen([args.java, "-Xms1G", "-Xmx3G", "-jar", "paper.jar", "--nogui"], cwd=server,
                            stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    done = threading.Event()

    def pump():
        for line in proc.stdout:
            log.write(line); log.flush()
            if "Done (" in line: done.set()
            if any(k in line for k in ("[arena]", "Recording", "kicked", "Exception", "NSAC")) and "Unsafe" not in line:
                print("  server:", line.rstrip()[:200])
    threading.Thread(target=pump, daemon=True).start()
    if not done.wait(240):
        sys.exit("server did not start")

    def cmd(c: str) -> None:
        proc.stdin.write(c + "\n"); proc.stdin.flush()

    # bots only: mineflayer's physics is not exact, so NSAC must not set them back (needs LuckPerms)
    cmd("lp group default permission set nsac.nosetback true")
    time.sleep(2)
    cmd("gamerule doDaylightCycle false"); cmd("time set day"); cmd("gamerule naturalRegeneration true")
    env = dict(os.environ, NODE_PATH=str(Path(args.node_modules) / "node_modules"))
    seconds = int(args.minutes * 60) + len(pairs) * 4 + 30
    arena = subprocess.Popen(["node", str(ROOT / "tools/ai/bots/arena.js"), str(25570), str(seconds), str(pairs_file)],
                             env=env, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    threading.Thread(target=lambda: [print("  bots:", l.rstrip()[:200]) for l in arena.stdout], daemon=True).start()

    time.sleep(len(pairs) * 3.5 + 12)
    for i, p in enumerate(pairs):
        x, z = (i % 4) * 48, (i // 4) * 48
        cmd(f"tp {p['target']} {x} -60 {z}"); cmd(f"tp {p['name']} {x + 3} -60 {z}")
        cmd(f"effect give {p['target']} resistance infinite 4 true")
        cmd(f"effect give {p['target']} regeneration infinite 4 true")
        cmd(f"effect give {p['name']} saturation infinite 1 true")
    time.sleep(5)
    for p in pairs:
        cmd(f"nsac record start {p['name']} {p['label']}")
    print(f"recording {len(pairs)} attackers for {args.minutes} min ...", flush=True)
    time.sleep(args.minutes * 60)
    for p in pairs:
        cmd(f"nsac record stop {p['name']}")
    time.sleep(5)
    arena.terminate()
    cmd("stop")
    proc.wait(120)

    out = ROOT / "run/ai-data" / time.strftime("%Y%m%d-%H%M%S")
    out.mkdir(parents=True, exist_ok=True)
    src = server / "plugins/NoSkillAntiCheat/recordings"
    files = sorted(src.glob("*.csv.gz"))
    for f in files:
        shutil.move(str(f), out / f.name)
    print(f"{len(files)} recordings -> {out}")


if __name__ == "__main__":
    main()
