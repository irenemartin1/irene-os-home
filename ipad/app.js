// Irene OS Home for iPad: themed home screen web app + icon packs + wallpapers.
(function () {
  const { rgba, mix, hexRgb } = window.Col;
  const C = window.Creatures;
  const $ = s => document.querySelector(s);

  // Apps the iPad can open from a web page (each opens the real app through its link).
  const CATALOG = [
    ['ireneos', 'Irene OS', 'https://ireneos.app', '#c9a0ff'],
    ['safari', 'Safari', 'https://www.google.com', '#3b8bf5'],
    ['messages', 'Messages', 'sms:', '#34c759'],
    ['facetime', 'FaceTime', 'facetime://', '#30d158'],
    ['mail', 'Mail', 'message://', '#2f7cf6'],
    ['photos', 'Photos', 'photos-redirect://', '#ff9f0a'],
    ['calendar', 'Calendar', 'calshow://', '#ff453a'],
    ['notes', 'Notes', 'mobilenotes://', '#ffd60a'],
    ['reminders', 'Reminders', 'x-apple-reminderkit://', '#ff9500'],
    ['maps', 'Maps', 'maps://', '#4cd964'],
    ['music', 'Music', 'music://', '#fa3e5f'],
    ['podcasts', 'Podcasts', 'podcasts://', '#b65cf0'],
    ['books', 'Books', 'ibooks://', '#ff8c1a'],
    ['appstore', 'App Store', 'itms-apps://', '#1a8cff'],
    ['files', 'Files', 'shareddocuments://', '#3f9cf5'],
    ['shortcuts', 'Shortcuts', 'shortcuts://', '#6b5cf6'],
    ['settings', 'Settings', 'App-prefs:', '#8e8e93'],
    ['appletv', 'TV', 'videos://', '#4a4a52'],
    ['youtube', 'YouTube', 'youtube://', '#ff2a2a'],
    ['netflix', 'Netflix', 'nflx://', '#e50914'],
    ['disney', 'Disney+', 'disneyplus://', '#1c3fae'],
    ['spotify', 'Spotify', 'spotify://', '#1ed760'],
    ['tiktok', 'TikTok', 'snssdk1233://', '#ff2c6d'],
    ['instagram', 'Instagram', 'instagram://', '#e1306c'],
    ['pinterest', 'Pinterest', 'pinterest://', '#e60023'],
    ['snapchat', 'Snapchat', 'snapchat://', '#fffc00'],
    ['whatsapp', 'WhatsApp', 'whatsapp://', '#25d366'],
    ['discord', 'Discord', 'discord://', '#5865f2'],
    ['gmail', 'Gmail', 'googlegmail://', '#ea4335'],
    ['chrome', 'Chrome', 'googlechrome://', '#fbbc04'],
    ['google', 'Google', 'google://', '#4285f4'],
    ['drive', 'Google Drive', 'googledrive://', '#1fa463'],
    ['zoom', 'Zoom', 'zoomus://', '#2d8cff'],
    ['claude', 'Claude', 'https://claude.ai', '#d97757'],
    ['chatgpt', 'ChatGPT', 'https://chatgpt.com', '#10a37f'],
    ['ebuilt', 'eBuilt', 'https://ebuilt.app', '#7b5cff'],
    ['amazon', 'Amazon', 'https://www.amazon.com', '#ff9900'],
    ['duolingo', 'Duolingo', 'duolingo://', '#58cc02'],
    ['roblox', 'Roblox', 'roblox://', '#e2231a'],
  ].map(([id, name, url, color]) => ({ id, name, url, color }));

  const DEFAULT_HOME = ['ireneos', 'messages', 'facetime', 'photos', 'mail', 'calendar', 'notes', 'reminders', 'maps', 'music', 'youtube', 'books', 'files', 'appstore', 'settings', 'ebuilt', 'claude'];
  const DEFAULT_DOCK = ['safari', 'messages', 'photos', 'ireneos'];

  // ───────── saved state ─────────
  const KEY = 'ireneHome.v1';
  let S = {};
  try { S = JSON.parse(localStorage.getItem(KEY) || '{}'); } catch (e) { S = {}; }
  S.theme = S.theme || 'forest'; S.land = S.land || 'forest'; S.note = S.note || '';
  S.home = S.home || DEFAULT_HOME.slice(); S.dock = S.dock || DEFAULT_DOCK.slice(); S.custom = S.custom || [];
  const save = () => { try { localStorage.setItem(KEY, JSON.stringify(S)); } catch (e) {} };

  const allApps = () => CATALOG.concat(S.custom);
  const appById = id => { const a = allApps().find(x => x.id === id); if (a && !a.role) a.role = C.roleOf(a.name, a.url); return a; };
  const theme = () => window.THEMES.find(t => t.key === S.theme) || window.THEMES[0];

  // ───────── background ─────────
  const scene = new window.Scene($('#bg'));

  function applyTheme() {
    const t = theme(), r = document.documentElement.style, light = !t.dark;
    document.body.classList.toggle('light', light);
    r.setProperty('--fg', t.fg); r.setProperty('--accent', t.accent);
    r.setProperty('--soft', rgba(t.fg, 170));
    r.setProperty('--card', light ? rgba(t.card, 175) : rgba(mix(t.base, t.accent, 0.12), 150));
    r.setProperty('--line', rgba(t.fg, light ? 30 : 45));
    r.setProperty('--drawer', rgba(t.base, light ? 215 : 205));
    r.setProperty('--sheet', light ? t.card : mix(t.base, t.accent, 0.1));
    r.setProperty('--sheetfg', t.fg); r.setProperty('--btn', rgba(t.fg, 20));
    const [R, G, B] = hexRgb(t.accent); r.setProperty('--onaccent', (R * 0.3 + G * 0.59 + B * 0.11) > 150 ? '#1a1a1a' : '#ffffff');
    document.querySelector('meta[name=apple-mobile-web-app-status-bar-style]').content = light ? 'default' : 'black-translucent';
    document.body.style.background = t.base;
    $('#hint').textContent = 'swipe up for all apps ' + t.emoji;
    $('#search').placeholder = 'search ' + t.name.toLowerCase() + ' ' + t.emoji;
    scene.setTheme(t); scene.start();
    iconCache.clear(); render();
  }

  // ───────── icons ─────────
  const iconCache = new Map();
  let ICON = 76;
  function iconFor(app) {
    const k = app.id;
    if (!iconCache.has(k)) iconCache.set(k, C.creatureCanvas(theme(), app, Math.round(ICON * Math.min(2, window.devicePixelRatio || 1))));
    const src = iconCache.get(k), cv = document.createElement('canvas'); cv.width = src.width; cv.height = src.height; cv.getContext('2d').drawImage(src, 0, 0); return cv;
  }
  function cell(app, where) {
    const d = document.createElement('div'); d.className = 'app'; d.dataset.id = app.id; d.dataset.where = where;
    d.appendChild(iconFor(app)); const s = document.createElement('span'); s.textContent = app.name; d.appendChild(s); return d;
  }

  // ───────── home layout ─────────
  function render() {
    const W = window.innerWidth;
    ICON = W >= 1000 ? 92 : W >= 700 ? 88 : 76;
    const cols = W >= 1000 ? 6 : W >= 700 ? 5 : 4, cellW = Math.floor(Math.min(130, (W * 0.92) / cols));
    document.documentElement.style.setProperty('--icon', ICON + 'px'); document.documentElement.style.setProperty('--cell', cellW + 'px');
    const pages = $('#pages'); pages.innerHTML = '';
    const ph = pages.clientHeight || (window.innerHeight * 0.55), rows = Math.max(1, Math.floor((ph - 10) / (ICON + 42)));
    const per = rows * cols, apps = S.home.map(appById).filter(Boolean);
    const n = Math.max(1, Math.ceil(apps.length / per));
    for (let p = 0; p < n; p++) {
      const pg = document.createElement('div'); pg.className = 'page'; pg.style.gridTemplateColumns = `repeat(${cols}, ${cellW}px)`;
      apps.slice(p * per, (p + 1) * per).forEach(a => pg.appendChild(cell(a, 'home'))); pages.appendChild(pg);
    }
    const dots = $('#dots'); dots.innerHTML = ''; if (n > 1) for (let i = 0; i < n; i++) dots.appendChild(document.createElement('i'));
    updateDots();
    const dock = $('#dock'); dock.innerHTML = ''; S.dock.map(appById).filter(Boolean).forEach(a => dock.appendChild(cell(a, 'dock')));
    renderAll();
  }
  function updateDots() { const p = $('#pages'), i = Math.round(p.scrollLeft / Math.max(1, p.clientWidth)); [...$('#dots').children].forEach((d, k) => d.classList.toggle('on', k === i)); }
  $('#pages').addEventListener('scroll', updateDots, { passive: true });

  function renderAll() {
    const q = $('#search').value.trim().toLowerCase(), W = window.innerWidth, cols = W >= 1000 ? 6 : W >= 700 ? 5 : 4;
    const all = $('#all'); all.innerHTML = ''; all.style.gridTemplateColumns = `repeat(${cols}, ${Math.floor(Math.min(130, (W * 0.92) / cols))}px)`;
    allApps().slice().sort((a, b) => a.name.localeCompare(b.name)).filter(a => !q || a.name.toLowerCase().includes(q)).forEach(a => { appById(a.id); all.appendChild(cell(a, 'all')); });
  }
  $('#search').addEventListener('input', renderAll);

  // ───────── note ─────────
  const note = $('#note'); note.value = S.note;
  note.addEventListener('input', () => { S.note = note.value; save(); });

  // ───────── opening apps ─────────
  function openApp(app) {
    if (/^https?:/i.test(app.url)) window.open(app.url, '_blank'); else window.location.href = app.url;
  }

  // ───────── touch: tap, long-press, swipe up / down ─────────
  let t0 = null, lpTimer = null, longPressed = false, drawerOpen = false;
  function targetApp(el) { const a = el.closest && el.closest('.app'); return a && a.dataset.where ? a : null; }
  document.addEventListener('touchstart', e => {
    if (e.target.closest('.sheet') || e.target === note) return;
    const p = e.touches[0]; t0 = { x: p.clientX, y: p.clientY, time: Date.now(), el: e.target, scrollTop: $('#all').scrollTop }; longPressed = false;
    clearTimeout(lpTimer);
    lpTimer = setTimeout(() => {
      longPressed = true; const a = targetApp(t0.el);
      if (navigator.vibrate) navigator.vibrate(10);
      if (a) appMenu(appById(a.dataset.id), a.dataset.where); else if (!drawerOpen) settingsMenu();
    }, 520);
  }, { passive: true });
  document.addEventListener('touchmove', e => {
    if (!t0) return; const p = e.touches[0];
    if (Math.abs(p.clientX - t0.x) > 10 || Math.abs(p.clientY - t0.y) > 10) clearTimeout(lpTimer);
  }, { passive: true });
  document.addEventListener('touchend', e => {
    clearTimeout(lpTimer); if (!t0 || longPressed) { t0 = null; return; }
    const p = e.changedTouches[0], dx = p.clientX - t0.x, dy = p.clientY - t0.y;
    if (!drawerOpen && dy < -70 && Math.abs(dy) > Math.abs(dx) * 1.3) { openDrawer(true); t0 = null; return; }
    if (drawerOpen && dy > 90 && Math.abs(dy) > Math.abs(dx) * 1.3 && t0.scrollTop <= 0) { openDrawer(false); t0 = null; return; }
    if (Math.abs(dx) < 10 && Math.abs(dy) < 10 && Date.now() - t0.time < 500) {
      const a = targetApp(t0.el); if (a) { e.preventDefault(); openApp(appById(a.dataset.id)); }
    }
    t0 = null;
  });
  // desktop mouse fallback (for testing on a computer)
  document.addEventListener('click', e => { if (!('ontouchstart' in window)) { const a = targetApp(e.target); if (a) openApp(appById(a.dataset.id)); } });
  document.addEventListener('contextmenu', e => { e.preventDefault(); const a = targetApp(e.target); if (a) appMenu(appById(a.dataset.id), a.dataset.where); else settingsMenu(); });
  $('#hint').addEventListener('click', () => openDrawer(true));

  function openDrawer(open) {
    drawerOpen = open; $('#drawer').classList.toggle('open', open);
    $('#home').style.opacity = open ? '0' : '1'; $('#home').style.transform = open ? 'translateY(-30px)' : 'none';
    if (!open) { $('#search').value = ''; $('#search').blur(); renderAll(); }
  }

  // ───────── sheets ─────────
  function sheet(html, onReady) {
    const sh = $('#sheet'); sh.innerHTML = '<div class="grab"></div>' + html; sh.scrollTop = 0;
    sh.classList.add('open'); $('#scrim').classList.add('open'); if (onReady) onReady(sh);
  }
  function closeSheet() { $('#sheet').classList.remove('open'); $('#scrim').classList.remove('open'); }
  $('#scrim').addEventListener('click', closeSheet);
  $('#scrim').addEventListener('touchend', e => { e.preventDefault(); closeSheet(); });
  function toast(msg) { const t = $('#toast'); t.textContent = msg; t.classList.add('show'); clearTimeout(toast.h); toast.h = setTimeout(() => t.classList.remove('show'), 1800); }
  const esc = s => String(s).replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));

  function settingsMenu() {
    const t = theme();
    const chips = window.THEMES.map(x => `<div class="chip ${x.key === S.theme ? 'on' : ''}" data-theme="${x.key}" style="background:linear-gradient(170deg,${x.sky.join(',')});color:${x.fg}">
      <span class="em">${x.emoji}</span><b>${esc(x.name)}</b><small>${esc(x.desc)}</small></div>`).join('');
    sheet(`<h2>${t.emoji} ${esc(t.name)}</h2>
      <button class="btn main" data-act="pack">get this theme for my real home screen ✦</button>
      <h3>themes · ${window.THEMES.length}</h3><div class="themes">${chips}</div>
      <h3>apps</h3>
      <button class="btn" data-act="apps">choose which apps are on my home screen</button>
      <button class="btn" data-act="add">add my own app or website</button>
      <h3>shake</h3>
      <button class="btn" data-act="shake">${S.shake ? 'shake 3× for enchanted underwater is on 🫧' : 'turn on: shake 3× to dive underwater 🫧'}</button>
      <h3>help</h3>
      <button class="btn" data-act="help">how to make this my iPad's home</button>`, sh => {
      sh.querySelectorAll('[data-theme]').forEach(el => el.onclick = () => { setTheme(el.dataset.theme); closeSheet(); });
      sh.querySelector('[data-act=pack]').onclick = () => packMenu();
      sh.querySelector('[data-act=apps]').onclick = () => appsMenu();
      sh.querySelector('[data-act=add]').onclick = () => addMenu();
      sh.querySelector('[data-act=shake]').onclick = () => enableShake(true);
      sh.querySelector('[data-act=help]').onclick = () => helpMenu();
    });
  }

  function setTheme(key) {
    if (key !== 'underwater') S.land = key; S.theme = key; save(); applyTheme();
    const t = theme(); toast(t.emoji + ' ' + t.name);
  }

  function appMenu(app, where) {
    if (!app) return;
    const inDock = S.dock.includes(app.id), onHome = S.home.includes(app.id), custom = S.custom.some(c => c.id === app.id);
    sheet(`<h2>${esc(app.name)}</h2>
      ${onHome ? '<div class="row"><button class="btn" data-a="left">← move earlier</button><button class="btn" data-a="right">move later →</button></div>' : ''}
      <button class="btn" data-a="home">${onHome ? 'remove from home screen' : 'add to home screen ✧'}</button>
      <button class="btn" data-a="dock">${inDock ? 'remove from dock' : 'put in dock'}</button>
      ${custom ? '<button class="btn" data-a="delete">delete this app shortcut</button>' : ''}
      <button class="btn" data-a="open">open ${esc(app.name)}</button>`, sh => {
      const go = (a, fn) => { const b = sh.querySelector(`[data-a=${a}]`); if (b) b.onclick = () => { fn(); save(); render(); closeSheet(); }; };
      go('left', () => { const i = S.home.indexOf(app.id); if (i > 0) [S.home[i - 1], S.home[i]] = [S.home[i], S.home[i - 1]]; });
      go('right', () => { const i = S.home.indexOf(app.id); if (i < S.home.length - 1) [S.home[i + 1], S.home[i]] = [S.home[i], S.home[i + 1]]; });
      go('home', () => { onHome ? S.home.splice(S.home.indexOf(app.id), 1) : S.home.push(app.id); });
      go('dock', () => { if (inDock) S.dock.splice(S.dock.indexOf(app.id), 1); else { if (S.dock.length >= 5) S.dock.shift(); S.dock.push(app.id); } });
      go('delete', () => { S.custom = S.custom.filter(c => c.id !== app.id); S.home = S.home.filter(i => i !== app.id); S.dock = S.dock.filter(i => i !== app.id); });
      go('open', () => openApp(app));
    });
  }

  function appsMenu() {
    const t = theme();
    const items = allApps().map(a => { appById(a.id); return `<div class="app ${S.home.includes(a.id) ? 'on' : ''}" data-id="${a.id}"><img src="${C.creatureCanvas(t, a, 120).toDataURL()}" width="60" height="60"><span>${esc(a.name)}</span></div>`; }).join('');
    sheet(`<h2>your apps</h2><p>Tap to add or remove from your home screen. Apps you don't have installed just won't open.</p><div class="catalog">${items}</div>
      <button class="btn main" data-a="done">done</button>`, sh => {
      sh.querySelectorAll('.catalog .app').forEach(el => el.onclick = () => {
        const id = el.dataset.id, i = S.home.indexOf(id);
        if (i >= 0) S.home.splice(i, 1); else S.home.push(id); el.classList.toggle('on', i < 0); save();
      });
      sh.querySelector('[data-a=done]').onclick = () => { render(); closeSheet(); };
    });
  }

  function addMenu() {
    sheet(`<h2>add an app or website</h2>
      <p>Give it a name and a link. A website link works (like <b>https://ireneos.app</b>), and so do app links like <b>spotify://</b>.</p>
      <input class="field" id="nName" placeholder="name (e.g. eBuilt Admin)">
      <input class="field" id="nUrl" placeholder="link (e.g. https://…)" autocapitalize="off" autocorrect="off">
      <button class="btn main" data-a="add">add to home screen ✧</button>`, sh => {
      sh.querySelector('[data-a=add]').onclick = () => {
        const name = sh.querySelector('#nName').value.trim(), url = sh.querySelector('#nUrl').value.trim();
        if (!name || !url) return toast('add a name and a link');
        const id = 'c' + Date.now().toString(36), palette = ['#ff8fb1', '#b197fc', '#7fd8ff', '#ffc93c', '#69db7c', '#ff9a76'];
        S.custom.push({ id, name, url: /^[a-z][a-z0-9+.-]*:/i.test(url) ? url : 'https://' + url, color: palette[S.custom.length % palette.length] });
        S.home.push(id); save(); render(); closeSheet(); toast('added ' + name + ' ✧');
      };
    });
  }

  function helpMenu() {
    sheet(`<h2>make Irene OS your iPad's home</h2>
      <p><b>1.</b> Open this page in <b>Safari</b>, tap the <b>Share</b> button (square with an arrow), then <b>Add to Home Screen</b>.</p>
      <p><b>2.</b> Open <b>Irene OS</b> from that new icon. It runs full screen, with no Safari bars.</p>
      <p><b>3.</b> Drag it into your iPad's dock so it's always one tap away.</p>
      <p><b>4.</b> For your real home screen and lock screen: long-press here, then <b>get this theme for my real home screen</b> to install creature icons and a matching wallpaper.</p>
      <p><b>Gestures:</b> swipe up for all apps, long-press an app to move it, long-press empty space for themes. Shake 3× to dive underwater once shake is on.</p>
      <button class="btn main" data-a="ok">got it</button>`, sh => { sh.querySelector('[data-a=ok]').onclick = closeSheet; });
  }

  // ───────── icon pack + wallpaper for the real home screen ─────────
  function packMenu() {
    const t = theme(), apps = S.home.concat(S.dock.filter(id => !S.home.includes(id))).map(appById).filter(Boolean);
    sheet(`<h2>${t.emoji} ${esc(t.name)} for your real home screen</h2>
      <h3>1 · all the creature icons at once</h3>
      <p>Tap below to download the icon profile, then go to <b>Settings → Profile Downloaded → Install</b>. If no prompt appears, open the <b>Files</b> app → <b>Downloads</b> and tap the file. Each icon opens the real app. Installing another theme's profile replaces this one.</p>
      <button class="btn main" data-a="profile">download ${esc(t.name)} icons (${apps.length})</button>
      <h3>2 · wallpaper (home + lock screen)</h3>
      <p>Press and hold the picture, tap <b>Add to Photos</b>, then go to <b>Settings → Wallpaper → Choose a New Wallpaper</b>.</p>
      <div id="wp"><p>drawing…</p></div>
      <h3>3 · single icons (optional)</h3>
      <p>To use one icon with a Shortcut, press and hold it, then tap <b>Add to Photos</b>.</p>
      <div class="icons" id="ic"></div>`, sh => {
      sh.querySelector('[data-a=profile]').onclick = () => downloadProfile(t, apps);
      setTimeout(() => {
        const img = new Image(); img.src = scene.snapshot(1536, 2048).toDataURL('image/jpeg', 0.92); img.alt = t.name + ' wallpaper';
        const wp = sh.querySelector('#wp'); wp.innerHTML = ''; wp.appendChild(img);
        const ic = sh.querySelector('#ic');
        apps.forEach(a => { const f = document.createElement('figure'); const im = new Image(); im.src = C.iconTile(t, a, 180).toDataURL(); f.appendChild(im); f.appendChild(document.createTextNode(a.name)); ic.appendChild(f); });
      }, 60);
    });
  }

  function uuid() { const b = new Uint8Array(16); crypto.getRandomValues(b); b[6] = (b[6] & 15) | 64; b[8] = (b[8] & 63) | 128; const h = [...b].map(x => x.toString(16).padStart(2, '0')).join(''); return `${h.slice(0, 8)}-${h.slice(8, 12)}-${h.slice(12, 16)}-${h.slice(16, 20)}-${h.slice(20)}`.toUpperCase(); }
  const xml = s => String(s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&apos;' }[c]));

  /** Builds an Apple configuration profile with one home-screen icon (Web Clip) per app. */
  function buildProfile(t, apps) {
    const clips = apps.map((a, i) => {
      const png = C.iconTile(t, a, 180).toDataURL('image/png').split(',')[1];
      return `<dict>
<key>FullScreen</key><false/>
<key>Icon</key><data>${png}</data>
<key>IsRemovable</key><true/>
<key>Label</key><string>${xml(a.name)}</string>
<key>PayloadDisplayName</key><string>${xml(a.name)}</string>
<key>PayloadIdentifier</key><string>app.irene.home.icons.${xml(a.id)}.${i}</string>
<key>PayloadType</key><string>com.apple.webClip.managed</string>
<key>PayloadUUID</key><string>${uuid()}</string>
<key>PayloadVersion</key><integer>1</integer>
<key>Precomposed</key><true/>
<key>URL</key><string>${xml(a.url)}</string>
</dict>`; }).join('\n');
    return `<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
<key>PayloadContent</key>
<array>
${clips}
</array>
<key>PayloadDescription</key><string>Irene OS creature icons in the ${xml(t.name)} theme. Each icon opens its app. Remove anytime in Settings → General → VPN &amp; Device Management.</string>
<key>PayloadDisplayName</key><string>Irene OS icons · ${xml(t.name)}</string>
<key>PayloadIdentifier</key><string>app.irene.home.icons</string>
<key>PayloadOrganization</key><string>Irene OS</string>
<key>PayloadRemovalDisallowed</key><false/>
<key>PayloadType</key><string>Configuration</string>
<key>PayloadUUID</key><string>${uuid()}</string>
<key>PayloadVersion</key><integer>1</integer>
</dict>
</plist>
`;
  }
  function downloadProfile(t, apps) {
    const blob = new Blob([buildProfile(t, apps)], { type: 'application/x-apple-aspen-config' });
    const a = document.createElement('a'); a.href = URL.createObjectURL(blob); a.download = `Irene OS icons - ${t.name.replace(/[^\w ]+/g, '').trim()}.mobileconfig`;
    document.body.appendChild(a); a.click(); a.remove(); toast('icon profile downloaded ✦');
  }
  window.IreneHome = { buildProfile, CATALOG, appById, scene, setTheme: k => setTheme(k) }; // used by the test page

  // ───────── shake 3× → underwater ─────────
  let shakes = [], lastShake = 0, cooldown = 0;
  function onMotion(e) {
    const a = e.accelerationIncludingGravity; if (!a || a.x == null) return;
    const g = Math.sqrt(a.x * a.x + a.y * a.y + a.z * a.z) / 9.81, now = Date.now();
    if (g > 2.2 && now - lastShake > 220) {
      lastShake = now; shakes = shakes.filter(x => now - x < 1500); shakes.push(now);
      if (shakes.length >= 3 && now > cooldown) { shakes = []; cooldown = now + 1500; setTheme(S.theme === 'underwater' ? (S.land || 'forest') : 'underwater'); }
    }
  }
  function enableShake(fromButton) {
    const done = ok => { if (ok) { window.addEventListener('devicemotion', onMotion); S.shake = true; save(); if (fromButton) { toast('shake 3× to dive underwater 🫧'); closeSheet(); } } else if (fromButton) toast('motion access was not allowed'); };
    if (typeof DeviceMotionEvent !== 'undefined' && typeof DeviceMotionEvent.requestPermission === 'function') DeviceMotionEvent.requestPermission().then(r => done(r === 'granted')).catch(() => done(false));
    else done(true);
  }
  if (S.shake) { // iPadOS asks again per launch: re-ask on the first tap
    if (typeof DeviceMotionEvent !== 'undefined' && typeof DeviceMotionEvent.requestPermission === 'function') document.addEventListener('touchend', function once() { document.removeEventListener('touchend', once); enableShake(false); });
    else enableShake(false);
  }

  // ───────── start ─────────
  document.addEventListener('visibilitychange', () => document.hidden ? scene.stop() : scene.start());
  let rz; window.addEventListener('resize', () => { clearTimeout(rz); rz = setTimeout(() => { iconCache.clear(); render(); }, 150); });
  applyTheme();
  // the Irene OS creature as this web app's home-screen icon
  try { $('#touchicon').href = C.iconTile(window.THEMES[0], appById('ireneos'), 180).toDataURL('image/png'); } catch (e) {}
  if (!S.welcomed && !window.navigator.standalone && !/[?&]test/.test(location.search)) { S.welcomed = true; save(); setTimeout(helpMenu, 600); }
})();
