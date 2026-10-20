package com.relentnet.cuttracker

import com.relentnet.cuttracker.data.DayMode
import com.relentnet.cuttracker.data.PlanItem
import com.relentnet.cuttracker.data.ProgramSpec
import com.relentnet.cuttracker.data.Vocab
import com.relentnet.cuttracker.ui.theme.Palettes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Both modes fill every word, the plain one says no climbing word, and climbing reads exactly as it did. */
class VocabTest {
    private val climbingWord = Regex("\\b(send|sends|sent|hold|holds|climb|climbed|climbing|beta|chalk|chalked|grade|grades|top out|topped)\\b", RegexOption.IGNORE_CASE)

    private fun words(v: Vocab): Map<String, String> = Vocab::class.java.declaredFields
        .filter { it.type == String::class.java }
        .associate { f -> f.isAccessible = true; f.name to f.get(v) as String }

    @Test fun everyKeyIsFilledInBothModes() {
        val keys = words(Vocab.CLIMBING).keys
        assertTrue(keys.size > 30, "reflection found the words")
        for (v in listOf(Vocab.CLIMBING, Vocab.plain("Run"), Vocab.plain(" "))) {
            val w = words(v)
            assertEquals(keys, w.keys)
            for ((k, s) in w) {
                assertTrue(s.isNotBlank(), "$k is blank")
                assertTrue('—' !in s, "$k has an em dash")
            }
        }
    }

    @Test fun plainModeSaysNoClimbingWord() {
        val plain = Vocab.plain("Run")
        for ((k, s) in words(plain)) assertTrue(!climbingWord.containsMatchIn(s), "$k says '$s'")
        for (p in Palettes.all) assertTrue(!climbingWord.containsMatchIn(plain.lookLine(p.id, p.line)), "${p.id} look says '${p.line}'")
        assertEquals("Your colors. Code SL-AB12.", plain.lookLine("custom_x", "Your chalk. Code SL-AB12."))
        for (m in DayMode.entries) assertTrue(!climbingWord.containsMatchIn(plain.day(m)))
        assertEquals("Level 1", plain.level(0))
        assertEquals("1 full day", plain.sendCount(1))
        assertEquals("Last checkpoint. Finish line.", plain.note("Last checkpoint. Top out."))
    }

    @Test fun climbingReadsAsBefore() {
        val v = Vocab.CLIMBING
        assertEquals("V3", v.level(3))
        assertEquals("Climb day", v.day(DayMode.CLIMB))
        assertEquals("Send", v.send)
        assertEquals("Beta", v.beta)
        assertEquals("2 sends", v.sendCount(2))
        assertEquals("1 send", v.sendCount(1))
        assertEquals("Chalk on dark stone. The one the app was drawn in.", v.lookLine("sandstone", Palettes.SANDSTONE.line))
        val session = ProgramSpec.defaultPlans().getValue(DayMode.CLIMB).items.single()
        assertEquals(session, v.item(session))
        assertEquals(v.session, session.label)
        assertEquals(v.sessionSub, session.sub)
    }

    @Test fun theSessionTakesTheUsersWordUntilItIsRenamed() {
        val session = ProgramSpec.defaultPlans().getValue(DayMode.CLIMB).items.single()
        assertEquals("Run", Vocab.plain("Run").item(session).label)
        assertEquals("Run session. Before lunch.", Vocab.plain("Run").item(session).sub)
        assertEquals("Training", Vocab.plain("").item(session).label)
        val renamed = session.copy(label = "Bouldering")
        assertEquals("Bouldering", Vocab.plain("Run").item(renamed).label)
        val other = PlanItem("x", "Climb", "Climbing session. Before lunch.", "", 0)
        assertEquals(other, Vocab.plain("Run").item(other))
    }
}
