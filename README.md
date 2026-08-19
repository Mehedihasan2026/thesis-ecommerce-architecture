# E-Commerce Architecture Comparison

This repository is the practical prototype for a master's thesis comparing **monolithic** and **microservices** architectures in the same e-commerce domain.

The project evaluates when moving from a monolith to microservices creates meaningful technical and business value. Both implementations provide the same core capabilities—catalog, cart, inventory, order, and payment—so their performance, failure behavior, operational effort, and estimated cost can be compared fairly.

> Application source code is written in Java. GitHub may detect Kotlin because the build uses Gradle Kotlin DSL and the repository includes generated Gradle cache files.

## Research Scope

The comparison uses the following indicators:

- Response time, p95 latency, p99 latency, throughput, and error rate
- CPU and memory observations from Spring Boot Actuator and Docker
- MTTR, failure isolation, and diagnosis time
- Deployment time and operational complexity
- Estimated infrastructure, labor, downtime, request, and checkout costs

## Architectures

| Architecture | Implementation | Deployment model | Data and communication |
| --- | --- | --- | --- |
| Monolith | `monolith/ecommerce-monolith` | One Spring Boot application | One H2 database and in-process service calls |
| Microservices | `microservices` | Five Spring Boot services | One database per service; `order-service` orchestrates checkout over HTTP |

The microservices implementation contains:

- `catalog-service` — product catalog
- `cart-service` — customer carts
- `inventory-service` — stock and reservations
- `payment-service` — payment processing
- `order-service` — checkout orchestration and orders

## Technology Stack

- Java 17 and Spring Boot 3
- Gradle with Kotlin DSL
- Spring Web, Spring Data JPA, H2, and Spring Boot Actuator
- Docker Compose
- k6 load testing
- Python scripts for result processing and financial modelling

## Repository Layout

```text
monolith/ecommerce-monolith/  Monolithic application
microservices/                Five-service implementation and Docker Compose stack
load-tests/                   k6 scenarios for both architectures
scripts/                      Automation, metrics collection, and result processing
results/                      Raw observations and thesis-ready comparison tables
financial-model/              Transparent cost assumptions and calculation scripts
```

## Run the Applications

### Monolith

Run locally:

```bash
cd monolith/ecommerce-monolith
./gradlew bootRun
```

Or run it with Docker:

```bash
cd monolith/ecommerce-monolith
docker compose up -d --build
```

The monolith listens on `http://localhost:8083`.

### Microservices

Start the complete stack with Docker Compose:

```bash
cd microservices
docker compose up -d --build
```

| Service | Port |
| --- | --- |
| Catalog | `8085` |
| Cart | `8086` |
| Inventory | `8087` |
| Payment | `8088` |
| Order | `8089` |

Stop the stack when finished:

```bash
docker compose down
```

## Test the APIs

Both implementations expose equivalent HTTP APIs. Typical endpoints include:

```text
GET  /api/products
POST /api/carts/{customerId}/items
GET  /api/carts/{customerId}
POST /api/orders/checkout/{customerId}
GET  /api/orders
```

For microservices, send checkout requests to `order-service` on port `8089`; for the monolith, use port `8083`.

## Failure Simulation

Checkout supports controlled failure experiments through query parameters:

```text
simulatePaymentFailure=true
simulateInventoryFailure=true
artificialPaymentDelayMs=1000
```

Example:

```bash
curl -X POST "http://localhost:8089/api/orders/checkout/1?simulatePaymentFailure=true"
```

These controls make it possible to compare error handling, fault isolation, and recovery behavior under comparable conditions.

## Load Tests and Metrics

Prerequisites:

- Java 17
- Docker Desktop and Docker Compose
- k6
- Python 3
- `curl` and a Bash-compatible shell

Run the monolith scenarios after starting the monolith:

```bash
bash scripts/run-monolith-tests.sh
```

Run the microservices workflow:

```bash
bash scripts/run-microservices-tests.sh
```

Run the end-to-end comparison workflow:

```bash
bash scripts/run-all-tests.sh
```

Measure microservices recovery time:

```bash
bash scripts/measure-mttr.sh
```

The scripts collect k6 summaries, Actuator snapshots, Docker statistics, runtime logs, and processed KPI tables. Full workflow details are available in [`scripts/README.md`](scripts/README.md).

## Results and Financial Model

Generated and captured results are stored in:

- `results/raw/` — k6 output, Actuator data, Docker statistics, and runtime logs
- `results/processed/` — extracted KPI summaries and thesis-ready comparison tables
- `results/isolated/` — separate monolith-only, microservices-only, and comparison runs

The financial model converts measured outputs and editable assumptions into estimated monthly costs:

```bash
python financial-model/scripts/calculate-costs.py
```

Edit the CSV files in `financial-model/input/` to change infrastructure, labor, and downtime assumptions. See [`financial-model/docs/financial-model-readme.md`](financial-model/docs/financial-model-readme.md) for formulas, outputs, and limitations.

## Reproducibility Notes

- Treat the recorded results as prototype measurements, not production benchmarks.
- Keep hardware, Docker resource limits, test data, and k6 scenarios consistent between architectures.
- Use the raw data to audit calculations and the processed tables to support thesis discussion.
- Financial outputs are scenario estimates; validate cloud prices with official provider calculators before making final conclusions.

## Thesis Question

The repository is designed to support this decision-oriented question:

> Under which technical and business conditions does migrating an e-commerce system from a monolith to microservices create enough value to justify the additional operational complexity?
