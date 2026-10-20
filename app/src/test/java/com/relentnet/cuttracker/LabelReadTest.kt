package com.relentnet.cuttracker

import com.relentnet.cuttracker.data.LabelRead
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LabelReadTest {
    /** A US panel, read in row order. */
    private val us = """
        Nutrition Facts
        8 servings per container
        Serving size 2/3 cup (55g)
        Amount per serving
        Calories 230
        % Daily Value*
        Total Fat 8g 10%
        Saturated Fat 1g 5%
        Sodium 160mg 7%
        Total Carbohydrate 37g 13%
        Dietary Fiber 4g 14%
        Total Sugars 12g
        Protein 3g
        Vitamin D 2mcg 10%
    """.trimIndent()

    /** An EU table: energy in kJ and kcal, decimal commas, per 100 g. */
    private val eu = """
        NUTRITION INFORMATION
        Typical values per 100g
        Energy 1046 kJ / 250 kcal
        Fat 9,5 g
        of which saturates 2,1 g
        Carbohydrate 30 g
        of which sugars 4,2 g
        Fibre 3,0 g
        Protein 12,5 g
        Salt 0,8 g
    """.trimIndent()

    /** OCR that split the columns: the words on one line, their numbers on the next. */
    private val split = """
        Nutrition Facts
        Serving Size
        1 scoop (31g)
        Servings Per Container 30
        Calories
        120
        Total Fat 1.5g
        Cholesterol 55mg
        Sodium 70mg
        Total Carbohydrate 3g
        Protein
        24g
    """.trimIndent()

    @Test fun readsAUsPanel() {
        val f = LabelRead.parse(us)
        assertEquals(3, f.protein)
        assertEquals(230, f.calories)
        assertEquals("2/3 cup (55g)", f.serving)
        assertEquals(listOf("Calories 230", "Protein 3g", "Serving size 2/3 cup (55g)").sorted(), f.lines.sorted())
    }

    @Test fun readsAnEuTableWithDecimalCommasAndKcal() {
        val f = LabelRead.parse(eu)
        assertEquals(13, f.protein)
        assertEquals(250, f.calories)
        assertEquals("100g", f.serving)
        assertTrue(f.lines.contains("Energy 1046 kJ / 250 kcal"))
    }

    @Test fun readsColumnSplitLines() {
        val f = LabelRead.parse(split)
        assertEquals(24, f.protein)
        assertEquals(120, f.calories)
        assertEquals("1 scoop (31g)", f.serving)
    }

    @Test fun nothingOnAnIngredientsList() {
        val f = LabelRead.parse("INGREDIENTS: Whey protein isolate, cocoa, natural flavors.\nContains milk.")
        assertNull(f.protein); assertNull(f.calories); assertFalse(f.any)
    }
}
