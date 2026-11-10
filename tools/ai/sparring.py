#!/usr/bin/env python3
"""Local sparring arena: a Paper 26.3 server with NSAC plus mineflayer bots to fight against.

    python3 tools/ai/sparring.py --player BwProo [--fighters 3] [--dummies 3]

Join with your normal client on  localhost:25570. The server listens on 127.0.0.1 only and runs in
offline mode, so only this computer can connect. Fighters attack you with human-like aim and gear;
dummies strafe and are targets for testing your own KillAura etc. Everything you type here goes to the
server console (for example "nsac alerts", "kill @e[type=item]"); type "stop" to end.
Your player gets OP and the BlacklistedClient bypass (so Meteor is not banned on join); all other
NSAC checks stay active with normal settings.
"""
from __future__ import annotations

import argparse
import os
import re
import subprocess
import sys
import threading
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
JOIN = re.compile(r"\b(\w{3,16}) joined the game")
GEAR = ["iron_sword", "iron_helmet", "iron_chestplate", "iron_leggings", "iron_boots"]


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--player", required=True, help="your Minecraft name")
    ap.add_argument("--fighters", type=int, default=3)
    ap.add_argument("--dummies", type=int, default=3)
    ap.add_argument("--server", default=str(ROOT / "run/sparring"))
    ap.add_argument("--node-modules", default=str(ROOT / "run/bots-node/node_modules"))
    ap.add_argument("--java", default="/usr/lib/jvm/java-25-openjdk/bin/java")
    args = ap.parse_args()
    server = Path(args.server)

    proc = subprocess.Popen([args.java, "-Xms1G", "-Xmx3G", "-jar", "paper.jar", "--nogui"], cwd=server,
                            stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    done, joined = threading.Event(), set()

    def pump():
        for line in proc.stdout:
            sys.stdout.write(line); sys.stdout.flush()
            if "Done (" in line: done.set()
            m = JOIN.search(line)
            if m:
                joined.add(m.group(1))
    threading.Thread(target=pump, daemon=True).start()

    def cmd(c: str) -> None:
        proc.stdin.write(c + "\n"); proc.stdin.flush()

    if not done.wait(240):
        sys.exit("server did not start")
    for c in ("difficulty peaceful", "gamerule keep_inventory true", "gamerule immediate_respawn true",
              "gamerule advance_time false", "kill @e[type=slime]", "time set day", f"op {args.player}",
              f"lp user {args.player} permission set nsac.exempt.blacklistedclient true",
              "lp group default permission unset nsac.nosetback"):
        cmd(c)

    bots = subprocess.Popen(["node", str(ROOT / "tools/ai/bots/sparring.js"), "25570", str(args.fighters), str(args.dummies)],
                            env=dict(os.environ, NODE_PATH=args.node_modules),
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    threading.Thread(target=lambda: [sys.stdout.write(l) for l in bots.stdout], daemon=True).start()

    exempted = set()

    def equip():
        # Bots are fully exempt from NSAC (mineflayer physics is not exact and would spam alerts).
        # LuckPerms only knows a name after its first join: grant, kick once, the bot rejoins exempt.
        while proc.poll() is None:
            for name in list(joined):
                joined.discard(name)
                if not name.startswith(("Fighter", "Dummy")):
                    continue
                if name not in exempted and not (server / "plugins/LuckPerms" / f".nsac-{name}").exists():
                    cmd(f"lp user {name} permission set nsac.exempt true")
                    (server / "plugins/LuckPerms" / f".nsac-{name}").touch()
                    exempted.add(name)
                    time.sleep(1)
                    cmd(f"kick {name} setting up bot")
                    continue
                exempted.add(name)
                if name.startswith("Fighter"):
                    cmd(f"clear {name}")
                    for item in GEAR:
                        cmd(f"give {name} {item}")
                cmd(f"effect give {name} regeneration infinite 0 true")
                cmd(f"execute as {args.player} at @s run tp {name} ~4 ~ ~4")
            time.sleep(3)
    threading.Thread(target=equip, daemon=True).start()

    print(f"\n>>> Sparring server ready: join localhost:25570 as {args.player}. Type 'stop' to end.\n", flush=True)
    try:
        for line in sys.stdin:
            cmd(line.strip())
            if line.strip() == "stop":
                break
    except KeyboardInterrupt:
        cmd("stop")
    bots.terminate()
    proc.wait(120)


if __name__ == "__main__":
    main()
