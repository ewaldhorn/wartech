'use strict';
const { spawn } = require('child_process');
const fs = require('fs');
const os = require('os');
const path = require('path');

const dir = path.resolve(process.argv[2] || 'dist');
const SECONDS = parseFloat(process.argv[3] || '4');
const CHROME = process.argv[4] || process.env.CHROME
  || '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';
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

(async () => {
  const server = spawn('python3', ['-m', 'http.server', String(PORT), '--bind', '127.0.0.1',
    '--directory', dir], { stdio: 'ignore' });

  const profile = fs.mkdtempSync(path.join(os.tmpdir(), 'wartech-chrome-'));
  const chrome = spawn(CHROME, [
    '--headless', '--disable-gpu', '--no-sandbox', '--mute-audio',
    '--no-first-run', '--no-default-browser-check',
    `--remote-debugging-port=${DEV}`, `--user-data-dir=${profile}`,
    '--window-size=1280,900', 'about:blank',
  ], { stdio: 'ignore' });

  const cleanup = () => { try { chrome.kill(); } catch (_) {} try { server.kill(); } catch (_) {} };
  process.on('exit', cleanup);

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
    console.log(`chrome            ${version.Browser}`);

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
        if (msg.params.type === 'error') {
          problems.push('console.error: ' + text);
        }
      }
    };
    const send = (method, params) => new Promise((res, rej) => {
      const id = ++nextId;
      pending.set(id, { res, rej });
      ws.send(JSON.stringify({ id, method, params }));
    });

    await send('Page.enable');
    await send('Runtime.enable');
    await send('Page.navigate', { url: `http://127.0.0.1:${PORT}/index.html` });

    await sleep(SECONDS * 1000);

    const read = async (expr) => {
      const r = await send('Runtime.evaluate', { expression: expr, returnByValue: true });
      return r.result ? r.result.value : undefined;
    };

    const state = await read(`(() => {
      const fps = document.getElementById('fps');
      const credits = document.getElementById('credits');
      const power = document.getElementById('power');
      const zoom = document.getElementById('zoomDisplay');
      const cv = document.getElementById('game');
      let nonZero = 0, sampled = 0;
      if (cv) {
        const g = cv.getContext('2d');
        const d = g.getImageData(0, 0, cv.width, cv.height).data;
        for (let i = 0; i < d.length; i += 37 * 4) {
          sampled++;
          if (d[i] > 0 || d[i+1] > 0 || d[i+2] > 0) nonZero++;
        }
      }
      return {
        fps: fps ? fps.textContent : null,
        credits: credits ? credits.textContent : null,
        power: power ? power.textContent : null,
        zoom: zoom ? zoom.textContent : null,
        canvas: cv ? cv.width + 'x' + cv.height : null,
        sampled, nonZero,
        ready: window.__wartechReady === true
      };
    })()`);

    const screenshot = await send('Page.captureScreenshot', { format: 'png' });
    fs.writeFileSync(path.join(OUT, 'screenshot.png'), Buffer.from(screenshot.data, 'base64'));

    console.log(`wartechReady      ${state.ready ? 'READY' : 'MISSING'}`);
    console.log(`HUD values        credits=${state.credits}, power=${state.power}, zoom=${state.zoom}, fps=${state.fps}`);
    console.log(`canvas buffer     ${state.canvas}, drawn pixels: ${state.nonZero}/${state.sampled}`);
    console.log(`screenshot        staged at ${path.join(OUT, 'screenshot.png')}`);

    // Deploy to battlefield: press Space to launch from the setup screen.
    await send('Input.dispatchKeyEvent', {
      type: 'keyDown', code: 'Space', key: ' ',
      windowsVirtualKeyCode: 32, nativeVirtualKeyCode: 32,
    });
    await send('Input.dispatchKeyEvent', {
      type: 'keyUp', code: 'Space', key: ' ',
      windowsVirtualKeyCode: 32, nativeVirtualKeyCode: 32,
    });

    await sleep(1500);

    const bState = await read(`(() => {
      const cv = document.getElementById('game');
      let nonZero = 0, sampled = 0;
      if (cv) {
        const g = cv.getContext('2d');
        const d = g.getImageData(0, 0, cv.width, cv.height).data;
        for (let i = 0; i < d.length; i += 37 * 4) {
          sampled++;
          if (d[i] > 0 || d[i+1] > 0 || d[i+2] > 0) nonZero++;
        }
      }
      return { sampled, nonZero };
    })()`);

    const bScreenshot = await send('Page.captureScreenshot', { format: 'png' });
    fs.writeFileSync(path.join(OUT, 'battlefield.png'), Buffer.from(bScreenshot.data, 'base64'));
    console.log(`battlefield       drawn pixels: ${bState.nonZero}/${bState.sampled}`);
    console.log(`screenshot        staged at ${path.join(OUT, 'battlefield.png')}`);

    // Sprites must render as pixel art, not solid blocks: a viewport crop of
    // real 64x64 shaded sprites contains far more distinct colors than flat fills.
    const variety = await read(`(() => {
      const cv = document.getElementById('game');
      const g = cv.getContext('2d');
      const d = g.getImageData(64, 64, 576, 640).data;
      const seen = new Set();
      for (let i = 0; i < d.length; i += 4 * 7) {
        seen.add((d[i] << 16) | (d[i+1] << 8) | d[i+2]);
        if (seen.size > 500) break;
      }
      return seen.size;
    })()`);
    console.log(`sprite variety    ${variety} distinct colors in viewport crop`);

    if (problems.length > 0) {
      console.error('\nPROBLEMS:');
      for (const p of problems) console.error('  ' + p);
      process.exit(1);
    }

    if (!state.ready) {
      console.error('FAIL: window.__wartechReady was not set by the TeaVM module');
      process.exit(1);
    }

    if (variety < 60) {
      console.error(`FAIL: only ${variety} distinct viewport colors (sprites may be solid blocks)`);
      process.exit(1);
    }

    console.log('\nPASS: WarTech running cleanly under TeaVM WASM-GC!');
    process.exit(0);
  } catch (err) {
    console.error('Check failed:', err);
    process.exit(1);
  }
})();
