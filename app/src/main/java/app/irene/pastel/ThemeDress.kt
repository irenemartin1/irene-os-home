package app.irene.pastel

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Dresses each app creature as one of its theme's characters (from Irene OS).
 *
 * Every theme has a small CAST of 3 looks (its signature character first), so the apps on one
 * screen are different creatures that still clearly belong to the same world: in Kawaii one app is a
 * bunny with a bow, the next a kitty with a heart, the next a teddy bear; in Bull Market a bull in a
 * bow tie, a bull in a top hat, and a "bear market" bear with gold glasses.
 * Each app always gets the same look (picked from its package name).
 *
 * Layers: back() behind the body (ears, wings, horns) → skin() on the body (spots, tiles, stripes)
 * → the belly with the app's symbol → front() on top (hats, bows, glasses).
 */
class ThemeDress(private val w: World, appColor: Int, seed: Int) {

    val look: List<String> = cast(w.key).let { it[Math.floorMod(seed / 8, it.size)] }
    val body: Int = bodyFor(w, appColor, look)

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val path = Path()
    private val rect = RectF()
    private val gold = Color.parseColor("#E9C25A")
    private val white = Color.WHITE
    private val ink = Color.parseColor("#2A2230")
    private val dark = World.mix(body, Color.BLACK, 0.45f)
    private val pink = Color.parseColor("#FF8FB1")
    private fun a(c: Int, al: Int) = World.withAlpha(c, al)

    companion object {
        /** The cast of every theme: 3 looks each, built from the parts below. */
        fun cast(key: String): List<List<String>> = when (key) {
            "digital" -> listOf(listOf("antenna", "visor"), listOf("antenna2", "headphones"), listOf("visor", "traces"))
            "nature" -> listOf(listOf("elf_ears", "leaf"), listOf("bunny_ears", "flower_crown"), listOf("fox_ears", "leaf_sprout"))
            "gallery" -> listOf(listOf("beret", "paint_splat"), listOf("glasses_gold", "bowtie_gold"), listOf("mustache", "beret_dark"))
            "productivity" -> listOf(listOf("owl_tufts", "glasses"), listOf("owl_tufts", "pencil"), listOf("bear_ears", "glasses"))
            "studio" -> listOf(listOf("headphones"), listOf("paint_splat", "beret"), listOf("star_glasses", "bunny_ears"))
            "kawaii" -> listOf(listOf("bunny_ears", "bow"), listOf("cat_ears", "heart_head", "blush_hearts"), listOf("bear_ears", "bow"))
            "bull" -> listOf(listOf("horns", "bowtie_gold"), listOf("horns", "top_hat"), listOf("bear_ears", "glasses_gold", "bowtie_gold"))
            "mushroom" -> listOf(listOf("stem_feet", "mushroom_cap"), listOf("stem_feet", "mushroom_cap_brown"), listOf("fox_ears", "leaf"))
            "coquette" -> listOf(listOf("bow_big", "lace"), listOf("cat_ears", "bow", "pearls"), listOf("bunny_ears", "bow_big"))
            "celestial" -> listOf(listOf("moon_halo", "star_forehead"), listOf("star_crown"), listOf("wizard_hat"))
            "romantic" -> listOf(listOf("cupid_wings", "heart_head"), listOf("flower_crown", "pearls"), listOf("bunny_ears", "heart_head", "blush_hearts"))
            "luxury" -> listOf(listOf("top_hat", "bowtie_gold"), listOf("monocle", "mustache"), listOf("pearls", "tiara"))
            "coastal" -> listOf(listOf("tail_fin", "seashell_clip"), listOf("starfish", "shell_necklace"), listOf("tail_fin", "sun_hat"))
            "zen" -> listOf(listOf("turtle_legs", "turtle_shell"), listOf("leaf_sprout", "glasses"), listOf("conical_hat"))
            "y2k" -> listOf(listOf("butterfly_wings", "star_forehead"), listOf("heart_glasses"), listOf("butterfly_clips", "star_forehead"))
            "retro" -> listOf(listOf("afro", "retro_sunglasses"), listOf("headband", "retro_sunglasses"), listOf("afro", "mustache"))
            "cyber" -> listOf(listOf("ninja_mask", "headband_tails"), listOf("antenna", "visor"), listOf("cat_ears", "visor"))
            "academia" -> listOf(listOf("cat_ears", "glasses_gold"), listOf("owl_tufts", "glasses_gold"), listOf("scarf", "beret_dark"))
            "princess" -> listOf(listOf("hair_long", "tiara"), listOf("crown"), listOf("bow_big", "pearls"))
            "circuit" -> listOf(listOf("pins", "traces"), listOf("antenna", "traces"), listOf("pins", "visor"))
            "circus" -> listOf(listOf("ringmaster_hat"), listOf("party_hat", "clown_nose"), listOf("bear_ears", "party_hat"))
            "disco" -> listOf(listOf("mirror_tiles", "sparkle"), listOf("afro", "star_glasses"), listOf("headband", "mirror_tiles"))
            "fitness" -> listOf(listOf("sweatband"), listOf("headphones"), listOf("visor_cap"))
            "pride" -> listOf(listOf("unicorn"), listOf("flower_crown", "heart_glasses"), listOf("unicorn", "sparkle"))
            "tropical" -> listOf(listOf("toucan_beak", "hibiscus"), listOf("hibiscus", "retro_sunglasses"), listOf("lei", "sun_hat"))
            "groovy" -> listOf(listOf("daisy_petals", "daisy_center"), listOf("headband", "round_sunglasses"), listOf("flower_crown", "round_sunglasses"))
            "glass" -> listOf(listOf("glass_wings", "stained_lines"), listOf("glass_wings", "tiara"), listOf("stained_lines", "star_forehead"))
            "wild" -> listOf(listOf("lion_mane"), listOf("sun_hat", "stripes", "cat_ears"), listOf("bear_ears", "leaf_sprout"))
            "dogs" -> listOf(listOf("floppy_ears", "dog_nose", "collar_tag"), listOf("fox_ears", "dog_nose", "bandana"), listOf("floppy_ears", "dog_nose", "bow"))
            "mediterranean" -> listOf(listOf("cat_ears", "lemon"), listOf("sun_hat", "dog_nose"), listOf("cat_ears", "flower_crown"))
            "leopard" -> listOf(listOf("cat_ears", "leopard_spots"), listOf("leopard_spots", "round_sunglasses", "pearls"), listOf("cat_ears", "heart_glasses"))
            "tiles" -> listOf(listOf("bird_tuft", "tile_pattern", "beak"), listOf("tile_pattern", "flower_crown"), listOf("cat_ears", "tile_pattern"))
            "preppy" -> listOf(listOf("visor_cap", "pearls"), listOf("bow_big", "pearls"), listOf("headband", "bow"))
            "junk" -> listOf(listOf("bear_ears", "raccoon_mask"), listOf("stickers", "bow"), listOf("stickers", "star_glasses"))
            "grunge" -> listOf(listOf("crow_wings", "beak"), listOf("beanie"), listOf("headphones", "scarf"))
            "indie" -> listOf(listOf("bucket_hat"), listOf("round_sunglasses", "scarf"), listOf("beanie", "flower_crown"))
            "vsco" -> listOf(listOf("turtle_legs", "turtle_shell"), listOf("scrunchie", "shell_necklace"), listOf("starfish", "sun_hat"))
            "minimal" -> listOf(listOf("fold_lines"), listOf("fold_lines", "dot"), listOf("dot"))
            "christmas" -> listOf(listOf("icing"), listOf("santa_hat", "scarf"), listOf("antlers", "clown_nose"))
            else -> listOf(listOf("sparkle"))
        }

        /** Every app keeps its own colors, tinted toward the theme so the whole screen feels like one world. */
        fun bodyFor(w: World, appColor: Int, look: List<String>): Int = when {
            "icing" in look -> World.mix(appColor, Color.parseColor("#b9774a"), 0.75f)
            w.key == "minimal" -> World.mix(appColor, Color.parseColor("#e4e2de"), 0.7f)
            w.key == "productivity" -> World.mix(appColor, Color.parseColor("#d9d9d4"), 0.55f)
            w.key in setOf("luxury", "academia", "gallery", "leopard", "grunge") -> World.mix(appColor, w.accent, 0.45f)
            w.key in setOf("digital", "cyber", "circuit", "disco", "fitness", "bull", "celestial") -> World.mix(appColor, w.accent, 0.3f)
            else -> World.mix(appColor, w.accent, 0.28f)
        } or 0xFF000000.toInt()

        /** Eye style: 0 normal, 1 glowing screen eyes (robots / chips), 2 tiny dot eyes (minimal). */
        fun eyeStyle(w: World) = when (w.key) { "digital", "circuit" -> 1; "minimal" -> 2; else -> 0 }

        private val SKIN = setOf("paint_splat", "traces", "mirror_tiles", "stained_lines", "stripes", "leopard_spots",
            "tile_pattern", "stickers", "fold_lines", "icing")
    }

