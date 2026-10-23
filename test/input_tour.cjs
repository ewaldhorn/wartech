'use strict';
// Input tour: exercises the setup screen, deploy, and battlefield input paths
// (mouse, wheel, keyboard) through real CDP events and asserts the game stays
// healthy: no console errors, HUD alive, canvas painting, zoom responding.
const { spawn } = require('child_process');
const fs = require('fs');
const os = require('os');
const path = require('path');

const dir = path.resolve(process.argv[2] || 'docs');
const STAGE = process.argv[3] || 'all';
const CHROME = process.argv[4] || process.env.CHROME
  || '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';

// Each stage gets a fresh browser: in-page reloads proved flaky headless,
// while single-navigation runs (like browser_check.cjs) are rock solid.
if (STAGE === 'all') {
  const { spawnSync } = require('child_process');
  for (const s of ['1', '2', '3']) {
    const r = spawnSync(process.execPath, [__filename, dir, s],
      { stdio: 'inherit', env: { ...process.env, CHROME } });
    if (r.status !== 0) process.exit(r.status);
  }
  console.log('\nPASS: input tour completed without errors (all 3 stages)');
  process.exit(0);
}
if (!['1', '2', '3'].includes(STAGE)) {
  console.error(`unknown stage ${STAGE} (want 1, 2, 3, or all)`);
  process.exit(2);
}
const PORT = 8400 + Math.floor(Math.random() * 500);
const DEV = 9400 + Math.floor(Math.random() * 500);
const OUT = path.resolve('test/out');

if (!fs.existsSync(path.join(dir, 'teavm', 'classes.wasm'))) {
  console.error(`no module in ${dir} -- run ./build-dist.sh first`);
  process.exit(2);
}
if (!fs.existsSync(CHROME)) {
  console.error(`no Chrome at ${CHROME}`);
  process.exit(2);
}
fs.mkdirSync(OUT, { recursive: true });

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

// Canvas-space geometry (1024x768 buffer), mirrored from Renderer constants.
const X0 = 112;
const click = { x: 0, y: 0 };

