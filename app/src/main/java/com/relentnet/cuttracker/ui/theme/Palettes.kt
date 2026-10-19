package com.relentnet.cuttracker.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.MonthDay

/** One face of a palette: the tokens that change between palettes. Text stays the base's. */
@Immutable
data class Face(
    val ground: Color, val surface: Color, val hairline: Color, val mutedBorder: Color, val wall: Color,
    val accent: Color, val onAccent: Color, val good: Color, val goodDim: Color, val freeze: Color, val miss: Color, val future: Color,
)

/** What a holiday look pins to the top of every hold. The number in the middle stays readable. */
enum class Decor { NONE, PUMPKIN, SNOW, HEART, STARS, BUNTING }

/** A named look. The stone (ground, surface, wall) and the three result colors change; the type and the layout never do. */
@Immutable
data class Palette(val id: String, val name: String, val line: String, val dark: Face, val light: Face, val decor: Decor = Decor.NONE) {
    fun colors(isDark: Boolean): CutColors {
        val base = if (isDark) DarkCutColors else LightCutColors
        val f = if (isDark) dark else light
        return base.copy(
            ground = f.ground, surface = f.surface, hairline = f.hairline, mutedBorder = f.mutedBorder, wall = f.wall,
            accent = f.accent, onAccent = f.onAccent, good = f.good, goodDim = f.goodDim, freeze = f.freeze, miss = f.miss, future = f.future,
        )
    }
}

private fun dark(stone: Long, surface: Long, hairline: Long, muted: Long, wall: Long, accent: Long, good: Long, goodDim: Long, freeze: Long, onAccent: Long = stone) =
    Face(Color(stone), Color(surface), Color(hairline), Color(muted), Color(wall), Color(accent), Color(onAccent), Color(good), Color(goodDim), Color(freeze), Color(hairline).copy(alpha = 1f), Color(surface))

private fun light(ground: Long, surface: Long, hairline: Long, muted: Long, wall: Long, accent: Long, good: Long, goodDim: Long, freeze: Long, onAccent: Long = 0xFFFFFFFF) =
    Face(Color(ground), Color(surface), Color(hairline), Color(muted), Color(wall), Color(accent), Color(onAccent), Color(good), Color(goodDim), Color(freeze), Color(muted), Color(hairline))

