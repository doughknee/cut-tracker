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

/** A named look. The stone (ground, surface, wall) and the three result colors change; the type and the layout never do. */
@Immutable
data class Palette(val id: String, val name: String, val line: String, val dark: Face, val light: Face) {
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
        dark(0xFF0A0C10, 0xFF13161C, 0xFF20242C, 0xFF384050, 0xFF10131A, 0xFF5AA9FF, 0xFFB8F04A, 0xFF46553A, 0xFFFF8A5B),
        light(0xFFEDF0F4, 0xFFFFFFFF, 0xFFDCE1E8, 0xFFBAC3CF, 0xFFE0E5EC, 0xFF1F6FD6, 0xFF4F8A0E, 0xFFC9DDB0, 0xFFE05A2A),
    )
    val MOSS = Palette(
        "moss", "Moss", "Forest floor. Amber chalk, green sends.",
        dark(0xFF0A0E0B, 0xFF131915, 0xFF1F2823, 0xFF37443C, 0xFF101612, 0xFFE9B949, 0xFF8FE388, 0xFF3C5A40, 0xFF6FB7FF),
        light(0xFFEFF3EC, 0xFFFFFFFF, 0xFFDCE4D8, 0xFFB8C6B2, 0xFFE2E9DD, 0xFFB07D0A, 0xFF2F8A3C, 0xFFBFE0C2, 0xFF2F7FD6),
    )
    val CHALK = Palette(
        "chalk", "Chalk", "Black, white, and nothing in between. Loudest contrast.",
        dark(0xFF000000, 0xFF101010, 0xFF222222, 0xFF444444, 0xFF080808, 0xFFFF3D00, 0xFF00E676, 0xFF0F5A34, 0xFF2979FF),
        light(0xFFFFFFFF, 0xFFF4F4F4, 0xFFDDDDDD, 0xFFB5B5B5, 0xFFEFEFEF, 0xFFE63600, 0xFF0A8F45, 0xFFB6E8C8, 0xFF1A5FD6),
    )
    val EMBER = Palette(
        "ember", "Ember", "Red chalk and gold. Hot days on the wall.",
        dark(0xFF100B0B, 0xFF1A1313, 0xFF2A1F1F, 0xFF463636, 0xFF160F0F, 0xFFFF3B3B, 0xFFFFD166, 0xFF5A4A28, 0xFFFF8A5B),
        light(0xFFF6EFEC, 0xFFFFFFFF, 0xFFEADDD8, 0xFFD2BDB6, 0xFFEEE3DF, 0xFFD62828, 0xFFB8860B, 0xFFEFDDA8, 0xFFE05A2A),
    )
    val DUSK = Palette(
        "dusk", "Dusk", "Purple stone after sundown. Mint sends.",
        dark(0xFF0D0A14, 0xFF171221, 0xFF241C32, 0xFF3F3454, 0xFF130F1C, 0xFFB388FF, 0xFF69F0AE, 0xFF2F5A48, 0xFFFFB74D),
        light(0xFFF2EEF8, 0xFFFFFFFF, 0xFFE2DBED, 0xFFC3B8D8, 0xFFE8E2F2, 0xFF6A3FD1, 0xFF1E9A66, 0xFFB9E8D2, 0xFFD67F1A),
    )
    val GLACIER = Palette(
        "glacier", "Glacier", "Deep water and ice. Cyan chalk, lime sends.",
        dark(0xFF08101A, 0xFF101A26, 0xFF1B2838, 0xFF2F4258, 0xFF0C1622, 0xFF00E5FF, 0xFFB2FF59, 0xFF43603A, 0xFFFF8A5B),
        light(0xFFEAF2F8, 0xFFFFFFFF, 0xFFD6E2EC, 0xFFB1C5D6, 0xFFDFE9F1, 0xFF0096B0, 0xFF5A9A10, 0xFFCDE6B0, 0xFFE05A2A),
    )
    val HARVEST = Palette(
        "harvest", "Harvest", "Pumpkin chalk, gold sends, a purple freeze. Autumn on the wall.",
        dark(0xFF0F0C09, 0xFF191410, 0xFF29211A, 0xFF46392E, 0xFF15100C, 0xFFFF8C1A, 0xFFD4A017, 0xFF5A4A18, 0xFF9B5DE5),
        light(0xFFF7F1E8, 0xFFFFFFFF, 0xFFEBE1D2, 0xFFD3C3AC, 0xFFF0E7DA, 0xFFD9700A, 0xFF9A7B00, 0xFFEADFA2, 0xFF6F3FC0),
    )
    val HALLOWEEN = Palette(
        "halloween", "Halloween", "Jack-o'-lantern orange on a purple-black wall. Acid green sends.",
        dark(0xFF0C0914, 0xFF17121F, 0xFF261D33, 0xFF433659, 0xFF120D1C, 0xFFFF7518, 0xFFB5FF4A, 0xFF4A6A20, 0xFF9B5DE5),
        light(0xFFF4EEF9, 0xFFFFFFFF, 0xFFE6DCF0, 0xFFCDBEDF, 0xFFEBE3F3, 0xFFE85D04, 0xFF5C9A00, 0xFFCDEDA3, 0xFF6F3FC0),
    )
    val WINTER = Palette(
        "winter", "Winter", "Pine and ember on dark ice. For the last week of the year.",
        dark(0xFF0A0F12, 0xFF121A1E, 0xFF1D282E, 0xFF344550, 0xFF0F161A, 0xFFE63946, 0xFF2FBF71, 0xFF1E5A3C, 0xFF8ECAE6),
        light(0xFFEDF3F5, 0xFFFFFFFF, 0xFFD8E4E9, 0xFFB3C7D0, 0xFFE2ECF0, 0xFFC1121F, 0xFF1B8A4B, 0xFFB8E3C8, 0xFF3A86C8),
    )
    val NEW_YEAR = Palette(
        "newyear", "New Year", "Gold chalk on black. One night a year.",
        dark(0xFF000000, 0xFF111008, 0xFF24220F, 0xFF46421C, 0xFF0A0906, 0xFFFFD60A, 0xFFF2EEE6, 0xFF5A5848, 0xFF5AA9FF, onAccent = 0xFF000000),
        light(0xFFFBF8EE, 0xFFFFFFFF, 0xFFEFE8CF, 0xFFD8CFA8, 0xFFF3EEDC, 0xFFC99A00, 0xFF4F8A0E, 0xFFC9DDB0, 0xFF2F7FD6),
    )
    val ROSE = Palette(
        "rose", "Rose", "Rose chalk, blush sends. Mid-February.",
        dark(0xFF120A0E, 0xFF1C1218, 0xFF2C1E26, 0xFF4A3540, 0xFF170F14, 0xFFFF5C8A, 0xFFFFB3C6, 0xFF5A3C46, 0xFF8ECAE6),
        light(0xFFF9EEF2, 0xFFFFFFFF, 0xFFEFDCE4, 0xFFDBBAC8, 0xFFF3E5EB, 0xFFD6336C, 0xFFB03060, 0xFFF0C6D4, 0xFF2F7FD6),
    )
    val FOURTH = Palette(
        "fourth", "Fourth", "Red chalk, white sends, blue freeze. Early July.",
        dark(0xFF0A0C14, 0xFF13161F, 0xFF202432, 0xFF384058, 0xFF10131A, 0xFFFF3B3B, 0xFFF2EEE6, 0xFF5A5858, 0xFF3A86FF),
        light(0xFFEFF2F8, 0xFFFFFFFF, 0xFFDDE3EE, 0xFFBAC6DA, 0xFFE3E9F2, 0xFFD62828, 0xFF1F3F8A, 0xFFBFCDEB, 0xFF3A86FF),
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
