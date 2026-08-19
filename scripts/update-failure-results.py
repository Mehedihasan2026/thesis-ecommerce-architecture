#!/usr/bin/env python3
from __future__ import annotations

import csv
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
RESULTS_DIR = REPO_ROOT / "results" / "processed"
DETAILED_FILE = RESULTS_DIR / "detailed-execution-results.csv"
LEGACY_FAILURE_FILE = RESULTS_DIR / "failure-results.csv"
OUTPUT_FILE = RESULTS_DIR / "failure-results.csv"

FIELDNAMES = [
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

REQUEST_LEVEL_NOTE = (
    "Error rate is request-level from k6 multi-step flow. It does not mean 33.33% of full checkout "
    "journeys failed; setup requests may have succeeded while the failure-triggering checkout request failed."
)
DELAY_NOTE = (
    "Average response time may be influenced by non-delayed setup requests; p95 and p99 latency better reflect delay impact."
)


def read_csv(path: Path) -> list[dict[str, str]]:
    if not path.exists():
        return []
    with path.open(newline="", encoding="utf-8") as handle:
        return list(csv.DictReader(handle))


def detailed_lookup() -> dict[tuple[str, str], dict[str, str]]:
    rows = {}
    for row in read_csv(DETAILED_FILE):
        rows[(row.get("Test ID", ""), row.get("Architecture", ""))] = row
    return rows


def parse_legacy_mttr() -> dict[str, dict[str, str]]:
    mttr_rows: dict[str, dict[str, str]] = {}
    for row in read_csv(LEGACY_FAILURE_FILE):
        architecture = row.get("Architecture", "")
        if architecture not in {"Monolith", "Microservices"}:
            continue
        failure_type = row.get("Failure Type", "")
        if failure_type in {"Application restart recovery", "Payment service restart recovery"}:
            mttr_rows[architecture] = row
        elif row.get("Scenario") == "payment-service-restart":
            mttr_rows["Microservices"] = {
                "Architecture": "Microservices",
                "Failure Type": "Payment service restart recovery",
                "Recovery Method": "Restart payment-service container and wait for successful checkout",
                "MTTR Seconds": row.get("MTTRSeconds", ""),
                "Notes": "Microservices MTTR measured at service level",
            }
    return mttr_rows


def build_failure_row(
    *,
    test_id: str,
    architecture: str,
    detailed: dict[str, str] | None,
    failure_type: str,
    injection_method: str,
    affected_component: str,
    user_impact: str,
    services_affected: str,
    recovery_method: str,
    consistency: str,
    fault_isolated: str,
    extra_notes: list[str],
    mttr_seconds: str = "",
) -> dict[str, str]:
    notes = []
    if detailed:
        error_rate = detailed.get("Error Rate %", "")
        if error_rate == "33.33":
            notes.append(REQUEST_LEVEL_NOTE)
        if detailed.get("Test ID") == "TC-07":
            notes.append(DELAY_NOTE)
    notes.extend(extra_notes)
    return {
        "Test ID": test_id,
        "Failure Type": failure_type,
        "Injection Method": injection_method,
        "Architecture": architecture,
        "Affected Component": affected_component,
        "User Impact": user_impact,
        "Services Affected": services_affected,
        "Recovery Method": recovery_method,
        "MTTR Seconds": mttr_seconds,
        "Data Consistency Preserved": consistency,
        "Fault Isolated": fault_isolated,
        "Notes": " ".join(note for note in notes if note),
    }


def main() -> None:
    detailed = detailed_lookup()
    mttr_rows = parse_legacy_mttr()

    rows: list[dict[str, str]] = []
    rows.append(
        build_failure_row(
            test_id="TC-05",
            architecture="Monolith",
            detailed=detailed.get(("TC-05", "Monolith")),
            failure_type="Payment failure simulation",
            injection_method="simulatePaymentFailure=true on checkout",
            affected_component="Payment module inside monolith",
            user_impact="Checkout fails while monolith remains reachable",
            services_affected="1 application",
            recovery_method="Retry checkout after removing payment failure simulation",
            consistency="Yes",
            fault_isolated="No/Low, failure occurs inside same deployable application",
            extra_notes=[
                "OrderService records failed payment then throws before inventory decrease and order creation, so consistency appears preserved."
            ],
        )
    )
    rows.append(
        build_failure_row(
            test_id="TC-05",
            architecture="Microservices",
            detailed=detailed.get(("TC-05", "Microservices")),
            failure_type="Payment failure simulation",
            injection_method="simulatePaymentFailure=true on order-service checkout",
            affected_component="payment-service",
            user_impact="Checkout fails while catalog and cart may remain available",
            services_affected="payment-service primarily affects checkout path",
            recovery_method="Retry checkout after removing payment failure simulation or restoring payment-service behavior",
            consistency="Yes",
            fault_isolated="Partial/Yes, payment-service failure affects checkout but catalog/cart may remain available",
            extra_notes=[
                "OrderManagementService releases reserved inventory after payment failure and throws before order creation, so consistency appears preserved."
            ],
        )
    )
    rows.append(
        build_failure_row(
            test_id="TC-06",
            architecture="Monolith",
            detailed=detailed.get(("TC-06", "Monolith")),
            failure_type="Inventory failure simulation",
            injection_method="simulateInventoryFailure=true on checkout",
            affected_component="Inventory module inside monolith",
            user_impact="Checkout fails while other monolith endpoints may remain reachable",
            services_affected="1 application",
            recovery_method="Retry checkout after removing inventory failure simulation",
            consistency="Yes",
            fault_isolated="No/Low, inventory module failure affects checkout inside same application",
            extra_notes=[
                "Inventory failure is thrown before inventory decrease, payment success, and order creation, so consistency appears preserved."
            ],
        )
    )
    rows.append(
        build_failure_row(
            test_id="TC-07",
            architecture="Monolith",
            detailed=detailed.get(("TC-07", "Monolith")),
            failure_type="Payment delay simulation",
            injection_method="artificialPaymentDelayMs=1000 on checkout",
            affected_component="Payment module inside monolith",
            user_impact="Checkout remains successful but experiences tail-latency increase",
            services_affected="1 application",
            recovery_method="Remove artificial delay or restore normal payment path",
            consistency="Yes",
            fault_isolated="No/Low, delay occurs inside same deployable application",
            extra_notes=[
                "Payment delay scenario is intended to demonstrate latency sensitivity rather than functional failure."
            ],
        )
    )
    rows.append(
        build_failure_row(
            test_id="TC-07",
            architecture="Microservices",
            detailed=detailed.get(("TC-07", "Microservices")),
            failure_type="Payment delay simulation",
            injection_method="artificialPaymentDelayMs=1000 on order-service checkout",
            affected_component="payment-service",
            user_impact="Checkout remains successful but experiences tail-latency increase",
            services_affected="payment-service primarily affects checkout path",
            recovery_method="Remove artificial delay or restore normal payment-service behavior",
            consistency="Yes",
            fault_isolated="Partial/Yes, delay is concentrated in payment-service path",
            extra_notes=[
                "Payment delay scenario is intended to demonstrate latency sensitivity rather than functional failure."
            ],
        )
    )
    rows.append(
        build_failure_row(
            test_id="TC-06",
            architecture="Microservices",
            detailed=detailed.get(("TC-06", "Microservices")),
            failure_type="Inventory failure simulation",
            injection_method="simulateInventoryFailure=true on order-service checkout",
            affected_component="inventory-service",
            user_impact="Checkout fails while catalog, cart, and payment may remain available",
            services_affected="inventory-service primarily affects checkout path",
            recovery_method="Retry checkout after removing inventory failure simulation or restoring inventory-service behavior",
            consistency="Yes",
            fault_isolated="Partial/Yes, inventory-service failure affects checkout but other services may remain available",
            extra_notes=[
                "Inventory reservation fails before payment processing and order creation, so consistency appears preserved."
            ],
        )
    )

    mono_mttr = mttr_rows.get("Monolith", {})
    rows.append(
        {
            "Test ID": "MTTR-MONOLITH",
            "Failure Type": "Application restart recovery",
            "Injection Method": "Restart monolith process",
            "Architecture": "Monolith",
            "Affected Component": "Whole monolith application",
            "User Impact": "Whole application unavailable during restart window",
            "Services Affected": "1 application",
            "Recovery Method": mono_mttr.get("Recovery Method", "Restart monolith application and wait for health/product endpoint"),
            "MTTR Seconds": mono_mttr.get("MTTR Seconds", ""),
            "Data Consistency Preserved": mono_mttr.get("Data Consistency Preserved", "TODO verify persistence semantics across restart"),
            "Fault Isolated": mono_mttr.get("Fault Isolated", "No, whole application restart"),
            "Notes": mono_mttr.get(
                "Notes",
                "TODO monolith MTTR has not been measured yet. Run bash scripts/measure-monolith-mttr.sh.",
            ),
        }
    )

    micro_mttr = mttr_rows.get("Microservices", {})
    rows.append(
        {
            "Test ID": "MTTR-MICROSERVICES",
            "Failure Type": "Payment service restart recovery",
            "Injection Method": "Stop and restart payment-service container",
            "Architecture": "Microservices",
            "Affected Component": "payment-service",
            "User Impact": "Checkout unavailable while other services may remain available",
            "Services Affected": "payment-service primarily affects checkout path",
            "Recovery Method": micro_mttr.get(
                "Recovery Method",
                "Restart payment-service container and wait for successful checkout",
            ),
            "MTTR Seconds": micro_mttr.get("MTTR Seconds", ""),
            "Data Consistency Preserved": "Yes",
            "Fault Isolated": "Partially/Yes, payment-service affected while other services remain available",
            "Notes": micro_mttr.get("Notes", "Microservices MTTR measured at service level"),
        }
    )

    with OUTPUT_FILE.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=FIELDNAMES)
        writer.writeheader()
        writer.writerows(rows)

    print(f"Wrote {OUTPUT_FILE}")


if __name__ == "__main__":
    main()
