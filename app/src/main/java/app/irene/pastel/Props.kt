package app.irene.pastel

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * What each creature is holding: a little drawing of what the app is FOR (Maps holds a map pin,
 * Camera a camera, Gmail a letter, Spotify a music note, bank apps a coin…), painted in the current
 * theme's colors. Apps with no obvious job hold the theme's own charm (🎀 in Coquette, 🌙 in Celestial…).
 */
object Props {
    const val NONE = 0; const val MAP = 1; const val CAMERA = 2; const val PHOTO = 3; const val MAIL = 4; const val CHAT = 5
    const val PHONE = 6; const val CLOCK = 7; const val CALENDAR = 8; const val SETTINGS = 9; const val BROWSER = 10
    const val MUSIC = 11; const val VIDEO = 12; const val SHOP = 13; const val MONEY = 14; const val FIT = 15; const val FOOD = 16
    const val GAME = 17; const val FILES = 18; const val NOTES = 19; const val WEATHER = 20; const val SOCIAL = 21; const val WORK = 22
    const val CONTACTS = 23; const val CALC = 24; const val SEARCH = 25; const val TRAVEL = 26; const val BOOK = 27; const val AI = 28

    private val rules: List<Pair<Int, List<String>>> = listOf(
        CONTACTS to listOf("contacts"),
        CALC to listOf("calculator"),
        CALENDAR to listOf("calendar"),
        CLOCK to listOf("clock", "alarm"),
        SETTINGS to listOf("settings"),
        WEATHER to listOf("weather"),
        AI to listOf("claude", "gemini", "chatgpt", "openai", "assistant", "copilot", "bard", "anthropic"),
        CAMERA to listOf("camera", "snapchat"),
        PHOTO to listOf("photos", "gallery", "pinterest", "instagram", "vsco", "lightroom", "picsart"),
        MAIL to listOf("gmail", "mail", "outlook", "email"),
        CHAT to listOf("messag", "whatsapp", "telegram", "discord", "slack", "signal", "chat", "sms", "mms"),
        PHONE to listOf("dialer", "phone", "tmobile", "verizon", "metropcs", "cricket"),
        MAP to listOf("maps", "waze", "uber", "lyft", "transit", "navigation", "gps"),
        TRAVEL to listOf("airbnb", "booking", "expedia", "airline", "delta", "united", "southwest", "hotel", "trip", "flight"),
        VIDEO to listOf("zhiliaoapp", "tiktok"),
        MUSIC to listOf("music", "spotify", "soundcloud", "pandora", "radio", "podcast", "shazam", "audio"),
        VIDEO to listOf("youtube", "netflix", "hulu", "disney", "video", "twitch", "tiktok", "hbomax", "com.wbd", "tubi", "peacock", "movie", "capcut"),
        SHOP to listOf("shop", "amazon", "store", "vending", "ebay", "etsy", "target", "walmart", "shein", "temu", "sephora", "ulta", "mercari", "depop"),
        MONEY to listOf("bank", "pay", "wallet", "cash", "venmo", "zelle", "chase", "finance", "money", "robinhood", "coinbase", "credit", "capitalone", "amex", "irene"),
        FIT to listOf("fit", "health", "strava", "workout", "gym", "steps", "sleep", "calm", "headspace", "peloton"),
        FOOD to listOf("food", "eats", "doordash", "grubhub", "starbucks", "mcdonald", "recipe", "instacart", "dunkin", "chipotle", "pizza"),
        GAME to listOf("game", "roblox", "minecraft", "candy", "pokemon", "chess", "puzzle", "play.games", "supercell"),
        FILES to listOf("files", "drive", "dropbox", "documentsui", "download"),
        NOTES to listOf("keep", "notes", "notion", "docs", "office.word", "onenote", "evernote", "writer", "journal"),
        SOCIAL to listOf("facebook", "twitter", "threads", "reddit", "bereal", "tumblr", "social", "dating", "tinder", "hinge", "bumble"),
        WORK to listOf("teams", "zoom", "meet", "office", "sheets", "slides", "excel", "powerpoint", "linkedin", "ebuilt", "jobs", "indeed"),
        BOOK to listOf("kindle", "books", "audible", "goodreads", "library", "duolingo", "learn", "bible"),
        BROWSER to listOf("chrome", "browser", "firefox", "edge", "opera", "brave", "duckduckgo"),
        SEARCH to listOf("googlequicksearchbox", "search", "lens"),
    )

