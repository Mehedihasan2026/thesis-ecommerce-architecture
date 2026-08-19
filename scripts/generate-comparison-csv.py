#!/usr/bin/env python3
import csv
from collections import defaultdict
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parent.parent
RESULTS_ROOT = Path(__import__("os").environ.get("RESULTS_ROOT", REPO_ROOT / "results"))
PROCESSED_DIR = RESULTS_ROOT / "processed"
DETAIL_FILE = PROCESSED_DIR / "detailed-execution-results.csv"
KPI_SUMMARY_FILE = PROCESSED_DIR / "kpi-summary.csv"
FAILURE_RESULTS_FILE = PROCESSED_DIR / "failure-results.csv"
OPS_COMPLEXITY_FILE = PROCESSED_DIR / "operational-complexity.csv"
THESIS_TABLE_FILE = PROCESSED_DIR / "thesis-ready-comparison-table.csv"

EXPECTED_TESTS = [
    ("TC-01", "Baseline", "Catalog Baseline"),
    ("TC-02", "Baseline", "Checkout Baseline"),
    ("TC-03", "Load", "Catalog Load"),
    ("TC-04", "Load", "Checkout Load"),
    ("TC-05", "Failure", "Payment Failure Simulation"),
    ("TC-06", "Failure", "Inventory Failure Simulation"),
    ("TC-07", "Failure", "Payment Delay Simulation"),
]


def read_rows(path: Path):
    if not path.exists():
        return []
    with path.open("r", newline="", encoding="utf-8") as handle:
        return list(csv.DictReader(handle))


def write_rows(path: Path, fieldnames, rows):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)


def as_float(value):
    try:
        return float(value)
    except (TypeError, ValueError):
        return None


def decide_better(monolith_row, micro_row):
    comparisons = []

    for key in ["Avg Response Time ms", "P95 Latency ms", "P99 Latency ms", "Error Rate %"]:
        left = as_float(monolith_row.get(key))
        right = as_float(micro_row.get(key))
        if left is None or right is None:
            return "Insufficient data"
        if left < right:
            comparisons.append("Monolith")
        elif right < left:
            comparisons.append("Microservices")
        else:
            comparisons.append("Tie")

    left = as_float(monolith_row.get("Throughput req/s"))
    right = as_float(micro_row.get("Throughput req/s"))
    if left is None or right is None:
        return "Insufficient data"
    if left > right:
        comparisons.append("Monolith")
    elif right > left:
        comparisons.append("Microservices")
    else:
        comparisons.append("Tie")

    winners = {item for item in comparisons if item != "Tie"}
    if not winners:
        return "Mixed"
    if len(winners) == 1:
        return winners.pop()
    return "Mixed"


def interpretation(monolith_row, micro_row, better):
    if better == "Insufficient data":
        return "At least one KPI is missing for this comparison."

    avg_mono = as_float(monolith_row.get("Avg Response Time ms"))
    avg_micro = as_float(micro_row.get("Avg Response Time ms"))
    thr_mono = as_float(monolith_row.get("Throughput req/s"))
    thr_micro = as_float(micro_row.get("Throughput req/s"))

    avg_note = ""
    thr_note = ""
    if avg_mono is not None and avg_micro is not None:
        avg_note = f"Latency delta {avg_micro - avg_mono:.2f} ms (microservices minus monolith)."
    if thr_mono is not None and thr_micro is not None:
        thr_note = f" Throughput delta {thr_micro - thr_mono:.2f} req/s."

    if better == "Mixed":
        return f"Trade-offs are split across latency, throughput, and error rate.{avg_note}{thr_note}".strip()
    return f"{better} leads on the majority of measured KPIs.{avg_note}{thr_note}".strip()


def ensure_failure_results():
    fieldnames = [
        "Scenario",
        "Architecture",
        "FailureType",
        "StartTimestamp",
        "EndTimestamp",
        "MTTRSeconds",
        "Notes",
    ]
    rows = read_rows(FAILURE_RESULTS_FILE)
    meaningful_rows = [
        row for row in rows
        if any((row.get("StartTimestamp"), row.get("EndTimestamp"), row.get("MTTRSeconds")))
    ]
    if meaningful_rows:
        rows = meaningful_rows
    elif not rows:
        rows = [
            {
                "Scenario": "payment-service-restart",
                "Architecture": "microservices",
                "FailureType": "payment-service outage",
                "StartTimestamp": "",
                "EndTimestamp": "",
                "MTTRSeconds": "",
                "Notes": "Populate via scripts/measure-mttr.sh",
            }
        ]
    write_rows(FAILURE_RESULTS_FILE, fieldnames, rows)