    fun back(c: Canvas, cx: Float, cy: Float, r: Float) { for (p in look) backPart(c, p, cx, cy, r) }
    fun skin(c: Canvas, cx: Float, cy: Float, r: Float) {
        c.save(); path.reset(); path.addCircle(cx, cy, r, Path.Direction.CW); c.clipPath(path)
        for (p in look) if (p in SKIN) skinPart(c, p, cx, cy, r)
        c.restore()
    }
    fun front(c: Canvas, cx: Float, cy: Float, r: Float) { for (p in look) if (p !in SKIN) frontPart(c, p, cx, cy, r) }

    // ───────── behind the body ─────────

    private fun backPart(c: Canvas, p: String, cx: Float, cy: Float, r: Float) {
        when (p) {
            "antenna", "antenna2" -> {
                val xs = if (p == "antenna") floatArrayOf(0f) else floatArrayOf(-0.45f, 0.45f)
                for (dx in xs) {
                    line.color = a(w.fg, 230); line.strokeWidth = r * 0.09f
                    c.drawLine(cx + dx * r, cy - r * 0.9f, cx + dx * r * 1.25f, cy - r * 1.35f, line)
                    fill.color = a(w.accent, 70); c.drawCircle(cx + dx * r * 1.25f, cy - r * 1.4f, r * 0.26f, fill)
                    fill.color = w.accent; c.drawCircle(cx + dx * r * 1.25f, cy - r * 1.4f, r * 0.13f, fill)
                }
            }
            "elf_ears" -> { fill.color = body; for (sx in S) { path.reset(); path.moveTo(cx + sx * r * 0.75f, cy - r * 0.45f); path.lineTo(cx + sx * r * 1.42f, cy - r * 0.85f); path.lineTo(cx + sx * r * 0.88f, cy - r * 0.05f); path.close(); c.drawPath(path, fill) } }
            "owl_tufts" -> { fill.color = dark; for (sx in S) { path.reset(); path.moveTo(cx + sx * r * 0.25f, cy - r * 0.9f); path.lineTo(cx + sx * r * 0.85f, cy - r * 1.3f); path.lineTo(cx + sx * r * 0.85f, cy - r * 0.55f); path.close(); c.drawPath(path, fill) } }
            "bunny_ears" -> for (sx in S) {
                fill.color = body; rect.set(cx + sx * r * 0.42f - r * 0.24f, cy - r * 1.75f, cx + sx * r * 0.42f + r * 0.24f, cy - r * 0.5f); c.drawOval(rect, fill)
                fill.color = pink; rect.inset(r * 0.09f, r * 0.15f); c.drawOval(rect, fill)
            }
            "cat_ears", "fox_ears" -> for (sx in S) {
                val tall = if (p == "fox_ears") 1.45f else 1.3f
                fill.color = body; path.reset(); path.moveTo(cx + sx * r * 0.15f, cy - r * 0.85f); path.lineTo(cx + sx * r * 0.8f, cy - r * tall); path.lineTo(cx + sx * r * 0.92f, cy - r * 0.35f); path.close(); c.drawPath(path, fill)
                fill.color = if (p == "fox_ears") white else pink
                path.reset(); path.moveTo(cx + sx * r * 0.38f, cy - r * 0.8f); path.lineTo(cx + sx * r * 0.76f, cy - r * (tall - 0.22f)); path.lineTo(cx + sx * r * 0.8f, cy - r * 0.55f); path.close(); c.drawPath(path, fill)
            }
            "bear_ears" -> for (sx in S) { fill.color = body; c.drawCircle(cx + sx * r * 0.68f, cy - r * 0.72f, r * 0.32f, fill); fill.color = World.mix(body, white, 0.45f); c.drawCircle(cx + sx * r * 0.68f, cy - r * 0.72f, r * 0.16f, fill) }
            "floppy_ears" -> for (sx in S) { fill.color = dark; c.save(); c.rotate(sx * 18f, cx + sx * r * 0.85f, cy - r * 0.6f); rect.set(cx + sx * r * 0.85f - r * 0.24f, cy - r * 0.75f, cx + sx * r * 0.85f + r * 0.24f, cy + r * 0.25f); c.drawOval(rect, fill); c.restore() }
            "horns" -> { fill.color = Color.parseColor("#F4EBD0"); for (sx in S) { path.reset(); path.moveTo(cx + sx * r * 0.55f, cy - r * 0.7f); path.quadTo(cx + sx * r * 1.35f, cy - r * 0.8f, cx + sx * r * 1.25f, cy - r * 1.35f); path.quadTo(cx + sx * r * 1.05f, cy - r * 0.95f, cx + sx * r * 0.35f, cy - r * 0.95f); path.close(); c.drawPath(path, fill) } }
            "antlers" -> { line.color = Color.parseColor("#8a5a34"); line.strokeWidth = r * 0.1f
                for (sx in S) { c.drawLine(cx + sx * r * 0.4f, cy - r * 0.85f, cx + sx * r * 0.75f, cy - r * 1.55f, line); c.drawLine(cx + sx * r * 0.6f, cy - r * 1.2f, cx + sx * r * 1.0f, cy - r * 1.3f, line); c.drawLine(cx + sx * r * 0.7f, cy - r * 1.45f, cx + sx * r * 0.5f, cy - r * 1.7f, line) } }
            "moon_halo" -> { fill.color = w.accent; c.drawCircle(cx + r * 0.75f, cy - r * 0.85f, r * 0.55f, fill); fill.color = w.base; c.drawCircle(cx + r * 1.0f, cy - r * 1.0f, r * 0.48f, fill)
                fill.color = a(white, 230); star(c, cx - r * 1.05f, cy - r * 0.9f, r * 0.17f); star(c, cx - r * 1.25f, cy - r * 0.2f, r * 0.1f) }
            "cupid_wings" -> { fill.color = a(white, 235); for (sx in S) for (k in 0 until 3) { val ey = cy - r * 0.35f + k * r * 0.28f; rect.set(cx + sx * r * 0.6f, ey - r * 0.2f, cx + sx * r * (1.45f - k * 0.15f), ey + r * 0.2f); rect.sort(); c.drawOval(rect, fill) } }
            "butterfly_wings" -> for (sx in S) {
                fill.color = a(w.accent, 210); rect.set(cx + sx * r * 0.3f, cy - r * 1.2f, cx + sx * r * 1.5f, cy + r * 0.05f); rect.sort(); c.drawOval(rect, fill)
                fill.color = a(Color.parseColor("#7FD8FF"), 210); rect.set(cx + sx * r * 0.4f, cy - r * 0.05f, cx + sx * r * 1.25f, cy + r * 0.95f); rect.sort(); c.drawOval(rect, fill)
                fill.color = a(white, 220); c.drawCircle(cx + sx * r * 1.0f, cy - r * 0.6f, r * 0.08f, fill); c.drawCircle(cx + sx * r * 0.85f, cy + r * 0.45f, r * 0.06f, fill)
            }
            "glass_wings" -> for (sx in S) { fill.color = a(w.accent, 90); rect.set(cx + sx * r * 0.5f, cy - r * 1.3f, cx + sx * r * 1.45f, cy - r * 0.1f); rect.sort(); c.drawOval(rect, fill); line.color = a(w.fg, 150); line.strokeWidth = r * 0.05f; c.drawOval(rect, line) }
            "crow_wings" -> { fill.color = Color.parseColor("#1c1a22"); for (sx in S) { path.reset(); path.moveTo(cx + sx * r * 0.7f, cy - r * 0.3f); path.lineTo(cx + sx * r * 1.5f, cy + r * 0.1f); path.lineTo(cx + sx * r * 1.25f, cy + r * 0.35f); path.lineTo(cx + sx * r * 1.4f, cy + r * 0.6f); path.lineTo(cx + sx * r * 0.8f, cy + r * 0.6f); path.close(); c.drawPath(path, fill) } }
            "unicorn" -> {
                val bands = intArrayOf(0xFFFF6B6B.toInt(), 0xFFFFA94D.toInt(), 0xFFFFE066.toInt(), 0xFF69DB7C.toInt(), 0xFF4DABF7.toInt(), 0xFFB197FC.toInt())
                for (k in bands.indices) { fill.color = bands[k]; c.drawCircle(cx - r * 0.9f + k * r * 0.05f, cy - r * 0.7f + k * r * 0.3f, r * 0.26f, fill) }
                fill.color = gold; path.reset(); path.moveTo(cx - r * 0.18f, cy - r * 0.85f); path.lineTo(cx + r * 0.1f, cy - r * 1.6f); path.lineTo(cx + r * 0.22f, cy - r * 0.8f); path.close(); c.drawPath(path, fill)
            }
            "lion_mane" -> { fill.color = Color.parseColor("#c8742a"); for (k in 0 until 12) { val an = k * PI / 6; c.drawCircle(cx + (r * 1.0f * cos(an)).toFloat(), cy + (r * 1.0f * sin(an)).toFloat(), r * 0.34f, fill) }
                fill.color = body; for (sx in S) c.drawCircle(cx + sx * r * 0.62f, cy - r * 0.82f, r * 0.2f, fill) }
            "afro" -> { fill.color = Color.parseColor("#4A2C1A"); for (k in 0 until 9) { val an = PI + k * PI / 8; c.drawCircle(cx + (r * 0.95f * cos(an)).toFloat(), cy - r * 0.15f + (r * 0.95f * sin(an)).toFloat(), r * 0.38f, fill) }; c.drawCircle(cx, cy - r * 0.55f, r * 0.75f, fill) }
            "hair_long" -> { fill.color = Color.parseColor("#F6C36B"); rect.set(cx - r * 1.1f, cy - r * 1.05f, cx + r * 1.1f, cy + r * 0.8f); c.drawOval(rect, fill) }
            "daisy_petals" -> { fill.color = white; for (k in 0 until 10) { c.save(); c.rotate(k * 36f, cx, cy); rect.set(cx + r * 0.7f, cy - r * 0.26f, cx + r * 1.45f, cy + r * 0.26f); c.drawOval(rect, fill); c.restore() } }
            "pins" -> { fill.color = gold; for (k in 0 until 12) { c.save(); c.rotate(k * 30f + 15f, cx, cy); c.drawRect(cx + r * 0.9f, cy - r * 0.07f, cx + r * 1.22f, cy + r * 0.07f, fill); c.restore() } }
            "tail_fin" -> { fill.color = World.mix(body, w.accent, 0.4f); path.reset(); path.moveTo(cx + r * 0.5f, cy + r * 0.7f); path.quadTo(cx + r * 1.2f, cy + r * 0.8f, cx + r * 1.45f, cy + r * 0.35f); path.quadTo(cx + r * 1.25f, cy + r * 0.85f, cx + r * 1.55f, cy + r * 1.15f); path.quadTo(cx + r * 1.0f, cy + r * 1.1f, cx + r * 0.3f, cy + r * 0.95f); path.close(); c.drawPath(path, fill) }
            "turtle_legs" -> { fill.color = World.mix(body, w.accent, 0.5f); for ((dx, dy) in listOf(-0.85f to 0.65f, 0.85f to 0.65f, -0.95f to -0.1f, 0.95f to -0.1f)) { rect.set(cx + dx * r - r * 0.22f, cy + dy * r - r * 0.15f, cx + dx * r + r * 0.22f, cy + dy * r + r * 0.15f); c.drawOval(rect, fill) } }
            "stem_feet" -> { fill.color = Color.parseColor("#F7EBD3"); rect.set(cx - r * 0.55f, cy + r * 0.7f, cx - r * 0.15f, cy + r * 1.1f); c.drawOval(rect, fill); rect.set(cx + r * 0.15f, cy + r * 0.7f, cx + r * 0.55f, cy + r * 1.1f); c.drawOval(rect, fill) }
            "bird_tuft" -> { fill.color = w.accent; for (k in -1..1) { c.save(); c.rotate(k * 22f, cx, cy - r * 0.9f); rect.set(cx - r * 0.1f, cy - r * 1.45f, cx + r * 0.1f, cy - r * 0.85f); c.drawOval(rect, fill); c.restore() } }
        }
    }

