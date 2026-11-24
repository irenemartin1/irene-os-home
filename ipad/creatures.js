// App creatures: same casts, costume pieces and "what the app is for" props as the Android launcher.
(function () {
  const PI = Math.PI, { rgba, mix } = window.Col, { star5, sparkle, heart, oval, rrect } = window.Shapes;
  const GOLD = '#e9c25a', INK = '#2a2230', PINK = '#ff8fb1', W_ = '#ffffff';
  const S = [-1, 1];

  const CAST = {
    forest: [['bunny_ears', 'leaf'], ['fox_ears', 'flower_crown'], ['owl_tufts', 'leaf_sprout'], ['bear_ears', 'mushroom_hat']],
    underwater: [['tail_fin', 'seashell_clip'], ['starfish', 'shell_necklace'], ['turtle_legs', 'turtle_shell'], ['fins', 'bubble_crown']],
    digital: [['antenna', 'visor'], ['antenna2', 'headphones'], ['visor', 'traces']],
    nature: [['elf_ears', 'leaf'], ['bunny_ears', 'flower_crown'], ['fox_ears', 'leaf_sprout']],
    gallery: [['beret', 'paint_splat'], ['glasses_gold', 'bowtie_gold'], ['mustache', 'beret_dark']],
    productivity: [['owl_tufts', 'glasses'], ['owl_tufts', 'pencil'], ['bear_ears', 'glasses']],
    studio: [['headphones'], ['paint_splat', 'beret'], ['star_glasses', 'bunny_ears']],
    kawaii: [['bunny_ears', 'bow'], ['cat_ears', 'heart_head', 'blush_hearts'], ['bear_ears', 'bow']],
    bull: [['horns', 'bowtie_gold'], ['horns', 'top_hat'], ['bear_ears', 'glasses_gold', 'bowtie_gold']],
    mushroom: [['stem_feet', 'mushroom_cap'], ['stem_feet', 'mushroom_cap_brown'], ['fox_ears', 'leaf']],
    coquette: [['bow_big', 'lace'], ['cat_ears', 'bow', 'pearls'], ['bunny_ears', 'bow_big']],
    celestial: [['moon_halo', 'star_forehead'], ['star_crown'], ['wizard_hat']],
    romantic: [['cupid_wings', 'heart_head'], ['flower_crown', 'pearls'], ['bunny_ears', 'heart_head', 'blush_hearts']],
    luxury: [['top_hat', 'bowtie_gold'], ['monocle', 'mustache'], ['pearls', 'tiara']],
    coastal: [['tail_fin', 'seashell_clip'], ['starfish', 'shell_necklace'], ['tail_fin', 'sun_hat']],
    zen: [['turtle_legs', 'turtle_shell'], ['leaf_sprout', 'glasses'], ['conical_hat']],
    y2k: [['butterfly_wings', 'star_forehead'], ['heart_glasses'], ['butterfly_clips', 'star_forehead']],
    retro: [['afro', 'retro_sunglasses'], ['headband', 'retro_sunglasses'], ['afro', 'mustache']],
    cyber: [['ninja_mask', 'headband_tails'], ['antenna', 'visor'], ['cat_ears', 'visor']],
    academia: [['cat_ears', 'glasses_gold'], ['owl_tufts', 'glasses_gold'], ['scarf', 'beret_dark']],
    princess: [['hair_long', 'tiara'], ['crown'], ['bow_big', 'pearls']],
    circuit: [['pins', 'traces'], ['antenna', 'traces'], ['pins', 'visor']],
    circus: [['ringmaster_hat'], ['party_hat', 'clown_nose'], ['bear_ears', 'party_hat']],
    disco: [['mirror_tiles', 'sparkle'], ['afro', 'star_glasses'], ['headband', 'mirror_tiles']],
    fitness: [['sweatband'], ['headphones'], ['visor_cap']],
    pride: [['unicorn'], ['flower_crown', 'heart_glasses'], ['unicorn', 'sparkle']],
    tropical: [['toucan_beak', 'hibiscus'], ['hibiscus', 'retro_sunglasses'], ['lei', 'sun_hat']],
    groovy: [['daisy_petals', 'daisy_center'], ['headband', 'round_sunglasses'], ['flower_crown', 'round_sunglasses']],
    glass: [['glass_wings', 'stained_lines'], ['glass_wings', 'tiara'], ['stained_lines', 'star_forehead']],
    wild: [['lion_mane'], ['sun_hat', 'stripes', 'cat_ears'], ['bear_ears', 'leaf_sprout']],
    dogs: [['floppy_ears', 'dog_nose', 'collar_tag'], ['fox_ears', 'dog_nose', 'bandana'], ['floppy_ears', 'dog_nose', 'bow']],
    mediterranean: [['cat_ears', 'lemon'], ['sun_hat', 'dog_nose'], ['cat_ears', 'flower_crown']],
    leopard: [['cat_ears', 'leopard_spots'], ['leopard_spots', 'round_sunglasses', 'pearls'], ['cat_ears', 'heart_glasses']],
    tiles: [['bird_tuft', 'tile_pattern', 'beak'], ['tile_pattern', 'flower_crown'], ['cat_ears', 'tile_pattern']],
    preppy: [['visor_cap', 'pearls'], ['bow_big', 'pearls'], ['headband', 'bow']],
    junk: [['bear_ears', 'raccoon_mask'], ['stickers', 'bow'], ['stickers', 'star_glasses']],
    grunge: [['crow_wings', 'beak'], ['beanie'], ['headphones', 'scarf']],
    indie: [['bucket_hat'], ['round_sunglasses', 'scarf'], ['beanie', 'flower_crown']],
    vsco: [['turtle_legs', 'turtle_shell'], ['scrunchie', 'shell_necklace'], ['starfish', 'sun_hat']],
    minimal: [['fold_lines'], ['fold_lines', 'dot'], ['dot']],
    christmas: [['icing'], ['santa_hat', 'scarf'], ['antlers', 'clown_nose']],
  };
  const SKIN = new Set(['paint_splat', 'traces', 'mirror_tiles', 'stained_lines', 'stripes', 'leopard_spots', 'tile_pattern', 'stickers', 'fold_lines', 'icing']);

  function hash(s) { let h = 0; for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) | 0; return h; }
  function bodyFor(t, app, look) {
    if (look.includes('icing')) return mix(app, '#b9774a', 0.75);
    if (t.key === 'minimal') return mix(app, '#e4e2de', 0.7);
    if (t.key === 'productivity') return mix(app, '#d9d9d4', 0.55);
    if (['luxury', 'academia', 'gallery', 'leopard', 'grunge'].includes(t.key)) return mix(app, t.accent, 0.45);
    if (['digital', 'cyber', 'circuit', 'disco', 'fitness', 'bull', 'celestial', 'forest', 'underwater'].includes(t.key)) return mix(app, t.accent, 0.3);
    return mix(app, t.accent, 0.28);
  }

  /** Draws one app creature into ctx, centered in a size×size box. */
  function drawCreature(c, size, t, app) {
    const cast = CAST[t.key] || [['sparkle']];
    const look = cast[Math.abs(hash(app.name + '*')) % cast.length];
    const body = bodyFor(t, app.color, look), dark = mix(body, '#000000', 0.45);
    const cx = size / 2, cy = size * 0.55, r = size * 0.3;
    const P = { c, t, body, dark, cx, cy, r };
    c.save(); c.lineCap = 'round'; c.lineJoin = 'round';
    // soft glow behind
    const g = c.createRadialGradient(cx, cy, r * 0.6, cx, cy, r * 1.55); g.addColorStop(0, rgba(t.accent, 90)); g.addColorStop(1, rgba(t.accent, 0)); c.fillStyle = g; c.fillRect(0, 0, size, size);
    for (const p of look) back(P, p);
    // body with a little shading
    const bg = c.createRadialGradient(cx - r * 0.35, cy - r * 0.4, r * 0.1, cx, cy, r * 1.05); bg.addColorStop(0, mix(body, '#ffffff', 0.25)); bg.addColorStop(1, body);
    c.fillStyle = bg; c.beginPath(); c.arc(cx, cy, r, 0, PI * 2); c.fill();
    c.save(); c.beginPath(); c.arc(cx, cy, r, 0, PI * 2); c.clip(); for (const p of look) if (SKIN.has(p)) skin(P, p); c.restore();
    // belly with the app's job drawn on it
    const belly = mix(body, '#ffffff', 0.72), br = r * 0.56, by = cy + r * 0.26;
    c.fillStyle = belly; c.beginPath(); c.arc(cx, by, br, 0, PI * 2); c.fill();
    drawProp(c, app.role, cx, by + br * 0.04, br * 1.05, belly, mix(app.color, '#000000', 0.35), app.color, t.emoji);
    for (const p of look) if (!SKIN.has(p)) front(P, p);
    face(P);
    c.fillStyle = 'rgba(255,255,255,.35)'; c.beginPath(); c.ellipse(cx - r * 0.5, cy - r * 0.65, r * 0.2, r * 0.12, -0.5, 0, PI * 2); c.fill();
    c.restore();
  }

  function face({ c, t, cx, cy, r }) {
    const ey = cy - r * 0.42, ex = r * 0.36, er = r * 0.17;
    if (t.key === 'minimal') { c.fillStyle = '#1a1a1a'; for (const s of S) { c.beginPath(); c.arc(cx + s * ex, ey, er * 0.38, 0, PI * 2); c.fill(); } }
    else if (t.key === 'digital' || t.key === 'circuit') { for (const s of S) { c.fillStyle = t.accent; c.beginPath(); c.arc(cx + s * ex, ey, er * 0.8, 0, PI * 2); c.fill(); c.fillStyle = 'rgba(255,255,255,.8)'; c.beginPath(); c.arc(cx + s * ex, ey, er * 0.35, 0, PI * 2); c.fill(); } }
    else for (const s of S) { c.fillStyle = '#fff'; c.beginPath(); c.arc(cx + s * ex, ey, er, 0, PI * 2); c.fill(); c.fillStyle = '#2a2230'; c.beginPath(); c.arc(cx + s * ex + er * 0.12, ey + er * 0.1, er * 0.62, 0, PI * 2); c.fill(); c.fillStyle = '#fff'; c.beginPath(); c.arc(cx + s * ex - er * 0.1, ey - er * 0.2, er * 0.24, 0, PI * 2); c.fill(); }
    c.fillStyle = 'rgba(255,120,150,.35)'; for (const s of S) { c.beginPath(); c.ellipse(cx + s * r * 0.62, cy - r * 0.16, r * 0.13, r * 0.08, 0, 0, PI * 2); c.fill(); }
    c.strokeStyle = 'rgba(42,34,48,.7)'; c.lineWidth = r * 0.05; c.beginPath(); c.arc(cx, cy - r * 0.24, r * 0.09, 0.3, PI - 0.3); c.stroke();
  }

  // little helpers
  const fl = (c, col) => { c.fillStyle = col; };
  const st = (c, col, w) => { c.strokeStyle = col; c.lineWidth = w; };
  const circ = (c, x, y, r) => { c.beginPath(); c.arc(x, y, r, 0, PI * 2); c.fill(); };
  const ovf = (c, x0, y0, x1, y1) => { oval(c, x0, y0, x1, y1); c.fill(); };
  const poly = (c, pts) => { c.beginPath(); pts.forEach(([x, y], i) => i ? c.lineTo(x, y) : c.moveTo(x, y)); c.closePath(); c.fill(); };
  const ln = (c, x0, y0, x1, y1) => { c.beginPath(); c.moveTo(x0, y0); c.lineTo(x1, y1); c.stroke(); };
  function star4(c, x, y, s) { sparkle(c, x, y, s); }
  function bow(c, x, y, s, col) { fl(c, col); poly(c, [[x, y], [x - s * 1.3, y - s * 0.75], [x - s * 1.3, y + s * 0.75]]); poly(c, [[x, y], [x + s * 1.3, y - s * 0.75], [x + s * 1.3, y + s * 0.75]]); fl(c, mix(col, '#000000', 0.2)); circ(c, x, y, s * 0.35); }
  function leaf(c, x, y, s, col) { fl(c, col); c.beginPath(); c.moveTo(x - s, y + s * 0.3); c.quadraticCurveTo(x - s * 0.2, y - s * 0.9, x + s, y - s * 0.3); c.quadraticCurveTo(x, y + s * 0.6, x - s, y + s * 0.3); c.fill(); }
  function flower(c, x, y, s, col) { fl(c, col); for (let k = 0; k < 5; k++) { const a = k * 2 * PI / 5; circ(c, x + s * Math.cos(a), y + s * Math.sin(a), s * 0.85); } fl(c, '#ffe066'); circ(c, x, y, s * 0.5); }
  function glasses(c, cx, cy, r, col) { st(c, col, r * 0.07); for (const s of S) { c.beginPath(); c.arc(cx + s * r * 0.36, cy - r * 0.42, r * 0.27, 0, PI * 2); c.stroke(); } ln(c, cx - r * 0.1, cy - r * 0.45, cx + r * 0.1, cy - r * 0.45); }
  function hexagon(c, x, y, s) { c.beginPath(); for (let k = 0; k < 6; k++) { const a = k * PI / 3; c.lineTo(x + s * Math.cos(a), y + s * Math.sin(a)); } c.closePath(); c.stroke(); }

  function back({ c, t, body, dark, cx, cy, r }, p) {
    switch (p) {
      case 'antenna': case 'antenna2': for (const dx of p === 'antenna' ? [0] : [-0.45, 0.45]) { st(c, rgba(t.fg, 230), r * 0.09); ln(c, cx + dx * r, cy - r * 0.9, cx + dx * r * 1.25, cy - r * 1.35); fl(c, rgba(t.accent, 70)); circ(c, cx + dx * r * 1.25, cy - r * 1.4, r * 0.26); fl(c, t.accent); circ(c, cx + dx * r * 1.25, cy - r * 1.4, r * 0.13); } break;
      case 'elf_ears': fl(c, body); for (const s of S) poly(c, [[cx + s * r * 0.75, cy - r * 0.45], [cx + s * r * 1.42, cy - r * 0.85], [cx + s * r * 0.88, cy - r * 0.05]]); break;
      case 'owl_tufts': fl(c, dark); for (const s of S) poly(c, [[cx + s * r * 0.25, cy - r * 0.9], [cx + s * r * 0.85, cy - r * 1.3], [cx + s * r * 0.85, cy - r * 0.55]]); break;
      case 'bunny_ears': for (const s of S) { fl(c, body); ovf(c, cx + s * r * 0.42 - r * 0.24, cy - r * 1.75, cx + s * r * 0.42 + r * 0.24, cy - r * 0.5); fl(c, PINK); ovf(c, cx + s * r * 0.42 - r * 0.15, cy - r * 1.6, cx + s * r * 0.42 + r * 0.15, cy - r * 0.65); } break;
      case 'cat_ears': case 'fox_ears': for (const s of S) { const tall = p === 'fox_ears' ? 1.45 : 1.3; fl(c, body); poly(c, [[cx + s * r * 0.15, cy - r * 0.85], [cx + s * r * 0.8, cy - r * tall], [cx + s * r * 0.92, cy - r * 0.35]]); fl(c, p === 'fox_ears' ? W_ : PINK); poly(c, [[cx + s * r * 0.38, cy - r * 0.8], [cx + s * r * 0.76, cy - r * (tall - 0.22)], [cx + s * r * 0.8, cy - r * 0.55]]); } break;
      case 'bear_ears': for (const s of S) { fl(c, body); circ(c, cx + s * r * 0.68, cy - r * 0.72, r * 0.32); fl(c, mix(body, W_, 0.45)); circ(c, cx + s * r * 0.68, cy - r * 0.72, r * 0.16); } break;
      case 'floppy_ears': fl(c, dark); for (const s of S) { c.save(); c.translate(cx + s * r * 0.85, cy - r * 0.25); c.rotate(s * 0.3); c.beginPath(); c.ellipse(0, 0, r * 0.24, r * 0.5, 0, 0, PI * 2); c.fill(); c.restore(); } break;
      case 'horns': fl(c, '#f4ebd0'); for (const s of S) { c.beginPath(); c.moveTo(cx + s * r * 0.55, cy - r * 0.7); c.quadraticCurveTo(cx + s * r * 1.35, cy - r * 0.8, cx + s * r * 1.25, cy - r * 1.35); c.quadraticCurveTo(cx + s * r * 1.05, cy - r * 0.95, cx + s * r * 0.35, cy - r * 0.95); c.fill(); } break;
      case 'antlers': st(c, '#8a5a34', r * 0.1); for (const s of S) { ln(c, cx + s * r * 0.4, cy - r * 0.85, cx + s * r * 0.75, cy - r * 1.55); ln(c, cx + s * r * 0.6, cy - r * 1.2, cx + s * r * 1.0, cy - r * 1.3); ln(c, cx + s * r * 0.7, cy - r * 1.45, cx + s * r * 0.5, cy - r * 1.7); } break;
      case 'moon_halo': fl(c, t.accent); circ(c, cx + r * 0.75, cy - r * 0.85, r * 0.55); fl(c, t.base); circ(c, cx + r * 1.0, cy - r * 1.0, r * 0.48); fl(c, 'rgba(255,255,255,.9)'); star4(c, cx - r * 1.05, cy - r * 0.9, r * 0.17); star4(c, cx - r * 1.25, cy - r * 0.2, r * 0.1); break;
      case 'cupid_wings': fl(c, 'rgba(255,255,255,.92)'); for (const s of S) for (let k = 0; k < 3; k++) { const ey = cy - r * 0.35 + k * r * 0.28; ovf(c, cx + s * r * 0.6, ey - r * 0.2, cx + s * r * (1.45 - k * 0.15), ey + r * 0.2); } break;
      case 'butterfly_wings': for (const s of S) { fl(c, rgba(t.accent, 210)); ovf(c, cx + s * r * 0.3, cy - r * 1.2, cx + s * r * 1.5, cy + r * 0.05); fl(c, rgba('#7fd8ff', 210)); ovf(c, cx + s * r * 0.4, cy - r * 0.05, cx + s * r * 1.25, cy + r * 0.95); } break;
      case 'glass_wings': for (const s of S) { fl(c, rgba(t.accent, 90)); oval(c, cx + s * r * 0.5, cy - r * 1.3, cx + s * r * 1.45, cy - r * 0.1); c.fill(); st(c, rgba(t.fg, 150), r * 0.05); c.stroke(); } break;
      case 'crow_wings': fl(c, '#1c1a22'); for (const s of S) poly(c, [[cx + s * r * 0.7, cy - r * 0.3], [cx + s * r * 1.5, cy + r * 0.1], [cx + s * r * 1.25, cy + r * 0.35], [cx + s * r * 1.4, cy + r * 0.6], [cx + s * r * 0.8, cy + r * 0.6]]); break;
      case 'fins': fl(c, mix(body, t.accent, 0.4)); for (const s of S) poly(c, [[cx + s * r * 0.85, cy - r * 0.1], [cx + s * r * 1.45, cy - r * 0.45], [cx + s * r * 1.3, cy + r * 0.35]]); break;
      case 'unicorn': ['#ff6b6b', '#ffa94d', '#ffe066', '#69db7c', '#4dabf7', '#b197fc'].forEach((col, k) => { fl(c, col); circ(c, cx - r * 0.9 + k * r * 0.05, cy - r * 0.7 + k * r * 0.3, r * 0.26); }); fl(c, GOLD); poly(c, [[cx - r * 0.18, cy - r * 0.85], [cx + r * 0.1, cy - r * 1.6], [cx + r * 0.22, cy - r * 0.8]]); break;
      case 'lion_mane': fl(c, '#c8742a'); for (let k = 0; k < 12; k++) { const a = k * PI / 6; circ(c, cx + r * Math.cos(a), cy + r * Math.sin(a), r * 0.34); } fl(c, body); for (const s of S) circ(c, cx + s * r * 0.62, cy - r * 0.82, r * 0.2); break;
      case 'afro': fl(c, '#4a2c1a'); for (let k = 0; k < 9; k++) { const a = PI + k * PI / 8; circ(c, cx + r * 0.95 * Math.cos(a), cy - r * 0.15 + r * 0.95 * Math.sin(a), r * 0.38); } circ(c, cx, cy - r * 0.55, r * 0.75); break;
      case 'hair_long': fl(c, '#f6c36b'); ovf(c, cx - r * 1.1, cy - r * 1.05, cx + r * 1.1, cy + r * 0.8); break;
      case 'daisy_petals': fl(c, W_); for (let k = 0; k < 10; k++) { c.save(); c.translate(cx, cy); c.rotate(k * PI / 5); c.beginPath(); c.ellipse(r * 1.08, 0, r * 0.38, r * 0.26, 0, 0, PI * 2); c.fill(); c.restore(); } break;
      case 'pins': fl(c, GOLD); for (let k = 0; k < 12; k++) { c.save(); c.translate(cx, cy); c.rotate((k * 30 + 15) * PI / 180); c.fillRect(r * 0.9, -r * 0.07, r * 0.32, r * 0.14); c.restore(); } break;
      case 'tail_fin': fl(c, mix(body, t.accent, 0.4)); c.beginPath(); c.moveTo(cx + r * 0.5, cy + r * 0.7); c.quadraticCurveTo(cx + r * 1.2, cy + r * 0.8, cx + r * 1.45, cy + r * 0.35); c.quadraticCurveTo(cx + r * 1.25, cy + r * 0.85, cx + r * 1.55, cy + r * 1.15); c.quadraticCurveTo(cx + r * 1.0, cy + r * 1.1, cx + r * 0.3, cy + r * 0.95); c.fill(); break;
      case 'turtle_legs': fl(c, mix(body, t.accent, 0.5)); for (const [dx, dy] of [[-0.85, 0.65], [0.85, 0.65], [-0.95, -0.1], [0.95, -0.1]]) ovf(c, cx + dx * r - r * 0.22, cy + dy * r - r * 0.15, cx + dx * r + r * 0.22, cy + dy * r + r * 0.15); break;
      case 'stem_feet': fl(c, '#f7ebd3'); ovf(c, cx - r * 0.55, cy + r * 0.7, cx - r * 0.15, cy + r * 1.1); ovf(c, cx + r * 0.15, cy + r * 0.7, cx + r * 0.55, cy + r * 1.1); break;
      case 'bird_tuft': fl(c, t.accent); for (let k = -1; k <= 1; k++) { c.save(); c.translate(cx, cy - r * 0.9); c.rotate(k * 0.38); c.beginPath(); c.ellipse(0, -r * 0.25, r * 0.1, r * 0.3, 0, 0, PI * 2); c.fill(); c.restore(); } break;
    }
  }

  function skin({ c, t, cx, cy, r }, p) {
    switch (p) {
      case 'paint_splat': [t.accent, t.inC, t.outC, '#ffd23f'].forEach((col, k) => { fl(c, rgba(col, 210)); const a = k * 1.7 + 0.4; circ(c, cx + r * 0.7 * Math.cos(a), cy + r * 0.7 * Math.sin(a), r * 0.2); }); break;
      case 'traces': st(c, rgba(GOLD, 200), r * 0.05); fl(c, GOLD); for (const s of S) { ln(c, cx + s * r, cy - r * 0.05, cx + s * r * 0.62, cy - r * 0.05); ln(c, cx + s * r * 0.62, cy - r * 0.05, cx + s * r * 0.62, cy + r * 0.5); circ(c, cx + s * r * 0.62, cy - r * 0.05, r * 0.06); } break;
      case 'mirror_tiles': st(c, 'rgba(255,255,255,.28)', r * 0.035); for (let k = -r; k <= r; k += r * 0.25) { ln(c, cx - r, cy + k, cx + r, cy + k); ln(c, cx + k, cy - r, cx + k, cy + r); } break;
      case 'stained_lines': st(c, rgba(t.fg, 120), r * 0.06); ln(c, cx - r, cy - r * 0.2, cx + r, cy - r * 0.05); ln(c, cx - r * 0.3, cy - r, cx - r * 0.05, cy - r * 0.15); ln(c, cx + r * 0.35, cy - r, cx + r * 0.1, cy - r * 0.1); break;
      case 'stripes': fl(c, rgba('#3a2610', 170)); for (const k of [-2, -1, 1, 2]) { const x = cx + k * r * 0.38; poly(c, [[x - r * 0.08, cy - r], [x + r * 0.08, cy - r], [x, cy - r * 0.45]]); } break;
      case 'leopard_spots': st(c, rgba('#2a1a0c', 200), r * 0.07); for (const [dx, dy] of [[-0.6, -0.6], [0.55, -0.7], [-0.85, 0.1], [0.85, 0.05], [0, -0.85], [-0.5, 0.75], [0.55, 0.75]]) { c.beginPath(); c.ellipse(cx + dx * r, cy + dy * r, r * 0.14, r * 0.11, 0, 0.35, 0.35 + 4.5); c.stroke(); } break;
      case 'tile_pattern': st(c, rgba(t.accent, 160), r * 0.04); for (let k = -r; k <= r; k += r * 0.5) { ln(c, cx + k, cy - r, cx + k, cy + r); ln(c, cx - r, cy + k, cx + r, cy + k); } fl(c, rgba(t.accent, 170)); for (const [dx, dy] of [[-0.5, -0.5], [0.5, -0.5], [-0.75, 0.25], [0.75, 0.25]]) circ(c, cx + dx * r, cy + dy * r, r * 0.1); break;
      case 'stickers': fl(c, '#ffd23f'); star5(c, cx - r * 0.7, cy - r * 0.55, r * 0.22); fl(c, t.accent); heart(c, cx + r * 0.72, cy - r * 0.5, r * 0.18); fl(c, t.inC); circ(c, cx - r * 0.85, cy + r * 0.25, r * 0.12); break;
      case 'fold_lines': st(c, rgba(t.fg, 40), r * 0.03); ln(c, cx, cy - r, cx, cy + r); ln(c, cx - r, cy - r, cx + r, cy + r); ln(c, cx + r, cy - r, cx - r, cy + r); break;
      case 'icing': st(c, W_, r * 0.07); c.beginPath(); c.moveTo(cx - 0.9 * r, cy - r * 0.72); for (let x = -0.9; x < 0.9; x += 0.2) c.quadraticCurveTo(cx + (x + 0.1) * r, cy - r * 0.6, cx + (x + 0.2) * r, cy - r * 0.72); c.stroke(); fl(c, '#e5394b'); circ(c, cx - r * 0.75, cy + r * 0.15, r * 0.09); circ(c, cx + r * 0.75, cy + r * 0.15, r * 0.09); break;
    }
  }

  function front({ c, t, body, cx, cy, r }, p) {
    switch (p) {
      case 'visor': fl(c, rgba(t.base, 235)); rrect(c, cx - r * 0.72, cy - r * 0.62, r * 1.44, r * 0.4, r * 0.2); c.fill(); st(c, rgba(t.accent, 200), r * 0.05); c.stroke(); break;
      case 'leaf': leaf(c, cx + r * 0.15, cy - r * 1.05, r * 0.42, '#6e8f55'); break;
      case 'leaf_sprout': st(c, '#55684a', r * 0.06); ln(c, cx, cy - r * 0.95, cx, cy - r * 1.25); leaf(c, cx - r * 0.22, cy - r * 1.3, r * 0.22, '#7fb069'); leaf(c, cx + r * 0.22, cy - r * 1.3, r * 0.22, '#7fb069'); break;
      case 'flower_crown': [PINK, '#ffd23f', W_, '#b197fc', PINK].forEach((col, k) => { const a = PI + (k + 0.5) * PI / 5; flower(c, cx + r * 0.92 * Math.cos(a), cy - r * 0.1 + r * 0.92 * Math.sin(a), r * 0.13, col); }); break;
      case 'mushroom_hat': fl(c, '#e5394b'); c.beginPath(); c.ellipse(cx, cy - r * 0.78, r * 0.75, r * 0.45, 0, PI, 0); c.fill(); fl(c, W_); circ(c, cx - r * 0.3, cy - r * 1.0, r * 0.09); circ(c, cx + r * 0.25, cy - r * 1.05, r * 0.11); break;
      case 'bubble_crown': st(c, 'rgba(230,251,255,.9)', r * 0.04); for (const [dx, dy, s] of [[-0.45, -1.05, 0.14], [0, -1.2, 0.18], [0.45, -1.05, 0.14]]) { c.beginPath(); c.arc(cx + dx * r, cy + dy * r, s * r, 0, PI * 2); c.stroke(); } break;
      case 'beret': case 'beret_dark': fl(c, p === 'beret' ? '#b5463b' : '#3b2a20'); c.save(); c.translate(cx - r * 0.05, cy - r * 0.88); c.rotate(-0.2); c.beginPath(); c.ellipse(0, 0, r * 0.8, r * 0.27, 0, 0, PI * 2); c.fill(); c.fillRect(-r * 0.04, -r * 0.42, r * 0.08, r * 0.2); c.restore(); break;
      case 'mustache': fl(c, '#3b2a20'); for (const s of S) ovf(c, cx + s * r * 0.02, cy - r * 0.2, cx + s * r * 0.42, cy - r * 0.02); break;
      case 'glasses': glasses(c, cx, cy, r, rgba(INK, 220)); break;
      case 'glasses_gold': glasses(c, cx, cy, r, GOLD); break;
      case 'monocle': st(c, GOLD, r * 0.06); c.beginPath(); c.arc(cx + r * 0.36, cy - r * 0.42, r * 0.27, 0, PI * 2); c.stroke(); ln(c, cx + r * 0.6, cy - r * 0.3, cx + r * 0.75, cy + r * 0.3); break;
      case 'round_sunglasses': fl(c, rgba(INK, 220)); for (const s of S) circ(c, cx + s * r * 0.36, cy - r * 0.42, r * 0.25); st(c, rgba(INK, 220), r * 0.06); ln(c, cx - r * 0.12, cy - r * 0.45, cx + r * 0.12, cy - r * 0.45); break;
      case 'retro_sunglasses': fl(c, rgba('#e0702b', 210)); for (const s of S) { rrect(c, s < 0 ? cx - r * 0.7 : cx + r * 0.08, cy - r * 0.62, r * 0.62, r * 0.4, r * 0.18); c.fill(); } break;
      case 'star_glasses': fl(c, rgba(t.accent, 220)); for (const s of S) star5(c, cx + s * r * 0.36, cy - r * 0.42, r * 0.3); break;
      case 'heart_glasses': fl(c, rgba(t.accent, 220)); for (const s of S) heart(c, cx + s * r * 0.36, cy - r * 0.45, r * 0.26); break;
      case 'headphones': st(c, rgba(INK, 230), r * 0.12); c.beginPath(); c.ellipse(cx, cy - r * 0.4, r * 0.95, r * 0.8, 0, PI * 1.11, PI * 1.89); c.stroke(); fl(c, t.accent); for (const s of S) { rrect(c, cx + s * r * 0.95 - r * 0.2, cy - r * 0.45, r * 0.4, r * 0.5, r * 0.15); c.fill(); } break;
      case 'bow': bow(c, cx + r * 0.55, cy - r * 0.85, r * 0.3, t.accent); break;
      case 'bow_big': bow(c, cx, cy - r * 0.98, r * 0.42, t.accent); break;
      case 'bowtie_gold': bow(c, cx, cy + r * 0.92, r * 0.26, GOLD); break;
      case 'top_hat': fl(c, '#111112'); c.fillRect(cx - r * 0.45, cy - r * 1.55, r * 0.9, r * 0.75); ovf(c, cx - r * 0.75, cy - r * 0.92, cx + r * 0.75, cy - r * 0.72); fl(c, GOLD); c.fillRect(cx - r * 0.45, cy - r * 1.02, r * 0.9, r * 0.12); break;
      case 'mushroom_cap': case 'mushroom_cap_brown': fl(c, p === 'mushroom_cap' ? t.accent : '#9a6a3c'); c.beginPath(); c.ellipse(cx, cy - r * 0.75, r * 1.15, r * 0.6, 0, PI, 0); c.fill(); fl(c, p === 'mushroom_cap' ? W_ : '#f3e3c3'); circ(c, cx - r * 0.5, cy - r * 0.95, r * 0.13); circ(c, cx + r * 0.2, cy - r * 1.1, r * 0.16); circ(c, cx + r * 0.7, cy - r * 0.8, r * 0.1); break;
      case 'lace': fl(c, 'rgba(255,255,255,.9)'); for (let k = 0; k < 7; k++) circ(c, cx - r * 0.6 + k * r * 0.2, cy + r * 0.88 - Math.abs(k - 3) * r * 0.06, r * 0.11); break;
      case 'pearls': for (let k = 0; k < 9; k++) { const a = PI * 0.15 + k * PI * 0.7 / 8; fl(c, '#fbf6ee'); circ(c, cx + r * 0.8 * Math.cos(a), cy + r * 0.8 * Math.sin(a), r * 0.09); } break;
      case 'star_forehead': fl(c, t.accent); star4(c, cx, cy - r * 0.78, r * 0.15); break;
      case 'star_crown': fl(c, t.accent); for (let k = -2; k <= 2; k++) star4(c, cx + k * r * 0.3, cy - r * (1.05 - Math.abs(k) * 0.08), r * (0.16 - Math.abs(k) * 0.02)); break;
      case 'wizard_hat': fl(c, mix(t.base, t.outC, 0.5)); poly(c, [[cx - r * 0.75, cy - r * 0.75], [cx + r * 0.25, cy - r * 1.75], [cx + r * 0.7, cy - r * 0.75]]); fl(c, t.accent); star4(c, cx - r * 0.05, cy - r * 1.05, r * 0.12); star4(c, cx + r * 0.3, cy - r * 1.35, r * 0.08); break;
      case 'heart_head': fl(c, t.accent); heart(c, cx + r * 0.5, cy - r * 0.9, r * 0.25); break;
      case 'blush_hearts': fl(c, rgba(PINK, 220)); for (const s of S) heart(c, cx + s * r * 0.62, cy - r * 0.12, r * 0.12); break;
      case 'seashell_clip': fl(c, '#ffd1c1'); c.beginPath(); c.ellipse(cx + r * 0.55, cy - r * 0.85, r * 0.3, r * 0.3, 0, PI, 0); c.fill(); st(c, rgba('#d2694c', 180), r * 0.04); for (let k = -2; k <= 2; k++) ln(c, cx + r * 0.55, cy - r * 0.85, cx + r * 0.55 + k * r * 0.12, cy - r * 1.1); break;
      case 'starfish': fl(c, '#ff9a76'); star5(c, cx + r * 0.55, cy - r * 0.85, r * 0.28); break;
      case 'shell_necklace': st(c, rgba(INK, 60), r * 0.03); c.beginPath(); c.ellipse(cx, cy + r * 0.38, r * 0.75, r * 0.58, 0, 0.35, PI - 0.35); c.stroke(); fl(c, '#ffd1c1'); c.beginPath(); c.ellipse(cx, cy + r * 0.92, r * 0.18, r * 0.14, 0, PI, 0); c.fill(); break;
      case 'sun_hat': fl(c, '#e9cf8f'); ovf(c, cx - r * 1.2, cy - r * 0.98, cx + r * 1.2, cy - r * 0.68); c.beginPath(); c.ellipse(cx, cy - r * 0.95, r * 0.55, r * 0.38, 0, PI, 0); c.fill(); fl(c, t.accent); c.fillRect(cx - r * 0.55, cy - r * 1.0, r * 1.1, r * 0.12); break;
      case 'turtle_shell': fl(c, mix(body, t.strong, 0.55)); c.beginPath(); c.ellipse(cx, cy - r * 0.56, r * 1.02, r * 0.52, 0, PI, 0); c.fill(); st(c, rgba(t.base, 170), r * 0.06); hexagon(c, cx, cy - r * 0.78, r * 0.22); hexagon(c, cx - r * 0.5, cy - r * 0.62, r * 0.16); hexagon(c, cx + r * 0.5, cy - r * 0.62, r * 0.16); break;
      case 'conical_hat': fl(c, '#d9c28a'); poly(c, [[cx - r * 1.2, cy - r * 0.72], [cx, cy - r * 1.45], [cx + r * 1.2, cy - r * 0.72]]); break;
      case 'butterfly_clips': fl(c, rgba(t.accent, 230)); for (const s of S) { const x = cx + s * r * 0.55, y = cy - r * 0.85; ovf(c, x - r * 0.22, y - r * 0.16, x, y + r * 0.1); ovf(c, x, y - r * 0.16, x + r * 0.22, y + r * 0.1); } break;
      case 'headband': st(c, t.accent, r * 0.14); c.beginPath(); c.ellipse(cx, cy - r * 0.81, r * 0.98, r * 0.21, 0, PI * 1.05, PI * 1.95); c.stroke(); break;
      case 'headband_tails': fl(c, t.accent); c.fillRect(cx - r * 0.95, cy - r * 0.92, r * 1.9, r * 0.16); st(c, t.accent, r * 0.1); ln(c, cx + r * 0.95, cy - r * 0.84, cx + r * 1.4, cy - r * 0.6); ln(c, cx + r * 0.95, cy - r * 0.84, cx + r * 1.35, cy - r * 1.05); break;
      case 'ninja_mask': fl(c, rgba('#120024', 230)); c.fillRect(cx - r * 0.98, cy - r * 0.66, r * 1.96, r * 0.46); break;
      case 'scarf': fl(c, t.accent); rrect(c, cx - r * 0.85, cy + r * 0.55, r * 1.7, r * 0.3, r * 0.15); c.fill(); c.fillRect(cx + r * 0.3, cy + r * 0.7, r * 0.25, r * 0.45); break;
      case 'tiara': fl(c, GOLD); poly(c, [[cx - r * 0.6, cy - r * 0.78], [cx - r * 0.55, cy - r * 1.15], [cx - r * 0.28, cy - r * 0.95], [cx, cy - r * 1.35], [cx + r * 0.28, cy - r * 0.95], [cx + r * 0.55, cy - r * 1.15], [cx + r * 0.6, cy - r * 0.78]]); fl(c, t.accent); circ(c, cx, cy - r * 0.98, r * 0.1); break;
      case 'crown': fl(c, GOLD); poly(c, [[cx - r * 0.65, cy - r * 0.75], [cx - r * 0.7, cy - r * 1.4], [cx - r * 0.35, cy - r * 1.05], [cx, cy - r * 1.5], [cx + r * 0.35, cy - r * 1.05], [cx + r * 0.7, cy - r * 1.4], [cx + r * 0.65, cy - r * 0.75]]); fl(c, t.accent); circ(c, cx, cy - r * 0.95, r * 0.1); fl(c, '#7fd8ff'); circ(c, cx - r * 0.38, cy - r * 0.9, r * 0.07); circ(c, cx + r * 0.38, cy - r * 0.9, r * 0.07); break;
      case 'ringmaster_hat': fl(c, t.accent); c.fillRect(cx - r * 0.42, cy - r * 1.5, r * 0.84, r * 0.68); fl(c, W_); c.fillRect(cx - r * 0.14, cy - r * 1.5, r * 0.28, r * 0.68); fl(c, '#1d6fb8'); ovf(c, cx - r * 0.72, cy - r * 0.92, cx + r * 0.72, cy - r * 0.72); fl(c, GOLD); c.fillRect(cx - r * 0.42, cy - r * 1.0, r * 0.84, r * 0.1); break;
      case 'party_hat': fl(c, '#1d6fb8'); poly(c, [[cx - r * 0.4, cy - r * 0.85], [cx + r * 0.1, cy - r * 1.7], [cx + r * 0.45, cy - r * 0.8]]); fl(c, '#ffd23f'); circ(c, cx + r * 0.1, cy - r * 1.72, r * 0.12); break;
      case 'clown_nose': fl(c, '#e5394b'); circ(c, cx, cy - r * 0.2, r * 0.15); break;
      case 'sparkle': fl(c, 'rgba(255,255,255,.95)'); star4(c, cx + r * 0.75, cy - r * 0.8, r * 0.2); fl(c, t.inC); star4(c, cx - r * 0.9, cy + r * 0.6, r * 0.12); break;
      case 'sweatband': fl(c, t.accent); c.fillRect(cx - r * 0.95, cy - r * 0.92, r * 1.9, r * 0.2); break;
      case 'visor_cap': fl(c, t.key === 'preppy' ? t.inC : t.accent); rrect(c, cx - r * 0.95, cy - r * 1.0, r * 1.9, r * 0.3, r * 0.12); c.fill(); ovf(c, cx - r * 0.3, cy - r * 0.86, cx + r * 1.25, cy - r * 0.62); break;
      case 'toucan_beak': fl(c, '#ffb020'); c.beginPath(); c.moveTo(cx - r * 0.2, cy - r * 0.3); c.quadraticCurveTo(cx + r * 0.9, cy - r * 0.45, cx + r * 1.15, cy - r * 0.05); c.quadraticCurveTo(cx + r * 0.5, cy + r * 0.05, cx - r * 0.2, cy - r * 0.05); c.fill(); break;
      case 'beak': fl(c, t.dark ? '#d9c7a0' : '#f2b134'); poly(c, [[cx - r * 0.15, cy - r * 0.22], [cx + r * 0.15, cy - r * 0.22], [cx, cy + r * 0.02]]); break;
      case 'hibiscus': flower(c, cx - r * 0.6, cy - r * 0.95, r * 0.17, '#ff5c8a'); break;
      case 'lei': ['#ff5c8a', '#ffd23f', W_].forEach(() => 0); for (let k = 0; k < 7; k++) { const a = PI * 0.15 + k * PI * 0.7 / 6; flower(c, cx + r * 0.85 * Math.cos(a), cy + r * 0.85 * Math.sin(a), r * 0.11, ['#ff5c8a', '#ffd23f', W_][k % 3]); } break;
      case 'daisy_center': fl(c, '#ffc93c'); circ(c, cx + r * 0.55, cy - r * 0.85, r * 0.14); break;
      case 'dog_nose': fl(c, '#2a2230'); ovf(c, cx - r * 0.13, cy - r * 0.28, cx + r * 0.13, cy - r * 0.1); break;
      case 'collar_tag': fl(c, t.outC); rrect(c, cx - r * 0.75, cy + r * 0.6, r * 1.5, r * 0.16, r * 0.08); c.fill(); fl(c, GOLD); circ(c, cx, cy + r * 0.88, r * 0.12); break;
      case 'bandana': fl(c, t.outC); poly(c, [[cx - r * 0.8, cy + r * 0.55], [cx + r * 0.8, cy + r * 0.55], [cx, cy + r * 1.05]]); break;
      case 'lemon': fl(c, '#ffd93b'); ovf(c, cx + r * 0.2, cy - r * 1.15, cx + r * 0.85, cy - r * 0.7); leaf(c, cx + r * 0.85, cy - r * 1.15, r * 0.16, '#5d9b4a'); break;
      case 'raccoon_mask': fl(c, rgba('#2a2230', 220)); for (const s of S) ovf(c, cx + s * r * 0.08, cy - r * 0.62, cx + s * r * 0.72, cy - r * 0.24); break;
      case 'beanie': { const col = t.dark ? t.accent : t.inC; fl(c, col); c.beginPath(); c.ellipse(cx, cy - r * 0.85, r * 0.95, r * 0.5, 0, PI, 0); c.fill(); fl(c, mix(col, '#000000', 0.2)); c.fillRect(cx - r * 0.95, cy - r * 0.9, r * 1.9, r * 0.18); break; }
      case 'bucket_hat': fl(c, t.inC); rrect(c, cx - r * 0.6, cy - r * 1.3, r * 1.2, r * 0.5, r * 0.25); c.fill(); poly(c, [[cx - r * 0.65, cy - r * 0.95], [cx + r * 0.65, cy - r * 0.95], [cx + r, cy - r * 0.65], [cx - r, cy - r * 0.65]]); break;
      case 'scrunchie': fl(c, t.accent); for (let k = 0; k < 6; k++) { const a = k * PI / 3; circ(c, cx + r * 0.6 + r * 0.15 * Math.cos(a), cy - r * 0.95 + r * 0.15 * Math.sin(a), r * 0.12); } break;
      case 'santa_hat': fl(c, t.accent); c.beginPath(); c.moveTo(cx - r * 0.8, cy - r * 0.75); c.quadraticCurveTo(cx, cy - r * 1.9, cx + r, cy - r * 1.1); c.lineTo(cx + r * 0.8, cy - r * 0.75); c.fill(); fl(c, W_); rrect(c, cx - r * 0.9, cy - r * 0.9, r * 1.8, r * 0.25, r * 0.12); c.fill(); circ(c, cx + r, cy - r * 1.1, r * 0.15); break;
      case 'pencil': c.save(); c.translate(cx + r * 0.85, cy - r * 0.8); c.rotate(-0.6); fl(c, '#ffc93c'); c.fillRect(-r * 0.1, -r * 0.45, r * 0.2, r * 0.8); fl(c, '#f2c9a0'); poly(c, [[-r * 0.1, r * 0.35], [r * 0.1, r * 0.35], [0, r * 0.55]]); fl(c, PINK); c.fillRect(-r * 0.1, -r * 0.55, r * 0.2, r * 0.1); c.restore(); break;
      case 'dot': fl(c, t.outC); circ(c, cx + r * 0.62, cy - r * 0.78, r * 0.1); break;
    }
  }

  // ───────── what each app is for ─────────
  const ROLE_RULES = [
    ['contacts', ['contacts']], ['calc', ['calculator']], ['calendar', ['calendar', 'calshow']], ['clock', ['clock', 'alarm']], ['settings', ['settings', 'prefs']],
    ['weather', ['weather']], ['ai', ['claude', 'gemini', 'chatgpt', 'openai', 'copilot']], ['camera', ['camera', 'snapchat']],
    ['photo', ['photos', 'pinterest', 'instagram', 'vsco']], ['mail', ['gmail', 'mail', 'outlook']], ['chat', ['messag', 'whatsapp', 'telegram', 'discord', 'slack', 'sms:']],
    ['phone', ['facetime', 'phone', 'tel:']], ['map', ['maps', 'waze', 'uber', 'lyft']], ['video', ['tiktok', 'youtube', 'netflix', 'hulu', 'disney', 'tv', 'twitch']],
    ['music', ['music', 'spotify', 'podcast', 'shazam']], ['shop', ['shop', 'amazon', 'app store', 'itms', 'etsy', 'target', 'shein']],
    ['money', ['bank', 'pay', 'wallet', 'cash', 'venmo', 'chase', 'irene os', 'robinhood']], ['fit', ['fitness', 'health', 'workout']], ['food', ['food', 'doordash', 'uber eats', 'starbucks']],
    ['game', ['game', 'roblox', 'minecraft']], ['files', ['files', 'drive', 'dropbox']], ['notes', ['notes', 'notion', 'docs', 'reminders']],
    ['social', ['facebook', 'twitter', 'threads', 'reddit']], ['work', ['zoom', 'teams', 'meet', 'ebuilt', 'linkedin']], ['book', ['books', 'kindle', 'duolingo']],
    ['browser', ['safari', 'chrome', 'browser']], ['search', ['google', 'search']], ['shortcuts', ['shortcuts']],
  ];
  function roleOf(name, url) { const k = (name + ' ' + url).toLowerCase(); for (const [r, ws] of ROLE_RULES) if (ws.some(w => k.includes(w))) return r; return 'none'; }

  function drawProp(c, role, x, y, s, bg, ink, accent, charm) {
    const u = s * 0.55; c.save(); c.lineCap = 'round'; c.lineJoin = 'round';
    fl(c, accent); st(c, ink, u * 0.18);
    switch (role) {
      case 'map': c.beginPath(); c.moveTo(x, y + u); c.bezierCurveTo(x - u * 1.2, y - u * 0.1, x - u * 0.6, y - u * 1.1, x, y - u); c.bezierCurveTo(x + u * 0.6, y - u * 1.1, x + u * 1.2, y - u * 0.1, x, y + u); c.fill(); fl(c, bg); circ(c, x, y - u * 0.35, u * 0.3); break;
      case 'camera': fl(c, ink); rrect(c, x - u, y - u * 0.55, u * 2, u * 1.3, u * 0.25); c.fill(); c.fillRect(x - u * 0.35, y - u * 0.8, u * 0.7, u * 0.3); fl(c, accent); circ(c, x, y + u * 0.1, u * 0.42); fl(c, bg); circ(c, x, y + u * 0.1, u * 0.18); break;
      case 'photo': rrect(c, x - u, y - u * 0.8, u * 2, u * 1.6, u * 0.15); c.stroke(); poly(c, [[x - u * 0.85, y + u * 0.65], [x - u * 0.2, y - u * 0.15], [x + u * 0.2, y + u * 0.3], [x + u * 0.45, y + u * 0.05], [x + u * 0.85, y + u * 0.65]]); fl(c, '#ffc93c'); circ(c, x + u * 0.45, y - u * 0.4, u * 0.18); break;
      case 'mail': rrect(c, x - u, y - u * 0.65, u * 2, u * 1.3, u * 0.12); c.fill(); st(c, bg, u * 0.16); c.beginPath(); c.moveTo(x - u, y - u * 0.6); c.lineTo(x, y + u * 0.1); c.lineTo(x + u, y - u * 0.6); c.stroke(); break;
      case 'chat': rrect(c, x - u, y - u * 0.8, u * 2, u * 1.3, u * 0.45); c.fill(); poly(c, [[x - u * 0.5, y + u * 0.4], [x - u * 0.7, y + u * 0.95], [x - u * 0.05, y + u * 0.45]]); fl(c, bg); for (let k = -1; k <= 1; k++) circ(c, x + k * u * 0.45, y - u * 0.15, u * 0.13); break;
      case 'phone': fl(c, ink); rrect(c, x - u * 0.95, y - u * 0.55, u * 1.35, u * 1.1, u * 0.25); c.fill(); fl(c, accent); poly(c, [[x + u * 0.45, y], [x + u * 1.0, y - u * 0.4], [x + u * 1.0, y + u * 0.4]]); break;
      case 'clock': circ(c, x, y, u); st(c, bg, u * 0.18); ln(c, x, y, x, y - u * 0.65); ln(c, x, y, x + u * 0.45, y + u * 0.2); break;
      case 'calendar': fl(c, '#fff'); rrect(c, x - u, y - u * 0.75, u * 2, u * 1.65, u * 0.15); c.fill(); st(c, ink, u * 0.1); c.stroke(); fl(c, accent); c.fillRect(x - u, y - u * 0.75, u * 2, u * 0.5); fl(c, ink); for (let rr = 0; rr < 2; rr++) for (let k = 0; k < 3; k++) circ(c, x - u * 0.55 + k * u * 0.55, y + u * 0.15 + rr * u * 0.4, u * 0.1); break;
      case 'settings': for (let k = 0; k < 8; k++) { c.save(); c.translate(x, y); c.rotate(k * PI / 4); c.fillRect(-u * 0.17, -u, u * 0.34, u * 0.5); c.restore(); } circ(c, x, y, u * 0.68); fl(c, bg); circ(c, x, y, u * 0.28); break;
      case 'search': st(c, accent, u * 0.25); c.beginPath(); c.arc(x - u * 0.2, y - u * 0.2, u * 0.55, 0, PI * 2); c.stroke(); ln(c, x + u * 0.2, y + u * 0.2, x + u * 0.85, y + u * 0.85); break;
      case 'browser': circ(c, x, y, u); st(c, bg, u * 0.1); c.beginPath(); c.ellipse(x, y, u * 0.45, u, 0, 0, PI * 2); c.stroke(); ln(c, x - u, y, x + u, y); ln(c, x, y - u, x, y + u); break;
      case 'music': circ(c, x - u * 0.45, y + u * 0.55, u * 0.3); circ(c, x + u * 0.55, y + u * 0.35, u * 0.3); st(c, accent, u * 0.16); ln(c, x - u * 0.2, y + u * 0.55, x - u * 0.2, y - u * 0.75); ln(c, x + u * 0.8, y + u * 0.35, x + u * 0.8, y - u * 0.95); st(c, accent, u * 0.28); ln(c, x - u * 0.2, y - u * 0.7, x + u * 0.8, y - u * 0.9); break;
      case 'video': rrect(c, x - u, y - u * 0.7, u * 2, u * 1.4, u * 0.3); c.fill(); fl(c, bg); poly(c, [[x - u * 0.3, y - u * 0.38], [x + u * 0.45, y], [x - u * 0.3, y + u * 0.38]]); break;
      case 'shop': poly(c, [[x - u * 0.85, y - u * 0.3], [x + u * 0.85, y - u * 0.3], [x + u * 0.7, y + u], [x - u * 0.7, y + u]]); st(c, ink, u * 0.14); c.beginPath(); c.arc(x, y - u * 0.3, u * 0.4, PI, 0); c.stroke(); break;
      case 'money': fl(c, GOLD); circ(c, x, y, u); st(c, '#b8902e', u * 0.1); c.beginPath(); c.arc(x, y, u * 0.78, 0, PI * 2); c.stroke(); fl(c, '#8a6a1f'); c.font = 'bold ' + (u * 1.3) + 'px -apple-system, sans-serif'; c.textAlign = 'center'; c.textBaseline = 'middle'; c.fillText('$', x, y + u * 0.05); break;
      case 'fit': fl(c, ink); c.fillRect(x - u * 0.6, y - u * 0.1, u * 1.2, u * 0.2); fl(c, accent); for (const sx of S) { rrect(c, x + sx * u * 0.85 - u * 0.2, y - u * 0.6, u * 0.4, u * 1.2, u * 0.1); c.fill(); rrect(c, x + sx * u * 0.55 - u * 0.12, y - u * 0.42, u * 0.24, u * 0.84, u * 0.08); c.fill(); } break;
      case 'food': fl(c, '#f2c9a0'); poly(c, [[x - u * 0.6, y], [x + u * 0.6, y], [x + u * 0.4, y + u], [x - u * 0.4, y + u]]); fl(c, accent); c.beginPath(); c.ellipse(x, y - u * 0.3, u * 0.75, u * 0.55, 0, PI, 0); c.fill(); fl(c, '#e5394b'); circ(c, x, y - u * 0.9, u * 0.17); break;
      case 'game': rrect(c, x - u, y - u * 0.55, u * 2, u * 1.15, u * 0.5); c.fill(); fl(c, bg); c.fillRect(x - u * 0.65, y - u * 0.06, u * 0.4, u * 0.12); c.fillRect(x - u * 0.51, y - u * 0.2, u * 0.12, u * 0.4); circ(c, x + u * 0.4, y - u * 0.12, u * 0.1); circ(c, x + u * 0.6, y + u * 0.1, u * 0.1); break;
      case 'files': poly(c, [[x - u, y - u * 0.6], [x - u * 0.3, y - u * 0.6], [x - u * 0.15, y - u * 0.4], [x + u, y - u * 0.4], [x + u, y + u * 0.7], [x - u, y + u * 0.7]]); break;
      case 'notes': fl(c, '#fff6c9'); rrect(c, x - u * 0.75, y - u, u * 1.5, u * 2, u * 0.12); c.fill(); st(c, accent, u * 0.12); for (let k = 0; k < 3; k++) ln(c, x - u * 0.45, y - u * 0.45 + k * u * 0.42, x + u * 0.45, y - u * 0.45 + k * u * 0.42); break;
      case 'weather': fl(c, '#ffc93c'); circ(c, x - u * 0.3, y - u * 0.3, u * 0.5); fl(c, '#fff'); circ(c, x + u * 0.05, y + u * 0.3, u * 0.4); circ(c, x + u * 0.5, y + u * 0.2, u * 0.45); circ(c, x - u * 0.4, y + u * 0.45, u * 0.3); break;
      case 'social': heart(c, x, y, u * 1.05); break;
      case 'work': rrect(c, x - u, y - u * 0.45, u * 2, u * 1.2, u * 0.15); c.fill(); st(c, ink, u * 0.14); rrect(c, x - u * 0.35, y - u * 0.8, u * 0.7, u * 0.5, u * 0.1); c.stroke(); break;
      case 'contacts': circ(c, x, y - u * 0.4, u * 0.42); c.beginPath(); c.ellipse(x, y + u * 0.75, u * 0.8, u * 0.65, 0, PI, 0); c.fill(); break;
      case 'calc': fl(c, ink); rrect(c, x - u * 0.75, y - u, u * 1.5, u * 2, u * 0.15); c.fill(); fl(c, accent); c.fillRect(x - u * 0.55, y - u * 0.8, u * 1.1, u * 0.45); fl(c, bg); for (let rr = 0; rr < 2; rr++) for (let k = 0; k < 3; k++) circ(c, x - u * 0.4 + k * u * 0.4, y + u * 0.05 + rr * u * 0.45, u * 0.12); break;
      case 'book': for (const sx of S) { c.beginPath(); c.moveTo(x, y - u * 0.5); c.quadraticCurveTo(x + sx * u * 0.5, y - u * 0.85, x + sx * u, y - u * 0.6); c.lineTo(x + sx * u, y + u * 0.7); c.quadraticCurveTo(x + sx * u * 0.5, y + u * 0.45, x, y + u * 0.8); c.fill(); } break;
      case 'ai': st(c, ink, u * 0.2); ln(c, x - u * 0.8, y + u * 0.8, x + u * 0.2, y - u * 0.2); fl(c, accent); sparkle(c, x + u * 0.4, y - u * 0.4, u * 0.6); fl(c, '#ffc93c'); sparkle(c, x - u * 0.5, y - u * 0.6, u * 0.3); break;
      case 'shortcuts': fl(c, accent); rrect(c, x - u * 0.9, y - u * 0.9, u * 1.2, u * 1.2, u * 0.3); c.fill(); fl(c, mix(accent, '#ffffff', 0.4)); rrect(c, x - u * 0.3, y - u * 0.3, u * 1.2, u * 1.2, u * 0.3); c.fill(); break;
      default: c.font = (u * 1.5) + 'px "Apple Color Emoji", "Noto Color Emoji", sans-serif'; c.textAlign = 'center'; c.textBaseline = 'middle'; c.fillText(charm, x, y + u * 0.08);
    }
    c.restore();
  }

  /** Renders an app creature to a new canvas (used for the home screen, drawer and icon packs). */
  function creatureCanvas(t, app, px) {
    const cv = document.createElement('canvas'); cv.width = cv.height = px;
    drawCreature(cv.getContext('2d'), px, t, app); return cv;
  }
  /** Same, on a solid tile (for real iPad home-screen icons, which can't be transparent). */
  function iconTile(t, app, px) {
    const cv = document.createElement('canvas'); cv.width = cv.height = px; const c = cv.getContext('2d');
    const g = c.createLinearGradient(0, 0, 0, px); t.sky.forEach((col, i) => g.addColorStop(t.sky.length > 1 ? i / (t.sky.length - 1) : 0, col)); c.fillStyle = g; c.fillRect(0, 0, px, px);
    c.drawImage(creatureCanvas(t, app, px), 0, -px * 0.03); return cv;
  }
  window.Creatures = { drawCreature, creatureCanvas, iconTile, roleOf, CAST };
})();
