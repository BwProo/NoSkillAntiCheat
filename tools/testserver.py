#!/usr/bin/env python3
"""Smoke test NoSkillAntiCheat on local Paper, Folia, Purpur and Spigot servers.

For each platform/version the script downloads the official server jar (checksum
verified where the API provides one), installs the plugin jar, starts the server,
waits for startup, runs `nsac version` on the console, stops the server and checks
the log for plugin errors. Results go to run/testservers/report.md.

Usage (from the repository root):
    python3 tools/testserver.py --accept-eula
    python3 tools/testserver.py --accept-eula --platforms paper,folia --versions 26.3

--accept-eula writes eula=true into the local test server folders. Only pass it
if you accept the Minecraft EULA (https://aka.ms/MinecraftEULA).
Uses only the Python standard library.
"""
from __future__ import annotations

import argparse
import glob
import hashlib
import json
import os
import queue
import shutil
import subprocess
import sys
import threading
import time
import urllib.request
from dataclasses import dataclass, field
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BASE = ROOT / "run" / "testservers"
CACHE = BASE / "cache"
USER_AGENT = "noskillanticheat-testserver/0.1 (local plugin smoke test)"
PLUGIN_NAME = "NoSkillAntiCheat"
BUILDTOOLS_URL = "https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar"


@dataclass
class Result:
    platform: str
    version: str
    status: str = "FAIL"
    startup_seconds: float | None = None
    notes: list[str] = field(default_factory=list)
    errors: list[str] = field(default_factory=list)


def http_get(url: str) -> bytes:
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(req, timeout=120) as resp:
        return resp.read()


def download(url: str, dest: Path, sha256: str | None = None, md5: str | None = None) -> Path:
    if dest.exists() and verify(dest, sha256, md5):
        return dest
    dest.parent.mkdir(parents=True, exist_ok=True)
    tmp = dest.with_suffix(dest.suffix + ".part")
    print(f"  downloading {url}")
    tmp.write_bytes(http_get(url))
    if not verify(tmp, sha256, md5):
        tmp.unlink()
        raise RuntimeError(f"checksum mismatch for {url}")
    tmp.replace(dest)
    return dest


def verify(path: Path, sha256: str | None, md5: str | None) -> bool:
    data = path.read_bytes()
    if sha256 and hashlib.sha256(data).hexdigest() != sha256.lower():
        return False
    if md5 and hashlib.md5(data).hexdigest() != md5.lower():
        return False
    return True


def fetch_server_jar(platform: str, version: str, buildtools_java: str, notes: list[str]) -> Path:
    if platform in ("paper", "folia"):
        info = json.loads(http_get(f"https://fill.papermc.io/v3/projects/{platform}/versions/{version}/builds/latest"))
        dl = info["downloads"]["server:default"]
        notes.append(f"{platform} build {info['id']} ({info.get('channel', '?')})")
        return download(dl["url"], CACHE / dl["name"], sha256=dl["checksums"]["sha256"])
    if platform == "purpur":
        info = json.loads(http_get(f"https://api.purpurmc.org/v2/purpur/{version}/latest"))
        notes.append(f"purpur build {info['build']}")
        return download(f"https://api.purpurmc.org/v2/purpur/{version}/{info['build']}/download",
                        CACHE / f"purpur-{version}-{info['build']}.jar", md5=info.get("md5"))
    if platform == "spigot":
        target = CACHE / f"spigot-{version}.jar"
        if target.exists():
            notes.append("spigot from cache (built earlier with BuildTools)")
            return target
        bt_dir = CACHE / "buildtools"
        bt = download(BUILDTOOLS_URL, bt_dir / "BuildTools.jar")
        print(f"  building Spigot {version} with BuildTools (this takes a while)")
        log = bt_dir / f"buildtools-{version}.log"
        with open(log, "w") as out:
            code = subprocess.call([buildtools_java, "-jar", str(bt), "--rev", version,
                                    "--output-dir", str(CACHE), "--disable-java-check"],
                                   cwd=bt_dir, stdout=out, stderr=subprocess.STDOUT)
        if code != 0 or not target.exists():
            raise RuntimeError(f"BuildTools failed for {version} (exit {code}), see {log}")
        notes.append("spigot built with BuildTools")
        return target
    raise ValueError(platform)


def find_plugin(explicit: str | None) -> Path:
    if explicit:
        return Path(explicit).resolve()
    jars = [p for p in glob.glob(str(ROOT / "bukkit/build/libs/noskillanticheat-bukkit-*.jar"))
            if not p.endswith(("-sources.jar", "-javadoc.jar"))]
    if not jars:
        sys.exit("No plugin jar found. Run ./gradlew build first.")
    return Path(max(jars, key=os.path.getmtime))


def is_plugin_error(line: str) -> bool:
    low = line.lower()
    if "ac.grim.grimac" in line and ("exception" in low or line.lstrip().startswith("at ")):
        return True
    if PLUGIN_NAME.lower() in low and any(k in low for k in ("error", "severe", "exception", "could not load")):
        return True
    return False


