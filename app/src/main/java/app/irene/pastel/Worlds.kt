package app.irene.pastel

import android.graphics.Color

/**
 * Every Irene OS theme, ready for the phone. Colors come straight from Irene OS.
 *
 * "forest" (Fairy / Enchanted Forest) and "underwater" (Enchanted Underwater) use their own
 * hand-made animated scenes. Every other theme uses ThemeSceneView with its own sky colors,
 * floating particles and ground, and CreatureIcon dresses each app as that theme's creature.
 */
data class World(
    val key: String,
    val name: String,
    val emoji: String,
    val character: String,
    val desc: String,
    val dark: Boolean,
    val base: Int,
    val fg: Int,
    val accent: Int,
    val strong: Int,
    val inColor: Int,
    val outColor: Int,
    val card: Int,
    val sky: IntArray,     // background gradient, top -> bottom
    val particle: Int,     // what floats on screen (see Particles)
    val ground: Int        // what sits at the bottom (see Grounds)
) {
    /** Turns this theme into the colors the launcher UI uses. */
    fun palette(): Palette {
        val softA = if (dark) 0xB0 else 0xA0
        return Palette(
            name, sky.first(), sky.last(),
            intArrayOf(accent, inColor, outColor),
            accent,
            if (dark) mix(base, accent, 0.18f) else mix(base, strong, 0.22f),
            fg, withAlpha(fg, softA),
            if (dark) withAlpha(mix(base, accent, 0.12f), 0xE6) else withAlpha(card, 0xE6),
            withAlpha(accent, 0x99),
            if (dark) withAlpha(mix(base, accent, 0.08f), 0x99) else withAlpha(card, 0xB3),
            emoji
        )
    }

    companion object {
        fun withAlpha(c: Int, a: Int) = (c and 0x00FFFFFF) or (a shl 24)
        fun mix(a: Int, b: Int, t: Float): Int = Color.rgb(
            (Color.red(a) + (Color.red(b) - Color.red(a)) * t).toInt(),
            (Color.green(a) + (Color.green(b) - Color.green(a)) * t).toInt(),
            (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t).toInt()
        )
    }
}

object Particles {
    const val NONE = 0; const val STARS = 1; const val HEARTS = 2; const val PETALS = 3; const val CONFETTI = 4
    const val BUBBLES = 5; const val SPARKLES = 6; const val LEAVES = 7; const val TICKERS = 8; const val EMBERS = 9
    const val DAISIES = 10; const val BUTTERFLIES = 11; const val NOTES = 12; const val DOTS = 13
}

object Grounds {
    const val NONE = 0; const val HILLS = 1; const val SKYLINE = 2; const val WAVES = 3; const val MUSHROOMS = 4
    const val DANCE_FLOOR = 5; const val BIG_TOP = 6; const val ZEN = 7; const val PALMS = 8; const val TRACES = 9
    const val BOOKS = 10; const val FRAMES = 11; const val CLOUDS = 12; const val CASTLE = 13; const val GRID = 14
    const val RAINBOW = 15; const val STAINED = 16; const val CHART = 17; const val SUNSET = 18
}

object Worlds {
    private fun c(h: String) = Color.parseColor(h)

    /** The two hand-made worlds come first, then every Irene OS theme. */
    data class Entry(val key: String, val label: String, val desc: String)

