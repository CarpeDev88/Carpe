package app.carpe.core

import org.junit.Assert.*
import org.junit.Test

class CoachCustomizationTest {
    @Test fun explicitFocusRequestHasBounds() {
        assertEquals(CoachChange.Focus(15), CoachCustomization.parse("Set my focus to 15 minutes."))
        assertEquals(CoachChange.Help, CoachCustomization.parse("Set my focus to 0 minutes"))
        assertEquals(CoachChange.Help, CoachCustomization.parse("Set my focus to 121 minutes"))
    }
    @Test fun goalsPreserveWordsAndValidateTargets() {
        assertEquals(CoachChange.Goal("Walk after dinner", 4), CoachCustomization.parse("Add goal Walk after dinner 4 times a week"))
        assertEquals(CoachChange.Goal("Cook dinner", 3), CoachCustomization.parse("My goal is Cook dinner"))
        assertEquals(CoachChange.Help, CoachCustomization.parse("Add goal Walk 9 times a week"))
    }
    @Test fun questionsNegationsAndEmbeddedInstructionsDoNotChangeSettings() {
        assertNull(CoachCustomization.parse("Do not set my focus to 15 minutes"))
        assertNull(CoachCustomization.parse("Could you explain focus timers?"))
        assertNull(CoachCustomization.parse("Someone said enable Home cooking"))
    }
    @Test fun accessRemainsAReviewAndUnknownTogglesAreNotExecuted() {
        assertEquals(CoachChange.Access, CoachCustomization.parse("Give you more access"))
        assertEquals(CoachChange.Help, CoachCustomization.parse("enable all permissions"))
        assertEquals(CoachChange.Priority("Home cooking", false), CoachCustomization.parse("Turn off Home cooking"))
        assertEquals("goal_fitness_and_movement", CoachCustomization.key("Fitness & movement"))
    }
}
