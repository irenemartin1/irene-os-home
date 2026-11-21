package app.irene.pastel

import android.graphics.Color

/**
 * Irene OS forest moods. Tap the greeting on the home screen to cycle.
 * Add your own: copy a block, change the hex codes. Colors are #RRGGBB or #AARRGGBB.
 */
data class Palette(
    val name: String,
    val top: Int,         // sky gradient top
    val bottom: Int,      // sky gradient bottom
    val mist: IntArray,   // 3 drifting glow clouds
    val glow: Int,        // firefly + star color
    val ground: Int,      // tree / mushroom silhouettes
    val ink: Int,         // main text + themed icon color
    val soft: Int,        // secondary text
    val bubble: Int,      // circle behind each icon
    val ring: Int,        // thin glowing outline on bubbles / cards
    val card: Int,        // dock / note / search glass
    val spark: String     // little sparkle in the greeting
)

object Palettes {
    private fun c(hex: String) = Color.parseColor(hex)

    val all = listOf(
        Palette(
            "midnight grove", c("#07040D"), c("#1A0D2E"),
            intArrayOf(c("#5B2A86"), c("#3B1E6E"), c("#8E4CC4")),
            c("#E9CCFF"), c("#05020A"),
            c("#F3E9FF"), c("#B9A3D9"), c("#E6231436"), c("#80B57CFF"), c("#991A1029"), "✦"
        ),
        Palette(
            "moonlit moss", c("#050807"), c("#141B22"),
            intArrayOf(c("#2E5A45"), c("#4A2C70"), c("#3C6E57")),
            c("#E4FFA8"), c("#030504"),
            c("#EAF7EE"), c("#A8C4B4"), c("#E6162420"), c("#809FE0B8"), c("#99101A17"), "☾"
        ),
        Palette(
            "amethyst glade", c("#0C0618"), c("#2A1050"),
            intArrayOf(c("#7B3FD1"), c("#B05ADB"), c("#4B2A9C")),
            c("#F6D9FF"), c("#08030F"),
            c("#FBF2FF"), c("#C9B0EE"), c("#E62C1650"), c("#99D7A6FF"), c("#99200E3C"), "✧"
        ),
        Palette(
            "witching hour", c("#060306"), c("#22081F"),
            intArrayOf(c("#7A1F6B"), c("#3D1458"), c("#A63C8F")),
            c("#FFC9EE"), c("#040104"),
            c("#FFEAF8"), c("#D6A2C6"), c("#E62A0E27"), c("#99F29BD9"), c("#991C0818"), "⋆"
        ),
        Palette(
            "fae lanterns", c("#08050C"), c("#1F1230"),
            intArrayOf(c("#4E2A7A"), c("#6B3A5E"), c("#3A2266")),
            c("#FFD98A"), c("#050308"),
            c("#FFF5E6"), c("#D4B8A0"), c("#E6261A33"), c("#99FFCF7A"), c("#991A1026"), "✺"
        ),
    )

    /**
     * Underwater moods. For these, `mist` = the 3 creature colors (fish + jellyfish),
     * `glow` = bubbles + light rays, `ground` = kelp + sand silhouettes.
     */
    val ocean = listOf(
        Palette(
            "moon lagoon", c("#1B2B5A"), c("#06061A"),
            intArrayOf(c("#FF9ECF"), c("#9EE6FF"), c("#C9A0FF")),
            c("#BFF4FF"), c("#040713"),
            c("#EAFBFF"), c("#9FC3E0"), c("#E6162348"), c("#9980E6FF"), c("#99101830"), "◦"
        ),
        Palette(
            "jellyfish dream", c("#2A1A5E"), c("#0A0518"),
            intArrayOf(c("#FFA8E8"), c("#B9A8FF"), c("#8FF0FF")),
            c("#F5D0FF"), c("#07030F"),
            c("#FBF0FF"), c("#C7B2EC"), c("#E6261650"), c("#99D7A6FF"), c("#991A0E33"), "°"
        ),
        Palette(
            "mermaid cove", c("#0F4A5A"), c("#041A22"),
            intArrayOf(c("#FF9F8A"), c("#FFE27A"), c("#D2A8FF")),
            c("#C8FFF0"), c("#02100F"),
            c("#EAFFFA"), c("#9CCFC4"), c("#E60F3038"), c("#9980FFE0"), c("#990A2228"), "~"
        ),
        Palette(
            "abyss glow", c("#0A1228"), c("#000004"),
            intArrayOf(c("#7CFFE0"), c("#FF7CE5"), c("#FFE27C")),
            c("#7CFFE0"), c("#000208"),
            c("#E8FFFA"), c("#8FB8C8"), c("#E60A1530"), c("#997CFFE0"), c("#99050A18"), "✧"
        ),
    )
}
