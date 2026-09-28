package app.carpe.core

import org.junit.Assert.assertEquals
import org.junit.Test

class CloudDataPolicyTest {
    @Test
    fun cloudContextContainsOnlyCurrentRequestAndExplicitProfileNotes() {
        val context = CloudDataPolicy.forUserRequest(
            "  Help me focus  ",
            "User-chosen goals and preferences:\n• I prefer quiet mornings\nPrivate device log: opened app 42 times\n• I want to cook at home"
        )

        assertEquals("Help me focus", context.userRequest)
        assertEquals(listOf("I prefer quiet mornings", "I want to cook at home"), context.userChosenPreferences)
        assertEquals(null, context.coarseObservation)
    }

    @Test
    fun cloudContextBoundsOversizedRequestsAndProfileNotes() {
        val longPreference = "p".repeat(300)
        val context = CloudDataPolicy.forUserRequest("r".repeat(3000), "• $longPreference")

        assertEquals(2000, context.userRequest.length)
        assertEquals(160, context.userChosenPreferences.single().length)
    }
}
