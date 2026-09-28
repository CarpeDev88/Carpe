# CARPE Intelligence Service

Small server-side gateway between the CARPE Android app and Gemini. Provider credentials stay on the server.

## Contract

- GET /health
- POST /v1/ask with { message, profile, history, purpose }
- response: { response }

## Required environment

- GEMINI_API_KEY (secret)
- GEMINI_MODEL (optional; defaults to gemini-2.5-flash)

## Cloud Run

From this directory:

    gcloud run deploy carpe-intelligence --source . --region us-west1 --allow-unauthenticated --set-secrets GEMINI_API_KEY=GEMINI_API_KEY:latest

Then set the GitHub Actions repository variable CARPE_AI_ENDPOINT to:

    https://YOUR-CLOUD-RUN-URL/v1/ask

The Android build workflow injects that URL at build time.

For a public alpha endpoint, add App Check or another attestation/authentication layer before broad distribution. Do not put the Gemini API key in the Android app.
