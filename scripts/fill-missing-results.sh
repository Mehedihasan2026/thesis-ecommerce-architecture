#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
PROCESSED_DIR="${REPO_ROOT}/results/processed"
BACKUP_DIR="${PROCESSED_DIR}/backups/$(date -u +"%Y%m%dT%H%M%SZ")"

if command -v python3 >/dev/null 2>&1; then
  PYTHON_CMD=(python3)
elif command -v python >/dev/null 2>&1; then
  PYTHON_CMD=(python)
elif [[ -x "/c/Windows/py.exe" ]]; then
  PYTHON_CMD=("/c/Windows/py.exe" -3)
else
  echo "Python interpreter not found in this shell environment." >&2
  exit 1
fi

mkdir -p "${BACKUP_DIR}"

for file in \
  "${PROCESSED_DIR}/failure-results.csv" \
  "${PROCESSED_DIR}/operational-complexity.csv" \
  "${PROCESSED_DIR}/thesis-ready-comparison-table.csv" \
  "${PROCESSED_DIR}/deployment-time-results.csv" \
  "${PROCESSED_DIR}/financial-summary-for-thesis.csv"; do
  if [[ -f "${file}" ]]; then
    cp "${file}" "${BACKUP_DIR}/"
  fi
done

"${PYTHON_CMD[@]}" "${SCRIPT_DIR}/update-operational-complexity.py"
"${PYTHON_CMD[@]}" "${SCRIPT_DIR}/update-failure-results.py"
"${PYTHON_CMD[@]}" "${SCRIPT_DIR}/enrich-thesis-comparison-notes.py"

echo "Backups saved under: ${BACKUP_DIR}"
echo "Non-disruptive CSV enrichment complete."
echo "Optional manual measurement commands:"
echo "  bash scripts/measure-monolith-mttr.sh"
echo "  bash scripts/measure-deployment-time.sh"
echo "These measurement scripts may start/stop services or rebuild containers, so run them manually."
