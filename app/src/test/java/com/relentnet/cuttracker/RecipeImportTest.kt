package com.relentnet.cuttracker

import com.relentnet.cuttracker.data.RecipeImport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeImportTest {
    private val withNutrition = """
        <html><head><title>x</title>
        <script type="application/ld+json">{"@context":"https://schema.org","@graph":[{"@type":"WebPage","name":"site"},
        {"@type":["Recipe","Thing"],"name":"Chicken tikka bowl","recipeYield":["4 servings"],
         "nutrition":{"@type":"NutritionInformation","calories":"540 kcal","proteinContent":"46 g"}}]}</script>
        </head><body></body></html>
    """.trimIndent()

    private val withoutNutrition = """
        <html><script type='application/ld+json'>
        {"@context":"https://schema.org","@type":"Recipe","name":"Lentil soup","recipeYield":"6"}
        </script></html>
    """.trimIndent()

    @Test fun readsNameServingsAndNutritionFromAGraph() {
        val r = RecipeImport.parse(withNutrition)!!
        assertEquals("Chicken tikka bowl", r.name)
        assertEquals(46, r.protein)
        assertEquals(540, r.calories)
        assertEquals(4, r.servings)
    }

    @Test fun aPageWithoutNutritionStillGivesTheName() {
        val r = RecipeImport.parse(withoutNutrition)!!
        assertEquals("Lentil soup", r.name)
        assertNull(r.protein)
        assertNull(r.calories)
        assertEquals(6, r.servings)
    }

    @Test fun aPageWithoutARecipeIsNull() {
        assertNull(RecipeImport.parse("<html><script type=\"application/ld+json\">{\"@type\":\"Article\"}</script></html>"))
        assertNull(RecipeImport.parse("<html><body>plain</body></html>"))
    }

    @Test fun onlyHttpsLinksAreUsable() {
        assertTrue(RecipeImport.isUsable("https://example.com/recipe"))
        assertFalse(RecipeImport.isUsable("http://example.com/recipe"))
        assertFalse(RecipeImport.isUsable("example.com"))
    }
}
