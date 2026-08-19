# Missing Results Methodology

## Separate Test Execution

Monolith and microservices should be tested separately because running both stacks on the same machine at the same time introduces CPU, memory, disk, and container-runtime contention. Separate execution reduces cross-architecture interference and makes the resulting KPI and recovery measurements easier to defend in the thesis.

## MTTR Definition

MTTR was defined as the elapsed time between a restart command being issued and service recovery being confirmed.

- For the monolith, recovery is defined as:
  1. `/actuator/health` returns `UP`
  2. `/api/products` returns HTTP `200`
  3. if practical, a checkout flow also succeeds

- For microservices payment-service MTTR, recovery is defined as:
  1. the payment-service container is restarted
  2. the payment-service health endpoint returns `UP`
  3. a checkout through order-service succeeds again

This gives one whole-application recovery measure for the monolith and one service-level recovery measure for microservices.

## Deployment Time Measurement

Deployment time is measured using wall-clock timestamps around build or redeploy commands. The measured value reflects local prototype execution time, not CI/CD pipeline time or production deployment latency.

Measured scenarios include:

- full monolith Gradle build
- full monolith Docker build when a Dockerfile exists
- full microservices Docker Compose build
- payment-service-only rebuild and restart
- full microservices `docker compose up -d --build`

## Failure-Test Error Rate Interpretation

Failure scenario error rates come from k6 request-level metrics across multi-step flows. A `33.33%` error rate in these tests does not mean one-third of complete user journeys failed end-to-end. It means some requests inside the scripted journey failed, while earlier setup requests such as product lookup or add-to-cart may still have succeeded.

## Why p95 and p99 Matter for Payment Delay

In the payment-delay scenario, the workflow includes both fast setup requests and the intentionally delayed checkout request. Because of that mix, average response time can understate the experienced delay. `p95` and `p99` latency are more meaningful because they better capture the upper tail created by the delayed checkout step.

## Automatically Measured Fields

The workflow can measure or derive these fields automatically when the relevant scripts are run:

- response time, p95, p99, throughput, and error rate from k6 summary files
- microservices payment-service MTTR
- monolith MTTR after explicit manual execution of the restart script
- deployment time after explicit manual execution of the deployment timing script
- financial estimates derived from editable assumptions and measured throughput

## Manual or Declarative Interpretation Fields

Some thesis fields require reasoned interpretation rather than pure automation:

- fault isolation interpretation
- monitoring complexity
- rollback complexity
- manual operational steps
- user impact wording
- whether data consistency appears preserved based on code-path inspection

These fields are filled using code inspection plus explicit explanatory notes so the resulting CSVs remain thesis-ready and auditable.