    // ───────── patterns on the body ─────────

    private fun skinPart(c: Canvas, p: String, cx: Float, cy: Float, r: Float) {
        when (p) {
            "paint_splat" -> { val cs = intArrayOf(w.accent, w.inColor, w.outColor, Color.parseColor("#ffd23f")); for (k in 0 until 4) { fill.color = a(cs[k], 210); val an = k * 1.7 + 0.4; c.drawCircle(cx + (r * 0.7f * cos(an)).toFloat(), cy + (r * 0.7f * sin(an)).toFloat(), r * 0.2f, fill) } }
            "traces" -> { line.color = a(gold, 200); line.strokeWidth = r * 0.05f; fill.color = gold
                for (sx in S) { c.drawLine(cx + sx * r, cy - r * 0.05f, cx + sx * r * 0.62f, cy - r * 0.05f, line); c.drawLine(cx + sx * r * 0.62f, cy - r * 0.05f, cx + sx * r * 0.62f, cy + r * 0.5f, line); c.drawCircle(cx + sx * r * 0.62f, cy - r * 0.05f, r * 0.06f, fill) } }
            "mirror_tiles" -> { line.color = a(white, 70); line.strokeWidth = r * 0.035f; var k = -r; while (k <= r) { c.drawLine(cx - r, cy + k, cx + r, cy + k, line); c.drawLine(cx + k, cy - r, cx + k, cy + r, line); k += r * 0.25f } }
            "stained_lines" -> { line.color = a(w.fg, 120); line.strokeWidth = r * 0.06f; c.drawLine(cx - r, cy - r * 0.2f, cx + r, cy - r * 0.05f, line); c.drawLine(cx - r * 0.3f, cy - r, cx - r * 0.05f, cy - r * 0.15f, line); c.drawLine(cx + r * 0.35f, cy - r, cx + r * 0.1f, cy - r * 0.1f, line) }
            "stripes" -> { fill.color = a(Color.parseColor("#3a2610"), 170); for (k in -2..2) { if (k == 0) continue; path.reset(); val x = cx + k * r * 0.38f; path.moveTo(x - r * 0.08f, cy - r); path.lineTo(x + r * 0.08f, cy - r); path.lineTo(x, cy - r * 0.45f); path.close(); c.drawPath(path, fill) } }
            "leopard_spots" -> { line.color = a(Color.parseColor("#2a1a0c"), 200); line.strokeWidth = r * 0.07f
                for ((dx, dy) in listOf(-0.6f to -0.6f, 0.55f to -0.7f, -0.85f to 0.1f, 0.85f to 0.05f, 0f to -0.85f, -0.5f to 0.75f, 0.55f to 0.75f)) { rect.set(cx + dx * r - r * 0.14f, cy + dy * r - r * 0.11f, cx + dx * r + r * 0.14f, cy + dy * r + r * 0.11f); c.drawArc(rect, 20f, 260f, false, line) } }
            "tile_pattern" -> { line.color = a(w.accent, 160); line.strokeWidth = r * 0.04f; var k = -r; while (k <= r) { c.drawLine(cx + k, cy - r, cx + k, cy + r, line); c.drawLine(cx - r, cy + k, cx + r, cy + k, line); k += r * 0.5f }
                fill.color = a(w.accent, 170); for ((dx, dy) in listOf(-0.5f to -0.5f, 0.5f to -0.5f, -0.75f to 0.25f, 0.75f to 0.25f)) { c.drawCircle(cx + dx * r, cy + dy * r, r * 0.1f, fill) } }
            "stickers" -> { fill.color = Color.parseColor("#ffd23f"); star(c, cx - r * 0.7f, cy - r * 0.55f, r * 0.2f); fill.color = w.accent; heart(c, cx + r * 0.72f, cy - r * 0.5f, r * 0.18f); fill.color = w.inColor; c.drawCircle(cx - r * 0.85f, cy + r * 0.25f, r * 0.12f, fill) }
            "fold_lines" -> { line.color = a(w.fg, 40); line.strokeWidth = r * 0.03f; c.drawLine(cx, cy - r, cx, cy + r, line); c.drawLine(cx - r, cy - r, cx + r, cy + r, line); c.drawLine(cx + r, cy - r, cx - r, cy + r, line) }
            "icing" -> { line.color = white; line.strokeWidth = r * 0.07f; path.reset(); var x = -0.9f; path.moveTo(cx + x * r, cy - r * 0.72f); while (x < 0.9f) { path.quadTo(cx + (x + 0.1f) * r, cy - r * 0.6f, cx + (x + 0.2f) * r, cy - r * 0.72f); x += 0.2f }; c.drawPath(path, line)
                fill.color = Color.parseColor("#E5394B"); c.drawCircle(cx - r * 0.75f, cy + r * 0.15f, r * 0.09f, fill); c.drawCircle(cx + r * 0.75f, cy + r * 0.15f, r * 0.09f, fill) }
        }
    }

