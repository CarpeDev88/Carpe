# Carpe

**The counter-algorithm.**

Carpe is an Android-first experiment in technology that optimizes for the user's life instead of engagement.

## Prototype principles

- Useful with minimal permissions.
- Optional access is explicit and reversible.
- No infinite feed, ads, or engagement-maximizing streaks.
- Promote deliberate offline action: movement, cooking, meaningful work, saving, and intentional technology use.
- Measure success by attention returned to the user.

## Build

The GitHub Actions workflow builds a debug APK on every push to `main`. Open **Actions → Build Android APK → latest successful run → Artifacts** and download `carpe-debug-apk`.

Local build requires JDK 17 and Gradle 8.9:

```
gradle assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## Status

v0.1 prototype. The Shield screen currently demonstrates permission architecture; OS-level interventions will be added incrementally and remain opt-in.


## Public-benefit commitment

Carpe is being built for human benefit, not profit maximization.

Product constraints:
- No advertising.
- No sale of personal data.
- No paid placement or pay-to-influence recommendations.
- No engagement optimization designed to keep people inside Carpe.
- AI recommendations optimize for goals the user chooses, not commercial outcomes.
- Optional data access requires informed opt-in and Carpe remains useful when access is declined.
- AI assessments should distinguish beneficial technology use from detrimental or unwanted use rather than treating all screen time as harmful.
- Users can question, correct, or reject Carpe's interpretation of their behavior.
- Success is measured by technology serving the user's life, including when the best outcome is spending less time in Carpe itself.
