# AGENTS.md

## Project context
This repository is for a business technology master's thesis comparing monolithic and microservices architectures using an e-commerce prototype.

## Main research objective
Evaluate when migration from a monolithic architecture to microservices creates meaningful technical and business value.

## Technology stack
Use:
- Java 17
- Spring Boot 3
- Gradle Kotlin DSL for build files
- Spring Web
- Spring Data JPA
- H2 for local testing
- Spring Boot Actuator
- Docker Compose
- k6 for load testing

## Important language rule
Application source code must be Java.
Use .java files under src/main/java and src/test/java.
Do not create Kotlin application source files.

## Architectures
Build the same e-commerce system in two styles:

1. Monolith:
- one Spring Boot application
- one database
- local service calls
- one deployable unit

2. Microservices:
- catalog-service
- cart-service
- inventory-service
- order-service
- payment-service
- each service has its own database
- order-service orchestrates checkout through HTTP calls

## Business capabilities
The system must include:
- product catalog
- cart
- inventory
- order
- payment

## Failure simulation
Support:
- simulatePaymentFailure=true
- simulateInventoryFailure=true
- artificialPaymentDelayMs=1000

## KPIs to collect
The system must support collection of:
- average response time
- p95 latency
- p99 latency
- throughput
- error rate
- CPU usage
- memory usage
- MTTR
- fault isolation
- diagnosis time
- deployment time
- operational complexity

## Commands
Use Gradle:
- ./gradlew test
- ./gradlew bootRun
- ./gradlew bootJar

Use k6 for load testing.

## Coding style
Keep code simple and readable.
Prefer thesis clarity over production complexity.
Avoid unnecessary frameworks.