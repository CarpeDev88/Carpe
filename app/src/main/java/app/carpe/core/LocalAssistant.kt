package app.carpe.core

/** Short, actionable guidance that works without a configured cloud service. */
object LocalAssistant {
    fun resolveIntent(routed: CarpeIntent, awaitingCookFollowup: Boolean): CarpeIntent =
        if (routed == CarpeIntent.UNKNOWN && awaitingCookFollowup) CarpeIntent.COOK else routed

    fun reply(intent: CarpeIntent, request: String): String = when (intent) {
        CarpeIntent.COOK -> cookReply(request)
        CarpeIntent.FOCUS -> "Choose one task you can finish or advance in 25 minutes. Write down the first physical step, silence anything that can wait, and start the timer. What task do you want to work on?"
        CarpeIntent.MOVE -> "Make it easy to begin: take a 10-minute walk, stretch, or do a short set of bodyweight movements. Pick an option that fits your energy and any physical limits you have. Which sounds right today?"
        CarpeIntent.SPEND -> "Pause before buying. What need would this meet, do you already have something that works, and would waiting until tomorrow change your choice? You can decide after weighing those answers."
        CarpeIntent.REFLECT -> "Try a small reset: put the phone down for 10 minutes, get some water, and choose one thing you meant to do. If you want, tell me what pulled you into the loop and we can make a practical plan."
        CarpeIntent.OFFLINE_ACTIVITY -> "Choose something offline that fits the time and energy you have: step outside, make a drink, stretch, read a few pages, cook, or talk with someone. Pick one, decide what done-for-now means, and close CARPE. You do not need to reduce screen time for this goal to count."
        CarpeIntent.GOAL -> goalReply(request)
        CarpeIntent.CONTENT_GOAL -> "If reducing porn use is a goal you chose, make a private pause plan: notice the situation you want to change, choose one alternative you would actually welcome, and decide what support or device settings you want to use. CARPE does not inspect browsing or viewing history, and you can change or ignore this plan."
        CarpeIntent.POLITICAL_BALANCE -> "Choose one issue. Write down the strongest good-faith argument from more than one viewpoint, check which claims are factual and which are values, then reach your own conclusion. You do not need to agree with either side."
        CarpeIntent.UNKNOWN -> "I’m working offline, so I can’t answer every open-ended question yet. I can still help you plan a meal, choose movement, start focused work, pause a purchase, or step away from a loop. What would be most useful right now?"
    }

    private fun cookReply(request: String): String {
        val details = request.trim()
        val hasDetails = details.split(',', '\n', ';').count { it.trim().length >= 2 } >= 2 ||
            (details.contains("i have ", ignoreCase = true) &&
                !details.contains("what i have", ignoreCase = true)) ||
            details.contains("ingredients are", ignoreCase = true)
        return if (hasDetails) {
            "With those ingredients, start by choosing a simple method that fits them: roast or sauté vegetables, cook any grain or pasta separately, and add a protein if you have one. Season, taste, and adjust as you go. Here are a few matching starter ideas below. What cooking time and equipment do you have? The matches stay on this device."
        } else {
            "Tell me what ingredients you have, plus any time limit, budget, dietary needs, or cooking equipment that matter. CARPE will show matching starter recipes below and works offline."
        }
    }

    private fun goalReply(request: String): String {
        val prefix = "this goal: "
        val suffix = ". Keep it practical and let me decide whether to do it."
        val chosenGoal = request.substringAfter(prefix, "").substringBefore(suffix).trim().take(80)
        val focus = chosenGoal.takeIf { it.isNotBlank() }?.let { "For “$it,” " }.orEmpty()
        return focus + "choose one step small enough to begin today. Decide when you will do it and what 'done for now' means, then close CARPE and try it. You can log a check-in later if that helps you."
    }
}
