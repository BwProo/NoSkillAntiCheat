// NoSkillAntiCheat bot arena: pairs of mineflayer bots fight on a local offline test server so
// NSAC can record labelled combat for the AI. Each pair = one attacker + one dummy that strafes.
//
//   NODE_PATH=<folder with node_modules> node tools/ai/bots/arena.js <port> <seconds> <pairs.json>
//
// Attacker modes:
//   legit     human-like: reaction delay, smoothing with random gain, aim noise and overshoot,
//             rotations quantized to the mouse-sensitivity grid, irregular clicks, misses
//   aura_snap KillAura: exact rotation to the target every tick, fixed attack rhythm
//   aura_smooth KillAura with smoothed but perfect tracking and jittered CPS
//   aimassist human clicks, but aim pulled strongly onto the target with almost no noise
// Only for your own local test server.
'use strict';
const mineflayer = require('mineflayer');

const [port, seconds, pairsFile] = process.argv.slice(2);
const pairs = require(require('path').resolve(pairsFile));
const DEG = Math.PI / 180;

function rng(seed) { // mulberry32, reproducible personalities
  let a = seed >>> 0;
  return () => { a |= 0; a = a + 0x6D2B79F5 | 0; let t = Math.imul(a ^ a >>> 15, 1 | a);
    t = t + Math.imul(t ^ t >>> 7, 61 | t) ^ t; return ((t ^ t >>> 14) >>> 0) / 4294967296; };
}
function gauss(r) { return Math.sqrt(-2 * Math.log(r() + 1e-12)) * Math.cos(2 * Math.PI * r()); }
function wrap(a) { while (a > Math.PI) a -= 2 * Math.PI; while (a < -Math.PI) a += 2 * Math.PI; return a; }
function lookAngles(from, to) { // mineflayer convention (see bot.lookAt)
  const dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z;
  return { yaw: Math.atan2(-dx, -dz), pitch: Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)) };
}

function makeBot(name) {
  return mineflayer.createBot({ host: '127.0.0.1', port: Number(port), username: name, auth: 'offline',
    version: '26.1', hideErrors: false, checkTimeoutInterval: 120000 });
}

function dummy(name, seed) {
  const bot = makeBot(name), r = rng(seed);
  let ticks = 0;
  bot.on('physicsTick', () => {
    if (++ticks % 20 !== 0) return; // new strafe decision every second
    for (const c of ['left', 'right', 'forward', 'back', 'jump']) bot.setControlState(c, false);
    const pick = r();
    if (pick < 0.35) bot.setControlState('left', true);
    else if (pick < 0.7) bot.setControlState('right', true);
    else if (pick < 0.8) bot.setControlState('forward', true);
    else if (pick < 0.88) bot.setControlState('back', true);
    if (r() < 0.25) bot.setControlState('jump', true);
  });
  return bot;
}

function attacker(p) {
  const bot = makeBot(p.name), r = rng(p.seed);
  // personality
  const gain = 0.18 + r() * 0.25, noise = 0.6 + r() * 1.6, reaction = 3 + Math.floor(r() * 4);
  const cpsMean = 6 + r() * 5, sens = 0.3 + r() * 0.5;
  const f = sens * 0.6 + 0.2, gcd = f * f * f * 8 * 0.15 * DEG; // vanilla mouse step in radians
  const history = [];
  let nextClick = 0, tick = 0, overshoot = 0, lastAttack = -99;
  bot.on('physicsTick', () => {
    tick++;
    const target = bot.players[p.target] && bot.players[p.target].entity;
    if (tick % 200 === 0) console.log(`[arena] ${p.name} tick=${tick} target=${!!target} pos=${bot.entity.position} dist=${target ? bot.entity.position.distanceTo(target.position).toFixed(1) : '-'}`);
    if (!target) return;
    const eye = bot.entity.position.offset(0, 1.62, 0), aimAt = target.position.offset(0, 0.9 + gauss(r) * 0.15, 0);
    history.push(aimAt); if (history.length > 10) history.shift();
    const dist = bot.entity.position.distanceTo(target.position);
    // movement: close in, keep ~2.6 blocks, strafe a bit
    bot.setControlState('forward', dist > 2.6); bot.setControlState('sprint', dist > 3.5);
    bot.setControlState('back', dist < 1.6);
    if (tick % 30 === 0) { const s = r(); bot.setControlState('left', s < 0.3); bot.setControlState('right', s > 0.7); }
    let yaw = bot.entity.yaw, pitch = bot.entity.pitch;
    if (p.mode === 'aura_snap') {
      ({ yaw, pitch } = lookAngles(eye, target.position.offset(0, 0.9, 0)));
    } else if (p.mode === 'aura_smooth') {
      const want = lookAngles(eye, target.position.offset(0, 0.9, 0));
      yaw += wrap(want.yaw - yaw) * 0.65; pitch += (want.pitch - pitch) * 0.65;
    } else {
      const delayed = history[Math.max(0, history.length - 1 - (p.mode === 'aimassist' ? 1 : reaction))];
      const want = lookAngles(eye, delayed);
      const k = p.mode === 'aimassist' ? 0.7 : gain * (0.7 + r() * 0.6);
      const n = p.mode === 'aimassist' ? 0.08 : noise;
      if (p.mode === 'legit' && r() < 0.03) overshoot = (r() - 0.5) * 12 * DEG;
      overshoot *= 0.8;
      let dy = wrap(want.yaw - yaw) * k + gauss(r) * n * DEG * 0.5 + overshoot;
      let dp = (want.pitch - pitch) * k + gauss(r) * n * DEG * 0.3;
      if (p.mode !== 'aimassist') { dy = Math.round(dy / gcd) * gcd; dp = Math.round(dp / gcd) * gcd; } // mouse grid
      yaw += dy; pitch = Math.max(-Math.PI / 2, Math.min(Math.PI / 2, pitch + dp));
    }
    bot.look(yaw, pitch, true);
    if (dist > 3.4 || tick < nextClick) return;
    // clicks
    const err = Math.abs(wrap(lookAngles(eye, target.position.offset(0, 0.9, 0)).yaw - yaw)) / DEG;
    if (p.mode === 'aura_snap') { nextClick = tick + 10; }                       // fixed rhythm
    else if (p.mode === 'aura_smooth') { nextClick = tick + 8 + Math.floor(r() * 4); }
    else { nextClick = tick + Math.max(1, Math.round(20 / Math.max(3, cpsMean + gauss(r) * 2.2))); }
    const onTarget = p.mode.startsWith('aura') || err < 8;
    if (onTarget) { bot.attack(target); lastAttack = tick; } else bot.swingArm();
  });
  return bot;
}

const bots = [];
pairs.forEach((p, i) => {
  setTimeout(() => {
    const d = dummy(p.target, p.seed + 1000); bots.push(d);
    setTimeout(() => { const a = attacker(p); bots.push(a);
      for (const b of [a, d]) { b.on('kicked', (why) => console.log(`[arena] ${b.username} kicked: ${JSON.stringify(why).slice(0, 200)}`));
        b.on('error', (e) => console.log(`[arena] ${b.username} error: ${e.message}`));
        b.once('spawn', () => console.log(`[arena] ${b.username} spawned`)); }
    }, 1500);
  }, i * 3500);
});
setTimeout(() => { console.log('[arena] time up'); bots.forEach((b) => b.quit()); setTimeout(() => process.exit(0), 1500); }, Number(seconds) * 1000);
