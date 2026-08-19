#!/usr/bin/env python3
from __future__ import annotations

import csv
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
RESULTS_DIR = REPO_ROOT / "results" / "processed"
COMPARISON_FILE = RESULTS_DIR / "thesis-ready-comparison-table.csv"
MONTHLY_COST_FILE = REPO_ROOT / "financial-model" / "output" / "monthly-cost-estimate.csv"
ARCH_COST_FILE = REPO_ROOT / "financial-model" / "output" / "architecture-cost-comparison.csv"
REQUEST_COST_FILE = REPO_ROOT / "financial-model" / "output" / "cost-per-request.csv"
CHECKOUT_COST_FILE = REPO_ROOT / "financial-model" / "output" / "cost-per-checkout.csv"
FINANCIAL_SUMMARY_FILE = RESULTS_DIR / "financial-summary-for-thesis.csv"


def read_csv(path: Path) -> list[dict[str, str]]:
    if not path.exists():
        return []
    with path.open(newline="", encoding="utf-8") as handle:
        return list(csv.DictReader(handle))


def to_float(value: str | None) -> float | None:
    if value is None:
        return None
    value = value.strip()
    if not value:
        return None
    return float(value)


def enrich_notes(rows: list[dict[str, str]]) -> list[dict[str, str]]:
    for row in rows:
        test_id = row.get("Test ID", "")
        category = row.get("Test Category", "")
        mono_avg = to_float(row.get("Monolith Avg Response Time ms"))
        micro_avg = to_float(row.get("Microservices Avg Response Time ms"))
        mono_tp = to_float(row.get("Monolith Throughput req/s"))
        micro_tp = to_float(row.get("Microservices Throughput req/s"))
        mono_err = to_float(row.get("Monolith Error Rate %"))
        micro_err = to_float(row.get("Microservices Error Rate %"))

        notes: list[str] = []
        if test_id in {"TC-01", "TC-03"} and micro_avg is not None and mono_avg is not None and micro_tp is not None and mono_tp is not None:
            if micro_avg < mono_avg and micro_tp > mono_tp:
                notes.append(
                    "Microservices performed better for isolated read-heavy catalog traffic, suggesting that separated catalog service can handle browsing workload efficiently."
                )
        elif test_id in {"TC-02", "TC-04"} and micro_avg is not None and mono_avg is not None and micro_tp is not None and mono_tp is not None:
            if mono_avg < micro_avg and mono_tp > micro_tp:
                notes.append(
                    "Monolith performed better for coordinated checkout flow, likely because local in-process calls avoid inter-service communication overhead."
                )
        elif category == "Failure" and test_id in {"TC-05", "TC-06"}:
            if mono_avg is not None and micro_avg is not None and mono_err is not None and micro_err is not None and abs(mono_err - micro_err) < 0.05:
                notes.append(
                    "Both architectures expose the simulated failure, but request-level error rate reflects the multi-step k6 flow. Monolith shows lower latency, while microservices may provide better component-level fault isolation."
                )
        if test_id == "TC-07":
            notes.append(
                "p95/p99 latency are more meaningful than average because the test includes fast setup requests and delayed checkout requests."
            )

        mono_p95 = row.get("Monolith P95 ms", "")
        micro_p95 = row.get("Microservices P95 ms", "")
        mono_p99 = row.get("Monolith P99 ms", "")
        micro_p99 = row.get("Microservices P99 ms", "")
        notes.append(
            f"Observed p95 values monolith={mono_p95} ms, microservices={micro_p95} ms; p99 values monolith={mono_p99} ms, microservices={micro_p99} ms."
        )
        row["Interpretation Notes"] = " ".join(notes)
    return rows


def write_financial_summary() -> None:
    monthly = {row["Architecture"]: row for row in read_csv(MONTHLY_COST_FILE)}
    architecture_costs = {row["Cost Category"]: row for row in read_csv(ARCH_COST_FILE)}
    request_costs = {row["Architecture"]: row for row in read_csv(REQUEST_COST_FILE)}
    checkout_costs = {row["Architecture"]: row for row in read_csv(CHECKOUT_COST_FILE)}

    rows = [
        {
            "Metric": "Total monthly monolith cost",
            "Value": monthly.get("Monolith", {}).get("Total Estimated Monthly Cost", ""),
            "Interpretation": "Financial model is estimate-based and should be interpreted as relative comparison rather than actual production invoice.",
        },
        {
            "Metric": "Total monthly microservices cost",
            "Value": monthly.get("Microservices", {}).get("Total Estimated Monthly Cost", ""),
            "Interpretation": "Financial model is estimate-based and should be interpreted as relative comparison rather than actual production invoice.",
        },
        {
            "Metric": "Infrastructure cost comparison",
            "Value": architecture_costs.get("Infrastructure Monthly Cost", {}).get("Lower Cost Architecture", ""),
            "Interpretation": "Monolith has lower estimated total monthly cost due to lower infrastructure and operational overhead.",
        },
        {
            "Metric": "Labor cost comparison",
            "Value": architecture_costs.get("Labor Monthly Cost", {}).get("Lower Cost Architecture", ""),
            "Interpretation": "Monolith has lower estimated total monthly cost due to lower infrastructure and operational overhead.",
        },
        {
            "Metric": "Downtime cost comparison",
            "Value": architecture_costs.get("Downtime Monthly Cost", {}).get("Lower Cost Architecture", ""),
            "Interpretation": "Microservices has lower estimated downtime cost if failures affect a smaller part of the system.",
        },
        {
            "Metric": "Cost per request",
            "Value": f"Monolith={request_costs.get('Monolith', {}).get('Cost Per Request', '')}, Microservices={request_costs.get('Microservices', {}).get('Cost Per Request', '')}",
            "Interpretation": "Financial model is estimate-based and should be interpreted as relative comparison rather than actual production invoice.",
        },
        {
            "Metric": "Cost per checkout",
            "Value": f"Monolith={checkout_costs.get('Monolith', {}).get('Cost Per Successful Checkout', '')}, Microservices={checkout_costs.get('Microservices', {}).get('Cost Per Successful Checkout', '')}",
            "Interpretation": "Financial model is estimate-based and should be interpreted as relative comparison rather than actual production invoice.",
        },
    ]

    with FINANCIAL_SUMMARY_FILE.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=["Metric", "Value", "Interpretation"])
        writer.writeheader()
        writer.writerows(rows)


def main() -> None:
    rows = read_csv(COMPARISON_FILE)
    enriched = enrich_notes(rows)
    with COMPARISON_FILE.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=rows[0].keys())
        writer.writeheader()
        writer.writerows(enriched)
    write_financial_summary()
    print(f"Wrote {COMPARISON_FILE}")
    print(f"Wrote {FINANCIAL_SUMMARY_FILE}")


if __name__ == "__main__":
    main()
