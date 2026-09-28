package app.carpe.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeCatalogTest {
    @Test
    fun ingredientSearchRanksMatchingOfflineRecipesFirst() {
        val results = RecipeCatalog.search("eggs, spinach, rice")
        assertEquals("Spinach and egg rice", results.first().title)
        assertTrue(results.size > 1)
    }

    @Test
    fun timeBudgetAndDietFiltersWorkTogether() {
        val results = RecipeCatalog.search("chickpeas rice vegan gluten-free budget 20 minutes")
        assertEquals("Chickpea and tomato rice bowl", results.first().title)
        assertTrue(results.all { it.minutes <= 20 && it.budgetTier == 1 })
        assertTrue(results.all { it.dietTags.containsAll(setOf("vegan", "gluten-free")) })
    }

    @Test
    fun emptySearchOffersShortStarterIdeas() {
        val results = RecipeCatalog.search("")
        assertEquals(8, results.size)
        assertTrue(results.first().minutes <= results.last().minutes)
    }

    @Test
    fun unrelatedIngredientDoesNotPretendThereIsAMatch() {
        assertTrue(RecipeCatalog.search("dragonfruit saffron").isEmpty())
    }
}
