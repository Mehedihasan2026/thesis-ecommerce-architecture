#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 2 ]]; then
  echo "Usage: $0 <architecture> <phase>" >&2
  exit 1
fi

ARCHITECTURE="$1"
PHASE="$2"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
RESULTS_ROOT="${RESULTS_ROOT:-${REPO_ROOT}/results}"
OUTPUT_DIR="${RESULTS_ROOT}/raw/actuator"
NOTES_FILE="${RESULTS_ROOT}/notes/actuator-collection-notes.txt"
TIMESTAMP="$(date -u +"%Y%m%dT%H%M%SZ")"

mkdir -p "$OUTPUT_DIR" "$(dirname "$NOTES_FILE")"

collect_metric() {
  local service_name="$1"
  local base_url="$2"
  local metric_path="$3"
  local metric_name="$4"
  local metric_safe_name

  metric_safe_name="$(echo "$metric_name" | tr './' '__')"
  local output_file="${OUTPUT_DIR}/${TIMESTAMP}_${ARCHITECTURE}_${PHASE}_${service_name}_${metric_safe_name}.json"
  local target_url

  if [[ "$metric_path" == "health" ]]; then
    target_url="${base_url}/actuator/health"
  else
    target_url="${base_url}/actuator/metrics/${metric_path}"
  fi

  if ! curl -fsS "$target_url" -o "$output_file"; then
    echo "${TIMESTAMP} TODO ${ARCHITECTURE} ${service_name} missing metric ${metric_name} from ${target_url}" >> "$NOTES_FILE"
    rm -f "$output_file"
  fi
}

case "$ARCHITECTURE" in
  monolith)
    declare -a SERVICES=(
      "monolith|${MONOLITH_BASE_URL:-http://localhost:8083}"
    )
    ;;
  microservices)
    declare -a SERVICES=(
      "catalog-service|${CATALOG_BASE_URL:-http://localhost:8085}"
      "cart-service|${CART_BASE_URL:-http://localhost:8086}"
      "inventory-service|${INVENTORY_BASE_URL:-http://localhost:8087}"
      "payment-service|${PAYMENT_BASE_URL:-http://localhost:8088}"
      "order-service|${ORDER_BASE_URL:-http://localhost:8089}"
    )
    ;;
  *)
    echo "Unsupported architecture: $ARCHITECTURE" >&2
    exit 1
    ;;
esac

for service_entry in "${SERVICES[@]}"; do
  IFS='|' read -r service_name base_url <<< "$service_entry"
  collect_metric "$service_name" "$base_url" "health" "health"
  collect_metric "$service_name" "$base_url" "process.cpu.usage" "process.cpu.usage"
  collect_metric "$service_name" "$base_url" "jvm.memory.used" "jvm.memory.used"
  collect_metric "$service_name" "$base_url" "http.server.requests" "http.server.requests"
done

echo "Actuator metrics collected for ${ARCHITECTURE} (${PHASE})"
