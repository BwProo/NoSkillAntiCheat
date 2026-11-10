#!/usr/bin/env python3
"""Builds the project website (docs/index.html in German, docs/en/index.html in English).

    python3 tools/site/build_site.py

Both pages come from one template, so design and structure always stay in sync; only the
text differs. Edit the TEXT dictionaries below, then rerun the script.
"""
from __future__ import annotations

import html
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SITE = "https://bwproo.github.io/NoSkillAntiCheat/"
REPO = "https://github.com/BwProo/NoSkillAntiCheat"
DL = REPO + "/releases/latest/download/"
VERIFY = "jn2XHEruDXjeotBXhscfzFm8PJfJhptaRFJvevp9qVM"

# Alert lines for the hero console: (player, check, count, experimental)
FEED = [
    ("Steve", "Flight", 3, False), ("x_Nova", "Reach", 1, False), ("Steve", "Flight", 7, False),
    ("CrystalKid", "CrystalAura/FastCrystal", 2, True), ("mxrv", "KillAura (aim lock)", 1, True),
    ("Builder42", "Scaffold (rotation)", 4, False), ("mxrv", "AutoClicker (too consistent)", 2, True),
    ("Digger", "Nuker/Baritone (no swing)", 12, False), ("Kai", "Velocity (anti-knockback)", 5, False),
    ("Lumi", "Hacked client", 1, False), ("Digger", "Xray (mining pattern)", 1, True),
    ("Kai", "Timer (game speed)", 3, False), ("x_Nova", "TriggerBot", 1, True),
]

CATEGORIES = {
    "movement": ["Flight", "Speed", "Step", "Spider", "WaterWalk / Jesus", "NoFall", "NoSlow", "Strafe",
                 "Timer", "Blink", "Phase", "NoJumpDelay", "ElytraFly", "BoatFly"],
    "combat": ["KillAura", "Reach", "Hitboxes", "AimAssist", "TriggerBot", "AutoClicker", "Velocity",
               "CrystalAura", "AnchorAura", "AutoBlock", "ShieldBreaker / MaceSwap"],
    "client": ["Meteor (sign probe)", "Brand and mod channels", "Plugin list hidden", "Server type hidden"],
    "world": ["Scaffold", "Tower / FastPlace", "Nuker", "FastBreak", "Baritone #mine", "Xray (mining pattern)",
              "Paper anti-xray"],
    "inventory": ["InvMove", "ChestStealer", "AutoArmor", "Inventory macros", "NoRotate"],
}