    // ───────── on top ─────────

    private fun frontPart(c: Canvas, p: String, cx: Float, cy: Float, r: Float) {
        when (p) {
            "visor" -> { fill.color = a(w.base, 235); rect.set(cx - r * 0.72f, cy - r * 0.62f, cx + r * 0.72f, cy - r * 0.22f); c.drawRoundRect(rect, r * 0.2f, r * 0.2f, fill); line.color = a(w.accent, 200); line.strokeWidth = r * 0.05f; c.drawRoundRect(rect, r * 0.2f, r * 0.2f, line) }
            "leaf" -> leaf(c, cx + r * 0.15f, cy - r * 1.05f, r * 0.42f, Color.parseColor("#6E8F55"))
            "leaf_sprout" -> { line.color = Color.parseColor("#55684a"); line.strokeWidth = r * 0.06f; c.drawLine(cx, cy - r * 0.95f, cx, cy - r * 1.25f, line); leaf(c, cx - r * 0.22f, cy - r * 1.3f, r * 0.22f, Color.parseColor("#7fb069")); leaf(c, cx + r * 0.22f, cy - r * 1.3f, r * 0.22f, Color.parseColor("#7fb069")) }
            "flower_crown" -> { val cs = intArrayOf(pink, Color.parseColor("#ffd23f"), white, Color.parseColor("#b197fc"), pink); for (k in 0 until 5) { val an = PI + (k + 0.5) * PI / 5; val x = cx + (r * 0.92f * cos(an)).toFloat(); val y = cy - r * 0.1f + (r * 0.92f * sin(an)).toFloat(); flower(c, x, y, r * 0.13f, cs[k]) } }
            "beret", "beret_dark" -> { fill.color = if (p == "beret") Color.parseColor("#B5463B") else Color.parseColor("#3b2a20"); c.save(); c.rotate(-12f, cx, cy - r * 0.9f); rect.set(cx - r * 0.85f, cy - r * 1.15f, cx + r * 0.75f, cy - r * 0.62f); c.drawOval(rect, fill); line.color = fill.color; line.strokeWidth = r * 0.09f; c.drawLine(cx, cy - r * 1.13f, cx + r * 0.05f, cy - r * 1.3f, line); c.restore() }
            "mustache" -> { fill.color = Color.parseColor("#3b2a20"); for (sx in S) { rect.set(cx + sx * r * 0.02f, cy - r * 0.2f, cx + sx * r * 0.42f, cy - r * 0.02f); rect.sort(); c.drawOval(rect, fill) } }
            "glasses" -> glasses(c, cx, cy, r, a(ink, 220))
            "glasses_gold" -> glasses(c, cx, cy, r, gold)
            "monocle" -> { line.color = gold; line.strokeWidth = r * 0.06f; c.drawCircle(cx + r * 0.36f, cy - r * 0.42f, r * 0.27f, line); c.drawLine(cx + r * 0.6f, cy - r * 0.3f, cx + r * 0.75f, cy + r * 0.3f, line) }
            "round_sunglasses" -> { fill.color = a(ink, 220); for (sx in S) c.drawCircle(cx + sx * r * 0.36f, cy - r * 0.42f, r * 0.25f, fill); line.color = fill.color; line.strokeWidth = r * 0.06f; c.drawLine(cx - r * 0.12f, cy - r * 0.45f, cx + r * 0.12f, cy - r * 0.45f, line) }
            "retro_sunglasses" -> { fill.color = a(Color.parseColor("#E0702B"), 210); for (sx in S) { rect.set(cx + sx * r * 0.08f, cy - r * 0.62f, cx + sx * r * 0.7f, cy - r * 0.22f); rect.sort(); c.drawRoundRect(rect, r * 0.2f, r * 0.2f, fill) } }
            "star_glasses" -> { fill.color = a(w.accent, 220); for (sx in S) star5(c, cx + sx * r * 0.36f, cy - r * 0.42f, r * 0.3f) }
            "heart_glasses" -> { fill.color = a(w.accent, 220); for (sx in S) heart(c, cx + sx * r * 0.36f, cy - r * 0.45f, r * 0.26f) }
            "headphones" -> { line.color = a(ink, 230); line.strokeWidth = r * 0.12f; rect.set(cx - r * 0.95f, cy - r * 1.2f, cx + r * 0.95f, cy + r * 0.4f); c.drawArc(rect, 200f, 140f, false, line)
                fill.color = w.accent; for (sx in S) { rect.set(cx + sx * r * 0.95f - r * 0.2f, cy - r * 0.45f, cx + sx * r * 0.95f + r * 0.2f, cy + r * 0.05f); c.drawRoundRect(rect, r * 0.15f, r * 0.15f, fill) } }
            "bow" -> { fill.color = w.accent; bow(c, cx + r * 0.55f, cy - r * 0.85f, r * 0.3f) }
            "bow_big" -> { fill.color = w.accent; bow(c, cx, cy - r * 0.98f, r * 0.42f) }
            "bowtie_gold" -> { fill.color = gold; bow(c, cx, cy + r * 0.92f, r * 0.26f) }
            "top_hat" -> { fill.color = Color.parseColor("#111112"); c.drawRect(cx - r * 0.45f, cy - r * 1.55f, cx + r * 0.45f, cy - r * 0.8f, fill); rect.set(cx - r * 0.75f, cy - r * 0.92f, cx + r * 0.75f, cy - r * 0.72f); c.drawOval(rect, fill); fill.color = gold; c.drawRect(cx - r * 0.45f, cy - r * 1.02f, cx + r * 0.45f, cy - r * 0.9f, fill) }
            "mushroom_cap", "mushroom_cap_brown" -> { fill.color = if (p == "mushroom_cap") w.accent else Color.parseColor("#9a6a3c")
                rect.set(cx - r * 1.15f, cy - r * 1.35f, cx + r * 1.15f, cy - r * 0.15f); c.drawArc(rect, 180f, 180f, true, fill)
                fill.color = if (p == "mushroom_cap") white else Color.parseColor("#f3e3c3"); c.drawCircle(cx - r * 0.5f, cy - r * 0.95f, r * 0.13f, fill); c.drawCircle(cx + r * 0.2f, cy - r * 1.1f, r * 0.16f, fill); c.drawCircle(cx + r * 0.7f, cy - r * 0.8f, r * 0.1f, fill) }
            "lace" -> { fill.color = a(white, 230); for (k in 0 until 7) c.drawCircle(cx - r * 0.6f + k * r * 0.2f, cy + r * 0.88f - abs(k - 3) * r * 0.06f, r * 0.11f, fill) }
            "pearls" -> { fill.color = Color.parseColor("#fbf6ee"); line.color = a(ink, 40); line.strokeWidth = r * 0.02f; for (k in 0 until 9) { val an = PI * 0.15 + k * PI * 0.7 / 8; val x = cx + (r * 0.8f * cos(an)).toFloat(); val y = cy + (r * 0.8f * sin(an)).toFloat(); c.drawCircle(x, y, r * 0.09f, fill); c.drawCircle(x, y, r * 0.09f, line) } }
            "star_forehead" -> { fill.color = w.accent; star(c, cx, cy - r * 0.78f, r * 0.15f) }
            "star_crown" -> { fill.color = w.accent; for (k in -2..2) star(c, cx + k * r * 0.3f, cy - r * (1.05f - abs(k) * 0.08f), r * (0.16f - abs(k) * 0.02f)) }
            "wizard_hat" -> { fill.color = World.mix(w.base, w.outColor, 0.5f); path.reset(); path.moveTo(cx - r * 0.75f, cy - r * 0.75f); path.lineTo(cx + r * 0.25f, cy - r * 1.75f); path.lineTo(cx + r * 0.7f, cy - r * 0.75f); path.close(); c.drawPath(path, fill)
                fill.color = w.accent; star(c, cx - r * 0.05f, cy - r * 1.05f, r * 0.12f); star(c, cx + r * 0.3f, cy - r * 1.35f, r * 0.08f) }
            "heart_head" -> { fill.color = w.accent; heart(c, cx + r * 0.5f, cy - r * 0.9f, r * 0.25f) }
            "blush_hearts" -> { fill.color = a(pink, 220); for (sx in S) heart(c, cx + sx * r * 0.62f, cy - r * 0.12f, r * 0.12f) }
            "seashell_clip" -> { fill.color = Color.parseColor("#FFD1C1"); rect.set(cx + r * 0.25f, cy - r * 1.15f, cx + r * 0.85f, cy - r * 0.55f); c.drawArc(rect, 180f, 180f, true, fill); line.color = a(Color.parseColor("#D2694C"), 180); line.strokeWidth = r * 0.04f; for (k in -2..2) c.drawLine(cx + r * 0.55f, cy - r * 0.85f, cx + r * 0.55f + k * r * 0.12f, cy - r * 1.1f, line) }
            "starfish" -> { fill.color = Color.parseColor("#ff9a76"); star5(c, cx + r * 0.55f, cy - r * 0.85f, r * 0.28f) }
            "shell_necklace" -> { line.color = a(ink, 60); line.strokeWidth = r * 0.03f; rect.set(cx - r * 0.75f, cy - r * 0.2f, cx + r * 0.75f, cy + r * 0.95f); c.drawArc(rect, 20f, 140f, false, line)
                fill.color = Color.parseColor("#FFD1C1"); rect.set(cx - r * 0.18f, cy + r * 0.78f, cx + r * 0.18f, cy + r * 1.05f); c.drawArc(rect, 180f, 180f, true, fill) }
            "sun_hat" -> { fill.color = Color.parseColor("#e9cf8f"); rect.set(cx - r * 1.2f, cy - r * 0.98f, cx + r * 1.2f, cy - r * 0.68f); c.drawOval(rect, fill); rect.set(cx - r * 0.55f, cy - r * 1.35f, cx + r * 0.55f, cy - r * 0.6f); c.drawArc(rect, 180f, 180f, true, fill)
                fill.color = w.accent; c.drawRect(cx - r * 0.55f, cy - r * 1.0f, cx + r * 0.55f, cy - r * 0.88f, fill) }
            "turtle_shell" -> { fill.color = World.mix(body, w.strong, 0.55f); rect.set(cx - r * 1.02f, cy - r * 1.08f, cx + r * 1.02f, cy - r * 0.05f); c.drawArc(rect, 180f, 180f, true, fill)
                line.color = a(w.base, 170); line.strokeWidth = r * 0.06f; hexagon(c, cx, cy - r * 0.78f, r * 0.22f); hexagon(c, cx - r * 0.5f, cy - r * 0.5f, r * 0.18f); hexagon(c, cx + r * 0.5f, cy - r * 0.5f, r * 0.18f) }
            "conical_hat" -> { fill.color = Color.parseColor("#d9c28a"); path.reset(); path.moveTo(cx - r * 1.2f, cy - r * 0.72f); path.lineTo(cx, cy - r * 1.45f); path.lineTo(cx + r * 1.2f, cy - r * 0.72f); path.close(); c.drawPath(path, fill)
                line.color = a(Color.parseColor("#8a6a3a"), 140); line.strokeWidth = r * 0.03f; for (k in -2..2) c.drawLine(cx, cy - r * 1.45f, cx + k * r * 0.45f, cy - r * 0.75f, line) }
            "butterfly_clips" -> for (sx in S) { val x = cx + sx * r * 0.55f; val y = cy - r * 0.85f; fill.color = a(w.accent, 230); rect.set(x - r * 0.22f, y - r * 0.16f, x, y + r * 0.1f); c.drawOval(rect, fill); rect.set(x, y - r * 0.16f, x + r * 0.22f, y + r * 0.1f); c.drawOval(rect, fill) }
            "headband" -> { fill.color = w.accent; rect.set(cx - r * 0.98f, cy - r * 1.02f, cx + r * 0.98f, cy - r * 0.6f); c.drawArc(rect, 190f, 160f, false, fill.apply { style = Paint.Style.STROKE; strokeWidth = r * 0.14f }); fill.style = Paint.Style.FILL }
            "headband_tails" -> { fill.color = w.accent; c.drawRect(cx - r * 0.95f, cy - r * 0.92f, cx + r * 0.95f, cy - r * 0.76f, fill); line.color = w.accent; line.strokeWidth = r * 0.1f; c.drawLine(cx + r * 0.95f, cy - r * 0.84f, cx + r * 1.4f, cy - r * 0.6f, line); c.drawLine(cx + r * 0.95f, cy - r * 0.84f, cx + r * 1.35f, cy - r * 1.05f, line) }
            "ninja_mask" -> { fill.color = a(Color.parseColor("#120024"), 230); c.drawRect(cx - r * 0.98f, cy - r * 0.66f, cx + r * 0.98f, cy - r * 0.2f, fill) }
            "scarf" -> { fill.color = w.accent; rect.set(cx - r * 0.85f, cy + r * 0.55f, cx + r * 0.85f, cy + r * 0.85f); c.drawRoundRect(rect, r * 0.15f, r * 0.15f, fill); c.drawRect(cx + r * 0.3f, cy + r * 0.7f, cx + r * 0.55f, cy + r * 1.15f, fill)
                line.color = a(white, 120); line.strokeWidth = r * 0.04f; c.drawLine(cx - r * 0.5f, cy + r * 0.55f, cx - r * 0.5f, cy + r * 0.85f, line); c.drawLine(cx, cy + r * 0.55f, cx, cy + r * 0.85f, line) }
            "tiara" -> { fill.color = gold; path.reset(); path.moveTo(cx - r * 0.6f, cy - r * 0.78f); path.lineTo(cx - r * 0.55f, cy - r * 1.15f); path.lineTo(cx - r * 0.28f, cy - r * 0.95f); path.lineTo(cx, cy - r * 1.35f); path.lineTo(cx + r * 0.28f, cy - r * 0.95f); path.lineTo(cx + r * 0.55f, cy - r * 1.15f); path.lineTo(cx + r * 0.6f, cy - r * 0.78f); path.close(); c.drawPath(path, fill); fill.color = w.accent; c.drawCircle(cx, cy - r * 0.98f, r * 0.1f, fill) }
            "crown" -> { fill.color = gold; path.reset(); path.moveTo(cx - r * 0.65f, cy - r * 0.75f); path.lineTo(cx - r * 0.7f, cy - r * 1.4f); path.lineTo(cx - r * 0.35f, cy - r * 1.05f); path.lineTo(cx, cy - r * 1.5f); path.lineTo(cx + r * 0.35f, cy - r * 1.05f); path.lineTo(cx + r * 0.7f, cy - r * 1.4f); path.lineTo(cx + r * 0.65f, cy - r * 0.75f); path.close(); c.drawPath(path, fill)
                fill.color = w.accent; c.drawCircle(cx, cy - r * 0.95f, r * 0.1f, fill); fill.color = Color.parseColor("#7fd8ff"); c.drawCircle(cx - r * 0.38f, cy - r * 0.9f, r * 0.07f, fill); c.drawCircle(cx + r * 0.38f, cy - r * 0.9f, r * 0.07f, fill) }
            "ringmaster_hat" -> { fill.color = w.accent; c.drawRect(cx - r * 0.42f, cy - r * 1.5f, cx + r * 0.42f, cy - r * 0.82f, fill); fill.color = white; c.drawRect(cx - r * 0.14f, cy - r * 1.5f, cx + r * 0.14f, cy - r * 0.82f, fill)
                fill.color = Color.parseColor("#1D6FB8"); rect.set(cx - r * 0.72f, cy - r * 0.92f, cx + r * 0.72f, cy - r * 0.72f); c.drawOval(rect, fill); fill.color = gold; c.drawRect(cx - r * 0.42f, cy - r * 1.0f, cx + r * 0.42f, cy - r * 0.9f, fill) }
            "party_hat" -> { fill.color = Color.parseColor("#1D6FB8"); path.reset(); path.moveTo(cx - r * 0.4f, cy - r * 0.85f); path.lineTo(cx + r * 0.1f, cy - r * 1.7f); path.lineTo(cx + r * 0.45f, cy - r * 0.8f); path.close(); c.drawPath(path, fill)
                fill.color = Color.parseColor("#ffd23f"); c.drawCircle(cx + r * 0.1f, cy - r * 1.72f, r * 0.12f, fill); fill.color = white; c.drawCircle(cx - r * 0.05f, cy - r * 1.1f, r * 0.07f, fill); c.drawCircle(cx + r * 0.2f, cy - r * 1.3f, r * 0.06f, fill) }
            "clown_nose" -> { fill.color = Color.parseColor("#E5394B"); c.drawCircle(cx, cy - r * 0.2f, r * 0.15f, fill); fill.color = a(white, 180); c.drawCircle(cx - r * 0.05f, cy - r * 0.25f, r * 0.04f, fill) }
            "sparkle" -> { fill.color = a(white, 240); star(c, cx + r * 0.75f, cy - r * 0.8f, r * 0.2f); fill.color = a(w.inColor, 240); star(c, cx - r * 0.9f, cy + r * 0.6f, r * 0.12f) }
            "sweatband" -> { fill.color = w.accent; c.drawRect(cx - r * 0.95f, cy - r * 0.92f, cx + r * 0.95f, cy - r * 0.72f, fill); fill.color = a(ink, 230)
                path.reset(); path.moveTo(cx + r * 0.05f, cy - r * 0.92f); path.lineTo(cx - r * 0.1f, cy - r * 0.8f); path.lineTo(cx + r * 0.02f, cy - r * 0.8f); path.lineTo(cx - r * 0.06f, cy - r * 0.72f); path.lineTo(cx + r * 0.14f, cy - r * 0.84f); path.lineTo(cx + r * 0.02f, cy - r * 0.84f); path.close(); c.drawPath(path, fill) }
            "visor_cap" -> { fill.color = if (w.key == "preppy") w.inColor else w.accent; rect.set(cx - r * 0.95f, cy - r * 1.0f, cx + r * 0.95f, cy - r * 0.7f); c.drawRoundRect(rect, r * 0.12f, r * 0.12f, fill)
                rect.set(cx - r * 0.3f, cy - r * 0.86f, cx + r * 1.25f, cy - r * 0.62f); c.drawOval(rect, fill) }
            "toucan_beak" -> { fill.color = Color.parseColor("#FFB020"); path.reset(); path.moveTo(cx - r * 0.2f, cy - r * 0.3f); path.quadTo(cx + r * 0.9f, cy - r * 0.45f, cx + r * 1.15f, cy - r * 0.05f); path.quadTo(cx + r * 0.5f, cy + r * 0.05f, cx - r * 0.2f, cy - r * 0.05f); path.close(); c.drawPath(path, fill)
                fill.color = Color.parseColor("#E8452E"); c.drawCircle(cx + r * 1.08f, cy - r * 0.08f, r * 0.1f, fill) }
            "beak" -> { fill.color = if (w.dark) Color.parseColor("#d9c7a0") else Color.parseColor("#f2b134"); path.reset(); path.moveTo(cx - r * 0.15f, cy - r * 0.22f); path.lineTo(cx + r * 0.15f, cy - r * 0.22f); path.lineTo(cx, cy + r * 0.02f); path.close(); c.drawPath(path, fill) }
            "hibiscus" -> { flower(c, cx - r * 0.6f, cy - r * 0.95f, r * 0.17f, Color.parseColor("#FF5C8A")) }
            "lei" -> { val cs = intArrayOf(Color.parseColor("#FF5C8A"), Color.parseColor("#ffd23f"), white); for (k in 0 until 7) { val an = PI * 0.15 + k * PI * 0.7 / 6; flower(c, cx + (r * 0.85f * cos(an)).toFloat(), cy + (r * 0.85f * sin(an)).toFloat(), r * 0.11f, cs[k % 3]) } }
            "daisy_center" -> { fill.color = Color.parseColor("#FFC93C"); c.drawCircle(cx + r * 0.55f, cy - r * 0.85f, r * 0.14f, fill) }
            "dog_nose" -> { fill.color = Color.parseColor("#2a2230"); rect.set(cx - r * 0.13f, cy - r * 0.28f, cx + r * 0.13f, cy - r * 0.1f); c.drawOval(rect, fill) }
            "collar_tag" -> { fill.color = w.outColor; rect.set(cx - r * 0.75f, cy + r * 0.6f, cx + r * 0.75f, cy + r * 0.76f); c.drawRoundRect(rect, r * 0.08f, r * 0.08f, fill); fill.color = gold; c.drawCircle(cx, cy + r * 0.88f, r * 0.12f, fill) }
            "bandana" -> { fill.color = w.outColor; path.reset(); path.moveTo(cx - r * 0.8f, cy + r * 0.55f); path.lineTo(cx + r * 0.8f, cy + r * 0.55f); path.lineTo(cx, cy + r * 1.05f); path.close(); c.drawPath(path, fill); fill.color = a(white, 200); c.drawCircle(cx - r * 0.25f, cy + r * 0.66f, r * 0.04f, fill); c.drawCircle(cx + r * 0.2f, cy + r * 0.72f, r * 0.04f, fill) }
            "lemon" -> { fill.color = Color.parseColor("#ffd93b"); rect.set(cx + r * 0.2f, cy - r * 1.15f, cx + r * 0.85f, cy - r * 0.7f); c.drawOval(rect, fill); leaf(c, cx + r * 0.85f, cy - r * 1.15f, r * 0.16f, Color.parseColor("#5d9b4a")) }
            "raccoon_mask" -> { fill.color = a(Color.parseColor("#2a2230"), 220); for (sx in S) { rect.set(cx + sx * r * 0.08f, cy - r * 0.62f, cx + sx * r * 0.72f, cy - r * 0.24f); rect.sort(); c.drawOval(rect, fill) } }
            "beanie" -> { fill.color = if (w.dark) w.accent else w.inColor; rect.set(cx - r * 0.95f, cy - r * 1.35f, cx + r * 0.95f, cy - r * 0.35f); c.drawArc(rect, 180f, 180f, true, fill)
                fill.color = World.mix(fill.color, Color.BLACK, 0.2f); c.drawRect(cx - r * 0.95f, cy - r * 0.9f, cx + r * 0.95f, cy - r * 0.72f, fill) }
            "bucket_hat" -> { fill.color = w.inColor; rect.set(cx - r * 0.6f, cy - r * 1.3f, cx + r * 0.6f, cy - r * 0.8f); c.drawRoundRect(rect, r * 0.25f, r * 0.25f, fill)
                path.reset(); path.moveTo(cx - r * 0.65f, cy - r * 0.95f); path.lineTo(cx + r * 0.65f, cy - r * 0.95f); path.lineTo(cx + r * 1.0f, cy - r * 0.65f); path.lineTo(cx - r * 1.0f, cy - r * 0.65f); path.close(); c.drawPath(path, fill)
                fill.color = a(white, 160); c.drawRect(cx - r * 0.6f, cy - r * 1.0f, cx + r * 0.6f, cy - r * 0.92f, fill) }
            "scrunchie" -> { fill.color = w.accent; for (k in 0 until 6) { val an = k * PI / 3; c.drawCircle(cx + r * 0.6f + (r * 0.15f * cos(an)).toFloat(), cy - r * 0.95f + (r * 0.15f * sin(an)).toFloat(), r * 0.12f, fill) } }
            "santa_hat" -> { fill.color = w.accent; path.reset(); path.moveTo(cx - r * 0.8f, cy - r * 0.75f); path.quadTo(cx, cy - r * 1.9f, cx + r * 1.0f, cy - r * 1.1f); path.lineTo(cx + r * 0.8f, cy - r * 0.75f); path.close(); c.drawPath(path, fill)
                fill.color = white; rect.set(cx - r * 0.9f, cy - r * 0.9f, cx + r * 0.9f, cy - r * 0.65f); c.drawRoundRect(rect, r * 0.12f, r * 0.12f, fill); c.drawCircle(cx + r * 1.0f, cy - r * 1.1f, r * 0.15f, fill) }
            "pencil" -> { c.save(); c.rotate(-35f, cx + r * 0.85f, cy - r * 0.8f); fill.color = Color.parseColor("#ffc93c"); c.drawRect(cx + r * 0.75f, cy - r * 1.25f, cx + r * 0.95f, cy - r * 0.45f, fill)
                fill.color = Color.parseColor("#f2c9a0"); path.reset(); path.moveTo(cx + r * 0.75f, cy - r * 0.45f); path.lineTo(cx + r * 0.95f, cy - r * 0.45f); path.lineTo(cx + r * 0.85f, cy - r * 0.25f); path.close(); c.drawPath(path, fill)
                fill.color = pink; c.drawRect(cx + r * 0.75f, cy - r * 1.35f, cx + r * 0.95f, cy - r * 1.25f, fill); c.restore() }
            "dot" -> { fill.color = w.outColor; c.drawCircle(cx + r * 0.62f, cy - r * 0.78f, r * 0.1f, fill) }
        }
    }

