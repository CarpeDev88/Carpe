# CARPE Intelligence Service

Small server-side gateway between the CARPE Android app and Gemini. Provider credentials stay on the server.

## Contract

- GET /health (HTTP 503 if the provider key is missing)
- POST /v1/ask with { message, profile, history, purpose }
- response: { response }

## Required environment

- GEMINI_API_KEY (secret)
- GEMINI_MODEL (optional; defaults to gemini-2.5-flash)

## Cloud Run

Before deploying, link an active billing account to project `phrasal-truck-368514` in [Google Cloud Billing](https://console.cloud.google.com/billing/linkedaccount?project=phrasal-truck-368514). From the repository root in Cloud Shell, run `bash deploy-carpe.sh`; it checks project access and billing, enables the APIs, deploys the service, and checks a real AI response. It prompts for a Gemini key only if `carpe-gemini-api-key` has no enabled version. To replace an invalid key, run `CARPE_ROTATE_KEY=1 bash deploy-carpe.sh`. An error naming project `670897038764` refers to the same project's numeric identifier; it does not mean the script used a different project ID.

For a manual deployment from this directory:

    gcloud run deploy carpe-intelligence --source . --region us-west1 --allow-unauthenticated --set-secrets GEMINI_API_KEY=GEMINI_API_KEY:latest

Then set the GitHub Actions repository variable CARPE_AI_ENDPOINT to:

    https://YOUR-CLOUD-RUN-URL/v1/ask

The Android build workflow injects that URL at build time.

For a public alpha endpoint, add App Check or another attestation/authentication layer before broad distribution. Do not put the Gemini API key in the Android app.
