#!/usr/bin/env bash
set -euo pipefail

LABEL="${1:-docker}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
RESULTS_ROOT="${RESULTS_ROOT:-${REPO_ROOT}/results}"
OUTPUT_DIR="${RESULTS_ROOT}/raw/docker"
NOTES_FILE="${RESULTS_ROOT}/notes/docker-stats-notes.txt"
TIMESTAMP="$(date -u +"%Y%m%dT%H%M%SZ")"

mkdir -p "$OUTPUT_DIR" "$(dirname "$NOTES_FILE")"

if ! command -v docker >/dev/null 2>&1; then
  echo "${TIMESTAMP} docker CLI not found; skipping Docker stats for ${LABEL}" >> "$NOTES_FILE"
  exit 0
fi

if ! docker info >/dev/null 2>&1; then
  echo "${TIMESTAMP} docker daemon unavailable; skipping Docker stats for ${LABEL}" >> "$NOTES_FILE"
  exit 0
fi

docker stats --no-stream > "${OUTPUT_DIR}/${TIMESTAMP}_${LABEL}_docker-stats.txt"
docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}' > "${OUTPUT_DIR}/${TIMESTAMP}_${LABEL}_docker-ps.txt"

echo "Docker stats collected for ${LABEL}"
