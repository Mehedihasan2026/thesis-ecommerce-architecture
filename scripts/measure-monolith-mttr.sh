#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
RESULTS_ROOT="${RESULTS_ROOT:-${REPO_ROOT}/results}"
MONOLITH_DIR="${REPO_ROOT}/monolith/ecommerce-monolith"
LOG_FILE="${RESULTS_ROOT}/raw/logs/monolith-mttr.log"
FAILURE_RESULTS="${RESULTS_ROOT}/processed/failure-results.csv"

MONOLITH_BASE_URL="${MONOLITH_BASE_URL:-http://localhost:8083}"
HEALTH_URL="${MONOLITH_BASE_URL}/actuator/health"
PRODUCTS_URL="${MONOLITH_BASE_URL}/api/products"
CART_BASE_URL="${MONOLITH_BASE_URL}"
ORDER_BASE_URL="${MONOLITH_BASE_URL}"
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-240}"

mkdir -p "$(dirname "${LOG_FILE}")" "$(dirname "${FAILURE_RESULTS}")"
exec > >(tee -a "${LOG_FILE}") 2>&1

if [[ ! -d "${MONOLITH_DIR}" ]]; then
  echo "Monolith directory not found at ${MONOLITH_DIR}" >&2
  exit 1
fi

port_in_use() {
  local port
  port="$(echo "${MONOLITH_BASE_URL}" | sed -E 's#.*:([0-9]+).*#\1#')"
  if command -v lsof >/dev/null 2>&1; then
    lsof -iTCP:"${port}" -sTCP:LISTEN >/dev/null 2>&1
  elif command -v ss >/dev/null 2>&1; then
    ss -ltn "( sport = :${port} )" | grep -q LISTEN
  else
    curl -fsS "${HEALTH_URL}" >/dev/null 2>&1
  fi
}

if port_in_use; then
  echo "Monolith appears to already be running at ${MONOLITH_BASE_URL}."
  echo "Stop it first, then rerun this script."
  echo "If you explicitly want this script to stop the running process, rerun with ALLOW_STOP_RUNNING_MONOLITH=true."
  if [[ "${ALLOW_STOP_RUNNING_MONOLITH:-false}" != "true" ]]; then
    exit 1
  fi
  echo "Attempting to stop the running monolith because ALLOW_STOP_RUNNING_MONOLITH=true was set."
  if command -v lsof >/dev/null 2>&1; then
    port="$(echo "${MONOLITH_BASE_URL}" | sed -E 's#.*:([0-9]+).*#\1#')"
    pids="$(lsof -tiTCP:"${port}" -sTCP:LISTEN || true)"
    if [[ -n "${pids}" ]]; then
      kill ${pids}
      sleep 3
    fi
  else
    echo "Unable to stop the running monolith automatically on this shell. Stop it manually and rerun." >&2
    exit 1
  fi
fi

