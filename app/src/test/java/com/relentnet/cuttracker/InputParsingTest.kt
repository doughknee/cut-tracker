package com.relentnet.cuttracker

import com.relentnet.cuttracker.ui.components.int
import com.relentnet.cuttracker.ui.components.matchesQuery
import com.relentnet.cuttracker.ui.components.num
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Numbers and searches the way people type them on a phone keyboard. */
class InputParsingTest {
    @Test fun numbersAsTyped() {
        assertEquals(181.4, "181.4".num())
        assertEquals(181.4, "181,4".num())
        assertEquals(2250.0, "2,250".num())
        assertEquals(12500.5, "12,500.5".num())
        assertEquals(165.0, " 165 g".num())
        assertEquals(33, "32.5".int())
        assertNull("".num())
        assertNull("abc".num())
    }

    @Test fun searchIgnoresOrderCaseAndAccents() {
        assertTrue(matchesQuery("rice chicken", "Chicken and rice bowl"))
        assertTrue(matchesQuery("creme", "Crème fraîche oats"))
        assertTrue(matchesQuery("oats", "Breakfast", "Overnight oats with whey"))
        assertTrue(matchesQuery("", "anything"))
        assertFalse(matchesQuery("steak salmon", "Steak + salad"))
    }
}
