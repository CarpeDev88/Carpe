# CARPE product and engineering boundaries

CARPE serves the user's chosen life goals above engagement, retention, or commercial outcomes. Keep it useful without optional permissions or AI.

- Do not add ads, paid placement, data sales, engagement loops, or features intended to maximize time in CARPE.
- Request access only for a user-visible capability, explain it before the Android prompt, and keep it optional and reversible.
- Keep device observations on-device unless the user makes a separate, informed choice to share them. Never send screen captures, OCR text, notification content, or usage history to an AI provider.
- Describe algorithm insights as observations with evidence and uncertainty. Do not claim access to an app's internal ranking or intent.
- Let users inspect, correct, clear, and decline CARPE's conclusions. Prefer an action that helps the user leave the app when that serves their goal.
- AI helper output is advisory. It cannot approve, merge, deploy, or claim checks passed without evidence.

Run `gradle testDebugUnitTest` and `gradle assembleDebug` for Android changes. Add focused tests for logic with meaningful edge cases. Review permissions, data retention, accessibility, and user-facing uncertainty for each new capability.
