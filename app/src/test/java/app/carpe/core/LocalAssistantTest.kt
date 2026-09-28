package app.carpe.core

import org.junit.Assert.assertEquals
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
        assertTrue(reply.contains("below"))
        assertTrue(reply.contains("offline", ignoreCase = true))
    }

    @Test
    fun cookingFollowupRespondsToProvidedIngredients() {
        val reply = LocalAssistant.reply(CarpeIntent.COOK, "eggs, spinach, rice")

        assertTrue(reply.contains("starter ideas", ignoreCase = true))
        assertTrue(reply.contains("cooking time", ignoreCase = true))
    }

    @Test
    fun unknownPromptIsHonestAboutOfflineLimitsAndOffersActions() {
        val reply = LocalAssistant.reply(CarpeIntent.UNKNOWN, "What should I do?")

        assertTrue(reply.contains("working offline", ignoreCase = true))
        assertTrue(reply.contains("focused work", ignoreCase = true))
    }

    @Test
    fun userGoalCanBeTurnedIntoASmallNextStepWithoutCloudAi() {
        val prompt = GoalStepPlanner.prompt("Walk after dinner\n")
        val intent = IntentRouter().classify(prompt).intent

        assertEquals(CarpeIntent.GOAL, intent)
        assertTrue(LocalAssistant.reply(intent, prompt).contains("close CARPE"))
    }

    @Test
    fun sensitiveGoalsOfferPrivateUserLedOfflineGuidance() {
        val router = IntentRouter()
        val contentIntent = router.classify("Help me reduce porn use").intent
        val politicsIntent = router.classify("Help me compare political viewpoints").intent

        assertEquals(CarpeIntent.CONTENT_GOAL, contentIntent)
        assertTrue(LocalAssistant.reply(contentIntent, "").contains("does not inspect browsing"))
        assertEquals(CarpeIntent.POLITICAL_BALANCE, politicsIntent)
        assertTrue(LocalAssistant.reply(politicsIntent, "").contains("your own conclusion"))
    }
}
