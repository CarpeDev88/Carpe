# CARPE Intelligence Service

Small server-side gateway between the CARPE Android app and Gemini. Provider credentials stay on the server.

## Contract

- GET /health (HTTP 503 if the provider key is missing)
- POST /v1/ask with { message, profile, history, purpose }
- response: { response }

## Required environment

- GEMINI_API_KEY (secret)
- GEMINI_MODEL (optional; defaults to gemini-2.5-flash)
- CARPE_APP_TOKEN (required for `/v1/ask`; use a private, random token)
- CARPE_RATE_LIMIT_MAX (optional; requests per client per minute, defaults to 10)

## Cloud Run

Before deploying, link an active billing account to project `phrasal-truck-368514` in [Google Cloud Billing](https://console.cloud.google.com/billing/linkedaccount?project=phrasal-truck-368514). From the repository root in Cloud Shell, run `bash deploy-carpe.sh`; it checks project access and billing, enables the APIs, deploys the service, and checks a real AI response. It prompts for a Gemini key only if `carpe-gemini-api-key` has no enabled version. It creates a random `CARPE_APP_TOKEN` secret on first deploy and prints the token so you can paste it into **Me → AI & privacy → Private service access token**. Treat the token like a password. To rotate it, run `CARPE_ROTATE_APP_TOKEN=1 bash deploy-carpe.sh`; update CARPE with the newly printed token. To replace an invalid Gemini key, run `CARPE_ROTATE_KEY=1 bash deploy-carpe.sh`. An error naming project `670897038764` refers to the same project's numeric identifier; it does not mean the script used a different project ID.

For a manual deployment from this directory:

    gcloud run deploy carpe-intelligence --source . --region us-west1 --allow-unauthenticated --set-secrets GEMINI_API_KEY=GEMINI_API_KEY:latest,CARPE_APP_TOKEN=CARPE_APP_TOKEN:latest

Then set the GitHub Actions repository variable CARPE_AI_ENDPOINT to:

    https://YOUR-CLOUD-RUN-URL/v1/ask

The Android build workflow injects that URL at build time. Save the matching access token in CARPE's AI & privacy settings. The backend applies a per-process IP request limit as a second layer; Cloud Run instances do not share this in-memory counter, so use a platform-level limiter or attestation before broad public distribution. The app token is not a substitute for per-user identity or platform abuse protection.

Do not put either provider credentials or the backend access token in the APK or source code. The app stores a user-entered service token encrypted with Android Keystore, but any credential used by a mobile client can be extracted from a compromised device. The shared token and in-memory limiter are suitable only for a small private alpha; add per-user authentication, attestation, and a shared rate limiter before broader distribution. Do not put the Gemini API key in the Android app when using the backend.
