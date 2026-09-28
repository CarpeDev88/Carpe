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

v0.16 Android alpha. Today offers local guidance and recipe search without an AI service. Coach uses local goals and optional usage access, Focus keeps its countdown across tab switches and app reopen, and Shield lets the user rate apps as helpful, mixed, or distracting. These features are a prototype; CARPE does not yet block other apps or alter their feeds.

Cloud conversation requires a deployed backend. In Google Cloud Shell, from this repository on `main`, run `bash deploy-carpe.sh`. It prints an HTTPS `/v1/ask` URL after a real provider test. Paste that URL in **Me → AI & privacy** in the app, save, and tap **Test AI**. Project billing must be linked before Cloud Run can deploy.

APK updates are currently manual. GitHub Actions debug builds may use different signing keys, so Android can refuse an in-place update; uninstalling an older debug build may erase its local app data. A stable private signing key and distribution channel are required before automatic updates are enabled.


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