TEXT = {
    "de": {
        "lang": "de", "locale": "de_DE", "path": "docs/index.html", "self": SITE, "other": SITE + "en/",
        "other_label": "English", "other_short": "EN",
        "title": "NoSkillAntiCheat – kostenloses Minecraft-Anticheat",
        "desc": "Kostenloses, leichtes Anticheat für Paper, Folia, Spigot und Purpur 26.x – mit Bewegungs-Simulation, Kampf-Checks, lokaler KI und Meteor-Erkennung.",
        "nav": ["Erkennung", "Funktionsweise", "Installation", "Befehle", "FAQ"],
        "eyebrow": "Kostenlos · Open Source · Minecraft 26.x",
        "h1": "Das Anticheat, das Cheater beim Namen nennt.",
        "lead": "NoSkillAntiCheat rechnet jede Bewegung nach, prüft Kampfverhalten und erkennt Cheat-Clients wie Meteor schon beim Beitritt. Kostenlos, ohne Cloud und so leicht, dass dein Server nichts davon merkt.",
        "cta_plugin": "Plugin herunterladen", "cta_proxy": "Velocity-Add-on", "cta_code": "Quellcode",
        "platforms": "Paper · Folia · Purpur · Spigot · Velocity 4.x · Java 25",
        "console_title": "Alarme im Team-Chat",
        "console_note": "* experimenteller Check – meldet, bestraft aber erst nach Freigabe",
        "catch_h": "Was erkannt wird",
        "catch_sub": "Über 130 Checks in fünf Bereichen. Jeder Alarm sagt dir, welcher Cheat es war.",
        "cats": {"movement": "Bewegung", "combat": "Kampf", "client": "Client", "world": "Welt", "inventory": "Inventar"},
        "limit": "Was kein Anticheat sieht: ESP, Tracers, Fullbright, Freecam und andere reine Anzeige-Cheats senden nichts an den Server. Gegen Xray hilft der eingebaute Anti-Xray, der Erze verschleiert.",
        "how_h": "Wie die Erkennung funktioniert",
        "diag": {"pred": "vorhergesagt", "act": "tatsächlich", "ground": "Boden", "offset": "Abweichung", "caption": "Ein Fly-Cheat: Die Physik sagt „fallen“, der Spieler bleibt oben. Die Abweichung wird gemessen und benannt."},
        "nav_dl": "Download",
        "how": [
            ("Simulation", "Jede Bewegung wird mit der echten Minecraft-Physik nachgerechnet, inklusive Ping-Ausgleich. Wer fliegt oder zu schnell läuft, wird zurückgesetzt."),
            ("Heuristiken", "Kampf, Klicks, Inventar und Abbauen werden statistisch geprüft: zu gleichmäßig, zu schnell oder unmöglich präzise fällt auf."),
            ("Lokale KI", "Aus aufgenommenen Sitzungen trainierst du ein eigenes Modell. Es bewertet Kampfverhalten direkt im Plugin, ohne Daten zu verschicken."),
        ],
        "install_h": "In vier Schritten startklar",
        "install": [
            ("Herunterladen", f'<a href="{DL}NoSkillAntiCheat.jar">NoSkillAntiCheat.jar</a> lädt immer die neueste Version.'),
            ("In den Plugin-Ordner legen", "Funktioniert auf Paper, Folia, Purpur und Spigot mit Minecraft 26.x und Java 25."),
            ("Server starten", "Die Config wird erzeugt und der Anti-Xray eingeschaltet. Einmal neu starten, dann ist er aktiv."),
            ("Mit Velocity?", f'<a href="{DL}NoSkillAntiCheat-Velocity.jar">NoSkillAntiCheat-Velocity.jar</a> nur auf den Proxy legen. Er versteckt dort den Server-Typ.'),
        ],
        "cmd_h": "Befehle und Rechte",
        "cmd_sub": "Alle Befehle beginnen mit /nsac. Rechte heißen nsac.*.",
        "cmd_head": ("Befehl", "Was er tut", "Recht"),
        "cmds": [
            ("/nsac alerts", "Alarme an- oder ausschalten", "nsac.alerts"),
            ("/nsac top", "Spieler mit den meisten Verstößen", "nsac.top"),
            ("/nsac spectate <spieler>", "Einem Verdächtigen zuschauen", "nsac.spectate"),
            ("/nsac knockback <spieler>", "Test-Rückstoß senden", "nsac.knockback"),
            ("/nsac session start <label>", "Fehlalarme pro Check messen", "nsac.stats"),
            ("/nsac record start <spieler> <label>", "Sitzung für die KI aufnehmen", "nsac.record"),
            ("/nsac history <spieler>", "Verlauf der Verstöße", "nsac.history"),
            ("–", "/pl, /version und /help nutzen und in Tab sehen", "nsac.commands.bypass"),
        ],
        "faq_h": "Häufige Fragen",
        "faq": [
            ("Ist es wirklich kostenlos?", "Ja. NoSkillAntiCheat steht unter der GPL-3.0, ohne bezahlte Funktionen und ohne Telemetrie."),
            ("Werden Spieler mit hohem Ping geflaggt?", "Die Simulation gleicht den Ping aus. Neue Checks melden zuerst nur und bestrafen erst, wenn du sie freigibst."),
            ("Erkennt es Meteor?", "Ja, beim Beitritt über die Schild-Probe, solange Meteor sie nicht blockiert. Alles Weitere fällt über das Verhalten auf."),
            ("Funktioniert es mit TotemGuard?", f'Ja. AutoTotem übernimmt das optionale Plugin <a href="https://github.com/Bram1903/TotemGuard">TotemGuard</a>. NoSkillAntiCheat bringt PacketEvents umbenannt mit, es gibt keine Konflikte.'),
            ("Woher kommt der Code?", f'Es ist ein Fork von <a href="https://github.com/GrimAnticheat/Grim">GrimAC</a> (GPL-3.0). Alle Änderungen stehen in der <a href="{REPO}/blob/main/NOTICE.md">NOTICE.md</a>.'),
        ],
        "foot_left": "NoSkillAntiCheat · basiert auf GrimAC · GPL-3.0",
        "foot_links": [("GitHub", REPO), ("Releases", REPO + "/releases"), ("Fehler melden", REPO + "/issues")],
        "claude": "Komplett mit Claude (KI von Anthropic) gebaut, auf Basis des Open-Source-Codes von GrimAC.",
        "currency": "EUR",
    },
    "en": {
        "lang": "en", "locale": "en_US", "path": "docs/en/index.html", "self": SITE + "en/", "other": SITE,
        "other_label": "Deutsch", "other_short": "DE",
        "title": "NoSkillAntiCheat – free Minecraft anticheat",
        "desc": "Free, lightweight anticheat for Paper, Folia, Spigot and Purpur 26.x – with movement simulation, combat checks, local AI and Meteor detection.",
        "nav": ["Detection", "How it works", "Install", "Commands", "FAQ"],
        "eyebrow": "Free · Open source · Minecraft 26.x",
        "h1": "The anticheat that calls cheats by their name.",
        "lead": "NoSkillAntiCheat recalculates every movement, checks combat behaviour and catches hacked clients such as Meteor the moment they join. Free, no cloud, and light enough that your server never notices.",
        "cta_plugin": "Download plugin", "cta_proxy": "Velocity add-on", "cta_code": "Source code",
        "platforms": "Paper · Folia · Purpur · Spigot · Velocity 4.x · Java 25",
        "console_title": "Alerts in staff chat",
        "console_note": "* experimental check – alerts, but only punishes once you enable it",
        "catch_h": "What it catches",
        "catch_sub": "More than 130 checks in five areas. Every alert tells you which cheat it was.",
        "cats": {"movement": "Movement", "combat": "Combat", "client": "Client", "world": "World", "inventory": "Inventory"},
        "limit": "What no anticheat can see: ESP, Tracers, Fullbright, Freecam and other visual-only cheats send nothing to the server. Against Xray, the built-in anti-xray hides ores from clients.",
        "how_h": "How detection works",
        "diag": {"pred": "predicted", "act": "actual", "ground": "ground", "offset": "offset", "caption": "A fly cheat: physics says fall, the player stays up. The offset is measured and named."},
        "nav_dl": "Download",
        "how": [
            ("Simulation", "Every movement is recalculated with real Minecraft physics, including latency compensation. Flying or running too fast gets you set back."),
            ("Heuristics", "Combat, clicks, inventory and mining are checked statistically: too regular, too fast or impossibly precise stands out."),
            ("Local AI", "Train your own model from recorded sessions. It scores combat behaviour inside the plugin and never sends data anywhere."),
        ],
        "install_h": "Ready in four steps",
        "install": [
            ("Download", f'<a href="{DL}NoSkillAntiCheat.jar">NoSkillAntiCheat.jar</a> always gets the latest version.'),
            ("Put it into plugins/", "Works on Paper, Folia, Purpur and Spigot with Minecraft 26.x and Java 25."),
            ("Start the server", "The config is created and the anti-xray is switched on. Restart once to activate it."),
            ("Using Velocity?", f'Put <a href="{DL}NoSkillAntiCheat-Velocity.jar">NoSkillAntiCheat-Velocity.jar</a> on the proxy only. It hides the server type there.'),
        ],
        "cmd_h": "Commands and permissions",
        "cmd_sub": "All commands start with /nsac. Permissions are named nsac.*.",
        "cmd_head": ("Command", "What it does", "Permission"),
        "cmds": [
            ("/nsac alerts", "Turn alerts on or off", "nsac.alerts"),
            ("/nsac top", "Players with the most violations", "nsac.top"),
            ("/nsac spectate <player>", "Spectate a suspect", "nsac.spectate"),
            ("/nsac knockback <player>", "Send test knockback", "nsac.knockback"),
            ("/nsac session start <label>", "Measure false positives per check", "nsac.stats"),
            ("/nsac record start <player> <label>", "Record a session for the AI", "nsac.record"),
            ("/nsac history <player>", "Violation history", "nsac.history"),
            ("–", "Use /pl, /version and /help and see them in tab", "nsac.commands.bypass"),
        ],
        "faq_h": "Questions",
        "faq": [
            ("Is it really free?", "Yes. NoSkillAntiCheat is GPL-3.0, with no paid features and no telemetry."),
            ("Are high-ping players flagged?", "The simulation compensates for ping. New checks only alert at first and punish once you enable them."),
            ("Does it detect Meteor?", "Yes, on join with the sign probe, as long as Meteor does not block it. Everything else is caught by behaviour."),
            ("Does it work with TotemGuard?", 'Yes. AutoTotem is left to the optional <a href="https://github.com/Bram1903/TotemGuard">TotemGuard</a> plugin. NoSkillAntiCheat ships a relocated PacketEvents, so there are no conflicts.'),
            ("Where does the code come from?", f'It is a fork of <a href="https://github.com/GrimAnticheat/Grim">GrimAC</a> (GPL-3.0). All changes are listed in <a href="{REPO}/blob/main/NOTICE.md">NOTICE.md</a>.'),
        ],
        "foot_left": "NoSkillAntiCheat · based on GrimAC · GPL-3.0",
        "foot_links": [("GitHub", REPO), ("Releases", REPO + "/releases"), ("Report a bug", REPO + "/issues")],
        "claude": "Fully built with Claude (AI by Anthropic), on top of GrimAC's open-source code.",
        "currency": "USD",
    },
}

