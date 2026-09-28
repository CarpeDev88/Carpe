package app.carpe.core

import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAssistantTest {
    @Test
    fun onlyOneUnclassifiedMessageKeepsCookingContext() {
        assertTrue(LocalAssistant.resolveIntent(CarpeIntent.UNKNOWN, true) == CarpeIntent.COOK)
        assertTrue(LocalAssistant.resolveIntent(CarpeIntent.UNKNOWN, false) == CarpeIntent.UNKNOWN)
        assertTrue(LocalAssistant.resolveIntent(CarpeIntent.MOVE, true) == CarpeIntent.MOVE)
    }

    @Test
    fun cookingPromptAsksForUsefulConstraints() {
        val reply = LocalAssistant.reply(CarpeIntent.COOK, "Help me cook a meal")

        assertTrue(reply.contains("ingredients", ignoreCase = true))
        assertTrue(reply.contains("time limit", ignoreCase = true))
        assertTrue(reply.contains("Find recipes"))
    }

    @Test
    fun cookingFollowupRespondsToProvidedIngredients() {
        val reply = LocalAssistant.reply(CarpeIntent.COOK, "eggs, spinach, rice")

        assertTrue(reply.contains("those ingredients", ignoreCase = true))
        assertTrue(reply.contains("cooking time", ignoreCase = true))
    }

    @Test
    fun unknownPromptIsHonestAboutOfflineLimitsAndOffersActions() {
        val reply = LocalAssistant.reply(CarpeIntent.UNKNOWN, "What should I do?")

        assertTrue(reply.contains("working offline", ignoreCase = true))
        assertTrue(reply.contains("focused work", ignoreCase = true))
    }
}
