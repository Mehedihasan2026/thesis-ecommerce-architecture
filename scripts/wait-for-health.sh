#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <health-url> [timeout-seconds] [interval-seconds]" >&2
  exit 1
fi

URL="$1"
TIMEOUT_SECONDS="${2:-120}"
INTERVAL_SECONDS="${3:-2}"
START_TS="$(date +%s)"

while true; do
  RESPONSE="$(curl -fsS "$URL" 2>/dev/null || true)"
  if [[ "$RESPONSE" == *"\"status\":\"UP\""* ]]; then
    echo "Health check passed for $URL"
    exit 0
  fi

  NOW_TS="$(date +%s)"
  ELAPSED="$((NOW_TS - START_TS))"
  if (( ELAPSED >= TIMEOUT_SECONDS )); then
    echo "Timed out waiting for $URL after ${TIMEOUT_SECONDS}s" >&2
    exit 1
  fi

  sleep "$INTERVAL_SECONDS"
done