def run_server(platform: str, version: str, jar: Path, plugin: Path, java: str, port: int,
               timeout: int, accept_eula: bool, result: Result) -> None:
    work = BASE / f"{platform}-{version}"
    (work / "plugins").mkdir(parents=True, exist_ok=True)
    for old in (work / "plugins").glob("noskillanticheat-*.jar"):
        old.unlink()
    shutil.copy2(plugin, work / "plugins" / plugin.name)
    if accept_eula:
        (work / "eula.txt").write_text("# Accepted by tools/testserver.py --accept-eula (local test server)\neula=true\n")
    props = work / "server.properties"
    if not props.exists():
        props.write_text(f"server-port={port}\nonline-mode=false\nlevel-type=minecraft\\:flat\n"
                         "generate-structures=false\nspawn-protection=0\nmax-players=5\nview-distance=4\n")

    log_path = work / "smoke-test.log"
    proc = subprocess.Popen([java, "-Xms1G", "-Xmx2G", "-jar", str(jar), "--nogui"], cwd=work,
                            stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                            text=True, bufsize=1, errors="replace")
    lines: "queue.Queue[str | None]" = queue.Queue()

    def pump() -> None:
        assert proc.stdout
        with open(log_path, "w") as log:
            for line in proc.stdout:
                log.write(line)
                lines.put(line.rstrip("\n"))
        lines.put(None)

    threading.Thread(target=pump, daemon=True).start()
    start = time.time()
    enabled = done = version_ok = stop_sent = session_ok = False
    session_sent = False
    version_sent_at = None

    def send(cmd: str) -> None:
        try:
            assert proc.stdin
            proc.stdin.write(cmd + "\n")
            proc.stdin.flush()
        except (BrokenPipeError, OSError):
            pass

    while True:
        elapsed = time.time() - start
        if elapsed > timeout:
            result.errors.append(f"timeout after {timeout}s")
            break
        try:
            line = lines.get(timeout=1)
        except queue.Empty:
            line = ""
        if line is None:
            break
        if line:
            if f"Enabling {PLUGIN_NAME}" in line:
                enabled = True
            if "Done (" in line and not done:
                done = True
                result.startup_seconds = round(elapsed, 1)
                send("nsac version")
                version_sent_at = time.time()
            if f"{PLUGIN_NAME} Version:" in line:
                version_ok = True
                if not session_sent:
                    send("nsac session start smoke")
                    send("nsac session stop")
                    session_sent = True
            if "Report saved:" in line:
                session_ok = True
            if "You need to agree to the EULA" in line:
                result.errors.append("EULA not accepted (run with --accept-eula)")
            if is_plugin_error(line):
                result.errors.append(line.strip()[:300])
        if done and not stop_sent and ((version_ok and session_ok) or (version_sent_at and time.time() - version_sent_at > 15)):
            send("stop")
            stop_sent = True

    try:
        proc.wait(timeout=60)
    except subprocess.TimeoutExpired:
        proc.kill()
        result.errors.append("server did not stop within 60s, killed")

    if not enabled:
        result.errors.append(f"'{PLUGIN_NAME}' was never enabled")
    if not done:
        result.errors.append("server never finished starting")
    if done and not version_ok:
        result.errors.append("'nsac version' gave no answer")
    if version_ok and not session_ok:
        result.errors.append("'nsac session stop' wrote no report")
    result.notes.append(f"log: {log_path.relative_to(ROOT)}")
    result.status = "PASS" if (enabled and done and version_ok and session_ok and not result.errors) else "FAIL"


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--platforms", default="paper,folia,purpur,spigot")
    ap.add_argument("--versions", default="26.3,26.2,26.1.2")
    ap.add_argument("--plugin", help="plugin jar (default: newest in bukkit/build/libs)")
    ap.add_argument("--java", default="/usr/lib/jvm/java-25-openjdk/bin/java", help="Java used to run servers")
    ap.add_argument("--buildtools-java", default="/usr/lib/jvm/java-27-openjdk/bin/java",
                    help="JDK java used to run Spigot BuildTools (needs a compiler)")
    ap.add_argument("--timeout", type=int, default=300, help="seconds per server")
    ap.add_argument("--accept-eula", action="store_true", help="accept the Minecraft EULA for the test servers")
    args = ap.parse_args()

    plugin = find_plugin(args.plugin)
    print(f"plugin: {plugin.name}")
    results: list[Result] = []
    port = 25600
    for version in args.versions.split(","):
        for platform in args.platforms.split(","):
            res = Result(platform, version)
            results.append(res)
            print(f"== {platform} {version}")
            try:
                jar = fetch_server_jar(platform, version, args.buildtools_java, res.notes)
                run_server(platform, version, jar, plugin, args.java, port, args.timeout, args.accept_eula, res)
            except Exception as e:  # report and continue with the next server
                res.status = "SKIP" if "404" in str(e) else "FAIL"
                res.errors.append(f"{type(e).__name__}: {e}")
            port += 1
            print(f"   {res.status} {'; '.join(res.errors[:3])}")

    BASE.mkdir(parents=True, exist_ok=True)
    report = BASE / "report.md"
    with open(report, "w") as f:
        f.write(f"# Smoke test {time.strftime('%Y-%m-%d %H:%M')}\n\nPlugin: `{plugin.name}`\n\n")
        f.write("| Platform | Version | Result | Startup (s) | Notes |\n|---|---|---|---|---|\n")
        for r in results:
            f.write(f"| {r.platform} | {r.version} | {r.status} | {r.startup_seconds or '-'} | {'; '.join(r.notes)} |\n")
        for r in results:
            if r.errors:
                f.write(f"\n## {r.platform} {r.version}\n\n" + "\n".join(f"- `{e}`" for e in r.errors[:20]) + "\n")
    print(f"report: {report.relative_to(ROOT)}")
    return 0 if all(r.status == "PASS" for r in results) else 1


if __name__ == "__main__":
    sys.exit(main())
