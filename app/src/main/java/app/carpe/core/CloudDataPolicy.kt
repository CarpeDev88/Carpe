package app.carpe.core

/**
 * The ONLY categories of personal context CARPE is allowed to send to cloud AI.
 * Raw app events, package names, notification contents, sensitive app labels,
 * browsing history, contacts, and raw behavior logs are deliberately absent.
 *
 * Any future cloud feature must express its payload through this contract.
 */
data class CloudAiContext(
 val userRequest: String,
 val userChosenGoals: List<String> = emptyList(),
 val userChosenPreferences: List<String> = emptyList(),
 val coarseObservation: String? = null
)

object CloudDataPolicy {
 fun forUserRequest(message:String, profileSummary:String = ""):CloudAiContext = sanitize(
  CloudAiContext(
   userRequest=message,
   userChosenPreferences=profileSummary.lineSequence()
    .map{it.trim()}
    .filter{it.startsWith("• ")}
    .map{it.removePrefix("• ").trim()}
    .filter{it.isNotBlank()}
    .toList()
  )
 )
 fun sanitize(c: CloudAiContext): CloudAiContext = c.copy(
  userRequest=c.userRequest.trim().take(2000),
  userChosenGoals=c.userChosenGoals.map{it.take(120)}.take(12),
  userChosenPreferences=c.userChosenPreferences.map{it.take(160)}.take(20),
  coarseObservation=c.coarseObservation?.take(240)
 )
 fun forbiddenDescription() =
  "CARPE never sends raw usage events, installed-app/package lists, notification content, sensitive app labels, contacts, or raw behavioral history to cloud AI."
}
