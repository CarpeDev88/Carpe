#!/usr/bin/env bash
set -euo pipefail

# Run this from a Google Cloud Shell session signed in to the CARPE project.
PROJECT_ID="phrasal-truck-368514"
REGION="us-west1"
SERVICE="carpe-intelligence"
SECRET="carpe-gemini-api-key"
APP_TOKEN_SECRET="carpe-app-access-token"

if ! gcloud projects describe "$PROJECT_ID" --format='value(projectId)' >/dev/null; then
  echo "Cannot access Google Cloud project $PROJECT_ID. Check the selected Cloud Shell account and project ID." >&2
  exit 1
fi

# Check billing before prompting for an API key or creating any resources.
if BILLING_ENABLED="$(gcloud billing projects describe "$PROJECT_ID" --format='value(billingEnabled)' 2>/dev/null)"; then
  if [[ "$BILLING_ENABLED" != "True" && "$BILLING_ENABLED" != "true" ]]; then
    echo "Billing is not enabled for $PROJECT_ID. Link an active billing account at https://console.cloud.google.com/billing/linkedaccount?project=$PROJECT_ID and rerun this script." >&2
    exit 1
  fi
else
  echo "Could not check billing status; attempting API activation. If it fails, check billing at https://console.cloud.google.com/billing/linkedaccount?project=$PROJECT_ID" >&2
fi

if ! gcloud services enable run.googleapis.com cloudbuild.googleapis.com artifactregistry.googleapis.com secretmanager.googleapis.com generativelanguage.googleapis.com --project "$PROJECT_ID"; then
  echo "API activation failed. Check project billing at https://console.cloud.google.com/billing/linkedaccount?project=$PROJECT_ID, then rerun this script." >&2
  exit 1
fi
if ! gcloud secrets describe "$SECRET" --project "$PROJECT_ID" >/dev/null 2>&1; then
  gcloud secrets create "$SECRET" --replication-policy=automatic --project "$PROJECT_ID"
fi
if [[ "${CARPE_ROTATE_KEY:-0}" == "1" || -z "$(gcloud secrets versions list "$SECRET" --project "$PROJECT_ID" --filter='state=ENABLED' --format='value(name)' --limit=1)" ]]; then
  printf 'Paste the Gemini API key (input hidden), then press Enter: ' >&2
  IFS= read -r -s GEMINI_KEY
  printf '\n' >&2
  if [[ -z "$GEMINI_KEY" ]]; then
    echo "No key entered; stopping." >&2
    exit 1
  fi
  printf '%s' "$GEMINI_KEY" | gcloud secrets versions add "$SECRET" --data-file=- --project "$PROJECT_ID"
  unset GEMINI_KEY
else
  echo "Using the existing enabled $SECRET secret version."
fi

if ! gcloud secrets describe "$APP_TOKEN_SECRET" --project "$PROJECT_ID" >/dev/null 2>&1; then
  gcloud secrets create "$APP_TOKEN_SECRET" --replication-policy=automatic --project "$PROJECT_ID"
fi
if [[ "${CARPE_ROTATE_APP_TOKEN:-0}" == "1" || -z "$(gcloud secrets versions list "$APP_TOKEN_SECRET" --project "$PROJECT_ID" --filter='state=ENABLED' --format='value(name)' --limit=1)" ]]; then
  APP_TOKEN="$(openssl rand -hex 32)"
  printf '%s' "$APP_TOKEN" | gcloud secrets versions add "$APP_TOKEN_SECRET" --data-file=- --project "$PROJECT_ID"
else
  echo "Using the existing enabled $APP_TOKEN_SECRET secret version."
  APP_TOKEN="$(gcloud secrets versions access latest --secret="$APP_TOKEN_SECRET" --project="$PROJECT_ID")"
fi

PROJECT_NUMBER="$(gcloud projects describe "$PROJECT_ID" --format='value(projectNumber)')"
gcloud secrets add-iam-policy-binding "$SECRET" \
  --member="serviceAccount:${PROJECT_NUMBER}-compute@developer.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor" \
  --project="$PROJECT_ID" >/dev/null
gcloud secrets add-iam-policy-binding "$APP_TOKEN_SECRET" \
  --member="serviceAccount:${PROJECT_NUMBER}-compute@developer.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor" \
  --project="$PROJECT_ID" >/dev/null

gcloud run deploy "$SERVICE" \
  --source backend \
  --project "$PROJECT_ID" \
  --region "$REGION" \
  --allow-unauthenticated \
  --set-secrets="GEMINI_API_KEY=${SECRET}:latest,CARPE_APP_TOKEN=${APP_TOKEN_SECRET}:latest" \
  --max-instances=1 \
  --concurrency=5

SERVICE_URL="$(gcloud run services describe "$SERVICE" --region "$REGION" --project "$PROJECT_ID" --format='value(status.url)')"
echo "CARPE_AI_ENDPOINT=${SERVICE_URL}/v1/ask"
echo "CARPE_APP_TOKEN=$APP_TOKEN"
echo "Paste this token into CARPE → Me → AI & privacy. Treat it like a password."
unset APP_TOKEN
curl --fail --silent --show-error "${SERVICE_URL}/health"
echo
echo "Checking a real AI response through the deployed service..."
curl --fail --silent --show-error --max-time 40 \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $(gcloud secrets versions access latest --secret="$APP_TOKEN_SECRET" --project="$PROJECT_ID")" \
  -d '{"message":"Reply with one short sentence confirming CARPE AI is responding."}' \
  "${SERVICE_URL}/v1/ask"
echo
