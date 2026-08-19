#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
MICROSERVICES_DIR="${REPO_ROOT}/microservices"
COMPOSE_FILE="${MICROSERVICES_DIR}/docker-compose.yml"
LOG_FILE="${REPO_ROOT}/results/raw/logs/microservices-test-run.log"
COMPOSE_LOG_FILE="${REPO_ROOT}/results/raw/logs/microservices-logs.txt"
NOTES_FILE="${REPO_ROOT}/results/notes/microservices-test-notes.txt"

CATALOG_BASE_URL="${CATALOG_BASE_URL:-http://localhost:8085}"
CART_BASE_URL="${CART_BASE_URL:-http://localhost:8086}"
INVENTORY_BASE_URL="${INVENTORY_BASE_URL:-http://localhost:8087}"
PAYMENT_BASE_URL="${PAYMENT_BASE_URL:-http://localhost:8088}"
ORDER_BASE_URL="${ORDER_BASE_URL:-http://localhost:8089}"

MICROSERVICE_TESTS=(
  "TC-01-catalog-baseline"
  "TC-02-checkout-baseline"
  "TC-03-catalog-load"
  "TC-04-checkout-load"
  "TC-05-payment-failure"
  "TC-06-inventory-failure"
  "TC-07-payment-delay"
)

mkdir -p \
  "${REPO_ROOT}/results/raw/k6" \
  "${REPO_ROOT}/results/raw/actuator" \
  "${REPO_ROOT}/results/raw/docker" \
  "${REPO_ROOT}/results/raw/logs" \
  "${REPO_ROOT}/results/processed" \
  "${REPO_ROOT}/results/notes"
exec > >(tee -a "$LOG_FILE") 2>&1
overall_status=0

echo "=== Microservices test run started at $(date -u +"%Y-%m-%dT%H:%M:%SZ") ==="
echo "Using service URLs: ${CATALOG_BASE_URL}, ${CART_BASE_URL}, ${INVENTORY_BASE_URL}, ${PAYMENT_BASE_URL}, ${ORDER_BASE_URL}"

if ! command -v docker >/dev/null 2>&1; then
  echo "docker is required but not found in PATH." >&2
  exit 1
fi

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

if [[ ! -f "$COMPOSE_FILE" ]]; then
  echo "Microservices docker-compose.yml not found at ${COMPOSE_FILE}" >&2
  exit 1
fi

(cd "$MICROSERVICES_DIR" && docker compose up -d --build)

"${SCRIPT_DIR}/wait-for-health.sh" "${CATALOG_BASE_URL}/actuator/health" 180
"${SCRIPT_DIR}/wait-for-health.sh" "${CART_BASE_URL}/actuator/health" 180
"${SCRIPT_DIR}/wait-for-health.sh" "${INVENTORY_BASE_URL}/actuator/health" 180
"${SCRIPT_DIR}/wait-for-health.sh" "${PAYMENT_BASE_URL}/actuator/health" 180
"${SCRIPT_DIR}/wait-for-health.sh" "${ORDER_BASE_URL}/actuator/health" 180

echo "$(date -u +"%Y-%m-%dT%H:%M:%SZ") NOTE: prompt assumptions used ports 8081-8085, but repository compatibility requires 8085-8089." >> "$NOTES_FILE"

"${SCRIPT_DIR}/collect-actuator-metrics.sh" microservices before
"${SCRIPT_DIR}/collect-docker-stats.sh" microservices-before

for test_id in "${MICROSERVICE_TESTS[@]}"; do
  test_file="${REPO_ROOT}/load-tests/microservices/${test_id}.js"
  summary_file="${REPO_ROOT}/results/raw/k6/microservices-${test_id}.json"
  k6_test_file="$(to_k6_path "$test_file")"
  k6_summary_file="$(to_k6_path "$summary_file")"

  echo "Running ${test_id}"
  if ! "$K6_BIN" run \
    --summary-trend-stats "avg,min,med,max,p(90),p(95),p(99)" \
    --summary-export "$k6_summary_file" \
    "$k6_test_file"; then
    echo "$(date -u +"%Y-%m-%dT%H:%M:%SZ") k6 thresholds failed for microservices ${test_id}" >> "$NOTES_FILE"
    overall_status=1
  fi
done

"${SCRIPT_DIR}/collect-actuator-metrics.sh" microservices after
"${SCRIPT_DIR}/collect-docker-stats.sh" microservices-after
(cd "$MICROSERVICES_DIR" && docker compose logs --no-color) > "$COMPOSE_LOG_FILE"

echo "=== Microservices test run completed at $(date -u +"%Y-%m-%dT%H:%M:%SZ") ==="
exit "$overall_status"
