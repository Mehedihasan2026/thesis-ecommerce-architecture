#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
RESULTS_ROOT="${RESULTS_ROOT:-${REPO_ROOT}/results}"
MONOLITH_DIR="${REPO_ROOT}/monolith/ecommerce-monolith"
LOG_FILE="${RESULTS_ROOT}/raw/logs/monolith-test-run.log"
NOTES_FILE="${RESULTS_ROOT}/notes/monolith-test-notes.txt"
MONOLITH_BASE_URL="${MONOLITH_BASE_URL:-http://localhost:8083}"
MONOLITH_TESTS=(
  "TC-01-catalog-baseline"
  "TC-02-checkout-baseline"
  "TC-03-catalog-load"
  "TC-04-checkout-load"
  "TC-05-payment-failure"
  "TC-06-inventory-failure"
  "TC-07-payment-delay"
)

mkdir -p \
  "${RESULTS_ROOT}/raw/k6" \
  "${RESULTS_ROOT}/raw/actuator" \
  "${RESULTS_ROOT}/raw/docker" \
  "${RESULTS_ROOT}/raw/logs" \
  "${RESULTS_ROOT}/processed" \
  "${RESULTS_ROOT}/notes"
exec > >(tee -a "$LOG_FILE") 2>&1
overall_status=0

echo "=== Monolith test run started at $(date -u +"%Y-%m-%dT%H:%M:%SZ") ==="
echo "Using monolith base URL: ${MONOLITH_BASE_URL}"

if command -v k6 >/dev/null 2>&1; then
  K6_BIN="$(command -v k6)"
elif [[ -x "/c/Program Files/k6/k6.exe" ]]; then
  K6_BIN="/c/Program Files/k6/k6.exe"
elif command -v wslpath >/dev/null 2>&1; then
  K6_WSL_PATH="$(wslpath 'C:\Program Files\k6\k6.exe' 2>/dev/null || true)"
  if [[ -n "${K6_WSL_PATH}" && -x "${K6_WSL_PATH}" ]]; then
    K6_BIN="${K6_WSL_PATH}"
  fi
fi

if [[ -z "${K6_BIN:-}" ]]; then
  echo "k6 is required but not found in PATH, /c/Program Files/k6/k6.exe, or via wslpath." >&2
  exit 1
else
  echo "Using k6 binary: ${K6_BIN}"
fi

to_k6_path() {
  local raw_path="$1"
  if [[ "${K6_BIN}" == *.exe ]] && command -v wslpath >/dev/null 2>&1; then
    wslpath -w "$raw_path"
  else
    echo "$raw_path"
  fi
}

if ! curl -fsS "${MONOLITH_BASE_URL}/actuator/health" >/dev/null 2>&1; then
  echo "Monolith health check failed at ${MONOLITH_BASE_URL}/actuator/health"
  echo "Start it first with one of these commands:"
  echo "  cd ${MONOLITH_DIR} && ./gradlew bootRun"
  echo "  cd ${MONOLITH_DIR} && docker compose up -d --build"
  echo "$(date -u +"%Y-%m-%dT%H:%M:%SZ") monolith unavailable at ${MONOLITH_BASE_URL}/actuator/health" >> "$NOTES_FILE"
  exit 0
fi

"${SCRIPT_DIR}/collect-actuator-metrics.sh" monolith before

if docker ps --format '{{.Names}} {{.Ports}}' | grep -q '8083->8083'; then
  "${SCRIPT_DIR}/collect-docker-stats.sh" monolith-before
else
  echo "$(date -u +"%Y-%m-%dT%H:%M:%SZ") monolith not detected in Docker; skipping Docker stats" >> "$NOTES_FILE"
fi

for test_id in "${MONOLITH_TESTS[@]}"; do
  test_file="${REPO_ROOT}/load-tests/monolith/${test_id}.js"
  summary_file="${RESULTS_ROOT}/raw/k6/monolith-${test_id}.json"
  k6_test_file="$(to_k6_path "$test_file")"
  k6_summary_file="$(to_k6_path "$summary_file")"

  echo "Running ${test_id}"
  if ! "$K6_BIN" run \
    --summary-trend-stats "avg,min,med,max,p(90),p(95),p(99)" \
    --summary-export "$k6_summary_file" \
    "$k6_test_file"; then
    echo "$(date -u +"%Y-%m-%dT%H:%M:%SZ") k6 thresholds failed for monolith ${test_id}" >> "$NOTES_FILE"
    overall_status=1
  fi
done

"${SCRIPT_DIR}/collect-actuator-metrics.sh" monolith after

if docker ps --format '{{.Names}} {{.Ports}}' | grep -q '8083->8083'; then
  "${SCRIPT_DIR}/collect-docker-stats.sh" monolith-after
fi

echo "=== Monolith test run completed at $(date -u +"%Y-%m-%dT%H:%M:%SZ") ==="
exit "$overall_status"
