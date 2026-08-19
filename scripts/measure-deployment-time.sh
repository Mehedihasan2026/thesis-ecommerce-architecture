#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
RESULTS_FILE="${REPO_ROOT}/results/processed/deployment-time-results.csv"
MONOLITH_DIR="${REPO_ROOT}/monolith/ecommerce-monolith"
MICROSERVICES_DIR="${REPO_ROOT}/microservices"

mkdir -p "$(dirname "${RESULTS_FILE}")"

measure_command_seconds() {
  local start end
  start="$(date +%s)"
  "$@"
  end="$(date +%s)"
  echo "$((end - start))"
}

write_header() {
  echo "Scenario,Architecture,Command Measured,Deployment Time Seconds,Components Affected,Services Redeployed,Notes" > "${RESULTS_FILE}"
}

append_row() {
  echo "$1,$2,\"$3\",$4,$5,$6,\"$7\"" >> "${RESULTS_FILE}"
}

write_header

if [[ -d "${MONOLITH_DIR}" ]]; then
  seconds="$(measure_command_seconds bash -lc "cd \"${MONOLITH_DIR}\" && ./gradlew bootJar >/dev/null")"
  append_row "Full monolith Gradle build" "Monolith" "cd monolith/ecommerce-monolith && ./gradlew bootJar" "${seconds}" "Whole monolith application" "1 whole application" "Measured with wall-clock timestamps"
else
  append_row "Full monolith Gradle build" "Monolith" "cd monolith/ecommerce-monolith && ./gradlew bootJar" "" "Whole monolith application" "1 whole application" "TODO monolith directory missing"
fi

if [[ -f "${MONOLITH_DIR}/Dockerfile" ]]; then
  seconds="$(measure_command_seconds bash -lc "cd \"${MONOLITH_DIR}\" && docker build -t thesis-monolith-timing . >/dev/null")"
  append_row "Full monolith Docker build" "Monolith" "cd monolith/ecommerce-monolith && docker build -t thesis-monolith-timing ." "${seconds}" "Whole monolith application image" "1 whole application" "Measured with wall-clock timestamps"
else
  append_row "Full monolith Docker build" "Monolith" "cd monolith/ecommerce-monolith && docker build -t thesis-monolith-timing ." "" "Whole monolith application image" "1 whole application" "TODO Dockerfile missing"
fi

if [[ -f "${MICROSERVICES_DIR}/docker-compose.yml" ]]; then
  seconds="$(measure_command_seconds bash -lc "cd \"${MICROSERVICES_DIR}\" && docker compose build >/dev/null")"
  append_row "Full microservices Docker Compose build" "Microservices" "cd microservices && docker compose build" "${seconds}" "All services" "5 services" "Measured with wall-clock timestamps"

  seconds="$(measure_command_seconds bash -lc "cd \"${MICROSERVICES_DIR}\" && docker compose build payment-service >/dev/null && docker compose up -d payment-service >/dev/null")"
  append_row "Payment-service-only microservices rebuild/restart" "Microservices" "cd microservices && docker compose build payment-service && docker compose up -d payment-service" "${seconds}" "payment-service" "1 service" "Measured with wall-clock timestamps"

  seconds="$(measure_command_seconds bash -lc "cd \"${MICROSERVICES_DIR}\" && docker compose up -d --build >/dev/null")"
  append_row "Full microservices docker compose up -d --build" "Microservices" "cd microservices && docker compose up -d --build" "${seconds}" "All services" "5 services" "Measured with wall-clock timestamps"
else
  append_row "Full microservices Docker Compose build" "Microservices" "cd microservices && docker compose build" "" "All services" "5 services" "TODO docker-compose.yml missing"
  append_row "Payment-service-only microservices rebuild/restart" "Microservices" "cd microservices && docker compose build payment-service && docker compose up -d payment-service" "" "payment-service" "1 service" "TODO docker-compose.yml missing"
  append_row "Full microservices docker compose up -d --build" "Microservices" "cd microservices && docker compose up -d --build" "" "All services" "5 services" "TODO docker-compose.yml missing"
fi

echo "Wrote ${RESULTS_FILE}"
