package com.relentnet.cuttracker

import com.relentnet.cuttracker.data.CustomLook
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The share code must round-trip every hue on every stone and refuse a typo; the derived colors must sit where the looks sit. */
class CustomLookTest {
    private fun hueGap(a: Float, b: Float): Float { val d = abs(a - b) % 360; return if (d > 180) 360 - d else d }

    @Test fun everyCodeRoundTrips() {
        for (h in 0 until 360) for (st in 0..5) {
            val code = CustomLook.encode(h, st)
            assertEquals(7, code.length, code)
            assertTrue(code.startsWith("SL-"), code)
            assertEquals(h to st, CustomLook.decode(code), code)
        }
    }

    @Test fun codeForgivesCaseAndPrefix() {
        val code = CustomLook.encode(24, 1)
        assertEquals(24 to 1, CustomLook.decode(code.lowercase()))
        assertEquals(24 to 1, CustomLook.decode(" ${code.removePrefix("SL-")} "))
    }

    @Test fun aTypoIsRefused() {
        val code = CustomLook.encode(200, 3)
        var refused = 0
        for (i in 3 until 7) for (ch in "0123456789ABCDEFGHJKMNPQRSTVWXYZ") {
            if (ch == code[i]) continue
            val typo = code.substring(0, i) + ch + code.substring(i + 1)
            if (CustomLook.decode(typo) != (200 to 3)) refused++
        }
        // 4 positions x 31 other letters. A checksum this size lets at most a handful through.
        assertTrue(refused >= 120, "only $refused of 124 typos refused")
        assertNull(CustomLook.decode("SL-7K2"))
        assertNull(CustomLook.decode("SL-7K2QQ"))
        assertNull(CustomLook.decode("SL-7I2Q"))
    }

    @Test fun pastedCodeGetsAName() {
        val look = CustomLook.fromCode(CustomLook.encode(190, 2), "custom_x")
        assertNotNull(look)
        assertEquals("Cyan on cool", look.name)
        assertEquals("SL-" + CustomLook.encode(190, 2).removePrefix("SL-"), look.code)
    }

    @Test fun sendsAndFreezeFollowTheHue() {
        for (h in listOf(0, 24, 90, 150, 210, 280, 330)) for (st in 0..5) for (dark in listOf(true, false)) {
            val f = CustomLook("c", "", h, st).face(dark)
            assertTrue(hueGap(CustomLook.hueOf(f.accent), h.toFloat()) < 3f, "accent hue off for $h/$st")
            assertTrue(hueGap(CustomLook.hueOf(f.good), (h + 110f) % 360) < 3f, "good hue off for $h/$st")
            assertTrue(hueGap(CustomLook.hueOf(f.freeze), (h + 200f) % 360) < 3f, "freeze hue off for $h/$st")
        }
    }

    @Test fun derivedColorsSitInTheLooksLightnessRange() {
        for (h in 0 until 360 step 15) for (st in 0..5) {
            val d = CustomLook("c", "", h, st).face(true)
            val l = CustomLook("c", "", h, st).face(false)
            // Dark: chalk bright on a stone that stays under 30% lightness. Light: the reverse.
            for (x in listOf(d.accent, d.good, d.freeze)) assertTrue(CustomLook.lightnessOf(x) in 0.5f..0.72f, "dark chalk $h/$st")
            for (x in listOf(d.ground, d.surface, d.wall)) assertTrue(CustomLook.lightnessOf(x) <= 0.12f, "dark stone $h/$st")
            assertTrue(CustomLook.lightnessOf(d.goodDim) in 0.2f..0.4f)
            for (x in listOf(l.accent, l.good, l.freeze)) assertTrue(CustomLook.lightnessOf(x) in 0.3f..0.52f, "light chalk $h/$st")
            for (x in listOf(l.ground, l.surface, l.wall)) assertTrue(CustomLook.lightnessOf(x) >= 0.88f, "light stone $h/$st")
            assertEquals(d.ground, d.onAccent)
            assertEquals(0xFFFFFFFF.toInt(), l.onAccent)
        }
    }

    @Test fun hslRoundTrips() {
        assertEquals(0xFFFF0000.toInt(), CustomLook.hsl(0, 1f, 0.5f))
        assertEquals(0xFF00FF00.toInt(), CustomLook.hsl(120, 1f, 0.5f))
        assertEquals(0xFF0000FF.toInt(), CustomLook.hsl(240, 1f, 0.5f))
        assertEquals(0xFF808080.toInt(), CustomLook.hsl(0, 0f, 0.5f))
        assertEquals(200f, CustomLook.hueOf(CustomLook.hsl(200, 0.8f, 0.6f)), 1.5f)
    }
}
