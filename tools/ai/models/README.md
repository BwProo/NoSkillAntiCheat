# Models

`bootstrap-bots.json` was trained on mineflayer bot fights only (`tools/ai/botfights.py`, 4 rounds,
48 bot players, about 70 minutes of combat). On unseen bots it caught every cheat window with no false
alarms, but bots are not people: use it in shadow mode only and retrain once real recordings exist
(`/nsac record start <player> legit_...` / `cheat_...`).

Install: copy to `plugins/NoSkillAntiCheat/ai/model.json`; the AICombat check reloads it automatically.
