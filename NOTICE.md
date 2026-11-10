# NoSkillAntiCheat – License and attribution

NoSkillAntiCheat is a modified version of **GrimAC**
(https://github.com/GrimAnticheat/Grim), forked from branch `2.0` at commit
`e457d5566` (2026-11-08).

- GrimAC is licensed under the **GNU General Public License v3.0**.
  This fork stays under the same license; see `LICENSE`.
- GrimAPI and its `grim-internal`, `grim-internal-shims` and
  `grim-bukkit-internal` modules (https://github.com/GrimAnticheat/GrimAPI)
  are licensed under the **MIT License**, Copyright (c) 2025 GrimAnticheat.
- Original copyright remains with the GrimAC contributors. Java packages are
  kept as `ac.grim.grimac` to make merging upstream fixes easier.

If you distribute a jar of NoSkillAntiCheat, you must also provide the
corresponding source code under the GPL-3.0 (GPL-3.0 section 6).

## Modifications (GPL-3.0 section 5a)

Changes made in this fork, starting 2026-11-10:

- Plugin renamed to NoSkillAntiCheat; root command is now `/nsac`
  (alias `/noskillanticheat`).
- Removed the Fabric platform modules; only Bukkit-based servers
  (Paper, Folia, Spigot, Purpur) are built.
- Removed bStats metrics and the automatic update check against Grim's API.
- `/nsac log` and `/nsac dump` save files locally in the plugin's `logs`
  folder instead of uploading them to paste.grim.ac.
- Removed the unregistered `Baritone` and `FlightA` checks.
- Removed GrimAC's release/publishing workflows.
- Added false-positive measurement: per-check flag, setback and shadow-flag
  counters (`FlagStats`), `/nsac stats` and `/nsac session start|stop`.
- Added experimental AutoClickerA/B/C checks (click-delay statistics in
  client ticks), written from scratch; ideas taken from public descriptions
  of Vulcan's checks, no Vulcan code.
- Added experimental AimGCD (sensitivity-grid mismatch in combat), AimSmooth
  (interpolated combat aim) and KillAuraSwitch (fast repeated target
  switching) checks, written from scratch from public Vulcan check ideas.
- Added BlacklistedClient (bans clients whose brand or registered mod
  channels match configurable patterns; default punishment is a ban),
  experimental Improbable meta-check (several distinct checks of one
  category within a time window) and `/nsac top`.
- Added experimental CrystalTiming (near-instant, constant end-crystal
  place-to-break gaps, as from AutoCrystal/FastCrystal cheats).
- Added experimental TriggerBot (instant hits on crosshair entry without air
  swings) and AnchorTiming (automated respawn-anchor PvP), the
  `/nsac knockback` test command, and an alert/kick/ban escalation for
  Improbable in punishments.yml.
- Added the session recorder (`/nsac record start|stop|status`): one CSV row
  per client tick, gzip-compressed, players stored as a salted SHA-256
  pseudonym, written on a single low-priority background thread; plus
  `tools/ai/recordings.py` to summarize recordings.
- Added the combat AI pipeline (tools/ai: features, training, synthetic
  test data) and the experimental AICombat check that scores combat windows
  with a local model file.
- In-game rebranding: permission nodes are `nsac.*` instead of `grim.*`,
  message prefix and Discord titles say NSAC, PlaceholderAPI identifier is
  `nsac`. `/nsac version` states "Based on GrimAC - licensed under GPL-3.0".
- Added command protection: players without nsac.commands.bypass cannot run
  /pl, /plugins, /?, /help, /version and similar commands or namespaced
  commands, and those are removed from tab completion.
- Added `shadow-flags.print-to-console` to print what experimental checks
  would flag (test servers).
- Compact alerts with hover details, click-to-spectate and a per-check
  cooldown; experimental NSAC checks alert staff (marked, no punishment);
  new experimental KillAuraAim check; server brand / server-list version
  name spoofing (default "vanilla").
- Sign translation probe (detects clients that know mod translation keys,
  default: Meteor) feeding BlacklistedClient; alerts name Simulation flags
  Flight or Speed and GroundSpoof-in-air NoFall.
- Experimental InventoryMove (movement keys or camera turns while clicking
  in an inventory) and InventorySpeed (clicks in too many consecutive ticks)
  checks; the sign probe closes the editor in the same batch it opens it.
- Readable German alert names for all checks (CheckNames, ID in hover),
  purple theme, MultiActionsC (InvMove) also sets the player back, and
  optional enabling of Paper's built-in anti-xray.
- Experimental QuickSwap (switch slot, act, switch back: ShieldBreaker,
  MaceSwap, AutoTool, KeybindPearl) check (AutoTotem is left to the optional
  TotemGuard plugin); flight attempts are labelled Flight.
- Experimental XrayMining check: rare-ore veins found by tunnelling (no cave
  next to them) per natural underground block mined, over a time window.
- NoSwingBreak is active (not experimental) with an AutoMining punishment
  group (kick at 100, ban at 300 flags within 5 minutes) against Baritone
  #mine and Nukers.
- Separate Velocity add-on (`velocity/`): hides the proxy software (F3 brand
  without " (Velocity)", server-list version without the software name).
- Experimental NoJumpDelay check (jumps closer than 10 ticks while jump is
  held); BadPacketsN is shown as "NoRotate / ignored teleport".
- CI workflow runs on the `main` branch.
- Experimental BedTiming (BedAura), BowSpam (BowSpam/Quiver), FastUse
  (ExpThrower/AutoMend) and AutoSign checks.