    // ───────── little shapes ─────────

    private val S = floatArrayOf(-1f, 1f)

    private fun glasses(c: Canvas, cx: Float, cy: Float, r: Float, col: Int) {
        line.color = col; line.strokeWidth = r * 0.07f
        c.drawCircle(cx - r * 0.36f, cy - r * 0.42f, r * 0.27f, line); c.drawCircle(cx + r * 0.36f, cy - r * 0.42f, r * 0.27f, line)
        c.drawLine(cx - r * 0.1f, cy - r * 0.45f, cx + r * 0.1f, cy - r * 0.45f, line)
    }

    private fun bow(c: Canvas, x: Float, y: Float, s: Float) {
        path.reset(); path.moveTo(x, y); path.lineTo(x - s * 1.3f, y - s * 0.75f); path.lineTo(x - s * 1.3f, y + s * 0.75f); path.close()
        path.moveTo(x, y); path.lineTo(x + s * 1.3f, y - s * 0.75f); path.lineTo(x + s * 1.3f, y + s * 0.75f); path.close()
        c.drawPath(path, fill)
        val keep = fill.color; fill.color = World.mix(keep, Color.BLACK, 0.2f); c.drawCircle(x, y, s * 0.35f, fill); fill.color = keep
    }

    private fun star(c: Canvas, x: Float, y: Float, s: Float) {
        path.reset(); path.moveTo(x, y - s); path.quadTo(x, y, x + s, y); path.quadTo(x, y, x, y + s); path.quadTo(x, y, x - s, y); path.quadTo(x, y, x, y - s); path.close()
        c.drawPath(path, fill)
    }

