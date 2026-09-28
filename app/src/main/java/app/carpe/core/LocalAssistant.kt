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
        CarpeIntent.UNKNOWN -> "I’m working offline, so I can’t answer every open-ended question yet. I can still help you plan a meal, choose movement, start focused work, pause a purchase, or step away from a loop. What would be most useful right now?"
    }

    private fun cookReply(request: String): String {
        val details = request.trim()
        val hasDetails = details.split(',', '\n', ';').count { it.trim().length >= 2 } >= 2 ||
            (details.contains("i have ", ignoreCase = true) &&
                !details.contains("what i have", ignoreCase = true)) ||
            details.contains("ingredients are", ignoreCase = true)
        return if (hasDetails) {
            "With those ingredients, start by choosing a simple method that fits them: roast or sauté vegetables, cook any grain or pasta separately, and add a protein if you have one. Season, taste, and adjust as you go. For a recipe matched to the exact ingredients, time, budget, or dietary needs, tap Find recipes to search the web. What cooking time and equipment do you have?"
        } else {
            "Tell me what ingredients you have, plus any time limit, budget, dietary needs, or cooking equipment that matter. I can help you narrow it down here; tap Find recipes to search the web for recipes using your request."
        }
    }
}
