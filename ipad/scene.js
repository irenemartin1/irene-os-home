// Animated theme backgrounds (port of the Android ThemeSceneView + the forest/underwater scenes).
(function () {
  const PI = Math.PI;
  function hexRgb(h) { h = h.replace('#', ''); return [parseInt(h.slice(0, 2), 16), parseInt(h.slice(2, 4), 16), parseInt(h.slice(4, 6), 16)]; }
  function rgba(h, a) { const [r, g, b] = hexRgb(h); return 'rgba(' + r + ',' + g + ',' + b + ',' + (a / 255).toFixed(3) + ')'; }
  function mix(a, b, t) { const x = hexRgb(a), y = hexRgb(b); return '#' + x.map((v, i) => Math.round(v + (y[i] - v) * t).toString(16).padStart(2, '0')).join(''); }
  function rng(seed) { return function () { seed |= 0; seed = seed + 0x6D2B79F5 | 0; let t = Math.imul(seed ^ seed >>> 15, 1 | seed); t = t + Math.imul(t ^ t >>> 7, 61 | t) ^ t; return ((t ^ t >>> 14) >>> 0) / 4294967296; }; }
  window.Col = { rgba, mix, hexRgb, rng };

  function star5(c, x, y, s) { c.beginPath(); for (let k = 0; k < 10; k++) { const rr = k % 2 ? s * 0.45 : s, an = -PI / 2 + k * PI / 5; c.lineTo(x + rr * Math.cos(an), y + rr * Math.sin(an)); } c.closePath(); c.fill(); }
  function sparkle(c, x, y, s) { c.beginPath(); c.moveTo(x, y - s); c.quadraticCurveTo(x, y, x + s, y); c.quadraticCurveTo(x, y, x, y + s); c.quadraticCurveTo(x, y, x - s, y); c.quadraticCurveTo(x, y, x, y - s); c.fill(); }
  function heart(c, x, y, s) { c.beginPath(); c.moveTo(x, y + s * 0.8); c.bezierCurveTo(x - s * 1.3, y - s * 0.1, x - s * 0.6, y - s * 1.1, x, y - s * 0.35); c.bezierCurveTo(x + s * 0.6, y - s * 1.1, x + s * 1.3, y - s * 0.1, x, y + s * 0.8); c.fill(); }
  function oval(c, x0, y0, x1, y1) { c.beginPath(); c.ellipse((x0 + x1) / 2, (y0 + y1) / 2, Math.abs(x1 - x0) / 2, Math.abs(y1 - y0) / 2, 0, 0, PI * 2); }
  function rrect(c, x, y, w, h, r) { c.beginPath(); c.moveTo(x + r, y); c.arcTo(x + w, y, x + w, y + h, r); c.arcTo(x + w, y + h, x, y + h, r); c.arcTo(x, y + h, x, y, r); c.arcTo(x, y, x + w, y, r); c.closePath(); }
  window.Shapes = { star5, sparkle, heart, oval, rrect };

  class Scene {
    constructor(canvas) {
      this.cv = canvas; this.c = canvas.getContext('2d');
      this.t = 0; this.ps = []; this.running = false; this.cache = null; this.theme = null;
      this.loop = this.loop.bind(this);
      window.addEventListener('resize', () => this.resize());
    }
    setTheme(t) { this.theme = t; this.cache = null; this.resize(); }
    resize() {
      const dpr = Math.min(window.devicePixelRatio || 1, 2);
      this.dpr = dpr; this.W = window.innerWidth; this.H = window.innerHeight;
      this.cv.width = Math.round(this.W * dpr); this.cv.height = Math.round(this.H * dpr);
      this.cv.style.width = this.W + 'px'; this.cv.style.height = this.H + 'px';
      this.cache = null; this.seed(); this.draw();
    }
    start() { if (!this.running) { this.running = true; this.last = 0; requestAnimationFrame(this.loop); } }
    stop() { this.running = false; }
    loop(ts) {
      if (!this.running) return;
      if (ts - this.last > 40) { this.last = ts; this.step(); this.draw(); }
      requestAnimationFrame(this.loop);
    }
    colors() { const t = this.theme; return [t.accent, t.inC, t.outC, t.strong]; }

    seed() {
      const t = this.theme; if (!t) return;
      const r = rng(7), W = this.W, H = this.H, cs = this.colors();
      this.ps = [];
      const n = { none: 0, stars: 70, dots: 60, snow: 60, tickers: 16, fireflies: 34, ocean: 30 }[t.particle] ?? 26;
      for (let i = 0; i < n; i++) this.ps.push({ x: r() * W, y: r() * H, s: 3 + r() * 6, sp: 0.3 + r() * 0.9, ph: r() * 6.28, col: cs[i % 4], kind: i % 5 });
      if (t.particle === 'ocean') { // fish + jellies swim across
        for (let i = 0; i < 9; i++) this.ps.push({ fish: true, x: r() * W, y: H * (0.15 + r() * 0.6), s: 10 + r() * 14, sp: 0.6 + r() * 1.2, ph: r() * 6, col: ['#ffb36b', '#ff8fb1', '#ffe066', '#9be7ff', '#b197fc'][i % 5], dir: r() < 0.5 ? -1 : 1, jelly: i % 4 === 3 });
      }
    }

    step() {
      this.t += 0.05; const W = this.W, H = this.H, part = this.theme.particle;
      const up = ['tickers', 'bubbles', 'embers', 'hearts', 'paws', 'smileys', 'ocean', 'fireflies'];
      for (const p of this.ps) {
        if (p.fish) { p.x += p.dir * p.sp * (p.jelly ? 0.3 : 1); if (p.jelly) p.y -= Math.sin(this.t * 2 + p.ph) * 0.4; if (p.x < -60) p.x = W + 60; if (p.x > W + 60) p.x = -60; continue; }
        if (['stars', 'dots', 'sparkles'].includes(part)) continue;
        if (up.includes(part)) { p.y -= p.sp * (part === 'fireflies' ? 0.5 : 1.6); p.x += Math.sin(this.t + p.ph) * (part === 'fireflies' ? 0.9 : 0.5); if (p.y < -20) { p.y = H + 20; p.x = Math.random() * W; } }
        else { p.y += p.sp * 1.6; p.x += Math.sin(this.t * 0.8 + p.ph) * 1.0; if (p.y > H + 20) { p.y = -20; p.x = Math.random() * W; } }
      }
    }

    draw() {
      const t = this.theme; if (!t || !this.W) return;
      const c = this.c; c.setTransform(this.dpr, 0, 0, this.dpr, 0, 0);
      if (!this.cache) this.cache = this.buildCache();
      c.drawImage(this.cache, 0, 0, this.W, this.H);
      if (t.ground === 'seabed') this.rays(c);
      for (const p of this.ps) this.particle(c, p);
    }

    /** Draws a still copy at any size (used for the wallpaper download). */
    snapshot(w, h) {
      const keep = [this.W, this.H, this.dpr, this.cache, this.ps];
      this.W = w; this.H = h; this.dpr = 1; this.cache = null; this.seed();
      const out = document.createElement('canvas'); out.width = w; out.height = h;
      const c = out.getContext('2d'); c.drawImage(this.buildCache(), 0, 0);
      if (this.theme.ground === 'seabed') this.rays(c);
      for (const p of this.ps) this.particle(c, p);
      [this.W, this.H, this.dpr, this.cache, this.ps] = keep;
      return out;
    }

    buildCache() {
      const t = this.theme, W = this.W, H = this.H, dpr = this.dpr;
      const cv = document.createElement('canvas'); cv.width = Math.round(W * dpr); cv.height = Math.round(H * dpr);
      const c = cv.getContext('2d'); c.scale(dpr, dpr);
      const g = c.createLinearGradient(0, 0, 0, H);
      t.sky.forEach((col, i) => g.addColorStop(t.sky.length > 1 ? i / (t.sky.length - 1) : 0, col));
      c.fillStyle = g; c.fillRect(0, 0, W, H);
      const gr = t.dark ? mix(t.base, t.accent, 0.16) : mix(t.base, t.strong, 0.18);
      this.ground(c, W, H, gr, rng(21));
      return cv;
    }

    hill(c, W, H, top, col) { c.fillStyle = col; c.beginPath(); c.moveTo(0, H); c.lineTo(0, H * top); c.bezierCurveTo(W * 0.3, H * (top - 0.05), W * 0.6, H * (top + 0.03), W, H * (top - 0.02)); c.lineTo(W, H); c.fill(); }

    ground(c, W, H, g, r) {
      const t = this.theme, cs = this.colors(), u = Math.min(W, H) / 400;
      switch (t.ground) {
        case 'forest': {
          for (let layer = 0; layer < 3; layer++) {
            c.fillStyle = rgba(mix(t.base, '#1f4a3a', 0.4 + layer * 0.15), 150 + layer * 50);
            for (let x = -40; x < W + 60; x += 70 + r() * 50) {
              const top = H * (0.42 + layer * 0.12 + r() * 0.08), w = 60 + r() * 50;
              c.fillRect(x + w * 0.45, top, w * 0.12, H - top);
              for (let k = 0; k < 4; k++) { c.beginPath(); c.arc(x + w / 2 + (r() - 0.5) * w * 0.6, top - k * w * 0.18, w * (0.42 - k * 0.06), 0, PI * 2); c.fill(); }
            }
          }
          this.hill(c, W, H, 0.9, '#14261f');
          for (let i = 0; i < 9; i++) { const x = r() * W, y = H * (0.9 + r() * 0.06), s = (8 + r() * 10) * u;
            const gl = c.createRadialGradient(x, y - s, 0, x, y - s, s * 3); gl.addColorStop(0, rgba(cs[i % 3], 110)); gl.addColorStop(1, rgba(cs[i % 3], 0)); c.fillStyle = gl; c.fillRect(x - s * 3, y - s * 4, s * 6, s * 6);
            c.fillStyle = '#efe3d0'; c.fillRect(x - s * 0.18, y - s * 0.6, s * 0.36, s * 0.7);
            c.fillStyle = cs[i % 3]; c.beginPath(); c.ellipse(x, y - s * 0.6, s * 0.65, s * 0.45, 0, PI, 0); c.fill(); }
          break; }
        case 'seabed': {
          c.fillStyle = '#d9c39a'; this.hill(c, W, H, 0.86, rgba('#e8d3a8', 230));
          for (let i = 0; i < 10; i++) { const x = r() * W, h = H * (0.12 + r() * 0.18); c.strokeStyle = rgba('#3fa36b', 200); c.lineWidth = 6 * u; c.lineCap = 'round';
            c.beginPath(); c.moveTo(x, H); for (let k = 1; k <= 10; k++) c.lineTo(x + Math.sin(k) * 10 * u, H - h * k / 10); c.stroke(); }
          for (let i = 0; i < 7; i++) { const x = r() * W, y = H * (0.9 + r() * 0.05), s = (10 + r() * 14) * u; c.fillStyle = ['#ff8fb1', '#ffb36b', '#b197fc'][i % 3];
            for (let k = -2; k <= 2; k++) { c.beginPath(); c.ellipse(x + k * s * 0.35, y - s * 0.5 - Math.abs(k) * -s * 0.1, s * 0.16, s * 0.6 - Math.abs(k) * s * 0.1, k * 0.2, 0, PI * 2); c.fill(); } }
          for (let i = 0; i < 4; i++) { c.fillStyle = '#ff9a76'; star5(c, r() * W, H * (0.93 + r() * 0.05), 9 * u); }
          break; }
        case 'hills': this.hill(c, W, H, 0.86, rgba(g, 170)); this.hill(c, W, H, 0.9, g); break;
        case 'mushrooms': this.hill(c, W, H, 0.9, g); for (const [fx, s] of [[0.12, 1], [0.2, 0.6], [0.82, 0.85]]) this.mushroom(c, W * fx, H * 0.9, W * 0.08 * s); break;
        case 'skyline': { let x = 0; while (x < W) { const bw = (28 + r() * 50) * u, bh = H * (0.08 + r() * 0.18); c.fillStyle = g; c.fillRect(x, H - bh, bw - 3 * u, bh); c.fillStyle = rgba(t.accent, 140);
            for (let wy = H - bh + 8 * u; wy < H - 8 * u; wy += 11 * u) for (let wx = x + 5 * u; wx < x + bw - 10 * u; wx += 9 * u) if (r() < 0.4) c.fillRect(wx, wy, 4 * u, 5 * u); x += bw; } break; }
        case 'waves': case 'palms':
          for (let k = 0; k < 3; k++) { c.fillStyle = rgba(k === 2 ? g : t.accent, 60 + k * 50); c.beginPath(); c.moveTo(0, H); for (let x = 0; x <= W; x += 8) c.lineTo(x, H * (0.86 + k * 0.04) + Math.sin(x / (40 * u) + k) * 8 * u); c.lineTo(W, H); c.fill(); }
          if (t.ground === 'palms') { this.palm(c, W * 0.1, H * 0.9, H * 0.25, g, u); this.palm(c, W * 0.88, H * 0.88, H * 0.2, g, u); }
          break;
        case 'zen': c.fillStyle = rgba(g, 120); c.fillRect(0, H * 0.8, W, H * 0.2); c.strokeStyle = rgba(t.fg, 40); c.lineWidth = 1.5 * u;
          for (let y = H * 0.82; y < H; y += 10 * u) { c.beginPath(); c.moveTo(0, y); c.lineTo(W, y + Math.sin(y) * 2); c.stroke(); }
          c.fillStyle = g; oval(c, W * 0.12, H * 0.83, W * 0.3, H * 0.89); c.fill(); oval(c, W * 0.68, H * 0.86, W * 0.8, H * 0.9); c.fill(); break;
        case 'dance_floor': { const tile = W / 8; for (let rr = 0; rr < 4; rr++) for (let col = 0; col < 8; col++) { c.fillStyle = rgba(cs[(rr + col) % 4], 40 + ((rr * 3 + col) % 3) * 25); c.fillRect(col * tile, H * 0.82 + rr * tile * 0.6, tile - 2, tile * 0.6 - 2); }
          c.fillStyle = rgba(t.fg, 200); c.beginPath(); c.arc(W * 0.5, H * 0.07, 26 * u, 0, PI * 2); c.fill(); break; }
        case 'big_top': for (let k = 0; k < 10; k++) { c.fillStyle = k % 2 ? 'rgba(255,255,255,.35)' : rgba(t.accent, 70); c.beginPath(); c.moveTo(W / 2, -H * 0.05); c.lineTo(W * k / 10, H * 0.12); c.lineTo(W * (k + 1) / 10, H * 0.12); c.fill(); }
          c.fillStyle = rgba(t.accent, 90); for (let k = 0; k < 10; k++) { c.beginPath(); c.arc(W * (k + 0.5) / 10, H * 0.12, W / 20, 0, PI * 2); c.fill(); } break;
        case 'traces': c.strokeStyle = rgba(t.accent, 70); c.lineWidth = 2 * u; c.fillStyle = rgba(t.accent, 120);
          for (let i = 0; i < 16; i++) { let x = r() * W, y = r() * H; c.beginPath(); c.moveTo(x, y); for (let k = 0; k < 4; k++) { if (r() < 0.5) x += (r() - 0.5) * 160 * u; else y += (r() - 0.5) * 160 * u; c.lineTo(x, y); } c.stroke(); c.beginPath(); c.arc(x, y, 3.5 * u, 0, PI * 2); c.fill(); } break;
        case 'books': { c.fillStyle = mix(t.base, t.accent, 0.25); c.fillRect(0, H * 0.86, W, H * 0.01); let x = 0;
          while (x < W) { const bw = (10 + r() * 14) * u, bh = H * (0.07 + r() * 0.05); c.fillStyle = rgba(mix(g, cs[Math.floor(r() * 4)], 0.35), 230); c.fillRect(x, H * 0.86 - bh, bw - 1.5, bh); x += bw; }
          c.fillStyle = rgba(g, 200); c.fillRect(0, H * 0.87, W, H * 0.13); break; }
        case 'frames': c.strokeStyle = rgba(t.accent, 150); c.lineWidth = 3 * u;
          for (const [fx, fy, fw, fh] of [[0.08, 0.16, 0.26, 0.16], [0.62, 0.12, 0.3, 0.22], [0.12, 0.6, 0.22, 0.14], [0.66, 0.64, 0.24, 0.12]]) { c.fillStyle = rgba(mix(t.base, t.outC, 0.25), 120); c.fillRect(W * fx, H * fy, W * fw, H * fh); c.strokeRect(W * fx, H * fy, W * fw, H * fh); } break;
        case 'clouds': c.fillStyle = 'rgba(255,255,255,.67)';
          for (const [cx, cy, s] of [[0.15, 0.9, 1.2], [0.5, 0.93, 1.5], [0.88, 0.9, 1.1], [0.75, 0.12, 0.7], [0.2, 0.18, 0.6]]) { const rr = 34 * u * s; for (const [dx, dy, k] of [[0, 0, 1], [-0.9, 0.2, 0.7], [0.9, 0.2, 0.7]]) { c.beginPath(); c.arc(W * cx + dx * rr, H * cy + dy * rr, rr * k, 0, PI * 2); c.fill(); } } break;
        case 'castle': { this.hill(c, W, H, 0.9, 'rgba(255,255,255,.6)'); c.fillStyle = rgba(t.accent, 90); const bx = W / 2, by = H * 0.88, s = 40 * u; c.fillRect(bx - s, by - s * 1.6, s * 2, s * 1.6);
          for (const k of [-1, 1]) { c.fillRect(bx + k * s * 1.3 - s * 0.35, by - s * 2.2, s * 0.7, s * 2.2); c.beginPath(); c.moveTo(bx + k * s * 1.3 - s * 0.45, by - s * 2.2); c.lineTo(bx + k * s * 1.3, by - s * 2.9); c.lineTo(bx + k * s * 1.3 + s * 0.45, by - s * 2.2); c.fill(); } break; }
        case 'grid': { c.strokeStyle = rgba(t.accent, 45); c.lineWidth = 1; const hz = H * 0.7; for (let k = -14; k <= 14; k++) { c.beginPath(); c.moveTo(W / 2 + k * 18 * u, hz); c.lineTo(W / 2 + k * 90 * u, H); c.stroke(); }
          let y = hz, gap = 6 * u; while (y < H) { c.beginPath(); c.moveTo(0, y); c.lineTo(W, y); c.stroke(); y += gap; gap *= 1.25; } break; }
        case 'rainbow': { const b = ['#ff6b6b', '#ffa94d', '#ffe066', '#69db7c', '#4dabf7', '#b197fc']; c.lineWidth = 14 * u;
          b.forEach((col, k) => { c.strokeStyle = rgba(col, 110); c.beginPath(); c.arc(W / 2, H * 0.95, Math.min(W * 0.62, H * 0.5) - k * 14 * u, PI, 0); c.stroke(); }); break; }
        case 'stained': { const cell = W / 5; for (let rr = 0; rr < 3; rr++) for (let col = 0; col < 5; col++) { const x0 = col * cell, y0 = H * 0.78 + rr * cell * 0.5;
            c.beginPath(); c.moveTo(x0, y0 + cell * 0.25); c.lineTo(x0 + cell / 2, y0); c.lineTo(x0 + cell, y0 + cell * 0.25); c.lineTo(x0 + cell, y0 + cell * 0.75); c.lineTo(x0, y0 + cell * 0.75); c.closePath();
            c.fillStyle = rgba(cs[(rr * 2 + col) % 4], 45); c.fill(); c.strokeStyle = rgba(t.fg, 60); c.lineWidth = 2 * u; c.stroke(); } break; }
        case 'chart': { c.strokeStyle = rgba(t.accent, 110); c.lineWidth = 3 * u; c.beginPath(); let y = H * 0.9, x = 0; c.moveTo(0, y); while (x < W) { x += 22 * u; y -= (r() - 0.35) * 18 * u; c.lineTo(x, y); } c.stroke(); c.lineTo(W, H); c.lineTo(0, H); c.fillStyle = rgba(t.accent, 30); c.fill(); break; }
        case 'sunset': c.fillStyle = rgba(t.accent, 90); c.beginPath(); c.arc(W / 2, H * 0.9, Math.min(W, H) * 0.32, 0, PI * 2); c.fill();
          c.fillStyle = rgba(t.base, 200); for (let k = 0; k < 5; k++) c.fillRect(0, H * 0.82 + k * 9 * u, W, (2 + k) * u);
          for (let k = 0; k < 3; k++) { c.fillStyle = rgba(cs[k], 70); c.beginPath(); c.moveTo(0, H); for (let x = 0; x <= W; x += 8) c.lineTo(x, H * (0.9 + k * 0.03) + Math.sin(x / (60 * u) + k * 1.5) * 12 * u); c.lineTo(W, H); c.fill(); } break;
        case 'savanna': c.fillStyle = rgba('#ffb347', 120); c.beginPath(); c.arc(W * 0.72, H * 0.78, Math.min(W, H) * 0.2, 0, PI * 2); c.fill(); this.hill(c, W, H, 0.86, rgba('#d9a55a', 200));
          this.acacia(c, W * 0.2, H * 0.87, Math.min(W, H) * 0.25, mix(t.fg, t.base, 0.25)); this.acacia(c, W * 0.86, H * 0.88, Math.min(W, H) * 0.17, mix(t.fg, t.base, 0.35)); break;
        case 'park': { this.hill(c, W, H, 0.88, rgba(t.inC, 130)); this.hill(c, W, H, 0.92, rgba(t.inC, 200)); c.strokeStyle = 'rgba(255,255,255,.9)'; c.lineWidth = 4 * u;
          for (let x = 8; x < W; x += 22 * u) { c.beginPath(); c.moveTo(x, H * 0.86); c.lineTo(x, H * 0.92); c.stroke(); } for (const y of [0.875, 0.9]) { c.beginPath(); c.moveTo(0, H * y); c.lineTo(W, H * y); c.stroke(); } break; }
        case 'domes': c.fillStyle = rgba('#9fd3f5', 110); c.fillRect(0, H * 0.9, W, H * 0.1);
          for (const [x0, bw, bh] of [[0.02, 0.2, 0.1], [0.2, 0.22, 0.14], [0.44, 0.18, 0.08], [0.6, 0.24, 0.12], [0.82, 0.18, 0.1]]) { c.fillStyle = '#fff'; c.fillRect(W * x0, H * (0.9 - bh), W * bw, H * bh); c.strokeStyle = rgba(t.fg, 30); c.lineWidth = 1; c.strokeRect(W * x0, H * (0.9 - bh), W * bw, H * bh);
            c.fillStyle = t.accent; c.beginPath(); c.ellipse(W * (x0 + bw / 2), H * (0.9 - bh), W * bw * 0.25, W * bw * 0.25, 0, PI, 0); c.fill(); c.fillStyle = rgba(t.accent, 200); c.fillRect(W * (x0 + bw * 0.4), H * (0.9 - bh * 0.45), W * bw * 0.2, H * bh * 0.45); } break;
        case 'spots': for (let i = 0; i < 30; i++) { const x = r() * W, y = r() * H, s = (10 + r() * 16) * u; c.strokeStyle = rgba(t.accent, 45); c.lineWidth = s * 0.28; c.beginPath(); c.ellipse(x, y, s, s * 0.7, 0, r() * 1.5, r() * 1.5 + 4); c.stroke(); c.fillStyle = rgba(t.accent, 25); c.beginPath(); c.ellipse(x, y, s * 0.65, s * 0.45, 0, 0, PI * 2); c.fill(); } break;
        case 'tile_row': { const cell = W / 6; for (let rr = 0; rr < 3; rr++) for (let col = 0; col < 6; col++) { const x0 = col * cell, y0 = H - (rr + 1) * cell; c.fillStyle = '#fff'; c.fillRect(x0, y0, cell, cell); c.strokeStyle = rgba(t.accent, 200); c.lineWidth = 1.5 * u; c.strokeRect(x0, y0, cell, cell);
            c.fillStyle = rgba(t.accent, 210); const mx = x0 + cell / 2, my = y0 + cell / 2; for (let k = 0; k < 4; k++) { c.save(); c.translate(mx, my); c.rotate((k * 90 + 45) * PI / 180); c.beginPath(); c.ellipse(0, -cell * 0.22, cell * 0.08, cell * 0.16, 0, 0, PI * 2); c.fill(); c.restore(); }
            for (const [a, b] of [[x0, y0], [x0 + cell, y0], [x0, y0 + cell], [x0 + cell, y0 + cell]]) { c.beginPath(); c.arc(a, b, cell * 0.1, 0, PI * 2); c.fill(); } } break; }
        case 'gingham': { const sq = 22 * u; c.fillStyle = rgba(t.accent, 22); for (let x = 0; x < W; x += sq * 2) c.fillRect(x, 0, sq, H); for (let y = 0; y < H; y += sq * 2) c.fillRect(0, y, W, sq); this.hill(c, W, H, 0.9, rgba(t.inC, 90)); break; }
        case 'stickers': for (let i = 0; i < 18; i++) { const x = r() * W, y = r() * H, s = (14 + r() * 12) * u; c.save(); c.translate(x, y); c.rotate((r() * 50 - 25) * PI / 180); c.fillStyle = '#fff';
            if (i % 3 === 0) { c.beginPath(); c.arc(0, 0, s * 1.15, 0, PI * 2); c.fill(); c.fillStyle = rgba(cs[i % 4], 150); star5(c, 0, 0, s); }
            else if (i % 3 === 1) { heart(c, 0, 0, s * 1.2); c.fillStyle = rgba(cs[i % 4], 150); heart(c, 0, 0, s); }
            else { c.beginPath(); c.arc(0, 0, s * 1.1, 0, PI * 2); c.fill(); c.fillStyle = rgba('#ffd23f', 170); c.beginPath(); c.arc(0, 0, s, 0, PI * 2); c.fill(); c.fillStyle = rgba(t.fg, 170); c.beginPath(); c.arc(-s * 0.35, -s * 0.2, s * 0.12, 0, PI * 2); c.arc(s * 0.35, -s * 0.2, s * 0.12, 0, PI * 2); c.fill(); c.strokeStyle = rgba(t.fg, 170); c.lineWidth = s * 0.1; c.beginPath(); c.arc(0, 0, s * 0.5, 0.5, PI - 0.5); c.stroke(); }
            c.restore(); } break;
        case 'torn': for (let k = 0; k < 2; k++) { c.fillStyle = k ? rgba(t.accent, 35) : rgba('#e8e0d0', 30); const top = k ? H * 0.82 : H * 0.08; c.beginPath(); c.moveTo(0, k ? H : 0); for (let x = 0; x <= W; x += 9) c.lineTo(x, top + (r() - 0.5) * 14 * u); c.lineTo(W, k ? H : 0); c.fill(); }
          c.strokeStyle = rgba(t.accent, 40); c.lineWidth = 3 * u; for (let y = H * 0.84; y < H; y += 18 * u) { c.beginPath(); c.moveTo(0, y); c.lineTo(W, y); c.stroke(); } for (let x = 0; x < W; x += 18 * u) { c.beginPath(); c.moveTo(x, H * 0.82); c.lineTo(x, H); c.stroke(); } break;
        case 'checker': { const sq = W / 10; c.fillStyle = rgba(t.fg, 50); for (let rr = 0; rr < 3; rr++) for (let col = 0; col < 10; col++) if ((rr + col) % 2 === 0) c.fillRect(col * sq, H - (rr + 1) * sq, sq, sq);
          c.fillStyle = rgba('#ffd23f', 120); c.beginPath(); c.arc(W * 0.82, H * 0.12, 26 * u, 0, PI * 2); c.fill(); break; }
        case 'xmas': this.hill(c, W, H, 0.88, '#fff'); this.hill(c, W, H, 0.92, '#e6eff6');
          for (const [fx, s] of [[0.16, 1], [0.84, 0.75]]) { const x = W * fx, y = H * 0.9, h = Math.min(H * 0.2, W * 0.35) * s; c.fillStyle = rgba(t.inC, 230);
            for (let k = 0; k < 3; k++) { const ty = y - h + k * h * 0.28; c.beginPath(); c.moveTo(x, ty); c.lineTo(x - h * (0.22 + k * 0.08), ty + h * 0.38); c.lineTo(x + h * (0.22 + k * 0.08), ty + h * 0.38); c.fill(); }
            c.fillStyle = '#ffd23f'; star5(c, x, y - h, h * 0.07); const bl = [t.accent, '#ffd23f', '#4dabf7']; for (let k = 0; k < 7; k++) { c.fillStyle = bl[k % 3]; c.beginPath(); c.arc(x + (k - 3) * h * 0.07, y - h * 0.35 + Math.abs(k - 3) * h * 0.04, 3 * u, 0, PI * 2); c.fill(); } }
          { const bl = [t.accent, '#ffd23f', t.inC, '#4dabf7']; c.strokeStyle = rgba(t.fg, 80); c.lineWidth = 1; c.beginPath(); c.moveTo(0, H * 0.05); c.quadraticCurveTo(W / 2, H * 0.11, W, H * 0.05); c.stroke();
            for (let k = 1; k < 14; k++) { const x = W * k / 14, y = H * 0.05 + (1 - ((x / W - 0.5) * 2) ** 2) * H * 0.03; c.fillStyle = bl[k % 4]; c.beginPath(); c.arc(x, y + 5 * u, 4.5 * u, 0, PI * 2); c.fill(); } }
          break;
      }
    }

    mushroom(c, x, y, s) { c.fillStyle = mix(this.theme.base, '#ffffff', 0.5); c.fillRect(x - s * 0.18, y - s * 0.6, s * 0.36, s * 0.6 + 4); c.fillStyle = this.theme.accent; c.beginPath(); c.ellipse(x, y - s * 0.7, s * 0.6, s * 0.35, 0, PI, 0); c.fill(); c.fillStyle = '#fff'; c.beginPath(); c.arc(x - s * 0.22, y - s * 0.82, s * 0.08, 0, PI * 2); c.arc(x + s * 0.2, y - s * 0.9, s * 0.06, 0, PI * 2); c.fill(); }
    palm(c, x, y, h, col, u) { c.strokeStyle = col; c.lineWidth = 7 * u; c.beginPath(); c.moveTo(x, y); c.quadraticCurveTo(x + h * 0.15, y - h * 0.5, x + h * 0.05, y - h); c.stroke(); c.fillStyle = col;
      for (let k = 0; k < 6; k++) { c.save(); c.translate(x + h * 0.05, y - h); c.rotate(-PI / 2 + (k - 2.5) * 0.5); c.beginPath(); c.ellipse(h * 0.22, 0, h * 0.22, 7 * u, 0, 0, PI * 2); c.fill(); c.restore(); } }
    acacia(c, x, y, s, col) { c.strokeStyle = col; c.lineWidth = s * 0.06; c.beginPath(); c.moveTo(x, y); c.lineTo(x, y - s * 0.7); c.moveTo(x, y - s * 0.5); c.lineTo(x - s * 0.3, y - s * 0.8); c.moveTo(x, y - s * 0.55); c.lineTo(x + s * 0.3, y - s * 0.82); c.stroke(); c.fillStyle = col; c.beginPath(); c.ellipse(x, y - s * 0.86, s * 0.7, s * 0.14, 0, 0, PI * 2); c.fill(); }

    rays(c) { const W = this.W, H = this.H; for (let k = 0; k < 5; k++) { const x = W * (0.1 + k * 0.2) + Math.sin(this.t * 0.4 + k) * 30; const g = c.createLinearGradient(0, 0, 0, H * 0.8); g.addColorStop(0, 'rgba(200,250,255,.16)'); g.addColorStop(1, 'rgba(200,250,255,0)'); c.fillStyle = g; c.beginPath(); c.moveTo(x - 20, 0); c.lineTo(x + 30, 0); c.lineTo(x + 140, H * 0.8); c.lineTo(x + 40, H * 0.8); c.fill(); } }

    particle(c, p) {
      const t = this.theme, tw = (Math.sin(this.t * 1.6 + p.ph) + 1) / 2, s = p.s;
      if (p.fish) return this.fish(c, p);
      switch (t.particle) {
        case 'stars': c.fillStyle = rgba(t.dark ? '#ffffff' : p.col, 50 + 170 * tw); c.beginPath(); c.arc(p.x, p.y, s * 0.25, 0, PI * 2); c.fill(); break;
        case 'dots': c.fillStyle = rgba(p.col, 40 + 140 * tw); c.beginPath(); c.arc(p.x, p.y, s * 0.22, 0, PI * 2); c.fill(); break;
        case 'sparkles': c.fillStyle = rgba(p.col, 60 + 190 * tw); sparkle(c, p.x, p.y, s * (0.5 + 0.5 * tw)); break;
        case 'embers': c.fillStyle = rgba(p.col, 70 + 150 * tw); c.beginPath(); c.arc(p.x, p.y, s * 0.3, 0, PI * 2); c.fill(); break;
        case 'fireflies': { const g = c.createRadialGradient(p.x, p.y, 0, p.x, p.y, s * 2.2); g.addColorStop(0, rgba('#fff7b0', 120 + 130 * tw)); g.addColorStop(1, 'rgba(255,240,150,0)'); c.fillStyle = g; c.beginPath(); c.arc(p.x, p.y, s * 2.2, 0, PI * 2); c.fill(); break; }
        case 'bubbles': case 'ocean': c.strokeStyle = rgba(t.particle === 'ocean' ? '#e6fbff' : p.col, 150); c.lineWidth = 1.3; c.beginPath(); c.arc(p.x, p.y, s, 0, PI * 2); c.stroke(); c.fillStyle = 'rgba(255,255,255,.7)'; c.beginPath(); c.arc(p.x - s * 0.35, p.y - s * 0.35, s * 0.2, 0, PI * 2); c.fill(); break;
        case 'hearts': c.fillStyle = rgba(p.col, 150); heart(c, p.x, p.y, s); break;
        case 'petals': c.fillStyle = rgba(p.col, 150); c.save(); c.translate(p.x, p.y); c.rotate((this.t * 40 + p.ph * 50) * PI / 180); c.beginPath(); c.ellipse(0, 0, s, s * 0.5, 0, 0, PI * 2); c.fill(); c.restore(); break;
        case 'leaves': c.fillStyle = rgba(p.col, 140); c.save(); c.translate(p.x, p.y); c.rotate((this.t * 30 + p.ph * 60) * PI / 180); c.beginPath(); c.moveTo(-s, 0); c.quadraticCurveTo(0, -s * 0.9, s, 0); c.quadraticCurveTo(0, s * 0.9, -s, 0); c.fill(); c.restore(); break;
        case 'confetti': c.fillStyle = rgba(p.col, 190); c.save(); c.translate(p.x, p.y); c.rotate((this.t * 90 + p.ph * 60) * PI / 180); c.fillRect(-s * 0.6, -s * 0.25, s * 1.2, s * 0.5); c.restore(); break;
        case 'daisies': c.fillStyle = 'rgba(255,255,255,.8)'; for (let k = 0; k < 6; k++) { const a = k * PI / 3 + this.t; c.beginPath(); c.arc(p.x + s * 0.7 * Math.cos(a), p.y + s * 0.7 * Math.sin(a), s * 0.42, 0, PI * 2); c.fill(); } c.fillStyle = rgba('#ffc93c', 230); c.beginPath(); c.arc(p.x, p.y, s * 0.4, 0, PI * 2); c.fill(); break;
        case 'butterflies': { const f = 0.4 + 0.6 * Math.abs(Math.sin(this.t * 4 + p.ph)); c.fillStyle = rgba(p.col, 170); oval(c, p.x - s * f, p.y - s * 0.7, p.x, p.y + s * 0.2); c.fill(); oval(c, p.x, p.y - s * 0.7, p.x + s * f, p.y + s * 0.2); c.fill(); oval(c, p.x - s * 0.6 * f, p.y, p.x, p.y + s * 0.6); c.fill(); oval(c, p.x, p.y, p.x + s * 0.6 * f, p.y + s * 0.6); c.fill(); break; }
        case 'tickers': c.fillStyle = rgba(p.ph > 3.14 ? t.inC : t.outC, 120); c.fillRect(p.x - s * 0.3, p.y - s * 1.4, s * 0.6, s * 2.8); c.fillRect(p.x - 0.6, p.y - s * 2.1, 1.2, s * 4.2); break;
        case 'notes': c.fillStyle = rgba(p.col, 160); c.beginPath(); c.arc(p.x, p.y, s * 0.4, 0, PI * 2); c.fill(); c.fillRect(p.x + s * 0.32, p.y - s * 1.4, 1.5, s * 1.4); break;
        case 'snow': c.fillStyle = 'rgba(255,255,255,.9)'; c.beginPath(); c.arc(p.x, p.y, s * 0.32, 0, PI * 2); c.fill(); break;
        case 'paws': c.fillStyle = rgba(p.col, 70); c.beginPath(); c.ellipse(p.x, p.y + s * 0.25, s * 0.5, s * 0.35, 0, 0, PI * 2); c.fill(); for (let k = -1; k <= 1; k++) { c.beginPath(); c.arc(p.x + k * s * 0.42, p.y - s * 0.4 + Math.abs(k) * s * 0.18, s * 0.2, 0, PI * 2); c.fill(); } break;
        case 'lemons': c.save(); c.translate(p.x, p.y); c.rotate((this.t * 25 + p.ph * 50) * PI / 180); c.fillStyle = rgba('#ffd93b', 200); c.beginPath(); c.ellipse(0, 0, s * 0.8, s * 0.55, 0, 0, PI * 2); c.fill(); c.fillStyle = rgba('#5d9b4a', 200); c.beginPath(); c.ellipse(s * 0.75, -s * 0.75, s * 0.35, s * 0.2, 0, 0, PI * 2); c.fill(); c.restore(); break;
        case 'tennis': c.fillStyle = rgba('#d7e84a', 210); c.beginPath(); c.arc(p.x, p.y, s * 0.6, 0, PI * 2); c.fill(); c.strokeStyle = 'rgba(255,255,255,.9)'; c.lineWidth = s * 0.1; c.beginPath(); c.arc(p.x - s * 0.5, p.y, s * 0.4, -0.9, 0.9); c.stroke(); c.beginPath(); c.arc(p.x + s * 0.5, p.y, s * 0.4, PI - 0.9, PI + 0.9); c.stroke(); break;
        case 'smileys': c.fillStyle = rgba('#ffd23f', 150); c.beginPath(); c.arc(p.x, p.y, s * 0.7, 0, PI * 2); c.fill(); c.fillStyle = rgba(t.fg, 150); c.beginPath(); c.arc(p.x - s * 0.25, p.y - s * 0.15, s * 0.08, 0, PI * 2); c.arc(p.x + s * 0.25, p.y - s * 0.15, s * 0.08, 0, PI * 2); c.fill(); c.strokeStyle = rgba(t.fg, 150); c.lineWidth = s * 0.08; c.beginPath(); c.arc(p.x, p.y + s * 0.05, s * 0.32, 0.5, PI - 0.5); c.stroke(); break;
      }
    }

    fish(c, p) {
      const s = p.s; c.save(); c.translate(p.x, p.y); c.scale(p.dir, 1);
      if (p.jelly) { const g = c.createRadialGradient(0, 0, 0, 0, 0, s * 1.6); g.addColorStop(0, rgba(p.col, 140)); g.addColorStop(1, rgba(p.col, 0)); c.fillStyle = g; c.beginPath(); c.arc(0, 0, s * 1.6, 0, PI * 2); c.fill();
        c.fillStyle = rgba(p.col, 200); c.beginPath(); c.ellipse(0, 0, s, s * 0.7, 0, PI, 0); c.fill(); c.strokeStyle = rgba(p.col, 170); c.lineWidth = 2;
        for (let k = -2; k <= 2; k++) { c.beginPath(); c.moveTo(k * s * 0.35, 0); c.quadraticCurveTo(k * s * 0.35 + Math.sin(this.t * 3 + k) * 6, s, k * s * 0.35, s * 1.6); c.stroke(); } }
      else { c.fillStyle = p.col; c.beginPath(); c.ellipse(0, 0, s, s * 0.6, 0, 0, PI * 2); c.fill(); c.beginPath(); c.moveTo(-s * 0.8, 0); c.lineTo(-s * 1.5, -s * 0.55 + Math.sin(this.t * 6 + p.ph) * 2); c.lineTo(-s * 1.5, s * 0.55); c.fill();
        c.fillStyle = '#fff'; c.beginPath(); c.arc(s * 0.45, -s * 0.12, s * 0.18, 0, PI * 2); c.fill(); c.fillStyle = '#2a2230'; c.beginPath(); c.arc(s * 0.5, -s * 0.12, s * 0.09, 0, PI * 2); c.fill(); }
      c.restore();
    }
  }
  window.Scene = Scene;
})();
