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

## GitHub AI helpers

After this change is merged, repository collaborators can ask for help on GitHub by commenting `/carpe-help` on an issue or `/carpe-review` on a pull request. The helpers only post advisory comments; they cannot edit code, approve, merge, or deploy. They run only on explicit commands from repository collaborators.

To enable them, add a `GEMINI_API_KEY` Actions secret in **Settings → Secrets and variables → Actions**. Never put the key in an issue or commit. The default model is `gemini-3.6-flash`; a repository variable named `GEMINI_MODEL` can override it. An explicit command sends the issue title, description, command comment, or pull request diff to Google's Gemini API, so don't invoke the bot on confidential or sensitive content. API quotas and pricing depend on Google's current account terms. If no key is configured, the workflow replies with setup instructions.

## Status

v0.24.0 Android alpha. Mirror can optionally compare up to five user-started screen samples by app/feed label using aggregate counts only; comparison is off by default and users can clear local summaries. Captured frames and recognized text remain transient and are never retained or sent to AI. The orange-and-white interface keeps the Today composer, user-chosen goals, and offline actions central. Coach uses local goals and optional usage access; Focus supports deliberate breaks; Mirror now labels time as an observed cue, explains its limits, and lets users record whether app use matched their intention or clear that local feedback. Mirror reports can also carry an optional user-entered app/feed label and show visible-cue counts as a proportion of the short sample; labels and results stay on-device. Visible-feed audits remain user-started and on-device. Optional screen audits require a fresh Android screen-sharing choice each time, show an ongoing notification with Stop, and end after at most two minutes. On-device text recognition keeps only aggregate counts; captured frames and OCR text are not saved or sent to AI. A sample can identify visible labels such as Sponsored or Suggested for you, but cannot reveal or prove an app's internal ranking logic. CARPE does not block apps or alter their feeds. Mirror stops polling while idle, and results from a stopped screen-audit session cannot affect a later report. Recipe ideas are bundled on-device: search ingredients, approximate time, budget, and broad diet labels without opening a web browser or sending the search elsewhere.

AI connection without a CARPE server: open **Me → AI & privacy**, use **Get a Gemini API key in AI Studio**, create a key, paste it into CARPE, save it, and tap **Test AI**. The direct connection uses Google AI Studio's free-tier Gemini API and does not need Cloud Run or linked Cloud billing. Free-tier quotas apply, and Google may use free-tier prompts to improve its products. Only the current request and optional saved profile preferences are sent to AI; earlier chat turns stay on-device. Don't send sensitive details. CARPE encrypts the key with Android Keystore, but client-side keys can still be extracted; this setup is intended for private testing. For a shared/public release, use a trusted backend proxy that keeps provider keys server-side.

An optional trusted CARPE service can still be configured in **Me → AI & privacy** using an HTTPS `/v1/ask` URL. To deploy the included Cloud Run backend, run `bash deploy-carpe.sh` from Google Cloud Shell; Cloud Run requires linked project billing.

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
- Screen inspection is user-started per session, time-limited, visible while active, processed locally, and summarized without retaining captured images or OCR text.
- CARPE reports observable cues and uncertainty; it does not claim access to a platform's internal recommendation algorithm.
- AI assessments should distinguish beneficial technology use from detrimental or unwanted use rather than treating all screen time as harmful.
- Users can question, correct, or reject Carpe's interpretation of their behavior.
- Success is measured by technology serving the user's life, including when the best outcome is spending less time in Carpe itself.
