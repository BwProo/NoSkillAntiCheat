# Third-party software in NoSkillAntiCheat

NoSkillAntiCheat is licensed under the GNU GPL v3.0 (see `LICENSE`). The released jars bundle the
libraries below (relocated under `ac.grim.grimac.shaded` / `net.noskillanticheat.velocity.shaded`).
All of them are under licenses that are compatible with GPL-3.0. Their source code is available from
the linked projects; this file and `LICENSE` are also included in each jar under `META-INF/`.

| Library | Used for | License |
|---|---|---|
| GrimAC (https://github.com/GrimAnticheat/Grim) | The anticheat this project is forked from | GPL-3.0 |
| GrimAPI (https://github.com/GrimAnticheat/GrimAPI) | API and internal modules | MIT, Copyright (c) 2025 GrimAnticheat |
| PacketEvents (https://github.com/retrooper/packetevents) | Packet handling (Bukkit and Velocity) | GPL-3.0 |
| Adventure (https://github.com/KyoriPowered/adventure) | Chat components | MIT |
| Cloud (https://github.com/Incendo/cloud) | Commands | MIT |
| GeantyRef (https://github.com/leangen/geantyref) | Required by Cloud | Apache-2.0 |
| fastutil (https://github.com/vigna/fastutil) | Collections | Apache-2.0 |
| SnakeYAML (https://github.com/snakeyaml/snakeyaml) | Config files | Apache-2.0 |
| Gson (https://github.com/google/gson) | JSON | Apache-2.0 |
| JSON.simple (https://github.com/fangyidong/json-simple) | JSON | Apache-2.0 |
| Configuralize (https://github.com/Scarsz/Configuralize) | Config handling | Unlicense (public domain) |
| weak-maps (https://github.com/alexheretic/weak-maps) | Maps | Apache-2.0 |
| HikariCP (https://github.com/brettwooldridge/HikariCP) | Database connections | Apache-2.0 |
| SLF4J (https://www.slf4j.org) | Logging for HikariCP | MIT |

The Apache-2.0 license text: https://www.apache.org/licenses/LICENSE-2.0
The MIT license text is included in each MIT project's repository.

Not bundled, only used by the developer tools in `tools/` on your own computer: mineflayer (MIT,
installed with npm, `tools/ai/bots/`). Not used at all: any code from Vulcan, MLSAC, TotemGuard or
cheat clients. Ideas taken from public descriptions of other anticheats are not copied code.

NoSkillAntiCheat is not an official Minecraft product and is not approved by or associated with
Mojang or Microsoft. "Minecraft" is a trademark of Mojang Synergies AB.