APP_PID=""
cleanup() {
  if [[ -n "${APP_PID}" ]] && kill -0 "${APP_PID}" >/dev/null 2>&1; then
    kill "${APP_PID}" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

START_ISO="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
START_EPOCH="$(date +%s)"
echo "Starting monolith MTTR measurement at ${START_ISO}"

(
  cd "${MONOLITH_DIR}"
  ./gradlew bootRun
) >> "${LOG_FILE}" 2>&1 &
APP_PID=$!

deadline=$((START_EPOCH + TIMEOUT_SECONDS))
health_ok=false
products_ok=false
checkout_ok=false
checkout_note=""

while [[ "$(date +%s)" -lt "${deadline}" ]]; do
  if curl -fsS "${HEALTH_URL}" | grep -q '"status":"UP"'; then
    health_ok=true
    break
  fi
  sleep 2
done

if [[ "${health_ok}" != "true" ]]; then
  echo "Monolith health endpoint did not become UP within ${TIMEOUT_SECONDS}s." >&2
  exit 1
fi

while [[ "$(date +%s)" -lt "${deadline}" ]]; do
  product_status="$(curl -sS -o /dev/null -w "%{http_code}" "${PRODUCTS_URL}")"
  if [[ "${product_status}" == "200" ]]; then
    products_ok=true
    break
  fi
  sleep 2
done

if [[ "${products_ok}" != "true" ]]; then
  echo "Products endpoint did not return HTTP 200 within ${TIMEOUT_SECONDS}s." >&2
  exit 1
fi

product_id="$(curl -fsS "${PRODUCTS_URL}" | python3 -c 'import json,sys; data=json.load(sys.stdin); print(data[0]["id"])' 2>/dev/null || true)"
if [[ -n "${product_id}" ]]; then
  customer_id="monolith-mttr-$(date +%s)"
  add_status="$(curl -sS -o /dev/null -w "%{http_code}" -X POST "${CART_BASE_URL}/api/carts/${customer_id}/items" \
    -H "Content-Type: application/json" \
    -d "{\"productId\": ${product_id}, \"quantity\": 1}" || true)"
  if [[ "${add_status}" == "200" || "${add_status}" == "201" ]]; then
    checkout_status="$(curl -sS -o /dev/null -w "%{http_code}" -X POST "${ORDER_BASE_URL}/api/orders/checkout/${customer_id}" || true)"
    if [[ "${checkout_status}" == "200" ]]; then
      checkout_ok=true
      checkout_note="Checkout endpoint also succeeded after restart."
    else
      checkout_note="Checkout verification did not succeed automatically; MTTR based on health and product endpoint recovery."
    fi
  else
    checkout_note="Cart setup did not succeed automatically; MTTR based on health and product endpoint recovery."
  fi
else
  checkout_note="Product discovery failed for checkout verification; MTTR based on health and product endpoint recovery."
fi

END_ISO="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
END_EPOCH="$(date +%s)"
MTTR_SECONDS="$((END_EPOCH - START_EPOCH))"

if [[ ! -f "${FAILURE_RESULTS}" ]]; then
  echo "Test ID,Failure Type,Injection Method,Architecture,Affected Component,User Impact,Services Affected,Recovery Method,MTTR Seconds,Data Consistency Preserved,Fault Isolated,Notes" > "${FAILURE_RESULTS}"
fi

temp_file="$(mktemp)"
python3 - "${FAILURE_RESULTS}" "${temp_file}" "${MTTR_SECONDS}" "${checkout_note}" <<'PY'
import csv
import sys
from pathlib import Path

source = Path(sys.argv[1])
target = Path(sys.argv[2])
mttr = sys.argv[3]
checkout_note = sys.argv[4]

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
            if row.get("Architecture") == "Monolith" and row.get("Failure Type") == "Application restart recovery":
                continue
            rows.append({key: row.get(key, "") for key in fieldnames})

rows.append(
    {
        "Test ID": "MTTR-MONOLITH",
        "Failure Type": "Application restart recovery",
        "Injection Method": "Restart monolith process",
        "Architecture": "Monolith",
        "Affected Component": "Whole monolith application",
        "User Impact": "Whole application unavailable during restart window",
        "Services Affected": "1 application",
        "Recovery Method": "Restart monolith application and wait for health/product endpoint",
        "MTTR Seconds": mttr,
        "Data Consistency Preserved": "TODO verify persistence semantics across restart",
        "Fault Isolated": "No, whole application restart",
        "Notes": f"Monolith MTTR measured as full application restart recovery. {checkout_note}".strip(),
    }
)

with target.open("w", newline="", encoding="utf-8") as handle:
    writer = csv.DictWriter(handle, fieldnames=fieldnames)
    writer.writeheader()
    writer.writerows(rows)
PY
mv "${temp_file}" "${FAILURE_RESULTS}"

echo "Measured monolith MTTR: ${MTTR_SECONDS}s"
if [[ "${checkout_ok}" == "true" ]]; then
  echo "Checkout verification succeeded after restart."
else
  echo "${checkout_note}"
fi
