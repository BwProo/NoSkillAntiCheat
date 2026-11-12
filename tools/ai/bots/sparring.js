// NoSkillAntiCheat sparring bots for a LOCAL test server: fighters that attack the nearest real
// player with human-like aim, and dummies that only strafe (targets for testing KillAura etc.).
//
//   NODE_PATH=<folder with node_modules> node tools/ai/bots/sparring.js <port> <fighters> <dummies>
'use strict';
const mineflayer = require('mineflayer');
const [port, fighters, dummies] = process.argv.slice(2).map(Number);
const DEG = Math.PI / 180;
const BOT = /^(Fighter|Dummy)\d+$/;

function gauss() { return Math.sqrt(-2 * Math.log(Math.random() + 1e-12)) * Math.cos(2 * Math.PI * Math.random()); }
function wrap(a) { while (a > Math.PI) a -= 2 * Math.PI; while (a < -Math.PI) a += 2 * Math.PI; return a; }
function angles(from, to) {
  const dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z;
  return { yaw: Math.atan2(-dx, -dz), pitch: Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)) };
}
function make(name, role) {
  const bot = mineflayer.createBot({ host: '127.0.0.1', port, username: name, auth: 'offline', version: '26.1' });
  bot.on('kicked', (r) => console.log(`[sparring] ${name} kicked: ${JSON.stringify(r).slice(0, 160)}`));
  bot.on('error', (e) => console.log(`[sparring] ${name} error: ${e.message}`));
  bot.once('spawn', () => console.log(`[sparring] ${name} joined`));
  bot.on('end', () => { console.log(`[sparring] ${name} left, rejoining in 5 s`); setTimeout(() => role(name), 5000); });
  return bot;
}

function nearestPlayer(bot, range) {
  let best = null, bestD = range;
  for (const p of Object.values(bot.players)) {
    if (!p.entity || p.username === bot.username || BOT.test(p.username)) continue;
    const d = bot.entity.position.distanceTo(p.entity.position);
    if (d < bestD) { best = p.entity; bestD = d; }
  }
  return best;
}

function fighter(name) {
  const bot = make(name, fighter);
  bot.setup = () => {
    const gain = 0.2 + Math.random() * 0.2, cps = 6 + Math.random() * 4, reaction = 3 + Math.floor(Math.random() * 3);
    const history = [];
    let tick = 0, nextClick = 0;
    bot.on('physicsTick', () => {
      tick++;
      const t = nearestPlayer(bot, 2000);
      for (const c of ['forward', 'sprint', 'back', 'left', 'right', 'jump']) bot.setControlState(c, false);
      if (!t) return;
      const eye = bot.entity.position.offset(0, 1.62, 0);
      history.push(t.position.offset(0, 1.0, 0)); if (history.length > 8) history.shift();
      const aim = history[Math.max(0, history.length - 1 - reaction)];
      const want = angles(eye, aim);
      const yaw = bot.entity.yaw + wrap(want.yaw - bot.entity.yaw) * gain + gauss() * 0.9 * DEG;
      const pitch = bot.entity.pitch + (want.pitch - bot.entity.pitch) * gain + gauss() * 0.5 * DEG;
      bot.look(yaw, pitch, true);
      const d = bot.entity.position.distanceTo(t.position);
      bot.setControlState('forward', d > 2.4); bot.setControlState('sprint', d > 3);
      if (d < 3.5 && (tick % 50) < 20) bot.setControlState(tick % 100 < 50 ? 'left' : 'right', true);
      if (d < 3 && Math.random() < 0.04) bot.setControlState('jump', true);
      if (d > 3.2 || tick < nextClick) return;
      nextClick = tick + Math.max(2, Math.round(20 / (cps + gauss() * 1.5)));
      const err = Math.abs(wrap(angles(eye, t.position.offset(0, 1, 0)).yaw - yaw)) / DEG;
      if (err < 10) bot.attack(t); else bot.swingArm();
    });
  };
  bot.setup();
  return bot;
}

function dummy(name) {
  const bot = make(name, dummy);
  bot.setup = () => {
    let tick = 0;
    bot.on('physicsTick', () => {
      const p = nearestPlayer(bot, 2000);
      if (p && bot.entity.position.distanceTo(p.position) > 10) { // stay close to the player
        bot.lookAt(p.position.offset(0, 1.6, 0), true);
        bot.setControlState('forward', true); bot.setControlState('left', false); bot.setControlState('right', false);
        return;
      }
      if (++tick % 20 !== 0) return;
      for (const c of ['left', 'right', 'forward', 'back', 'jump']) bot.setControlState(c, false);
      const r = Math.random();
      bot.setControlState(r < 0.4 ? 'left' : r < 0.8 ? 'right' : 'forward', true);
      if (Math.random() < 0.3) bot.setControlState('jump', true);
    });
  };
  bot.setup();
  return bot;
}

for (let i = 1; i <= fighters; i++) setTimeout(() => fighter('Fighter' + i), i * 1500);
for (let i = 1; i <= dummies; i++) setTimeout(() => dummy('Dummy' + i), (fighters + i) * 1500);
