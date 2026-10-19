package com.relentnet.cuttracker.data

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A look the user chalked: one accent hue on one of six stones. Everything else (sends, freeze, both faces) is derived
 * in [CustomLook.face] so nothing clashes. Pure Kotlin so the derivation and the share code can be unit tested.
 */
@Serializable
data class CustomLook(val id: String, val name: String, val hue: Int, val stone: Int) {
    /** The code a friend can paste: SL- plus four letters, with a checksum so a typo is refused. */
    val code: String get() = encode(hue, stone)

    /** Chalk names, so a pasted code gets a name without asking. */
    val autoName: String get() = "${HUE_NAMES[((hue % 360 + 360) % 360) / 30]} on ${STONES[stone.coerceIn(0, 5)].name.lowercase()}"

    /** ARGB ints for one face. Order matches ui.theme.Face. */
    data class FaceInts(
        val ground: Int, val surface: Int, val hairline: Int, val muted: Int, val wall: Int,
        val accent: Int, val onAccent: Int, val good: Int, val goodDim: Int, val freeze: Int, val miss: Int, val future: Int,
    )

    fun face(dark: Boolean): FaceInts {
        val st = STONES[stone.coerceIn(0, 5)]
        val h = ((hue % 360) + 360) % 360
        val hGood = (h + 110) % 360
        val hFreeze = (h + 200) % 360
        val l = if (dark) st.dark else st.light
        val ground = hsl(st.hue, st.sat, l[0]); val surface = hsl(st.hue, st.sat, l[1]); val hairline = hsl(st.hue, st.sat, l[2])
        val muted = hsl(st.hue, st.sat, l[3]); val wall = hsl(st.hue, st.sat, l[4])
        return if (dark) FaceInts(
            ground, surface, hairline, muted, wall,
            accent = hsl(h, 1f, 0.56f), onAccent = ground,
            good = hsl(hGood, 0.85f, 0.62f), goodDim = hsl(hGood, 0.25f, 0.30f), freeze = hsl(hFreeze, 0.95f, 0.66f),
            miss = hairline, future = surface,
        ) else FaceInts(
            ground, surface, hairline, muted, wall,
            accent = hsl(h, 0.9f, 0.45f), onAccent = 0xFFFFFFFF.toInt(),
            good = hsl(hGood, 0.8f, 0.36f), goodDim = hsl(hGood, 0.4f, 0.78f), freeze = hsl(hFreeze, 0.7f, 0.48f),
            miss = muted, future = hairline,
        )
    }

    /** A stone: its tint and the lightness ladder (ground, surface, hairline, muted, wall) for each face. */
    class Stone(val name: String, val hue: Int, val sat: Float, val dark: FloatArray, val light: FloatArray)

    companion object {
        const val PREFIX = "custom_"
        val HUE_NAMES = listOf("Red", "Orange", "Amber", "Lime", "Green", "Teal", "Cyan", "Blue", "Indigo", "Violet", "Magenta", "Rose")
        private val DARK = floatArrayOf(0.05f, 0.09f, 0.15f, 0.26f, 0.07f)
        private val LIGHT = floatArrayOf(0.94f, 0.99f, 0.88f, 0.76f, 0.90f)
        val STONES = listOf(
            Stone("Neutral", 220, 0.04f, DARK, LIGHT),
            Stone("Warm", 30, 0.14f, DARK, LIGHT),
            Stone("Cool", 215, 0.28f, DARK, LIGHT),
            Stone("Green", 140, 0.20f, DARK, LIGHT),
            Stone("Purple", 268, 0.24f, DARK, LIGHT),
            Stone("Black", 0, 0f, floatArrayOf(0f, 0.06f, 0.13f, 0.27f, 0.03f), floatArrayOf(1f, 0.96f, 0.87f, 0.71f, 0.94f)),
        )

        // Crockford-style alphabet: no I, L, O or U, so a code read aloud survives.
        private const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"

        private fun check(v: Int) = (v * 7 + (v shr 5) * 3 + 11) and 0xFF

        fun encode(hue: Int, stone: Int): String {
            val v = (((hue % 360) + 360) % 360) * 8 + stone.coerceIn(0, 5)
            var w = (v shl 8) or check(v)
            val sb = StringBuilder()
            repeat(4) { sb.append(ALPHABET[w and 31]); w = w shr 5 }
            return "SL-" + sb.reverse()
        }

        /** Hue and stone from a code, or null when it is not one of ours. Case and the SL- prefix are forgiven. */
        fun decode(code: String): Pair<Int, Int>? {
            val body = code.trim().uppercase().removePrefix("SL-").removePrefix("SL").replace("-", "")
            if (body.length != 4) return null
            var w = 0
            for (ch in body) { val i = ALPHABET.indexOf(ch); if (i < 0) return null; w = (w shl 5) or i }
            val v = w shr 8
            if ((w and 0xFF) != check(v)) return null
            val hue = v / 8; val stone = v % 8
            if (hue >= 360 || stone > 5) return null
            return hue to stone
        }

        fun fromCode(code: String, id: String): CustomLook? = decode(code)?.let { (h, s) -> CustomLook(id, "", h, s).let { it.copy(name = it.autoName) } }

        /** HSL to opaque ARGB. */
        fun hsl(h: Int, s: Float, l: Float): Int {
            val hh = (((h % 360) + 360) % 360) / 60f
            val c = (1 - abs(2 * l - 1)) * s
            val x = c * (1 - abs(hh % 2 - 1))
            val m = l - c / 2
            val (r, g, b) = when (hh.toInt()) {
                0 -> Triple(c, x, 0f); 1 -> Triple(x, c, 0f); 2 -> Triple(0f, c, x)
                3 -> Triple(0f, x, c); 4 -> Triple(x, 0f, c); else -> Triple(c, 0f, x)
            }
            fun ch(v: Float) = ((v + m) * 255).roundToInt().coerceIn(0, 255)
            return (0xFF shl 24) or (ch(r) shl 16) or (ch(g) shl 8) or ch(b)
        }

        /** Hue in degrees and lightness of an ARGB int; for tests and the hue ring knob. */
        fun hueOf(argb: Int): Float {
            val r = ((argb shr 16) and 0xFF) / 255f; val g = ((argb shr 8) and 0xFF) / 255f; val b = (argb and 0xFF) / 255f
            val max = maxOf(r, g, b); val min = minOf(r, g, b); val d = max - min
            if (d == 0f) return 0f
            val h = when (max) { r -> ((g - b) / d) % 6; g -> (b - r) / d + 2; else -> (r - g) / d + 4 } * 60
            return if (h < 0) h + 360 else h
        }

        fun lightnessOf(argb: Int): Float {
            val r = ((argb shr 16) and 0xFF) / 255f; val g = ((argb shr 8) and 0xFF) / 255f; val b = (argb and 0xFF) / 255f
            return (maxOf(r, g, b) + minOf(r, g, b)) / 2
        }
    }
}
