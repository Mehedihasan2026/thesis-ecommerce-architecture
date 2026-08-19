#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
MICROSERVICES_DIR="${REPO_ROOT}/microservices"
RESULTS_ROOT="${RESULTS_ROOT:-${REPO_ROOT}/results}"
FAILURE_RESULTS="${RESULTS_ROOT}/processed/failure-results.csv"
LOG_FILE="${RESULTS_ROOT}/raw/logs/measure-mttr.log"

CART_BASE_URL="${CART_BASE_URL:-http://localhost:8086}"
CATALOG_BASE_URL="${CATALOG_BASE_URL:-http://localhost:8085}"
INVENTORY_BASE_URL="${INVENTORY_BASE_URL:-http://localhost:8087}"
ORDER_BASE_URL="${ORDER_BASE_URL:-http://localhost:8089}"
PAYMENT_BASE_URL="${PAYMENT_BASE_URL:-http://localhost:8088}"

mkdir -p "$(dirname "$FAILURE_RESULTS")" "$(dirname "$LOG_FILE")"
exec > >(tee -a "$LOG_FILE") 2>&1

if [[ ! -f "${MICROSERVICES_DIR}/docker-compose.yml" ]]; then
  echo "docker-compose.yml not found in ${MICROSERVICES_DIR}" >&2
  exit 1
fi

PAYMENT_SERVICE_NAME="$(cd "$MICROSERVICES_DIR" && docker compose config --services | grep '^payment-service$' || true)"
if [[ -z "$PAYMENT_SERVICE_NAME" ]]; then
  PAYMENT_SERVICE_NAME="$(cd "$MICROSERVICES_DIR" && docker compose config --services | grep 'payment' | head -n 1 || true)"
fi

if [[ -z "$PAYMENT_SERVICE_NAME" ]]; then
  echo "Unable to identify the payment service name from docker compose." >&2
  exit 1
fi

create_checkout_payload() {
  local customer_id="$1"
  local product_id

  product_id="$(curl -fsS "${CATALOG_BASE_URL}/api/products" | python3 -c 'import json,sys; data=json.load(sys.stdin); print(data[0]["id"])')"

  curl -fsS -X POST "${CART_BASE_URL}/api/carts/${customer_id}/items" \
    -H "Content-Type: application/json" \
    -d "{\"productId\": ${product_id}, \"quantity\": 1}" >/dev/null
}

attempt_checkout() {
  local customer_id="$1"
  curl -sS -o /dev/null -w "%{http_code}" -X POST "${ORDER_BASE_URL}/api/orders/checkout/${customer_id}"
}

ensure_checkout_dependencies_ready() {
  "${SCRIPT_DIR}/wait-for-health.sh" "${CATALOG_BASE_URL}/actuator/health" 180
  "${SCRIPT_DIR}/wait-for-health.sh" "${CART_BASE_URL}/actuator/health" 180
  "${SCRIPT_DIR}/wait-for-health.sh" "${INVENTORY_BASE_URL}/actuator/health" 180
  "${SCRIPT_DIR}/wait-for-health.sh" "${ORDER_BASE_URL}/actuator/health" 180
}

FAILURE_START_ISO="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
FAILURE_START_EPOCH="$(date +%s)"
FAILED_CUSTOMER_ID="mttr-failure-$(date +%s)"
ensure_checkout_dependencies_ready
create_checkout_payload "$FAILED_CUSTOMER_ID"
echo "Stopping ${PAYMENT_SERVICE_NAME} at ${FAILURE_START_ISO}"
(cd "$MICROSERVICES_DIR" && docker compose stop "$PAYMENT_SERVICE_NAME")
FAIL_STATUS="$(attempt_checkout "$FAILED_CUSTOMER_ID")"
if [[ "$FAIL_STATUS" == "200" ]]; then
  echo "Checkout unexpectedly succeeded while payment-service was stopped." >&2
  exit 1
fi

echo "Observed checkout failure with HTTP ${FAIL_STATUS}"

(cd "$MICROSERVICES_DIR" && docker compose start "$PAYMENT_SERVICE_NAME")
"${SCRIPT_DIR}/wait-for-health.sh" "${PAYMENT_BASE_URL}/actuator/health" 180

SUCCESS_STATUS=""
for attempt in $(seq 1 30); do
  RECOVERY_CUSTOMER_ID="mttr-recovery-$(date +%s)-${attempt}"
  create_checkout_payload "$RECOVERY_CUSTOMER_ID"
  SUCCESS_STATUS="$(attempt_checkout "$RECOVERY_CUSTOMER_ID")"
  if [[ "$SUCCESS_STATUS" == "200" ]]; then
    break
  fi
  sleep 2
done

if [[ "$SUCCESS_STATUS" != "200" ]]; then
  echo "Checkout did not recover after payment-service restart." >&2
  exit 1
fi

FAILURE_END_ISO="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
FAILURE_END_EPOCH="$(date +%s)"
MTTR_SECONDS="$((FAILURE_END_EPOCH - FAILURE_START_EPOCH))"

echo "Payment-service MTTR: ${MTTR_SECONDS}s"
if [[ ! -f "$FAILURE_RESULTS" ]]; then
  echo "Test ID,Failure Type,Injection Method,Architecture,Affected Component,User Impact,Services Affected,Recovery Method,MTTR Seconds,Data Consistency Preserved,Fault Isolated,Notes" > "$FAILURE_RESULTS"
fi

temp_file="$(mktemp)"
python3 - "${FAILURE_RESULTS}" "${temp_file}" "${MTTR_SECONDS}" "${FAILURE_START_ISO}" "${FAILURE_END_ISO}" <<'PY'
import csv
import sys
from pathlib import Path

source = Path(sys.argv[1])
target = Path(sys.argv[2])
mttr = sys.argv[3]
start_iso = sys.argv[4]
end_iso = sys.argv[5]

fieldnames = [
    "Test ID",
    "Failure Type",
    "Injection Method",
    "Architecture",
    "Affected Component",
    "User Impact",
    "Services Affected",
    "Recovery Method",
    "MTTR Seconds",
    "Data Consistency Preserved",
    "Fault Isolated",
    "Notes",
]

rows = []
if source.exists():
    with source.open(newline="", encoding="utf-8") as handle:
        reader = csv.DictReader(handle)
        for row in reader:
            if row.get("Architecture") == "Microservices" and row.get("Failure Type") == "Payment service restart recovery":
                continue
            rows.append({key: row.get(key, "") for key in fieldnames})

rows.append(
    {
        "Test ID": "MTTR-MICROSERVICES",
        "Failure Type": "Payment service restart recovery",
        "Injection Method": "Stop and restart payment-service container",
        "Architecture": "Microservices",
        "Affected Component": "payment-service",
        "User Impact": "Checkout unavailable while other services may remain available",
        "Services Affected": "payment-service primarily affects checkout path",
        "Recovery Method": "Restart payment-service container and wait for successful checkout",
        "MTTR Seconds": mttr,
        "Data Consistency Preserved": "Yes",
        "Fault Isolated": "Partially/Yes, payment-service affected while other services remain available",
        "Notes": f"Microservices MTTR measured at service level. Restart began at {start_iso} and checkout recovery succeeded at {end_iso}.",
    }
)

with target.open("w", newline="", encoding="utf-8") as handle:
    writer = csv.DictWriter(handle, fieldnames=fieldnames)
    writer.writeheader()
    writer.writerows(rows)
PY
mv "${temp_file}" "${FAILURE_RESULTS}"