/** The looks you can pick. The first one is the app as drawn in the prototype. */
object Palettes {
    val SANDSTONE = Palette(
        "sandstone", "Sandstone", "Chalk on dark stone. The one the app was drawn in.",
        dark(0xFF0C0D0E, 0xFF15171A, 0xFF23252A, 0xFF3A3D44, 0xFF121417, 0xFFFF5A1F, 0xFFC8F55A, 0xFF4F5A3A, 0xFF5AA9FF),
        light(0xFFF3F1EC, 0xFFFFFFFF, 0xFFE3E0D8, 0xFFC9C5BB, 0xFFE6E3DC, 0xFFE8450A, 0xFF4F8A0E, 0xFFC9DDB0, 0xFF2F7FD6),
    )
    val GRANITE = Palette(
        "granite", "Granite", "Cold grey stone, ice blue holds. For the gym at 6 am.",
        dark(0xFF090C12, 0xFF121823, 0xFF1F2938, 0xFF364860, 0xFF0E1420, 0xFF5AA9FF, 0xFFB8F04A, 0xFF46553A, 0xFFFF8A5B),
        light(0xFFE4EAF2, 0xFFF9FBFD, 0xFFCDD8E5, 0xFFA4B5C9, 0xFFD5DEE9, 0xFF1F6FD6, 0xFF4F8A0E, 0xFFC9DDB0, 0xFFE05A2A),
    )
    val MOSS = Palette(
        "moss", "Moss", "Forest floor. Amber chalk, green sends.",
        dark(0xFF080D09, 0xFF111A13, 0xFF1D2C21, 0xFF34503C, 0xFF0D160F, 0xFFE9B949, 0xFF8FE388, 0xFF3C5A40, 0xFF6FB7FF),
        light(0xFFE6EEE2, 0xFFF8FBF6, 0xFFCFDDC8, 0xFFA7BF9F, 0xFFD8E4D2, 0xFFB07D0A, 0xFF2F8A3C, 0xFFBFE0C2, 0xFF2F7FD6),
    )
    val CHALK = Palette(
        "chalk", "Chalk", "Black, white, and nothing in between. Loudest contrast.",
        dark(0xFF000000, 0xFF101010, 0xFF222222, 0xFF444444, 0xFF080808, 0xFFFF3D00, 0xFF00E676, 0xFF0F5A34, 0xFF2979FF),
        light(0xFFFFFFFF, 0xFFF4F4F4, 0xFFDDDDDD, 0xFFB5B5B5, 0xFFEFEFEF, 0xFFE63600, 0xFF0A8F45, 0xFFB6E8C8, 0xFF1A5FD6),
    )
    val EMBER = Palette(
        "ember", "Ember", "Red chalk and gold. Hot days on the wall.",
        dark(0xFF120808, 0xFF1F1010, 0xFF301B1B, 0xFF503030, 0xFF190C0C, 0xFFFF3B3B, 0xFFFFD166, 0xFF5A4A28, 0xFFFF8A5B),
        light(0xFFF5E6E2, 0xFFFDF7F5, 0xFFE8D0CA, 0xFFCFA9A0, 0xFFEEDAD4, 0xFFD62828, 0xFFB8860B, 0xFFEFDDA8, 0xFFE05A2A),
    )
    val DUSK = Palette(
        "dusk", "Dusk", "Purple stone after sundown. Mint sends.",
        dark(0xFF0D0816, 0xFF181027, 0xFF281C3C, 0xFF463464, 0xFF130C1F, 0xFFB388FF, 0xFF69F0AE, 0xFF2F5A48, 0xFFFFB74D),
        light(0xFFECE4F6, 0xFFF9F6FD, 0xFFD8CBEA, 0xFFB7A3D4, 0xFFE2D8EF, 0xFF6A3FD1, 0xFF1E9A66, 0xFFB9E8D2, 0xFFD67F1A),
    )
    val GLACIER = Palette(
        "glacier", "Glacier", "Deep water and ice. Cyan chalk, lime sends.",
        dark(0xFF060E18, 0xFF0E1A28, 0xFF192A3D, 0xFF2C4660, 0xFF0A1420, 0xFF00E5FF, 0xFFB2FF59, 0xFF43603A, 0xFFFF8A5B),
        light(0xFFE0ECF5, 0xFFF6FAFD, 0xFFC8DAE8, 0xFFA0BBD0, 0xFFD3E3EE, 0xFF0096B0, 0xFF5A9A10, 0xFFCDE6B0, 0xFFE05A2A),
    )
    val HARVEST = Palette(
        "harvest", "Harvest", "Pumpkin chalk, gold sends, a purple freeze. Autumn on the wall.",
        dark(0xFF120C06, 0xFF1E150C, 0xFF302316, 0xFF503D2A, 0xFF18100A, 0xFFFF8C1A, 0xFFD4A017, 0xFF5A4A18, 0xFF9B5DE5),
        light(0xFFF3E6D3, 0xFFFCF7EE, 0xFFE4D2BA, 0xFFC9AE8A, 0xFFEBDCC5, 0xFFD9700A, 0xFF9A7B00, 0xFFEADFA2, 0xFF6F3FC0),
    )
    val HALLOWEEN = Palette(
        "halloween", "Halloween", "Jack-o'-lantern orange on a purple-black wall. Every hold gets a face.",
        dark(0xFF0B0612, 0xFF190F2B, 0xFF2B1B45, 0xFF4A3473, 0xFF140A24, 0xFFFF6A00, 0xFFB5FF4A, 0xFF4A6A20, 0xFF9B5DE5),
        light(0xFFE8DEF5, 0xFFF7F2FC, 0xFFD6C6EA, 0xFFB39ACF, 0xFFDCCFF0, 0xFFE85D04, 0xFF5C9A00, 0xFFCDEDA3, 0xFF6F3FC0),
        decor = Decor.PUMPKIN,
    )
    val WINTER = Palette(
        "winter", "Winter", "Pine and ember on dark ice, snow on every hold. The last week of the year.",
        dark(0xFF070D14, 0xFF101C27, 0xFF1B2D3C, 0xFF31485C, 0xFF0C1620, 0xFFE63946, 0xFF2FBF71, 0xFF1E5A3C, 0xFF8ECAE6),
        light(0xFFE3EDF3, 0xFFF7FBFD, 0xFFCBDCE6, 0xFFA3BCCA, 0xFFD6E4EC, 0xFFC1121F, 0xFF1B8A4B, 0xFFB8E3C8, 0xFF3A86C8),
        decor = Decor.SNOW,
    )
    val NEW_YEAR = Palette(
        "newyear", "New Year", "Gold chalk on black, a spark on every hold. One night a year.",
        dark(0xFF000000, 0xFF14120A, 0xFF2A2612, 0xFF4E4820, 0xFF0B0A05, 0xFFFFD60A, 0xFFF2EEE6, 0xFF5A5848, 0xFF5AA9FF, onAccent = 0xFF000000),
        light(0xFFF7F0D8, 0xFFFEFBF2, 0xFFEADFB8, 0xFFD0C290, 0xFFF1E8CC, 0xFFC99A00, 0xFF4F8A0E, 0xFFC9DDB0, 0xFF2F7FD6),
        decor = Decor.STARS,
    )
    val ROSE = Palette(
        "rose", "Rose", "Rose chalk, blush sends, a heart on every hold. Mid-February.",
        dark(0xFF14070C, 0xFF221017, 0xFF361C27, 0xFF583345, 0xFF1A0B11, 0xFFFF5C8A, 0xFFFFB3C6, 0xFF5A3C46, 0xFF8ECAE6),
        light(0xFFF8E3EA, 0xFFFEF7F9, 0xFFEFCDD9, 0xFFDBA7BB, 0xFFF3DAE2, 0xFFD6336C, 0xFFB03060, 0xFFF0C6D4, 0xFF2F7FD6),
        decor = Decor.HEART,
    )
    val FOURTH = Palette(
        "fourth", "Fourth", "Red chalk, white sends, blue freeze, bunting on every hold. Early July.",
        dark(0xFF070A16, 0xFF101526, 0xFF1C2440, 0xFF334066, 0xFF0C1020, 0xFFFF3B3B, 0xFFF2EEE6, 0xFF5A5858, 0xFF3A86FF),
        light(0xFFE6EBF7, 0xFFF8FAFE, 0xFFD0D9ED, 0xFFA8B7D8, 0xFFDCE3F2, 0xFFD62828, 0xFF1F3F8A, 0xFFBFCDEB, 0xFF3A86FF),
        decor = Decor.BUNTING,
    )