CSS = """
:root{
  --paper:#f4f3f8; --surface:#ffffff; --ink:#16131f; --muted:#5d5870; --line:#dddae6;
  --violet:#6d28d9; --violet-ink:#4c1d95; --lilac:#ede9fb; --console:#191622; --console-line:#2c2838;
  --ok:#15803d; --exp:#b45309;
  --display:"Archivo",system-ui,sans-serif; --body:"IBM Plex Sans",system-ui,sans-serif; --mono:"JetBrains Mono",ui-monospace,monospace;
}
*{box-sizing:border-box}
html{scroll-behavior:smooth;-webkit-text-size-adjust:100%}
body{margin:0;background:var(--paper);color:var(--ink);font:17px/1.6 var(--body)}
a{color:var(--violet)}
a:hover{color:var(--violet-ink)}
:focus-visible{outline:3px solid var(--violet);outline-offset:3px;border-radius:4px}
.wrap{max-width:1160px;margin:0 auto;padding:0 24px}
code,.mono{font-family:var(--mono);font-size:.86em}
/* nav */
.nav{position:sticky;top:0;z-index:10;background:rgba(244,243,248,.86);backdrop-filter:saturate(1.4) blur(10px);border-bottom:1px solid transparent;transition:border-color .3s}
.nav.scrolled{border-bottom-color:var(--line)}
.nav .wrap{display:flex;align-items:center;gap:28px;height:64px}
.brand{display:flex;align-items:center;gap:10px;font:800 1.02rem/1 var(--display);font-stretch:112%;color:var(--ink);text-decoration:none;letter-spacing:.2px}
.brand img{width:26px;height:26px;border-radius:6px}
.nav ul{display:flex;gap:22px;margin:0 0 0 auto;padding:0;list-style:none}
.nav ul a{color:var(--muted);text-decoration:none;font-size:.94rem}
.nav ul a:hover{color:var(--ink)}
.lang{font:600 .8rem/1 var(--mono);border:1px solid var(--line);border-radius:6px;padding:6px 8px;color:var(--ink);text-decoration:none}
/* hero */
.hero{padding:76px 0 88px}
.hero .wrap{display:grid;grid-template-columns:1.05fr .95fr;gap:56px;align-items:center}
.eyebrow{font:600 .78rem/1 var(--mono);letter-spacing:.08em;text-transform:uppercase;color:var(--violet)}
h1{font:800 clamp(2.5rem,5.4vw,4.4rem)/1.02 var(--display);font-stretch:118%;letter-spacing:-.02em;margin:18px 0 22px}
h1 em{font-style:normal;color:var(--violet)}
.lead{font-size:1.14rem;color:var(--muted);max-width:34em;margin:0 0 32px}
.ctas{display:flex;flex-wrap:wrap;gap:12px}
.btn{display:inline-flex;align-items:center;gap:10px;padding:13px 20px;border-radius:10px;font:600 .98rem/1 var(--body);text-decoration:none;border:1px solid var(--line);color:var(--ink);background:var(--surface);transition:transform .15s ease,border-color .15s,background .15s}
.btn:hover{transform:translateY(-2px);border-color:var(--ink);color:var(--ink)}
.btn.primary{background:var(--violet);border-color:var(--violet);color:#fff}
.btn.primary:hover{background:var(--violet-ink);border-color:var(--violet-ink);color:#fff}
.btn svg{width:16px;height:16px}
.platforms{margin-top:22px;font:500 .84rem/1.5 var(--mono);color:var(--muted)}
/* console (signature) */
.console{background:var(--console);border-radius:14px;box-shadow:0 30px 60px -28px rgba(22,19,31,.45);overflow:hidden;font:500 .86rem/1.65 var(--mono);color:#d8d4e6}
.console header{display:flex;align-items:center;gap:8px;padding:12px 16px;border-bottom:1px solid var(--console-line);color:#8f89a6;font-size:.78rem}
.console header i{width:10px;height:10px;border-radius:50%;background:#3a3548;display:inline-block}
.console header span{margin-left:8px}
.feed{list-style:none;margin:0;padding:14px 16px;height:318px;overflow:hidden;display:flex;flex-direction:column;justify-content:flex-end;gap:2px}
.feed li{white-space:nowrap;overflow:hidden;text-overflow:ellipsis;animation:line-in .45s ease both}
.feed .p{color:#a78bfa;font-weight:700}
.feed .s{color:#6f6984}
.feed .n{color:#fff}
.feed .c{color:#f1a7c4}
.feed .x{color:#8f89a6}
.console footer{padding:10px 16px;border-top:1px solid var(--console-line);color:#8f89a6;font-size:.74rem}
@keyframes line-in{from{opacity:0;transform:translateY(8px)}to{opacity:1;transform:none}}
/* sections */
section{padding:88px 0;border-top:1px solid var(--line)}
h2{font:800 clamp(1.8rem,3.2vw,2.5rem)/1.1 var(--display);font-stretch:112%;letter-spacing:-.01em;margin:0 0 12px}
.sub{color:var(--muted);margin:0 0 40px;max-width:40em}
/* categories */
.cats{display:grid;grid-template-columns:repeat(5,1fr);gap:0;border:1px solid var(--line);border-radius:14px;background:var(--surface);overflow:hidden}
.cat{padding:22px 20px;border-right:1px solid var(--line)}
.cat:last-child{border-right:0}
.cat h3{font:700 .8rem/1 var(--mono);letter-spacing:.07em;text-transform:uppercase;color:var(--violet);margin:0 0 14px}
.cat ul{list-style:none;margin:0;padding:0;font-size:.95rem}
.cat li{padding:5px 0;border-bottom:1px dashed var(--line)}
.cat li:last-child{border-bottom:0}
.limit{margin:26px 0 0;color:var(--muted);font-size:.95rem;max-width:52em}
/* how */
.how{display:grid;grid-template-columns:repeat(3,1fr);gap:32px}
.how h3{font:700 1.15rem/1.2 var(--display);font-stretch:108%;margin:0 0 10px}
.how p{margin:0;color:var(--muted)}
.how div{border-top:3px solid var(--ink);padding-top:18px}
.how-wrap{display:grid;grid-template-columns:1.15fr .85fr;gap:48px;align-items:start;margin-top:36px}
.how-wrap .how{grid-template-columns:1fr;gap:26px}
.diagram{background:var(--surface);border:1px solid var(--line);border-radius:14px;padding:22px 22px 16px}
.diagram svg{width:100%;height:auto;display:block}
.diagram figcaption{color:var(--muted);font-size:.9rem;margin-top:10px}
.d-grid{stroke:var(--line);stroke-width:1}
.d-ground{stroke:var(--ink);stroke-width:2}
.d-label{font:500 12px var(--mono);fill:var(--muted)}
.d-pred{fill:none;stroke:#9a95ad;stroke-width:2.5;stroke-dasharray:6 6}
.d-act{fill:none;stroke:var(--violet);stroke-width:3.5;stroke-linecap:round;stroke-dasharray:520;stroke-dashoffset:520}
.d-off{stroke:#c2410c;stroke-width:2;opacity:0}
.d-offlbl{font:600 12px var(--mono);fill:#c2410c;opacity:0}
.d-chip{opacity:0}
.d-chip rect{fill:var(--console)}
.d-chip text{font:600 12.5px var(--mono)}
.diagram.play .d-act{animation:draw 2.2s .3s ease-out forwards}
.diagram.play .d-off,.diagram.play .d-offlbl{animation:fade .5s 2.5s forwards}
.diagram.play .d-chip{animation:chip .5s 3.1s forwards}
@keyframes draw{to{stroke-dashoffset:0}}
@keyframes fade{to{opacity:1}}
@keyframes chip{from{opacity:0;transform:translateY(6px)}to{opacity:1;transform:none}}
.nav .dl{font:600 .86rem/1 var(--body);background:var(--violet);color:#fff;padding:9px 14px;border-radius:8px;text-decoration:none;transition:background .15s}
.nav .dl:hover{background:var(--violet-ink);color:#fff}
.cat h3 span{color:var(--muted);font-weight:500;margin-left:6px}
/* install: a real sequence, so it is numbered */
.steps{list-style:none;margin:0;padding:0;display:grid;grid-template-columns:repeat(4,1fr);gap:20px;counter-reset:step}
.steps li{counter-increment:step;background:var(--surface);border:1px solid var(--line);border-radius:12px;padding:22px}
.steps li::before{content:counter(step);display:block;font:800 2.2rem/1 var(--display);font-stretch:120%;color:var(--violet);margin-bottom:14px}
.steps h3{margin:0 0 6px;font-size:1.02rem}
.steps p{margin:0;color:var(--muted);font-size:.95rem}
/* commands */
.table{width:100%;border-collapse:collapse;background:var(--surface);border:1px solid var(--line);border-radius:12px;overflow:hidden;font-size:.95rem}
.table th,.table td{text-align:left;padding:12px 16px;border-bottom:1px solid var(--line);vertical-align:top}
.table th{font:600 .76rem/1 var(--mono);letter-spacing:.06em;text-transform:uppercase;color:var(--muted);background:#faf9fc}
.table tr:last-child td{border-bottom:0}
.table td:first-child,.table td:last-child{font-family:var(--mono);font-size:.84rem;white-space:nowrap}
.scroll{overflow-x:auto}
/* faq */
details{border-bottom:1px solid var(--line);padding:18px 0}
summary{cursor:pointer;font-weight:600;list-style:none;display:flex;justify-content:space-between;gap:16px}
summary::-webkit-details-marker{display:none}
summary::after{content:"+";font:400 1.4rem/1 var(--mono);color:var(--violet);transition:transform .2s}
details[open] summary::after{transform:rotate(45deg)}
details p{color:var(--muted);margin:10px 0 0;max-width:46em}
/* footer */
footer.site{padding:40px 0;border-top:1px solid var(--line);color:var(--muted);font-size:.9rem}
footer.site .wrap{display:flex;flex-wrap:wrap;gap:12px 28px;justify-content:space-between}
footer.site a{color:var(--muted)}
footer.site .claude{flex-basis:100%;font-size:.85rem}
/* motion */
.rise{opacity:0;transform:translateY(16px);transition:opacity .7s ease,transform .7s ease}
.rise.in{opacity:1;transform:none}
.hero .rise:nth-child(2){transition-delay:.08s}.hero .rise:nth-child(3){transition-delay:.16s}.hero .rise:nth-child(4){transition-delay:.24s}.hero .rise:nth-child(5){transition-delay:.32s}
@media (prefers-reduced-motion:reduce){*{animation:none!important;transition:none!important;scroll-behavior:auto!important}.rise{opacity:1;transform:none}.d-act{stroke-dashoffset:0}.d-off,.d-offlbl,.d-chip{opacity:1}}
/* responsive */
@media (max-width:980px){.how-wrap{grid-template-columns:1fr}.nav .dl{display:none}.hero .wrap{grid-template-columns:1fr}.cats{grid-template-columns:1fr 1fr}.cat{border-bottom:1px solid var(--line)}.steps{grid-template-columns:1fr 1fr}.how{grid-template-columns:1fr}.nav ul{display:none}.lang{margin-left:auto}}
@media (max-width:560px){.wrap{padding:0 16px}.hero{padding:48px 0 60px}section{padding:60px 0}.cats,.steps{grid-template-columns:1fr}.cat{border-right:0}.feed{height:260px;font-size:.78rem}}
"""