def write_operational_complexity():
    fieldnames = [
        "Scenario",
        "Architecture",
        "Components Changed",
        "Services Redeployed",
        "Deployment Time Seconds",
        "Manual Steps Required",
        "Rollback Complexity",
        "Monitoring Complexity",
        "Notes",
    ]
    rows = [
        {
            "Scenario": "Payment logic change in monolith",
            "Architecture": "Monolith",
            "Components Changed": "Payment module within single deployable",
            "Services Redeployed": "1",
            "Deployment Time Seconds": "",
            "Manual Steps Required": "",
            "Rollback Complexity": "",
            "Monitoring Complexity": "",
            "Notes": "",
        },
        {
            "Scenario": "Payment logic change in microservices",
            "Architecture": "Microservices",
            "Components Changed": "payment-service and any dependent contract updates",
            "Services Redeployed": "",
            "Deployment Time Seconds": "",
            "Manual Steps Required": "",
            "Rollback Complexity": "",
            "Monitoring Complexity": "",
            "Notes": "",
        },
        {
            "Scenario": "Full system deployment in monolith",
            "Architecture": "Monolith",
            "Components Changed": "Single application package",
            "Services Redeployed": "1",
            "Deployment Time Seconds": "",
            "Manual Steps Required": "",
            "Rollback Complexity": "",
            "Monitoring Complexity": "",
            "Notes": "",
        },
        {
            "Scenario": "Full system deployment in microservices",
            "Architecture": "Microservices",
            "Components Changed": "catalog-service, cart-service, inventory-service, payment-service, order-service",
            "Services Redeployed": "5",
            "Deployment Time Seconds": "",
            "Manual Steps Required": "",
            "Rollback Complexity": "",
            "Monitoring Complexity": "",
            "Notes": "",
        },
    ]
    write_rows(OPS_COMPLEXITY_FILE, fieldnames, rows)


def main():
    rows = read_rows(DETAIL_FILE)
    grouped = defaultdict(dict)
    for row in rows:
        grouped[row["Test ID"]][row["Architecture"]] = row

    kpi_fieldnames = [
        "Test ID",
        "Architecture",
        "Test Name",
        "Scenario Type",
        "Requests Sent",
        "Failed Requests",
        "Successful Requests",
        "Avg Response Time ms",
        "P95 Latency ms",
        "P99 Latency ms",
        "Throughput req/s",
        "Error Rate %",
        "Notes",
    ]
    kpi_rows = [{field: row.get(field, "") for field in kpi_fieldnames} for row in rows]
    write_rows(KPI_SUMMARY_FILE, kpi_fieldnames, kpi_rows)

    thesis_rows = []
    for test_id, default_category, default_name in EXPECTED_TESTS:
        architectures = grouped.get(test_id, {})
        monolith_row = architectures.get("Monolith", {})
        micro_row = architectures.get("Microservices", {})
        better = decide_better(monolith_row, micro_row) if monolith_row and micro_row else "Insufficient data"
        representative_row = monolith_row or micro_row
        thesis_rows.append(
            {
                "Test ID": test_id,
                "Test Category": representative_row.get("Scenario Type", default_category),
                "Test Name": representative_row.get("Test Name", default_name),
                "Monolith Avg Response Time ms": monolith_row.get("Avg Response Time ms", ""),
                "Microservices Avg Response Time ms": micro_row.get("Avg Response Time ms", ""),
                "Monolith P95 ms": monolith_row.get("P95 Latency ms", ""),
                "Microservices P95 ms": micro_row.get("P95 Latency ms", ""),
                "Monolith P99 ms": monolith_row.get("P99 Latency ms", ""),
                "Microservices P99 ms": micro_row.get("P99 Latency ms", ""),
                "Monolith Throughput req/s": monolith_row.get("Throughput req/s", ""),
                "Microservices Throughput req/s": micro_row.get("Throughput req/s", ""),
                "Monolith Error Rate %": monolith_row.get("Error Rate %", ""),
                "Microservices Error Rate %": micro_row.get("Error Rate %", ""),
                "Better Architecture Based on KPI": better,
                "Interpretation Notes": interpretation(monolith_row, micro_row, better) if monolith_row and micro_row else "At least one architecture result is missing.",
            }
        )

    thesis_fieldnames = [
        "Test ID",
        "Test Category",
        "Test Name",
        "Monolith Avg Response Time ms",
        "Microservices Avg Response Time ms",
        "Monolith P95 ms",
        "Microservices P95 ms",
        "Monolith P99 ms",
        "Microservices P99 ms",
        "Monolith Throughput req/s",
        "Microservices Throughput req/s",
        "Monolith Error Rate %",
        "Microservices Error Rate %",
        "Better Architecture Based on KPI",
        "Interpretation Notes",
    ]
    write_rows(THESIS_TABLE_FILE, thesis_fieldnames, thesis_rows)

    ensure_failure_results()
    write_operational_complexity()

    print(f"Wrote {KPI_SUMMARY_FILE}")
    print(f"Wrote {FAILURE_RESULTS_FILE}")
    print(f"Wrote {OPS_COMPLEXITY_FILE}")
    print(f"Wrote {THESIS_TABLE_FILE}")


if __name__ == "__main__":
    main()
