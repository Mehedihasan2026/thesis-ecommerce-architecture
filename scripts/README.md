# Test Automation Workflow

This folder contains the thesis comparison workflow for the existing Spring Boot monolith and microservices implementations. The scripts add repeatable load, failure, and metrics collection without changing business logic.

## Prerequisites

- Java 17
- Gradle wrapper support in the repository
- Docker Desktop with Docker Compose
- `curl`
- `k6`
- Python 3
- Bash-compatible shell for the `*.sh` scripts

## Important Port Compatibility Note

The repository does not use the placeholder ports from the original task text. The automation has been adapted to the real ports found in the project:

- Monolith: `http://localhost:8083`
- Catalog service: `http://localhost:8085`
- Cart service: `http://localhost:8086`
- Inventory service: `http://localhost:8087`
- Payment service: `http://localhost:8088`
- Order service: `http://localhost:8089`

These defaults can be overridden with environment variables such as `MONOLITH_BASE_URL`, `CATALOG_BASE_URL`, `CART_BASE_URL`, `INVENTORY_BASE_URL`, `PAYMENT_BASE_URL`, and `ORDER_BASE_URL`.

## How To Run Monolith Tests

Start the monolith first if it is not already running:

```bash
cd monolith/ecommerce-monolith
./gradlew bootRun
```

Or with Docker:

```bash
cd monolith/ecommerce-monolith
docker compose up -d --build
```

Then run:

```bash
bash scripts/run-monolith-tests.sh
```

## How To Run Microservices Tests

The script brings the stack up automatically from `microservices/docker-compose.yml`:

```bash
bash scripts/run-microservices-tests.sh
```

## How To Run All Tests

This runs monolith tests if the monolith health endpoint is reachable, then runs microservices tests, extracts raw k6 summaries, and generates processed CSV outputs:

```bash
bash scripts/run-all-tests.sh
```

## MTTR Measurement

The MTTR script performs a semi-automated payment-service outage and recovery measurement for the microservices architecture:

```bash
bash scripts/measure-mttr.sh
```

## Raw Results Location

- k6 summaries: `results/raw/k6/`
- Actuator snapshots: `results/raw/actuator/`
- Docker stats snapshots: `results/raw/docker/`
- Runtime logs: `results/raw/logs/`
- Collection notes: `results/notes/`

## Processed CSV Output Location

- Detailed extracted execution data: `results/processed/detailed-execution-results.csv`
- KPI summary: `results/processed/kpi-summary.csv`
- Failure results and MTTR: `results/processed/failure-results.csv`
- Operational complexity template: `results/processed/operational-complexity.csv`
- Thesis-ready comparison table: `results/processed/thesis-ready-comparison-table.csv`

## KPI Interpretation

- Average latency: mean response time across requests. Lower is better.
- P95 latency: 95 percent of requests complete at or below this value. Lower is better.
- P99 latency: tail latency showing near worst-case user experience. Lower is better.
- Throughput: requests handled per second. Higher is better.
- Error rate: percentage of failed requests. Lower is better.
- MTTR: mean time to recovery after an injected failure. Lower is better.

Use the thesis-ready comparison table for side-by-side architectural discussion and the operational complexity template for qualitative deployment and maintenance analysis.
