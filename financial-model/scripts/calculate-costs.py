#!/usr/bin/env python3
"""Generate thesis-ready financial comparison CSVs from editable assumptions."""

from __future__ import annotations

import argparse
import csv
from pathlib import Path
from typing import Dict, Iterable, List, Tuple

ARCHITECTURES = ("Monolith", "Microservices")
MONTH_SECONDS = 60 * 60 * 24 * 30


def parse_args() -> argparse.Namespace:
    repo_root = Path(__file__).resolve().parents[2]
    default_results = repo_root / "results" / "processed"
    default_input = repo_root / "financial-model" / "input"
    default_output = repo_root / "financial-model" / "output"
    return argparse.ArgumentParser(description=__doc__).parse_args(
        []
    ) if False else _build_parser(default_results, default_input, default_output).parse_args()


def _build_parser(default_results: Path, default_input: Path, default_output: Path) -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--architectures",
        nargs="+",
        default=list(ARCHITECTURES),
        help="Architectures to calculate. Use Monolith and/or Microservices.",
    )
    parser.add_argument(
        "--results-dir",
        default=str(default_results),
        help="Directory containing processed KPI CSV files.",
    )
    parser.add_argument(
        "--input-dir",
        default=str(default_input),
        help="Directory containing editable financial assumption CSV files.",
    )
    parser.add_argument(
        "--output-dir",
        default=str(default_output),
        help="Directory to write generated cost CSV files.",
    )
    return parser


def normalize_architecture(value: str) -> str:
    lowered = value.strip().lower()
    if lowered == "monolith":
        return "Monolith"
    if lowered == "microservices":
        return "Microservices"
    raise ValueError(f"Unsupported architecture '{value}'")


def read_csv(path: Path) -> List[Dict[str, str]]:
    if not path.exists():
        return []
    with path.open(newline="", encoding="utf-8") as handle:
        return list(csv.DictReader(handle))


def to_float(value: str | None) -> float | None:
    if value is None:
        return None
    text = value.strip()
    if not text:
        return None
    return float(text)


