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
ANSI = re.compile(r"\x1b\[[0-9;?]*[A-Za-z]")
JOIN = re.compile(r"(?:^|[\s:])(\w{3,16}) joined the game")
GEAR = ["iron_sword", "iron_helmet", "iron_chestplate", "iron_leggings", "iron_boots"]


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--player", required=True, help="your Minecraft name")
    ap.add_argument("--fighters", type=int, default=3)
    ap.add_argument("--dummies", type=int, default=3)
    ap.add_argument("--server", default=str(ROOT / "run/sparring"))
    ap.add_argument("--node-modules", default=str(ROOT / "run/bots-node/node_modules"))
    ap.add_argument("--port", type=int, default=25570)
    ap.add_argument("--record", metavar="LABEL", help="record your fights for the AI, e.g. cheat_meteor_killaura or legit_me")
    ap.add_argument("--java", default="/usr/lib/jvm/java-25-openjdk/bin/java")
    args = ap.parse_args()
    server = Path(args.server)
    props = server / "server.properties"
    props.write_text(re.sub(r"(?m)^server-port=.*$", f"server-port={args.port}", props.read_text()))

    model = ROOT / "tools/ai/models/bootstrap-bots.json"
    ai_dir = server / "plugins/NoSkillAntiCheat/ai"
    if model.exists() and not (ai_dir / "model.json").exists():
        ai_dir.mkdir(parents=True, exist_ok=True)
        (ai_dir / "model.json").write_bytes(model.read_bytes())

    proc = subprocess.Popen([args.java, "-Xms1G", "-Xmx3G", "-jar", "paper.jar", "--nogui"], cwd=server,
                            stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    done, joined = threading.Event(), set()

    def pump():
        for line in proc.stdout:
            sys.stdout.write(line); sys.stdout.flush()
            if "Done (" in line: done.set()
            m = JOIN.search(ANSI.sub("", line))  # the console may colour names
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

    bots = subprocess.Popen(["node", str(ROOT / "tools/ai/bots/sparring.js"), str(args.port), str(args.fighters), str(args.dummies)],
                            env=dict(os.environ, NODE_PATH=args.node_modules),
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    threading.Thread(target=lambda: [sys.stdout.write(l) for l in bots.stdout], daemon=True).start()

    exempted = set()

    def equip_safe():
        try:
            equip()
        except Exception as e:  # keep the arena usable and say why
            print(f"[sparring] bot setup failed: {e!r}", flush=True)

    def equip():
        # Bots are fully exempt from NSAC (mineflayer physics is not exact and would spam alerts).
        # LuckPerms only knows a name after its first join: grant, kick once, the bot rejoins exempt.
        while proc.poll() is None:
            for name in list(joined):
                joined.discard(name)
                if not name.startswith(("Fighter", "Dummy")):
                    if args.record and name.lower() == args.player.lower():
                        time.sleep(2)
                        cmd(f"nsac record start {name} {args.record}")
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
    threading.Thread(target=equip_safe, daemon=True).start()

    print(f"\n>>> Sparring server ready: join localhost:{args.port} as {args.player}. Type 'stop' to end.\n", flush=True)
    try:
        for line in sys.stdin:
            if line.strip() == "stop" and args.record:
                cmd(f"nsac record stop {args.player}")
                time.sleep(2)
            cmd(line.strip())
            if line.strip() == "stop":
                break
    except KeyboardInterrupt:
        cmd("stop")
    bots.terminate()
    proc.wait(120)


if __name__ == "__main__":
    main()