    /** What the palette grid shows, in order. Holiday looks sit at the end so they can be picked by hand too. */
    val all: List<Palette> = listOf(SANDSTONE, GRANITE, MOSS, CHALK, EMBER, DUSK, GLACIER, HARVEST, HALLOWEEN, WINTER, NEW_YEAR, ROSE, FOURTH)

    fun byId(id: String): Palette = all.firstOrNull { it.id == id } ?: SANDSTONE

    /** A holiday window: inclusive month-days, the look, and the line the Appearance page shows. */
    data class Holiday(val name: String, val from: MonthDay, val to: MonthDay, val palette: Palette) {
        val span: String get() = "${from.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${from.dayOfMonth} to " +
            (if (from.month == to.month) "${to.dayOfMonth}" else "${to.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${to.dayOfMonth}")
        fun covers(d: LocalDate): Boolean {
            val md = MonthDay.from(d)
            return if (from <= to) md >= from && md <= to else md >= from || md <= to
        }
    }

    val holidays: List<Holiday> = listOf(
        Holiday("Valentine's", MonthDay.of(2, 13), MonthDay.of(2, 14), ROSE),
        Holiday("The Fourth", MonthDay.of(7, 3), MonthDay.of(7, 4), FOURTH),
        Holiday("Halloween", MonthDay.of(10, 24), MonthDay.of(10, 31), HALLOWEEN),
        Holiday("Winter", MonthDay.of(12, 20), MonthDay.of(12, 26), WINTER),
        Holiday("New Year", MonthDay.of(12, 31), MonthDay.of(1, 1), NEW_YEAR),
    )

    /** The look each month wears when the calendar drives the theme. */
    fun forMonth(m: Month): Palette = when (m) {
        Month.JANUARY -> GLACIER
        Month.FEBRUARY -> DUSK
        Month.MARCH, Month.APRIL -> MOSS
        Month.MAY, Month.JUNE -> SANDSTONE
        Month.JULY, Month.AUGUST -> EMBER
        Month.SEPTEMBER, Month.OCTOBER, Month.NOVEMBER -> HARVEST
        Month.DECEMBER -> GRANITE
    }

    /** The look for a day under the calendar, and the reason in words. */
    fun forDate(d: LocalDate): Pair<Palette, String> {
        val h = holidays.firstOrNull { it.covers(d) }
        if (h != null) return h.palette to "${h.name}, ${h.span}"
        val m = d.month.name.lowercase().replaceFirstChar { it.uppercase() }
        return forMonth(d.month) to m
    }

    /** Dark from 19:00 to 07:00 when the mode is a schedule. */
    fun scheduledDark(t: LocalTime): Boolean = t.hour >= 19 || t.hour < 7
}
