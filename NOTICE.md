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
  (alias `/noskillanticheat`). Permissions keep the `grim.*` names.
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
- CI workflow runs on the `main` branch.