    /** Works out what an app is for from its package name and label. */
    fun roleOf(pkg: String, label: String): Int {
        val k = (pkg + " " + label).lowercase()
        for ((role, words) in rules) if (words.any { k.contains(it) }) return role
        return NONE
    }

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val path = Path()
    private val rect = RectF()

    /** Draws the prop on a little round tag at (x, y) of radius s. */
    fun draw(c: Canvas, role: Int, x: Float, y: Float, s: Float, bg: Int, ink: Int, accent: Int, charm: String) {
        fill.color = bg; c.drawCircle(x, y, s, fill)
        line.color = accent; line.strokeWidth = s * 0.12f; c.drawCircle(x, y, s, line)
        val u = s * 0.55f // icon half-size
        line.strokeWidth = u * 0.18f
        fill.color = accent; line.color = ink
        when (role) {
            MAP -> { path.reset(); path.moveTo(x, y + u); path.cubicTo(x - u * 1.2f, y - u * 0.1f, x - u * 0.6f, y - u * 1.1f, x, y - u * 1.0f); path.cubicTo(x + u * 0.6f, y - u * 1.1f, x + u * 1.2f, y - u * 0.1f, x, y + u); path.close(); c.drawPath(path, fill)
                fill.color = bg; c.drawCircle(x, y - u * 0.35f, u * 0.3f, fill) }
            CAMERA -> { fill.color = ink; rect.set(x - u, y - u * 0.55f, x + u, y + u * 0.75f); c.drawRoundRect(rect, u * 0.25f, u * 0.25f, fill); c.drawRect(x - u * 0.35f, y - u * 0.8f, x + u * 0.35f, y - u * 0.5f, fill)
                fill.color = accent; c.drawCircle(x, y + u * 0.1f, u * 0.42f, fill); fill.color = bg; c.drawCircle(x, y + u * 0.1f, u * 0.18f, fill) }
            PHOTO -> { line.color = ink; rect.set(x - u, y - u * 0.8f, x + u, y + u * 0.8f); c.drawRoundRect(rect, u * 0.15f, u * 0.15f, line)
                path.reset(); path.moveTo(x - u * 0.85f, y + u * 0.65f); path.lineTo(x - u * 0.2f, y - u * 0.15f); path.lineTo(x + u * 0.2f, y + u * 0.3f); path.lineTo(x + u * 0.45f, y + u * 0.05f); path.lineTo(x + u * 0.85f, y + u * 0.65f); path.close(); c.drawPath(path, fill)
                fill.color = Color.parseColor("#ffc93c"); c.drawCircle(x + u * 0.45f, y - u * 0.4f, u * 0.18f, fill) }
            MAIL -> { rect.set(x - u, y - u * 0.65f, x + u, y + u * 0.65f); c.drawRoundRect(rect, u * 0.12f, u * 0.12f, fill); line.color = bg; path.reset(); path.moveTo(x - u, y - u * 0.6f); path.lineTo(x, y + u * 0.1f); path.lineTo(x + u, y - u * 0.6f); c.drawPath(path, line) }
            CHAT -> { rect.set(x - u, y - u * 0.8f, x + u, y + u * 0.5f); c.drawRoundRect(rect, u * 0.45f, u * 0.45f, fill); path.reset(); path.moveTo(x - u * 0.5f, y + u * 0.4f); path.lineTo(x - u * 0.7f, y + u * 0.95f); path.lineTo(x - u * 0.05f, y + u * 0.45f); path.close(); c.drawPath(path, fill)
                fill.color = bg; for (k in -1..1) c.drawCircle(x + k * u * 0.45f, y - u * 0.15f, u * 0.13f, fill) }
            PHONE -> { line.color = accent; line.strokeWidth = u * 0.42f; rect.set(x - u * 0.8f, y - u * 0.8f, x + u * 0.8f, y + u * 0.8f); c.drawArc(rect, 110f, 140f, false, line)
                fill.color = accent; c.drawCircle(x - u * 0.27f, y + u * 0.75f, u * 0.3f, fill); c.drawCircle(x - u * 0.27f, y - u * 0.75f, u * 0.3f, fill) }
            CLOCK -> { c.drawCircle(x, y, u, fill); line.color = bg; c.drawLine(x, y, x, y - u * 0.65f, line); c.drawLine(x, y, x + u * 0.45f, y + u * 0.2f, line) }
            CALENDAR -> { fill.color = Color.WHITE; rect.set(x - u, y - u * 0.75f, x + u, y + u * 0.9f); c.drawRoundRect(rect, u * 0.15f, u * 0.15f, fill); line.color = ink; line.strokeWidth = u * 0.1f; c.drawRoundRect(rect, u * 0.15f, u * 0.15f, line)
                fill.color = accent; c.drawRect(x - u, y - u * 0.75f, x + u, y - u * 0.25f, fill); fill.color = ink
                for (r in 0 until 2) for (k in 0 until 3) c.drawCircle(x - u * 0.55f + k * u * 0.55f, y + u * 0.15f + r * u * 0.4f, u * 0.1f, fill) }
            SETTINGS -> { for (k in 0 until 8) { c.save(); c.rotate(k * 45f, x, y); c.drawRect(x - u * 0.17f, y - u, x + u * 0.17f, y - u * 0.5f, fill); c.restore() }; c.drawCircle(x, y, u * 0.68f, fill); fill.color = bg; c.drawCircle(x, y, u * 0.28f, fill) }
            BROWSER, SEARCH -> if (role == SEARCH) { line.color = accent; line.strokeWidth = u * 0.25f; c.drawCircle(x - u * 0.2f, y - u * 0.2f, u * 0.55f, line); c.drawLine(x + u * 0.2f, y + u * 0.2f, x + u * 0.85f, y + u * 0.85f, line) }
                else { c.drawCircle(x, y, u, fill); line.color = bg; line.strokeWidth = u * 0.1f; rect.set(x - u * 0.45f, y - u, x + u * 0.45f, y + u); c.drawOval(rect, line); c.drawLine(x - u, y, x + u, y, line); c.drawLine(x, y - u, x, y + u, line) }
            MUSIC -> { fill.color = accent; c.drawCircle(x - u * 0.45f, y + u * 0.55f, u * 0.3f, fill); c.drawCircle(x + u * 0.55f, y + u * 0.35f, u * 0.3f, fill)
                line.color = accent; line.strokeWidth = u * 0.16f; c.drawLine(x - u * 0.2f, y + u * 0.55f, x - u * 0.2f, y - u * 0.75f, line); c.drawLine(x + u * 0.8f, y + u * 0.35f, x + u * 0.8f, y - u * 0.95f, line); line.strokeWidth = u * 0.28f; c.drawLine(x - u * 0.2f, y - u * 0.7f, x + u * 0.8f, y - u * 0.9f, line) }
            VIDEO -> { rect.set(x - u, y - u * 0.7f, x + u, y + u * 0.7f); c.drawRoundRect(rect, u * 0.3f, u * 0.3f, fill); fill.color = bg; path.reset(); path.moveTo(x - u * 0.3f, y - u * 0.38f); path.lineTo(x + u * 0.45f, y); path.lineTo(x - u * 0.3f, y + u * 0.38f); path.close(); c.drawPath(path, fill) }
            SHOP -> { path.reset(); path.moveTo(x - u * 0.85f, y - u * 0.3f); path.lineTo(x + u * 0.85f, y - u * 0.3f); path.lineTo(x + u * 0.7f, y + u); path.lineTo(x - u * 0.7f, y + u); path.close(); c.drawPath(path, fill)
                line.color = ink; line.strokeWidth = u * 0.14f; rect.set(x - u * 0.4f, y - u * 0.95f, x + u * 0.4f, y + u * 0.2f); c.drawArc(rect, 180f, 180f, false, line) }
            MONEY -> { fill.color = Color.parseColor("#E9C25A"); c.drawCircle(x, y, u, fill); line.color = Color.parseColor("#b8902e"); line.strokeWidth = u * 0.1f; c.drawCircle(x, y, u * 0.78f, line)
                text.color = Color.parseColor("#8a6a1f"); text.textSize = u * 1.3f; text.isFakeBoldText = true; c.drawText("$", x, y + u * 0.45f, text) }
            FIT -> { fill.color = ink; c.drawRect(x - u * 0.6f, y - u * 0.1f, x + u * 0.6f, y + u * 0.1f, fill); fill.color = accent
                for (sx in floatArrayOf(-1f, 1f)) { rect.set(x + sx * u * 0.85f - u * 0.2f, y - u * 0.6f, x + sx * u * 0.85f + u * 0.2f, y + u * 0.6f); c.drawRoundRect(rect, u * 0.1f, u * 0.1f, fill); rect.set(x + sx * u * 0.55f - u * 0.12f, y - u * 0.42f, x + sx * u * 0.55f + u * 0.12f, y + u * 0.42f); c.drawRoundRect(rect, u * 0.08f, u * 0.08f, fill) } }
            FOOD -> { fill.color = Color.parseColor("#f2c9a0"); path.reset(); path.moveTo(x - u * 0.6f, y); path.lineTo(x + u * 0.6f, y); path.lineTo(x + u * 0.4f, y + u); path.lineTo(x - u * 0.4f, y + u); path.close(); c.drawPath(path, fill)
                fill.color = accent; rect.set(x - u * 0.75f, y - u * 0.85f, x + u * 0.75f, y + u * 0.25f); c.drawArc(rect, 180f, 180f, true, fill); fill.color = Color.parseColor("#E5394B"); c.drawCircle(x, y - u * 0.9f, u * 0.17f, fill) }
            GAME -> { rect.set(x - u, y - u * 0.55f, x + u, y + u * 0.6f); c.drawRoundRect(rect, u * 0.5f, u * 0.5f, fill); fill.color = bg
                c.drawRect(x - u * 0.65f, y - u * 0.06f, x - u * 0.25f, y + u * 0.06f, fill); c.drawRect(x - u * 0.51f, y - u * 0.2f, x - u * 0.39f, y + u * 0.2f, fill); c.drawCircle(x + u * 0.4f, y - u * 0.12f, u * 0.1f, fill); c.drawCircle(x + u * 0.6f, y + u * 0.1f, u * 0.1f, fill) }
            FILES -> { path.reset(); path.moveTo(x - u, y - u * 0.6f); path.lineTo(x - u * 0.3f, y - u * 0.6f); path.lineTo(x - u * 0.15f, y - u * 0.4f); path.lineTo(x + u, y - u * 0.4f); path.lineTo(x + u, y + u * 0.7f); path.lineTo(x - u, y + u * 0.7f); path.close(); c.drawPath(path, fill) }
            NOTES -> { fill.color = Color.parseColor("#fff6c9"); rect.set(x - u * 0.75f, y - u, x + u * 0.75f, y + u); c.drawRoundRect(rect, u * 0.12f, u * 0.12f, fill); line.color = accent; line.strokeWidth = u * 0.12f
                for (k in 0 until 3) c.drawLine(x - u * 0.45f, y - u * 0.45f + k * u * 0.42f, x + u * 0.45f, y - u * 0.45f + k * u * 0.42f, line) }
            WEATHER -> { fill.color = Color.parseColor("#ffc93c"); c.drawCircle(x - u * 0.3f, y - u * 0.3f, u * 0.5f, fill); fill.color = Color.WHITE
                c.drawCircle(x + u * 0.05f, y + u * 0.3f, u * 0.4f, fill); c.drawCircle(x + u * 0.5f, y + u * 0.2f, u * 0.45f, fill); c.drawCircle(x - u * 0.4f, y + u * 0.45f, u * 0.3f, fill); line.color = accent; line.strokeWidth = u * 0.06f; c.drawCircle(x + u * 0.5f, y + u * 0.2f, u * 0.45f, line) }
            SOCIAL -> { path.reset(); path.moveTo(x, y + u * 0.85f); path.cubicTo(x - u * 1.4f, y - u * 0.1f, x - u * 0.6f, y - u * 1.2f, x, y - u * 0.35f); path.cubicTo(x + u * 0.6f, y - u * 1.2f, x + u * 1.4f, y - u * 0.1f, x, y + u * 0.85f); path.close(); c.drawPath(path, fill) }
            WORK -> { rect.set(x - u, y - u * 0.45f, x + u, y + u * 0.75f); c.drawRoundRect(rect, u * 0.15f, u * 0.15f, fill); line.color = ink; line.strokeWidth = u * 0.14f; rect.set(x - u * 0.35f, y - u * 0.8f, x + u * 0.35f, y - u * 0.3f); c.drawRoundRect(rect, u * 0.1f, u * 0.1f, line)
                fill.color = bg; c.drawRect(x - u, y + u * 0.05f, x + u, y + u * 0.15f, fill) }
            CONTACTS -> { c.drawCircle(x, y - u * 0.4f, u * 0.42f, fill); rect.set(x - u * 0.8f, y + u * 0.1f, x + u * 0.8f, y + u * 1.4f); c.drawArc(rect, 180f, 180f, true, fill) }
            CALC -> { fill.color = ink; rect.set(x - u * 0.75f, y - u, x + u * 0.75f, y + u); c.drawRoundRect(rect, u * 0.15f, u * 0.15f, fill); fill.color = accent; c.drawRect(x - u * 0.55f, y - u * 0.8f, x + u * 0.55f, y - u * 0.35f, fill)
                fill.color = bg; for (r in 0 until 2) for (k in 0 until 3) c.drawCircle(x - u * 0.4f + k * u * 0.4f, y + u * 0.05f + r * u * 0.45f, u * 0.12f, fill) }
            TRAVEL -> { path.reset(); path.moveTo(x - u, y); path.lineTo(x + u, y - u * 0.7f); path.lineTo(x + u * 0.1f, y + u * 0.9f); path.lineTo(x - u * 0.1f, y + u * 0.2f); path.close(); c.drawPath(path, fill); line.color = bg; line.strokeWidth = u * 0.08f; c.drawLine(x - u * 0.1f, y + u * 0.2f, x + u, y - u * 0.7f, line) }
            BOOK -> { for (sx in floatArrayOf(-1f, 1f)) { path.reset(); path.moveTo(x, y - u * 0.5f); path.quadTo(x + sx * u * 0.5f, y - u * 0.85f, x + sx * u, y - u * 0.6f); path.lineTo(x + sx * u, y + u * 0.7f); path.quadTo(x + sx * u * 0.5f, y + u * 0.45f, x, y + u * 0.8f); path.close(); c.drawPath(path, fill) }
                line.color = bg; line.strokeWidth = u * 0.08f; c.drawLine(x, y - u * 0.5f, x, y + u * 0.8f, line) }
            AI -> { line.color = ink; line.strokeWidth = u * 0.2f; c.drawLine(x - u * 0.8f, y + u * 0.8f, x + u * 0.2f, y - u * 0.2f, line)
                fill.color = accent; sparkle(x + u * 0.4f, y - u * 0.4f, u * 0.6f); c.drawPath(path, fill); fill.color = Color.parseColor("#ffc93c"); sparkle(x - u * 0.5f, y - u * 0.6f, u * 0.3f); c.drawPath(path, fill) }
            else -> { text.color = Color.BLACK; text.textSize = u * 1.5f; text.isFakeBoldText = false; c.drawText(charm, x, y + u * 0.52f, text) }
        }
    }

    private fun sparkle(x: Float, y: Float, s: Float) {
        path.reset(); path.moveTo(x, y - s); path.quadTo(x, y, x + s, y); path.quadTo(x, y, x, y + s); path.quadTo(x, y, x - s, y); path.quadTo(x, y, x, y - s); path.close()
    }

    @Suppress("unused") private fun ring(x: Float, y: Float, r: Float) = (0 until 6).map { x + (r * cos(it * PI / 3)).toFloat() to y + (r * sin(it * PI / 3)).toFloat() }
}
