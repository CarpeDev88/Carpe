#!/usr/bin/env bash
set -euo pipefail

# Run this from a Google Cloud Shell session signed in to the CARPE project.
PROJECT_ID="phrasal-truck-368514"
REGION="us-west1"
SERVICE="carpe-intelligence"
SECRET="carpe-gemini-api-key"

gcloud config set project "$PROJECT_ID"
gcloud services enable run.googleapis.com cloudbuild.googleapis.com artifactregistry.googleapis.com secretmanager.googleapis.com
if ! gcloud secrets describe "$SECRET" --project "$PROJECT_ID" >/dev/null 2>&1; then
  gcloud secrets create "$SECRET" --replication-policy=automatic --project "$PROJECT_ID"
fi
printf 'Paste the Gemini API key (input hidden), then press Enter: ' >&2
IFS= read -r -s GEMINI_KEY
printf '\n' >&2
if [[ -z "$GEMINI_KEY" ]]; then
  echo "No key entered; stopping." >&2
  exit 1
fi
printf '%s' "$GEMINI_KEY" | gcloud secrets versions add "$SECRET" --data-file=- --project "$PROJECT_ID"
unset GEMINI_KEY

PROJECT_NUMBER="$(gcloud projects describe "$PROJECT_ID" --format='value(projectNumber)')"
gcloud secrets add-iam-policy-binding "$SECRET" \
  --member="serviceAccount:${PROJECT_NUMBER}-compute@developer.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor" \
  --project="$PROJECT_ID" >/dev/null

gcloud run deploy "$SERVICE" \
  --source backend \
  --project "$PROJECT_ID" \
  --region "$REGION" \
  --allow-unauthenticated \
  --set-secrets="GEMINI_API_KEY=${SECRET}:latest" \
  --max-instances=1 \
  --concurrency=5

SERVICE_URL="$(gcloud run services describe "$SERVICE" --region "$REGION" --project "$PROJECT_ID" --format='value(status.url)')"
echo "CARPE_AI_ENDPOINT=${SERVICE_URL}/v1/ask"
curl --fail --silent --show-error "${SERVICE_URL}/health"
echo
