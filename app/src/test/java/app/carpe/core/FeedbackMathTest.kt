package app.carpe.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackMathTest {
    @Test
    fun emptyFeedbackStartsNeutral() {
        assertEquals(0.5f, FeedbackMath.smoothedHelpfulRate(0, 0), 0.0001f)
    }

    @Test
    fun earlyRatingsAreDampedByBalancedPrior() {
        assertEquals(0.6f, FeedbackMath.smoothedHelpfulRate(1, 1), 0.0001f)
        assertEquals(0.4f, FeedbackMath.smoothedHelpfulRate(0, 1), 0.0001f)
    }

    @Test
    fun moreRatingsMoveTheEstimateTowardTheUsersPattern() {
        val oneHelpful = FeedbackMath.smoothedHelpfulRate(1, 1)
        val threeHelpful = FeedbackMath.smoothedHelpfulRate(3, 4)
        assertTrue(threeHelpful > oneHelpful)
        assertEquals(0.625f, threeHelpful, 0.0001f)
    }

    @Test
    fun invalidCountsAreClamped() {
        assertEquals(0.5f, FeedbackMath.smoothedHelpfulRate(-1, -3), 0.0001f)
        assertEquals(0.75f, FeedbackMath.smoothedHelpfulRate(5, 2), 0.0001f)
    }
}
