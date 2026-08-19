#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
MONOLITH_BASE_URL="${MONOLITH_BASE_URL:-http://localhost:8083}"

mkdir -p \
  "${REPO_ROOT}/results/raw/k6" \
  "${REPO_ROOT}/results/raw/actuator" \
  "${REPO_ROOT}/results/raw/docker" \
  "${REPO_ROOT}/results/raw/logs" \
  "${REPO_ROOT}/results/processed" \
  "${REPO_ROOT}/results/notes"
overall_status=0

if curl -fsS "${MONOLITH_BASE_URL}/actuator/health" >/dev/null 2>&1; then
  if ! "${SCRIPT_DIR}/run-monolith-tests.sh"; then
    overall_status=1
  fi
else
  echo "Monolith not reachable at ${MONOLITH_BASE_URL}; skipping monolith test execution."
fi

echo "Running microservices test workflow against the repository's actual ports 8085-8089."
if ! "${SCRIPT_DIR}/run-microservices-tests.sh"; then
  overall_status=1
fi

python3 "${SCRIPT_DIR}/extract-k6-results.py"
python3 "${SCRIPT_DIR}/generate-comparison-csv.py"

echo "Processed outputs:"
echo "  ${REPO_ROOT}/results/processed/detailed-execution-results.csv"
echo "  ${REPO_ROOT}/results/processed/kpi-summary.csv"
echo "  ${REPO_ROOT}/results/processed/failure-results.csv"
echo "  ${REPO_ROOT}/results/processed/operational-complexity.csv"
echo "  ${REPO_ROOT}/results/processed/thesis-ready-comparison-table.csv"
exit "$overall_status"