(async () => {
  const server = spawn('python3', ['-m', 'http.server', String(PORT), '--bind', '127.0.0.1',
    '--directory', dir], { stdio: 'ignore' });

  const profile = fs.mkdtempSync(path.join(os.tmpdir(), 'wartech-tour-'));
  const chrome = spawn(CHROME, [
    '--headless', '--disable-gpu', '--no-sandbox', '--mute-audio',
    '--no-first-run', '--no-default-browser-check',
    `--remote-debugging-port=${DEV}`, `--user-data-dir=${profile}`,
    '--window-size=1280,900', 'about:blank',
  ], { stdio: 'ignore' });

  const cleanup = () => { try { chrome.kill(); } catch (_) {} try { server.kill(); } catch (_) {} };
  process.on('exit', cleanup);
  const fail = (msg) => { console.error(`FAIL: ${msg}`); process.exit(1); };

  try {
    let version = null;
    for (let i = 0; i < 60; i++) {
      try {
        const r = await fetch(`http://127.0.0.1:${DEV}/json/version`);
        if (r.ok) { version = await r.json(); break; }
      } catch (_) {}
      await sleep(250);
    }
    if (!version) throw new Error('Chrome never opened its debugger port');

    const list = await (await fetch(`http://127.0.0.1:${DEV}/json/list`)).json();
    const page = list.find((t) => t.type === 'page');
    if (!page) throw new Error('no page target');

    const ws = new WebSocket(page.webSocketDebuggerUrl);
    let nextId = 0;
    const pending = new Map();
    const problems = [];
    await new Promise((res, rej) => { ws.onopen = res; ws.onerror = rej; });
    ws.onmessage = (m) => {
      const msg = JSON.parse(m.data);
      if (msg.id && pending.has(msg.id)) {
        const p = pending.get(msg.id);
        pending.delete(msg.id);
        msg.error ? p.rej(new Error(JSON.stringify(msg.error))) : p.res(msg.result);
      } else if (msg.method === 'Runtime.exceptionThrown') {
        const d = msg.params.exceptionDetails;
        problems.push('uncaught: ' + (d.exception && (d.exception.description || d.exception.value) || d.text));
      } else if (msg.method === 'Runtime.consoleAPICalled') {
        const text = msg.params.args.map((a) => a.value || a.description).join(' ');
        if (msg.params.type === 'error') problems.push('console.error: ' + text);
      }
    };
    const send = (method, params) => new Promise((res, rej) => {
      const id = ++nextId;
      pending.set(id, { res, rej });
      ws.send(JSON.stringify({ id, method, params }));
    });
    const read = async (expr) => {
      const r = await send('Runtime.evaluate', { expression: expr, returnByValue: true });
      return r.result ? r.result.value : undefined;
    };
    const checkProblems = (stage) => {
      if (problems.length > 0) {
        console.error(`PROBLEMS after ${stage}:`);
        for (const p of problems) console.error('  ' + p);
        process.exit(1);
      }
    };

    await send('Page.enable');
    await send('Runtime.enable');

    // Map canvas-buffer coords to page coords via the live canvas rect.
    const toPage = async (cx, cy) => {
      const r = await read(`(() => {
        const cv = document.getElementById('game');
        const b = cv.getBoundingClientRect();
        return { l: b.left, t: b.top, w: b.width, h: b.height,
                 cw: cv.width, ch: cv.height };
      })()`);
      return { x: r.l + (cx / r.cw) * r.w, y: r.t + (cy / r.ch) * r.h };
    };
    const mouseClick = async (cx, cy, button = 'left') => {
      const p = await toPage(cx, cy);
      await send('Input.dispatchMouseEvent', { type: 'mousePressed', x: p.x, y: p.y, button, clickCount: 1 });
      await sleep(60);
      await send('Input.dispatchMouseEvent', { type: 'mouseReleased', x: p.x, y: p.y, button, clickCount: 1 });
      await sleep(120);
    };
    const mouseDrag = async (x1, y1, x2, y2, button = 'left') => {
      const a = await toPage(x1, y1);
      const b = await toPage(x2, y2);
      await send('Input.dispatchMouseEvent', { type: 'mousePressed', x: a.x, y: a.y, button, clickCount: 1 });
      await sleep(60);
      for (let i = 1; i <= 4; i++) {
        await send('Input.dispatchMouseEvent', {
          type: 'mouseMoved', x: a.x + ((b.x - a.x) * i) / 4, y: a.y + ((b.y - a.y) * i) / 4,
        });
        await sleep(40);
      }
      await send('Input.dispatchMouseEvent', { type: 'mouseReleased', x: b.x, y: b.y, button, clickCount: 1 });
      await sleep(120);
    };
    const keyPress = async (code, key, vkey) => {
      await send('Input.dispatchKeyEvent', { type: 'keyDown', code, key, windowsVirtualKeyCode: vkey, nativeVirtualKeyCode: vkey });
      await sleep(60);
      await send('Input.dispatchKeyEvent', { type: 'keyUp', code, key, windowsVirtualKeyCode: vkey, nativeVirtualKeyCode: vkey });
      await sleep(120);
    };
    const wheel = async (cx, cy, deltaY) => {
      const p = await toPage(cx, cy);
      await send('Input.dispatchMouseEvent', { type: 'mouseWheel', x: p.x, y: p.y, deltaX: 0, deltaY });
      await sleep(150);
    };
    const hud = () => read(`(() => ({
      credits: document.getElementById('credits').textContent,
      power: document.getElementById('power').textContent,
      zoom: document.getElementById('zoomDisplay').textContent,
      fps: document.getElementById('fps').textContent,
      ready: window.__wartechReady === true,
    }))()`);
    const painted = () => read(`(() => {
      const cv = document.getElementById('game');
      const d = cv.getContext('2d').getImageData(0, 0, cv.width, cv.height).data;
      let nonZero = 0;
      for (let i = 0; i < d.length; i += 61 * 4) {
        if (d[i] > 0 || d[i+1] > 0 || d[i+2] > 0) nonZero++;
      }
      return nonZero;
    })()`);
    const shot = async (name) => {
      const s = await send('Page.captureScreenshot', { format: 'png' });
      fs.writeFileSync(path.join(OUT, name), Buffer.from(s.data, 'base64'));
    };
    const load = async () => {
      await send('Page.navigate', { url: `http://127.0.0.1:${PORT}/index.html` });
      let h = null;
      for (let i = 0; i < 40; i++) {
        await sleep(500);
        h = await hud().catch(() => null);
        if (h && h.ready) break;
      }
      if (!h || !h.ready) fail('game did not boot (no __wartechReady)');
      return h;
    };
    const waitPainted = async (label) => {
      let px = 0;
      for (let i = 0; i < 16; i++) {
        await sleep(500);
        px = await painted();
        if (px > 0) break;
      }
      if (px <= 0) fail(`${label} canvas stayed empty`);
      return px;
    };

    let px = 0;
    if (STAGE === '1') {
      // ---- Load 1: setup options, deploy button, battlefield tour ----
      console.log('tour: load 1 (setup options + deploy + battlefield)');
      await load();
      await mouseClick(X0 + 536 + 120, 150 + 41);   // difficulty: Commander
      await mouseClick(X0 + 24 + 120, 150 + 41);     // difficulty: Recruit
      await mouseClick(X0 + 280 + 120, 150 + 41);    // difficulty: Veteran (default)
      await mouseClick(X0 + 600 + 89, 280 + 20);     // funds: 5000
      await mouseClick(X0 + 216 + 89, 280 + 20);     // funds: 1200 (default)
      await mouseClick(X0 + 346 + 25, 356 + 19);     // seed +
      await mouseClick(X0 + 280 + 25, 356 + 19);     // seed -
      await mouseClick(X0 + 412 + 125, 356 + 19);    // randomize seed
      await mouseClick(X0 + 408 + 184, 436 + 20);    // scale: 128
      await mouseClick(X0 + 24 + 184, 436 + 20);     // scale: 64 (default)
      checkProblems('setup option clicks');
      await shot('tour_setup.png');

      await mouseClick(X0 + 180 + 220, 576 + 27);    // DEPLOY
      checkProblems('deploy click');
      px = await waitPainted('battlefield after deploy click');
      console.log(`tour: deployed, painted pixels=${px}`);

      await mouseClick(352, 384);                    // select attempt
      await mouseDrag(100, 100, 300, 300);           // selection box
      await mouseClick(500, 500, 'right');           // order
      await mouseDrag(600, 200, 650, 250, 'right');  // right-drag (minimap-style path)
      const z0 = (await hud()).zoom;
      await wheel(352, 384, -120);                   // zoom in
      await wheel(352, 384, 120);                    // zoom out
      await keyPress('BracketLeft', '[', 219);       // zoom out
      const z1 = (await hud()).zoom;
      if (z0 === z1) fail(`zoom HUD did not respond to [ key (${z0} -> ${z1})`);
      await keyPress('BracketRight', ']', 221);      // zoom in
      await keyPress('Minus', '-', 189);
      await keyPress('Equal', '=', 187);
      await keyPress('KeyO', 'o', 79);               // objectives on
      await keyPress('KeyO', 'o', 79);               // objectives off
      await keyPress('KeyW', 'w', 87);
      await keyPress('KeyA', 'a', 65);
      await keyPress('KeyS', 's', 83);
      await keyPress('KeyD', 'd', 68);
      await keyPress('ArrowLeft', 'ArrowLeft', 37);
      await keyPress('Escape', 'Escape', 27);
      checkProblems('battlefield input tour');
      px = await painted();
      if (px <= 0) fail('battlefield canvas went empty during input tour');
      await shot('tour_battlefield.png');
      console.log(`tour: battlefield input done, painted pixels=${px}, zoom ${z0} -> ${z1}`);
    }

    if (STAGE === '2') {
      // ---- Load 2: skirmish mode card ----
      console.log('tour: load 2 (skirmish mode card)');
      await load();
      await mouseClick(X0 + 24 + 120, 502 + 22);     // PLAY SKIRMISH
      checkProblems('skirmish card click');
      await shot('tour_skirmish.png');
      px = await waitPainted('battlefield after skirmish card click');
      console.log(`tour: skirmish card deployed, painted pixels=${px}`);
    }

    if (STAGE === '3') {
      // ---- Load 3: campaign card + briefing confirm ----
      console.log('tour: load 3 (campaign card + briefing)');
      await load();
      await mouseClick(X0 + 280 + 120, 502 + 22);    // START CAMPAIGN
      await sleep(1200);
      checkProblems('campaign card click');
      await shot('tour_briefing.png');
      await keyPress('Enter', 'Enter', 13);          // confirm briefing
      checkProblems('briefing confirm');
      px = await waitPainted('mission after briefing confirm');
      await shot('tour_mission.png');
      console.log(`tour: campaign mission started, painted pixels=${px}`);
    }

    console.log(`\nPASS: input tour stage ${STAGE} completed without errors`);
    process.exit(0);
  } catch (err) {
    console.error('Tour failed:', err);
    process.exit(1);
  }
})();
