#!/usr/bin/env python3
import csv
import json
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parent.parent
RESULTS_ROOT = Path(__import__("os").environ.get("RESULTS_ROOT", REPO_ROOT / "results"))
RAW_DIR = RESULTS_ROOT / "raw" / "k6"
OUTPUT_FILE = RESULTS_ROOT / "processed" / "detailed-execution-results.csv"

FIELDNAMES = [
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
    "Source File",
    "Notes",
]

TEST_NAME_MAP = {
    "TC-01": "Catalog Baseline",
    "TC-02": "Checkout Baseline",
    "TC-03": "Catalog Load",
    "TC-04": "Checkout Load",
    "TC-05": "Payment Failure Simulation",
    "TC-06": "Inventory Failure Simulation",
    "TC-07": "Payment Delay Simulation",
}

SCENARIO_TYPE_MAP = {
    "TC-01": "Baseline",
    "TC-02": "Baseline",
    "TC-03": "Load",
    "TC-04": "Load",
    "TC-05": "Failure",
    "TC-06": "Failure",
    "TC-07": "Failure",
}

EXPECTED_ARCHITECTURES = ("Monolith", "Microservices")
EXPECTED_TEST_IDS = tuple(TEST_NAME_MAP.keys())


def infer_metadata(source_file: Path) -> tuple[str, str, str]:
    name = source_file.stem
    architecture = "Monolith" if name.startswith("monolith-") else "Microservices"
    remainder = name.split("-", 1)[1]
    parts = remainder.split("-")
    test_id = "-".join(parts[:2])
    return test_id, architecture, name


def metric_value(metrics: dict, metric_name: str, key: str):
    metric = metrics.get(metric_name, {})
    if isinstance(metric, dict):
        if key in metric:
            return metric.get(key)
        if key == "rate" and "value" in metric:
            return metric.get("value")
        values = metric.get("values", {})
        if isinstance(values, dict):
            return values.get(key)
    return None


def fmt(value, digits=2):
    if value is None:
        return ""
    return f"{value:.{digits}f}"


def main():
    OUTPUT_FILE.parent.mkdir(parents=True, exist_ok=True)
    rows_by_key = {}

    for source_file in sorted(RAW_DIR.glob("*.json")):
        notes = []
        with source_file.open("r", encoding="utf-8") as handle:
            data = json.load(handle)

        test_id, architecture, inferred_name = infer_metadata(source_file)
        metrics = data.get("metrics", {})

        requests_sent = metric_value(metrics, "http_reqs", "count")
        throughput = metric_value(metrics, "http_reqs", "rate")
        avg_latency = metric_value(metrics, "http_req_duration", "avg")
        p95_latency = metric_value(metrics, "http_req_duration", "p(95)")
        p99_latency = metric_value(metrics, "http_req_duration", "p(99)")
        failed_rate = metric_value(metrics, "http_req_failed", "rate")

        if requests_sent is None:
            notes.append("TODO missing http_reqs.count")
        if throughput is None:
            notes.append("TODO missing http_reqs.rate")
        if avg_latency is None:
            notes.append("TODO missing http_req_duration.avg")
        if p95_latency is None:
            notes.append("TODO missing http_req_duration p(95)")
        if p99_latency is None:
            notes.append("TODO missing http_req_duration p(99)")
        if failed_rate is None:
            notes.append("TODO missing http_req_failed.rate")

        failed_requests = None
        successful_requests = None
        error_rate = None
        if requests_sent is not None and failed_rate is not None:
            failed_requests = round(float(requests_sent) * float(failed_rate))
            successful_requests = int(float(requests_sent) - failed_requests)
            error_rate = float(failed_rate) * 100.0
        else:
            notes.append("TODO failed/success counts derived from rate unavailable")

        rows_by_key[(test_id, architecture)] = {
            "Test ID": test_id,
            "Architecture": architecture,
            "Test Name": TEST_NAME_MAP.get(test_id, inferred_name),
            "Scenario Type": SCENARIO_TYPE_MAP.get(test_id, ""),
            "Requests Sent": "" if requests_sent is None else int(float(requests_sent)),
            "Failed Requests": "" if failed_requests is None else failed_requests,
            "Successful Requests": "" if successful_requests is None else successful_requests,
            "Avg Response Time ms": fmt(avg_latency),
            "P95 Latency ms": fmt(p95_latency),
            "P99 Latency ms": fmt(p99_latency),
            "Throughput req/s": fmt(throughput),
            "Error Rate %": fmt(error_rate),
            "Source File": source_file.name,
            "Notes": " | ".join(notes),
        }

    rows = []
    for test_id in EXPECTED_TEST_IDS:
        for architecture in EXPECTED_ARCHITECTURES:
            row = rows_by_key.get((test_id, architecture))
            if row is None:
                source_prefix = "monolith" if architecture == "Monolith" else "microservices"
                source_name = f"{source_prefix}-{test_id}-{TEST_NAME_MAP[test_id].lower().replace(' ', '-')}.json"
                row = {
                    "Test ID": test_id,
                    "Architecture": architecture,
                    "Test Name": TEST_NAME_MAP[test_id],
                    "Scenario Type": SCENARIO_TYPE_MAP.get(test_id, ""),
                    "Requests Sent": "",
                    "Failed Requests": "",
                    "Successful Requests": "",
                    "Avg Response Time ms": "",
                    "P95 Latency ms": "",
                    "P99 Latency ms": "",
                    "Throughput req/s": "",
                    "Error Rate %": "",
                    "Source File": source_name,
                    "Notes": "TODO no k6 summary file found",
                }
            rows.append(row)

    with OUTPUT_FILE.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=FIELDNAMES)
        writer.writeheader()
        writer.writerows(rows)

    print(f"Wrote {OUTPUT_FILE}")


if __name__ == "__main__":
    main()
