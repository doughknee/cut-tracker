package com.relentnet.cuttracker

import com.relentnet.cuttracker.data.FoodTable
import com.relentnet.cuttracker.data.Ingredient
import com.relentnet.cuttracker.data.OpenFoodFacts
import com.relentnet.cuttracker.data.kcalSum
import com.relentnet.cuttracker.data.proteinSum
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FoodTableTest {
    /** The bundled table, read the way the app reads it. Unit tests run with the app module as the working directory. */
    private val foods by lazy { FoodTable.parse(File("src/main/res/raw/fdc_tsv.gz").inputStream()) }

    @Test fun bundledTableParsesAndFindsChickenBreast() {
        assertTrue("table has ${foods.size} foods", foods.size > 5000)
        val hits = FoodTable.search(foods, "chicken breast")
        assertTrue(hits.isNotEmpty())
        assertTrue(hits.all { "chicken" in it.key && "breast" in it.key })
        val raw = hits.first { "raw" in it.key }
        assertTrue("protein per 100 g ${raw.protein100}", raw.protein100 in 18.0..26.0)
        assertTrue(raw.kcal100 in 90.0..160.0)
    }

    @Test fun portionsGiveTheStartingGrams() {
        val egg = FoodTable.search(foods, "egg whole raw fresh").first()
        assertEquals("1 large", egg.portions.first().label)
        assertEquals(50.0, egg.defaultGrams, 0.01)
        assertEquals(100.0, FoodTable.parseLine("Thing\t1\t2\t")!!.defaultGrams, 0.01)
    }

    @Test fun twoIngredientsAddUp() {
        val lines = listOf(Ingredient("Chicken", 200.0, 31.0, 165.0), Ingredient("Rice", 150.0, 2.7, 130.0))
        assertEquals(66, lines.proteinSum())   // 62 + 4.05
        assertEquals(525, lines.kcalSum())     // 330 + 195
    }

    @Test fun searchNeedsEveryWordAndEmptyQueryFindsNothing() {
        assertTrue(FoodTable.search(foods, "").isEmpty())
        assertTrue(FoodTable.search(foods, "zzzz qqqq").isEmpty())
    }

    @Test fun openFoodFactsAnswerParses() {
        val body = """{"code":"3017620422003","status":1,"product":{"product_name":"Nutella","serving_size":"15 g","serving_quantity":"15",
            "nutriments":{"proteins_100g":6.3,"energy-kcal_100g":539,"energy_100g":2255}}}"""
        val p = OpenFoodFacts.parse(body)!!
        assertEquals("Nutella", p.name)
        assertEquals(6.3, p.protein100!!, 0.01)
        assertEquals(539.0, p.kcal100!!, 0.01)
        assertEquals(15.0, p.servingGrams!!, 0.01)
        assertNull(OpenFoodFacts.parse("""{"code":"1","status":0,"status_verbose":"product not found"}"""))
        val kj = OpenFoodFacts.parse("""{"product":{"product_name":"X","serving_size":"1 cup (240 ml)","nutriments":{"energy_100g":418.4}}}""")!!
        assertEquals(100.0, kj.kcal100!!, 0.01)
        assertNull(kj.servingGrams)
        assertTrue(OpenFoodFacts.isBarcode("3017620422003"))
        assertTrue(!OpenFoodFacts.isBarcode("https://x"))
    }
}