JS = """
(function(){
  var nav=document.querySelector('.nav');
  addEventListener('scroll',function(){nav.classList.toggle('scrolled',scrollY>8)},{passive:true});
  var reduce=matchMedia('(prefers-reduced-motion: reduce)').matches;
  var els=document.querySelectorAll('.rise');
  if(reduce||!('IntersectionObserver' in window)){els.forEach(function(e){e.classList.add('in')});}
  else{var io=new IntersectionObserver(function(es){es.forEach(function(e){if(e.isIntersecting){e.target.classList.add('in');io.unobserve(e.target)}})},{threshold:.12});els.forEach(function(e){io.observe(e)});}
  var dg=document.querySelector('.diagram');
  if(dg){ if(reduce){dg.classList.add('play');} else if('IntersectionObserver' in window){
    var dio=new IntersectionObserver(function(es){es.forEach(function(e){if(e.isIntersecting){dg.classList.add('play');dio.disconnect();
      setInterval(function(){dg.classList.remove('play');void dg.offsetWidth;dg.classList.add('play')},9000);}})},{threshold:.4});dio.observe(dg);} else dg.classList.add('play'); }
  var feed=document.querySelector('.feed'); if(!feed) return;
  var lines=JSON.parse(feed.dataset.lines), i=0, max=12;
  function add(){var l=lines[i%lines.length];i++;var li=document.createElement('li');
    li.innerHTML='<span class="p">NSAC</span> <span class="s">»</span> <span class="n">'+l[0]+'</span> <span class="s">»</span> <span class="c">'+l[1]+(l[3]?'*':'')+'</span> <span class="x">(x'+l[2]+')</span>';
    feed.appendChild(li); while(feed.children.length>max) feed.removeChild(feed.firstChild);}
  for(var k=0;k<5;k++) add();
  if(!reduce) setInterval(add,1600);
})();
"""