    private fun star5(c: Canvas, x: Float, y: Float, s: Float) {
        path.reset()
        for (k in 0 until 10) { val rr = if (k % 2 == 0) s else s * 0.45f; val an = -PI / 2 + k * PI / 5; val px = x + (rr * cos(an)).toFloat(); val py = y + (rr * sin(an)).toFloat(); if (k == 0) path.moveTo(px, py) else path.lineTo(px, py) }
        path.close(); c.drawPath(path, fill)
    }

    private fun heart(c: Canvas, x: Float, y: Float, s: Float) {
        path.reset(); path.moveTo(x, y + s * 0.8f)
        path.cubicTo(x - s * 1.3f, y - s * 0.1f, x - s * 0.6f, y - s * 1.1f, x, y - s * 0.35f)
        path.cubicTo(x + s * 0.6f, y - s * 1.1f, x + s * 1.3f, y - s * 0.1f, x, y + s * 0.8f); path.close()
        c.drawPath(path, fill)
    }

    private fun flower(c: Canvas, x: Float, y: Float, s: Float, col: Int) {
        fill.color = col; for (k in 0 until 5) { val an = k * 2 * PI / 5; c.drawCircle(x + (s * cos(an)).toFloat(), y + (s * sin(an)).toFloat(), s * 0.85f, fill) }
        fill.color = Color.parseColor("#FFE066"); c.drawCircle(x, y, s * 0.5f, fill)
    }

    private fun leaf(c: Canvas, x: Float, y: Float, s: Float, col: Int) {
        fill.color = col
        path.reset(); path.moveTo(x - s, y + s * 0.3f); path.quadTo(x - s * 0.2f, y - s * 0.9f, x + s, y - s * 0.3f); path.quadTo(x, y + s * 0.6f, x - s, y + s * 0.3f); path.close()
        c.drawPath(path, fill)
    }

    private fun hexagon(c: Canvas, x: Float, y: Float, s: Float) {
        path.reset()
        for (k in 0 until 6) { val an = k * PI / 3; val px = x + (s * cos(an)).toFloat(); val py = y + (s * sin(an)).toFloat(); if (k == 0) path.moveTo(px, py) else path.lineTo(px, py) }
        path.close(); c.drawPath(path, line)
    }
}