    val all: List<World> = listOf(
        World("digital", "Digital World", "🤖", "A little robot", "Futuristic, immersive and electric.", true,
            c("#05060f"), c("#e6fdff"), c("#00f0ff"), c("#00f0ff"), c("#00f0ff"), c("#ff4fd8"), c("#0b1030"),
            intArrayOf(c("#05060f"), c("#0b0a24"), c("#2a0b5e")), Particles.DOTS, Grounds.GRID),
        World("nature", "Nature", "🧝‍♀️", "A forest elf", "Botanical, earthy and calm.", false,
            c("#eef2e4"), c("#2f3d27"), c("#6e8f55"), c("#4f7a3a"), c("#4f7a3a"), c("#b5583f"), c("#ffffff"),
            intArrayOf(c("#eef2e4"), c("#dce5cc")), Particles.LEAVES, Grounds.HILLS),
        World("gallery", "Art Gallery", "🧑‍🎨", "An artist", "Paintings, editorial and elegant.", true,
            c("#17140f"), c("#f5ebd5"), c("#e7c574"), c("#e7c574"), c("#e7c574"), c("#e0907a"), c("#2a2620"),
            intArrayOf(c("#2a2620"), c("#17140f")), Particles.EMBERS, Grounds.FRAMES),
        World("productivity", "Productivity", "🦉", "A calm owl", "Structured, minimal and functional.", false,
            c("#f6f6f3"), c("#151515"), c("#111111"), c("#111111"), c("#1f7a4d"), c("#c2410c"), c("#ffffff"),
            intArrayOf(c("#f6f6f3"), c("#ececE7")), Particles.NONE, Grounds.NONE),
        World("studio", "Creative Studio", "🧑‍🎤", "A creative", "Bright, colorful and full of ideas.", false,
            c("#fff4ec"), c("#2a2320"), c("#e4572e"), c("#e4572e"), c("#2a9d6f"), c("#e4572e"), c("#ffffff"),
            intArrayOf(c("#ffe9f3"), c("#fff4d6"), c("#e3f6ec"), c("#e2eeff")), Particles.CONFETTI, Grounds.NONE),
        World("kawaii", "Kawaii", "🐰", "A pastel bunny", "Pastel, sweet and extra cute.", false,
            c("#ffe4ee"), c("#6a3350"), c("#ff6b8b"), c("#c2437a"), c("#d9477a"), c("#8a6ad1"), c("#ffffff"),
            intArrayOf(c("#ffe4ee"), c("#ffd6e6"), c("#fbc4da")), Particles.HEARTS, Grounds.CLOUDS),
        World("bull", "Bull Market", "🐂", "A bull in a suit", "Net worth, tickers and wins, front and center.", true,
            c("#07140f"), c("#f4ebd0"), c("#34d399"), c("#34d399"), c("#34d399"), c("#f87171"), c("#0b1f17"),
            intArrayOf(c("#0b1f17"), c("#07140f")), Particles.TICKERS, Grounds.CHART),
        World("mushroom", "Mushroom Cottage", "🍄", "A mushroom sprite", "Cozy, storybook and woodland.", false,
            c("#f4ead8"), c("#3b2a20"), c("#b8432f"), c("#b8432f"), c("#4f6b2f"), c("#b8432f"), c("#fffaf0"),
            intArrayOf(c("#fbf3e4"), c("#efe0c4"), c("#e3cfa8")), Particles.LEAVES, Grounds.MUSHROOMS),
        World("coquette", "Coquette", "🎀", "A ribbon swan", "Bows, lace and soft pink.", false,
            c("#fdf2f4"), c("#5a2a3a"), c("#e8a0b4"), c("#b84a6c"), c("#b84a6c"), c("#8e5bb5"), c("#ffffff"),
            intArrayOf(c("#fff8f9"), c("#fbe5ec"), c("#f6d3df")), Particles.HEARTS, Grounds.CLOUDS),
        World("celestial", "Celestial", "🌙", "A moon spirit", "Stars, moons and cosmic calm.", true,
            c("#0b0d24"), c("#eef0ff"), c("#f2d58a"), c("#f2d58a"), c("#f2d58a"), c("#a5b4fc"), c("#13163a"),
            intArrayOf(c("#2a2f6e"), c("#13163a"), c("#0b0d24")), Particles.STARS, Grounds.HILLS),
        World("romantic", "Romantic", "💘", "A little cupid", "Soft florals, love notes and candlelight.", false,
            c("#fbeeee"), c("#4a2230"), c("#e07a8f"), c("#b8475f"), c("#8a6a1f"), c("#b8475f"), c("#ffffff"),
            intArrayOf(c("#fff6f4"), c("#f8e0de"), c("#f3d1d1")), Particles.PETALS, Grounds.CLOUDS),
        World("luxury", "Luxury", "🤵", "A butler", "Black, gold and quietly expensive.", true,
            c("#0c0c0d"), c("#f3efe6"), c("#d4af37"), c("#d4af37"), c("#d4af37"), c("#e5a3a3"), c("#1a1a1c"),
            intArrayOf(c("#1a1a1c"), c("#0c0c0d")), Particles.SPARKLES, Grounds.NONE),
        World("coastal", "Coastal", "🧜‍♀️", "A mermaid", "Sea breeze, sand and ocean blues.", false,
            c("#f2f8fa"), c("#16384a"), c("#2a8fb8"), c("#2a8fb8"), c("#1f8a7d"), c("#d2694c"), c("#ffffff"),
            intArrayOf(c("#e3f3f8"), c("#f2f8fa"), c("#f7efe3")), Particles.BUBBLES, Grounds.WAVES),
        World("zen", "Zen Garden", "🐢", "A wise turtle", "Stones, raked sand and quiet focus.", false,
            c("#f3f1ea"), c("#2d332b"), c("#7a8f6a"), c("#55684a"), c("#55684a"), c("#a86b4c"), c("#fbfaf6"),
            intArrayOf(c("#f6f4ee"), c("#e7e4d8")), Particles.LEAVES, Grounds.ZEN),
        World("y2k", "Y2K", "🦋", "A glitter butterfly", "Chrome, glitter and 2000s pop.", false,
            c("#f3eaff"), c("#2a1a4a"), c("#ff2fb3"), c("#c4128a"), c("#0072c6"), c("#c4128a"), c("#ffffff"),
            intArrayOf(c("#c9f3ff"), c("#e9dcff"), c("#ffe0f5")), Particles.BUTTERFLIES, Grounds.NONE),
        World("retro", "Retro", "🕺", "A disco dancer", "Warm 70s colors and vintage vibes.", false,
            c("#f6ead2"), c("#3a2414"), c("#e0702b"), c("#b8521a"), c("#2f7d63"), c("#b8521a"), c("#fff8ea"),
            intArrayOf(c("#f9efd9"), c("#f1d9b0")), Particles.DOTS, Grounds.SUNSET),
        World("cyber", "Cyber City", "🥷", "A cyber ninja", "Neon nights and city lights.", true,
            c("#090014"), c("#f5e9ff"), c("#ff2a6d"), c("#ff2a6d"), c("#05d9e8"), c("#ff2a6d"), c("#120024"),
            intArrayOf(c("#090014"), c("#120024"), c("#3a0060")), Particles.STARS, Grounds.SKYLINE),
        World("academia", "Dark Academia", "🐈‍⬛", "A library cat", "Old books, candlelight and tweed.", true,
            c("#15110d"), c("#ece2d0"), c("#b08d57"), c("#b08d57"), c("#a3b18a"), c("#c2785c"), c("#2b2117"),
            intArrayOf(c("#2b2117"), c("#15110d")), Particles.EMBERS, Grounds.BOOKS),
        World("princess", "Princess Diaries", "👸", "A princess", "Tiaras, pink and a royal glow-up.", false,
            c("#fff0f6"), c("#4a1d3a"), c("#e75a9b"), c("#c13584"), c("#9a7410"), c("#c13584"), c("#ffffff"),
            intArrayOf(c("#fff7fb"), c("#ffe0ef"), c("#fbcfe3")), Particles.SPARKLES, Grounds.CASTLE),
        World("circuit", "Circuit", "🔌", "A chip critter", "Circuit boards, gold traces and a green glow.", true,
            c("#04140c"), c("#dcffe9"), c("#39e58c"), c("#39e58c"), c("#39e58c"), c("#ff8a65"), c("#061d12"),
            intArrayOf(c("#0b3a22"), c("#061d12"), c("#04140c")), Particles.DOTS, Grounds.TRACES),
        World("circus", "Circus", "🎪", "A ringmaster", "Big-top stripes, popcorn and showtime.", false,
            c("#fff7ea"), c("#3a1212"), c("#d62839"), c("#b51f2f"), c("#1d6fb8"), c("#b51f2f"), c("#ffffff"),
            intArrayOf(c("#fff1d6"), c("#fff7ea"), c("#ffeccc")), Particles.CONFETTI, Grounds.BIG_TOP),
        World("disco", "Disco", "🪩", "A disco ball", "Mirror balls, spotlights and a dance floor.", true,
            c("#12061f"), c("#fff0fb"), c("#ff4fd8"), c("#ff4fd8"), c("#f5c542"), c("#ff7ab8"), c("#1d0833"),
            intArrayOf(c("#4a1373"), c("#1d0833"), c("#12061f")), Particles.SPARKLES, Grounds.DANCE_FLOOR),
        World("fitness", "Super Fitness", "💪", "A gym hero", "Bold, sporty and ready to move.", true,
            c("#0b0d10"), c("#f3f7ee"), c("#c6ff3d"), c("#c6ff3d"), c("#c6ff3d"), c("#ff6b6b"), c("#171b21"),
            intArrayOf(c("#171b21"), c("#0b0d10")), Particles.TICKERS, Grounds.GRID),
        World("pride", "Rainbow Pride", "🌈", "A rainbow unicorn", "Every color, love and pride.", false,
            c("#fffaf6"), c("#2a1f3d"), c("#7b3fe4"), c("#6a2fd6"), c("#0f8a5f"), c("#d61f56"), c("#ffffff"),
            intArrayOf(c("#fff0f0"), c("#fff7ea"), c("#fdfde8"), c("#ecfaf1"), c("#ecf1ff"), c("#f5edff")), Particles.CONFETTI, Grounds.RAINBOW),
        World("tropical", "Tropical", "🌺", "A toucan", "Palms, hibiscus and island sun.", false,
            c("#f2fbf6"), c("#11372c"), c("#0f9d76"), c("#0b7d5e"), c("#0b7d5e"), c("#d9472b"), c("#ffffff"),
            intArrayOf(c("#ddf6ec"), c("#f2fbf6"), c("#fff3dd")), Particles.PETALS, Grounds.PALMS),
        World("groovy", "Groovy", "🌼", "A groovy daisy", "Funky waves, flower power and good vibes.", false,
            c("#fff1d6"), c("#3a1f2a"), c("#e2508f"), c("#c2357a"), c("#1f7a6c"), c("#c2461f"), c("#fffaf0"),
            intArrayOf(c("#ffe7bd"), c("#fff1d6"), c("#ffdcd2")), Particles.DAISIES, Grounds.SUNSET),
        World("glass", "Painted Glass", "🪟", "A glass fairy", "See-through stained glass in jewel colors.", false,
            c("#f3efe8"), c("#1f2430"), c("#2f5fd0"), c("#2a52b8"), c("#1f7a50"), c("#b8352a"), c("#ffffff"),
            intArrayOf(c("#f6f1e8"), c("#ece6dc")), Particles.SPARKLES, Grounds.STAINED),
    )

    fun byKey(key: String): World? = all.firstOrNull { it.key == key }

    /** Everything shown in the theme picker, in order. */
    val entries: List<Entry> = listOf(
        Entry("forest", "🧚 Fairy · Enchanted Forest", "Fireflies, fairy trees and glowing mushrooms."),
        Entry("underwater", "🫧 Enchanted Underwater", "Fish, jellyfish, bubbles and flowing water."),
    ) + all.map { Entry(it.key, "${it.emoji} ${it.name}", it.desc) }
}