SHIELD = '<svg viewBox="0 0 64 64" aria-hidden="true"><path d="M32 4 8 14v16c0 15 10 26 24 30 14-4 24-15 24-30V14z" fill="#6d28d9"/><path d="M22 33l7 7 14-15" stroke="#fff" stroke-width="6" fill="none" stroke-linecap="round" stroke-linejoin="round"/></svg>'
DOWN = '<svg viewBox="0 0 16 16" aria-hidden="true"><path d="M8 2v9m0 0L4.5 7.5M8 11l3.5-3.5M3 14h10" stroke="currentColor" stroke-width="1.7" fill="none" stroke-linecap="round" stroke-linejoin="round"/></svg>'


def esc(s: str) -> str:
    return html.escape(s, quote=False)


def diagram(t: dict) -> str:
    d = t["diag"]
    # x: time, y: height. Predicted jump arc (falls back) vs actual (keeps rising, then hovers).
    return f"""<figure class="diagram rise" aria-label="{esc(d['caption'])}">
<svg viewBox="0 0 520 260" role="img">
  <line class="d-grid" x1="40" y1="60" x2="500" y2="60"/><line class="d-grid" x1="40" y1="120" x2="500" y2="120"/><line class="d-grid" x1="40" y1="180" x2="500" y2="180"/>
  <line class="d-ground" x1="30" y1="220" x2="505" y2="220"/>
  <text class="d-label" x="34" y="242">{esc(d['ground'])}</text>
  <path class="d-pred" d="M50 220 C 130 110, 200 110, 280 220"/>
  <text class="d-label" x="210" y="158">{esc(d['pred'])}</text>
  <path class="d-act" d="M50 220 C 120 120, 170 95, 230 92 S 400 92, 480 92"/>
  <text class="d-label" x="404" y="80" style="fill:#6d28d9">{esc(d['act'])}</text>
  <line class="d-off" x1="340" y1="92" x2="340" y2="220" stroke-dasharray="4 4"/>
  <text class="d-offlbl" x="348" y="160">{esc(d['offset'])}</text>
  <g class="d-chip"><rect x="250" y="16" width="230" height="30" rx="7"/>
    <text x="262" y="36"><tspan fill="#a78bfa">NSAC</tspan><tspan fill="#6f6984"> » </tspan><tspan fill="#ffffff">Steve</tspan><tspan fill="#6f6984"> » </tspan><tspan fill="#f1a7c4">Flight</tspan><tspan fill="#8f89a6"> (x1)</tspan></text></g>
</svg>
<figcaption>{esc(d['caption'])}</figcaption>
</figure>"""


