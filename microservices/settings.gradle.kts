plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "ecommerce-microservices"

include(
    "catalog-service",
    "cart-service",
    "inventory-service",
    "payment-service",
    "order-service"
)