def write_csv(path: Path, fieldnames: Iterable[str], rows: Iterable[Dict[str, object]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(fieldnames))
        writer.writeheader()
        for row in rows:
            writer.writerow(row)


def format_money(value: float | None) -> str:
    return "" if value is None else f"{value:.2f}"


def format_number(value: float | None, digits: int = 2) -> str:
    return "" if value is None else f"{value:.{digits}f}"


def average(values: Iterable[float]) -> float | None:
    values = list(values)
    if not values:
        return None
    return sum(values) / len(values)


def load_assumptions(input_dir: Path) -> Tuple[Dict[str, Dict[str, float]], Dict[str, Dict[str, float]], Dict[str, Dict[str, float]]]:
    cost_rows = read_csv(input_dir / "cost-assumptions.csv")
    labor_rows = read_csv(input_dir / "labor-assumptions.csv")
    downtime_rows = read_csv(input_dir / "downtime-assumptions.csv")

    def convert(rows: List[Dict[str, str]]) -> Dict[str, Dict[str, float]]:
        data: Dict[str, Dict[str, float]] = {}
        for row in rows:
            arch = normalize_architecture(row["architecture"])
            data[arch] = {
                key: float(value)
                for key, value in row.items()
                if key != "architecture" and value != ""
            }
        return data

    return convert(cost_rows), convert(labor_rows), convert(downtime_rows)


def load_kpi_data(results_dir: Path) -> Dict[str, List[Dict[str, str]]]:
    rows = read_csv(results_dir / "detailed-execution-results.csv")
    grouped: Dict[str, List[Dict[str, str]]] = {arch: [] for arch in ARCHITECTURES}
    for row in rows:
        try:
            grouped[normalize_architecture(row["Architecture"])].append(row)
        except Exception:
            continue
    return grouped


def build_metrics(rows: List[Dict[str, str]]) -> Dict[str, object]:
    throughput_values = [to_float(row.get("Throughput req/s")) for row in rows]
    throughput_values = [value for value in throughput_values if value is not None]
    avg_throughput = average(throughput_values)

    checkout_rows = [row for row in rows if "checkout" in row.get("Test Name", "").lower()]
    successful_checkout_rate_values: List[float] = []
    checkout_throughputs: List[float] = []
    for row in checkout_rows:
        requests_sent = to_float(row.get("Requests Sent"))
        failed_requests = to_float(row.get("Failed Requests"))
        throughput = to_float(row.get("Throughput req/s"))
        if requests_sent and requests_sent > 0 and failed_requests is not None:
            success_rate = max(requests_sent - failed_requests, 0.0) / requests_sent
            successful_checkout_rate_values.append(success_rate)
        if throughput is not None:
            checkout_throughputs.append(throughput)

    avg_checkout_throughput = average(checkout_throughputs)
    avg_checkout_success_rate = average(successful_checkout_rate_values)

    estimated_monthly_requests = avg_throughput * MONTH_SECONDS if avg_throughput is not None else None
    estimated_monthly_successful_checkouts = None
    if avg_checkout_throughput is not None and avg_checkout_success_rate is not None:
        estimated_monthly_successful_checkouts = avg_checkout_throughput * MONTH_SECONDS * avg_checkout_success_rate

    notes: List[str] = []
    if avg_throughput is None:
        notes.append("TODO throughput data missing")
    if estimated_monthly_successful_checkouts is None:
        notes.append("TODO checkout data missing")

    return {
        "average_throughput": avg_throughput,
        "estimated_monthly_requests": estimated_monthly_requests,
        "estimated_monthly_successful_checkouts": estimated_monthly_successful_checkouts,
        "notes": "; ".join(notes),
    }


def calculate_architecture_costs(
    architecture: str,
    cost_data: Dict[str, float] | None,
    labor_data: Dict[str, float] | None,
    downtime_data: Dict[str, float] | None,
    metrics: Dict[str, object] | None,
) -> Dict[str, object]:
    if not cost_data or not labor_data or not downtime_data:
        return {
            "architecture": architecture,
            "notes": "TODO missing assumptions for architecture",
        }

    compute_cost = cost_data["compute_instance_count"] * cost_data["compute_cost_per_instance_month"]
    database_cost = cost_data["database_instance_count"] * cost_data["database_cost_per_instance_month"]
    storage_cost = cost_data["storage_gb"] * cost_data["storage_cost_per_gb_month"]
    network_cost = cost_data["network_gb"] * cost_data["network_cost_per_gb"]
    infrastructure_monthly_cost = (
        compute_cost
        + database_cost
        + storage_cost
        + network_cost
        + cost_data["monitoring_cost_month"]
        + cost_data["backup_cost_month"]
        + cost_data["other_tooling_cost_month"]
    )

    development_cost = labor_data["monthly_development_maintenance_hours"] * labor_data["developer_hourly_rate"]
    operations_cost = labor_data["monthly_operations_hours"] * labor_data["operations_hourly_rate"]
    deployment_labor_cost = (
        (labor_data["average_deployment_minutes"] / 60.0)
        * labor_data["deployments_per_month"]
        * labor_data["operations_hourly_rate"]
    )
    debugging_labor_cost = (
        (labor_data["average_debugging_minutes_per_incident"] / 60.0)
        * labor_data["incidents_per_month"]
        * labor_data["developer_hourly_rate"]
    )
    labor_monthly_cost = development_cost + operations_cost + deployment_labor_cost + debugging_labor_cost

    downtime_hours = downtime_data["incidents_per_month"] * downtime_data["average_mttr_minutes"] / 60.0
    downtime_cost = downtime_hours * downtime_data["estimated_revenue_per_hour"] * downtime_data["percentage_of_users_affected"]

    total_monthly_cost = infrastructure_monthly_cost + labor_monthly_cost + downtime_cost

    average_throughput = metrics.get("average_throughput") if metrics else None
    estimated_monthly_requests = metrics.get("estimated_monthly_requests") if metrics else None
    estimated_monthly_successful_checkouts = metrics.get("estimated_monthly_successful_checkouts") if metrics else None
    metrics_notes = metrics.get("notes") if metrics else "TODO KPI input missing"

    cost_per_request = None
    if estimated_monthly_requests:
        cost_per_request = total_monthly_cost / estimated_monthly_requests

    cost_per_successful_checkout = None
    if estimated_monthly_successful_checkouts:
        cost_per_successful_checkout = total_monthly_cost / estimated_monthly_successful_checkouts

    notes: List[str] = []
    if metrics_notes:
        notes.append(str(metrics_notes))

    return {
        "architecture": architecture,
        "compute_cost": compute_cost,
        "database_cost": database_cost,
        "storage_cost": storage_cost,
        "network_cost": network_cost,
        "monitoring_cost": cost_data["monitoring_cost_month"],
        "backup_cost": cost_data["backup_cost_month"],
        "tooling_cost": cost_data["other_tooling_cost_month"],
        "infrastructure_monthly_cost": infrastructure_monthly_cost,
        "development_cost": development_cost,
        "operations_cost": operations_cost,
        "deployment_labor_cost": deployment_labor_cost,
        "debugging_labor_cost": debugging_labor_cost,
        "labor_monthly_cost": labor_monthly_cost,
        "downtime_cost": downtime_cost,
        "total_monthly_cost": total_monthly_cost,
        "average_throughput": average_throughput,
        "estimated_monthly_requests": estimated_monthly_requests,
        "cost_per_request": cost_per_request,
        "estimated_monthly_successful_checkouts": estimated_monthly_successful_checkouts,
        "cost_per_successful_checkout": cost_per_successful_checkout,
        "incidents_per_month": downtime_data["incidents_per_month"],
        "average_mttr_minutes": downtime_data["average_mttr_minutes"],
        "estimated_revenue_per_hour": downtime_data["estimated_revenue_per_hour"],
        "percentage_users_affected": downtime_data["percentage_of_users_affected"],
        "monthly_maintenance_hours": labor_data["monthly_development_maintenance_hours"],
        "monthly_operations_hours": labor_data["monthly_operations_hours"],
        "deployments_per_month": labor_data["deployments_per_month"],
        "average_deployment_minutes": labor_data["average_deployment_minutes"],
        "debugging_minutes_per_incident": labor_data["average_debugging_minutes_per_incident"],
        "notes": "; ".join(part for part in notes if part),
    }


def compare_costs(monolith: Dict[str, object] | None, microservices: Dict[str, object] | None) -> List[Dict[str, object]]:
    categories = [
        ("Infrastructure Monthly Cost", "infrastructure_monthly_cost"),
        ("Labor Monthly Cost", "labor_monthly_cost"),
        ("Downtime Monthly Cost", "downtime_cost"),
        ("Total Estimated Monthly Cost", "total_monthly_cost"),
        ("Cost Per Request", "cost_per_request"),
        ("Cost Per Successful Checkout", "cost_per_successful_checkout"),
    ]
    rows: List[Dict[str, object]] = []
    for label, key in categories:
        mono_value = monolith.get(key) if monolith else None
        micro_value = microservices.get(key) if microservices else None
        difference = None
        lower = "Insufficient data"
        notes = ""
        if isinstance(mono_value, (int, float)) and isinstance(micro_value, (int, float)):
            difference = micro_value - mono_value
            if mono_value < micro_value:
                lower = "Monolith"
            elif micro_value < mono_value:
                lower = "Microservices"
            else:
                lower = "Equal"
        else:
            notes = "TODO one architecture has not been calculated yet"

        rows.append(
            {
                "Cost Category": label,
                "Monolith Cost": format_money(mono_value if isinstance(mono_value, (int, float)) else None),
                "Microservices Cost": format_money(micro_value if isinstance(micro_value, (int, float)) else None),
                "Difference": format_money(difference),
                "Lower Cost Architecture": lower,
                "Notes": notes,
            }
        )
    return rows


def main() -> None:
    args = parse_args()
    requested_architectures = {normalize_architecture(value) for value in args.architectures}
    input_dir = Path(args.input_dir)
    output_dir = Path(args.output_dir)
    results_dir = Path(args.results_dir)

    cost_assumptions, labor_assumptions, downtime_assumptions = load_assumptions(input_dir)
    kpi_rows = load_kpi_data(results_dir)

    results: Dict[str, Dict[str, object]] = {}
    for architecture in ARCHITECTURES:
        if architecture not in requested_architectures:
            continue
        metrics = build_metrics(kpi_rows.get(architecture, []))
        results[architecture] = calculate_architecture_costs(
            architecture,
            cost_assumptions.get(architecture),
            labor_assumptions.get(architecture),
            downtime_assumptions.get(architecture),
            metrics,
        )

    monthly_rows = []
    request_rows = []
    checkout_rows = []
    failure_rows = []
    operational_rows = []

    for architecture in ARCHITECTURES:
        result = results.get(architecture)
        if result is None:
            monthly_rows.append(
                {
                    "Architecture": architecture,
                    "Compute Cost": "",
                    "Database Cost": "",
                    "Storage Cost": "",
                    "Network Cost": "",
                    "Monitoring Cost": "",
                    "Backup Cost": "",
                    "Tooling Cost": "",
                    "Infrastructure Monthly Cost": "",
                    "Labor Monthly Cost": "",
                    "Downtime Monthly Cost": "",
                    "Total Estimated Monthly Cost": "",
                }
            )
            request_rows.append(
                {
                    "Architecture": architecture,
                    "Total Monthly Cost": "",
                    "Average Throughput req/s": "",
                    "Estimated Monthly Requests": "",
                    "Cost Per Request": "",
                    "Notes": "TODO architecture not calculated in this run",
                }
            )
            checkout_rows.append(
                {
                    "Architecture": architecture,
                    "Total Monthly Cost": "",
                    "Estimated Monthly Successful Checkouts": "",
                    "Cost Per Successful Checkout": "",
                    "Notes": "TODO architecture not calculated in this run",
                }
            )
            failure_rows.append(
                {
                    "Architecture": architecture,
                    "Incidents Per Month": "",
                    "Average MTTR Minutes": "",
                    "Revenue Per Hour": "",
                    "Percentage Users Affected": "",
                    "Estimated Downtime Cost Per Month": "",
                    "Notes": "TODO architecture not calculated in this run",
                }
            )
            operational_rows.append(
                {
                    "Architecture": architecture,
                    "Monthly Maintenance Hours": "",
                    "Monthly Operations Hours": "",
                    "Deployments Per Month": "",
                    "Average Deployment Minutes": "",
                    "Debugging Minutes Per Incident": "",
                    "Labor Monthly Cost": "",
                    "Notes": "TODO architecture not calculated in this run",
                }
            )
            continue

        monthly_rows.append(
            {
                "Architecture": architecture,
                "Compute Cost": format_money(result["compute_cost"]),
                "Database Cost": format_money(result["database_cost"]),
                "Storage Cost": format_money(result["storage_cost"]),
                "Network Cost": format_money(result["network_cost"]),
                "Monitoring Cost": format_money(result["monitoring_cost"]),
                "Backup Cost": format_money(result["backup_cost"]),
                "Tooling Cost": format_money(result["tooling_cost"]),
                "Infrastructure Monthly Cost": format_money(result["infrastructure_monthly_cost"]),
                "Labor Monthly Cost": format_money(result["labor_monthly_cost"]),
                "Downtime Monthly Cost": format_money(result["downtime_cost"]),
                "Total Estimated Monthly Cost": format_money(result["total_monthly_cost"]),
            }
        )
        request_rows.append(
            {
                "Architecture": architecture,
                "Total Monthly Cost": format_money(result["total_monthly_cost"]),
                "Average Throughput req/s": format_number(result["average_throughput"]),
                "Estimated Monthly Requests": format_number(result["estimated_monthly_requests"], digits=0),
                "Cost Per Request": format_number(result["cost_per_request"], digits=10),
                "Notes": result["notes"],
            }
        )
        checkout_rows.append(
            {
                "Architecture": architecture,
                "Total Monthly Cost": format_money(result["total_monthly_cost"]),
                "Estimated Monthly Successful Checkouts": format_number(result["estimated_monthly_successful_checkouts"], digits=0),
                "Cost Per Successful Checkout": format_number(result["cost_per_successful_checkout"], digits=10),
                "Notes": result["notes"],
            }
        )
        failure_rows.append(
            {
                "Architecture": architecture,
                "Incidents Per Month": format_number(result["incidents_per_month"]),
                "Average MTTR Minutes": format_number(result["average_mttr_minutes"]),
                "Revenue Per Hour": format_money(result["estimated_revenue_per_hour"]),
                "Percentage Users Affected": format_number(result["percentage_users_affected"]),
                "Estimated Downtime Cost Per Month": format_money(result["downtime_cost"]),
                "Notes": "Assumption-driven monthly downtime estimate",
            }
        )
        operational_rows.append(
            {
                "Architecture": architecture,
                "Monthly Maintenance Hours": format_number(result["monthly_maintenance_hours"]),
                "Monthly Operations Hours": format_number(result["monthly_operations_hours"]),
                "Deployments Per Month": format_number(result["deployments_per_month"]),
                "Average Deployment Minutes": format_number(result["average_deployment_minutes"]),
                "Debugging Minutes Per Incident": format_number(result["debugging_minutes_per_incident"]),
                "Labor Monthly Cost": format_money(result["labor_monthly_cost"]),
                "Notes": "Based on editable labor assumptions",
            }
        )

    comparison_rows = compare_costs(results.get("Monolith"), results.get("Microservices"))

    write_csv(
        output_dir / "monthly-cost-estimate.csv",
        [
            "Architecture",
            "Compute Cost",
            "Database Cost",
            "Storage Cost",
            "Network Cost",
            "Monitoring Cost",
            "Backup Cost",
            "Tooling Cost",
            "Infrastructure Monthly Cost",
            "Labor Monthly Cost",
            "Downtime Monthly Cost",
            "Total Estimated Monthly Cost",
        ],
        monthly_rows,
    )
    write_csv(
        output_dir / "cost-per-request.csv",
        [
            "Architecture",
            "Total Monthly Cost",
            "Average Throughput req/s",
            "Estimated Monthly Requests",
            "Cost Per Request",
            "Notes",
        ],
        request_rows,
    )
    write_csv(
        output_dir / "cost-per-checkout.csv",
        [
            "Architecture",
            "Total Monthly Cost",
            "Estimated Monthly Successful Checkouts",
            "Cost Per Successful Checkout",
            "Notes",
        ],
        checkout_rows,
    )
    write_csv(
        output_dir / "failure-cost-comparison.csv",
        [
            "Architecture",
            "Incidents Per Month",
            "Average MTTR Minutes",
            "Revenue Per Hour",
            "Percentage Users Affected",
            "Estimated Downtime Cost Per Month",
            "Notes",
        ],
        failure_rows,
    )
    write_csv(
        output_dir / "operational-cost-comparison.csv",
        [
            "Architecture",
            "Monthly Maintenance Hours",
            "Monthly Operations Hours",
            "Deployments Per Month",
            "Average Deployment Minutes",
            "Debugging Minutes Per Incident",
            "Labor Monthly Cost",
            "Notes",
        ],
        operational_rows,
    )
    write_csv(
        output_dir / "architecture-cost-comparison.csv",
        [
            "Cost Category",
            "Monolith Cost",
            "Microservices Cost",
            "Difference",
            "Lower Cost Architecture",
            "Notes",
        ],
        comparison_rows,
    )


if __name__ == "__main__":
    main()