def page(t: dict) -> str:
    ids = ["checks", "how", "install", "commands", "faq"]
    nav = "".join(f'<li><a href="#{i}">{esc(n)}</a></li>' for i, n in zip(ids, t["nav"]))
    other_rel = "en/" if t["lang"] == "de" else "../"
    h1 = esc(t["h1"])
    # highlight one phrase in purple: the cheats' names are the product's promise
    key = "beim Namen nennt" if t["lang"] == "de" else "by their name"
    h1 = h1.replace(key, f"<em>{key}</em>")
    cats = "".join(
        f'<div class="cat"><h3>{esc(t["cats"][k])}<span>{len(v)}</span></h3><ul>' + "".join(f"<li>{esc(x)}</li>" for x in v) + "</ul></div>"
        for k, v in CATEGORIES.items())
    how = "".join(f"<div class=\"rise\"><h3>{esc(h)}</h3><p>{esc(p)}</p></div>" for h, p in t["how"])
    steps = "".join(f"<li class=\"rise\"><h3>{esc(h)}</h3><p>{p}</p></li>" for h, p in t["install"])
    rows = "".join(f"<tr><td>{esc(a)}</td><td>{esc(b)}</td><td>{esc(c)}</td></tr>" for a, b, c in t["cmds"])
    head = "".join(f"<th>{esc(h)}</th>" for h in t["cmd_head"])
    faq = "".join(f"<details><summary>{esc(q)}</summary><p>{a}</p></details>" for q, a in t["faq"])
    foot = " · ".join(f'<a href="{u}">{esc(n)}</a>' for n, u in t["foot_links"])
    ld = json.dumps({"@context": "https://schema.org", "@type": "SoftwareApplication", "name": "NoSkillAntiCheat",
                     "applicationCategory": "GameApplication",
                     "operatingSystem": "Paper, Folia, Purpur, Spigot (Minecraft 26.x), Velocity 4.x",
                     "description": t["desc"], "url": t["self"], "downloadUrl": DL + "NoSkillAntiCheat.jar",
                     "codeRepository": REPO, "image": SITE + "icon-512.png", "license": "https://www.gnu.org/licenses/gpl-3.0.html",
                     "offers": {"@type": "Offer", "price": "0", "priceCurrency": t["currency"]}}, ensure_ascii=False)
    feed = html.escape(json.dumps(FEED, ensure_ascii=False))
    site_ld = json.dumps({"@context": "https://schema.org", "@type": "WebSite", "name": "NoSkillAntiCheat",
                          "alternateName": ["NSAC", "NoSkillAntiCheat Minecraft Anticheat"], "url": SITE,
                          "inLanguage": t["lang"]}, ensure_ascii=False)
    return f"""<!doctype html>
<html lang="{t['lang']}">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{esc(t['title'])}</title>
<meta name="description" content="{esc(t['desc'])}">
<link rel="canonical" href="{t['self']}">
<link rel="alternate" hreflang="de" href="{SITE}">
<link rel="alternate" hreflang="en" href="{SITE}en/">
<link rel="alternate" hreflang="x-default" href="{SITE}en/">
<meta name="robots" content="index, follow">
<meta name="google-site-verification" content="{VERIFY}" />
<meta property="og:type" content="website">
<meta property="og:title" content="{esc(t['title'])}">
<meta property="og:description" content="{esc(t['desc'])}">
<meta property="og:url" content="{t['self']}">
<meta property="og:locale" content="{t['locale']}">
<meta property="og:site_name" content="NoSkillAntiCheat">
<meta property="og:image" content="{SITE}og-image.png">
<meta property="og:image:width" content="1200">
<meta property="og:image:height" content="630">
<meta name="twitter:card" content="summary_large_image">
<meta name="application-name" content="NoSkillAntiCheat">
<meta name="theme-color" content="#6d28d9">
<link rel="icon" type="image/svg+xml" href="{SITE}favicon.svg">
<link rel="icon" type="image/png" sizes="48x48" href="{SITE}favicon-48.png">
<link rel="icon" type="image/png" sizes="192x192" href="{SITE}icon-192.png">
<link rel="apple-touch-icon" href="{SITE}apple-touch-icon.png">
<link rel="manifest" href="{SITE}site.webmanifest">
<script type="application/ld+json">{site_ld}</script>
<script type="application/ld+json">{ld}</script>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Archivo:wdth,wght@100..125,600..900&family=IBM+Plex+Sans:wght@400;500;600&family=JetBrains+Mono:wght@500;600;700&display=swap" rel="stylesheet">
<style>{CSS}</style>
</head>
<body>
<nav class="nav"><div class="wrap">
  <a class="brand" href="#"><img src="{SITE}favicon.svg" alt="" width="26" height="26">NoSkillAntiCheat</a>
  <ul>{nav}</ul>
  <a class="dl" href="{DL}NoSkillAntiCheat.jar">{esc(t['nav_dl'])}</a>
  <a class="lang" href="{other_rel}" hreflang="{'en' if t['lang']=='de' else 'de'}">{t['other_short']}</a>
</div></nav>

<header class="hero"><div class="wrap">
  <div>
    <div class="eyebrow rise">{esc(t['eyebrow'])}</div>
    <h1 class="rise">{h1}</h1>
    <p class="lead rise">{esc(t['lead'])}</p>
    <div class="ctas rise">
      <a class="btn primary" href="{DL}NoSkillAntiCheat.jar">{DOWN}{esc(t['cta_plugin'])}</a>
      <a class="btn" href="{DL}NoSkillAntiCheat-Velocity.jar">{DOWN}{esc(t['cta_proxy'])}</a>
      <a class="btn" href="{REPO}">{esc(t['cta_code'])}</a>
    </div>
    <div class="platforms rise">{esc(t['platforms'])}</div>
  </div>
  <div class="console rise" aria-label="{esc(t['console_title'])}">
    <header><i></i><i></i><i></i><span>{esc(t['console_title'])}</span></header>
    <ul class="feed" data-lines="{feed}" aria-live="off"></ul>
    <footer>{esc(t['console_note'])}</footer>
  </div>
</div></header>

<section id="checks"><div class="wrap">
  <h2 class="rise">{esc(t['catch_h'])}</h2>
  <p class="sub rise">{esc(t['catch_sub'])}</p>
  <div class="cats rise">{cats}</div>
  <p class="limit rise">{esc(t['limit'])}</p>
</div></section>

<section id="how"><div class="wrap">
  <h2 class="rise">{esc(t['how_h'])}</h2>
  <div class="how-wrap">{diagram(t)}<div class="how">{how}</div></div>
</div></section>

<section id="install"><div class="wrap">
  <h2 class="rise">{esc(t['install_h'])}</h2>
  <ol class="steps" style="margin-top:36px">{steps}</ol>
</div></section>

<section id="commands"><div class="wrap">
  <h2 class="rise">{esc(t['cmd_h'])}</h2>
  <p class="sub rise">{esc(t['cmd_sub'])}</p>
  <div class="scroll rise"><table class="table"><tr>{head}</tr>{rows}</table></div>
</div></section>

<section id="faq"><div class="wrap">
  <h2 class="rise">{esc(t['faq_h'])}</h2>
  <div class="rise" style="margin-top:20px">{faq}</div>
</div></section>

<footer class="site"><div class="wrap">
  <span>{esc(t['foot_left'])}</span>
  <span>{foot} · <a href="{other_rel}" hreflang="{'en' if t['lang']=='de' else 'de'}">{t['other_label']}</a></span>
  <span class="claude">{esc(t['claude'])}</span>
</div></footer>
<script>{JS}</script>
</body>
</html>
"""


def main() -> None:
    for t in TEXT.values():
        out = ROOT / t["path"]
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(page(t), encoding="utf-8")
        print(f"wrote {out.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
