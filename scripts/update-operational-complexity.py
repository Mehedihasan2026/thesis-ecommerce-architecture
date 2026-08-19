#!/usr/bin/env python3
from __future__ import annotations

import csv
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
RESULTS_DIR = REPO_ROOT / "results" / "processed"
DEPLOYMENT_FILE = RESULTS_DIR / "deployment-time-results.csv"
OUTPUT_FILE = RESULTS_DIR / "operational-complexity.csv"

FIELDNAMES = [
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


def read_csv(path: Path) -> list[dict[str, str]]:
    if not path.exists():
        return []
    with path.open(newline="", encoding="utf-8") as handle:
        return list(csv.DictReader(handle))


def deployment_map() -> dict[tuple[str, str], dict[str, str]]:
    mapping: dict[tuple[str, str], dict[str, str]] = {}
    for row in read_csv(DEPLOYMENT_FILE):
        mapping[(row.get("Scenario", ""), row.get("Architecture", ""))] = row
    return mapping


def deployment_time(mapping: dict[tuple[str, str], dict[str, str]], scenario: str, architecture: str) -> tuple[str, str]:
    row = mapping.get((scenario, architecture))
    if not row:
        return "", "TODO measured deployment time not available yet"
    seconds = row.get("Deployment Time Seconds", "")
    notes = row.get("Notes", "") or ""
    if not seconds:
        return "", "TODO measured deployment time not available yet"
    return seconds, notes


def main() -> None:
    mapping = deployment_map()
    rows: list[dict[str, str]] = []

    mono_payment_time, mono_payment_note = deployment_time(mapping, "Full monolith Gradle build", "Monolith")
    micro_payment_time, micro_payment_note = deployment_time(mapping, "Payment-service-only microservices rebuild/restart", "Microservices")
    mono_full_time, mono_full_note = deployment_time(mapping, "Full monolith Gradle build", "Monolith")
    micro_full_time, micro_full_note = deployment_time(mapping, "Full microservices docker compose up -d --build", "Microservices")

    rows.append(
        {
            "Scenario": "Payment logic change",
            "Architecture": "Monolith",
            "Components Changed": "Payment module inside monolith",
            "Services Redeployed": "1 whole application",
            "Deployment Time Seconds": mono_payment_time,
            "Manual Steps Required": "Build and restart monolith",
            "Rollback Complexity": "Redeploy previous monolith version",
            "Monitoring Complexity": "Low, one application to monitor",
            "Notes": "Change is local in code but deployment affects whole application"
            + (f"; {mono_payment_note}" if mono_payment_note else ""),
        }
    )
    rows.append(
        {
            "Scenario": "Payment logic change",
            "Architecture": "Microservices",
            "Components Changed": "payment-service",
            "Services Redeployed": "1 service",
            "Deployment Time Seconds": micro_payment_time,
            "Manual Steps Required": "Build and restart payment-service container",
            "Rollback Complexity": "Redeploy previous payment-service image",
            "Monitoring Complexity": "Medium/High, multiple services and dependencies to monitor",
            "Notes": "Change can be deployed independently but requires service-level monitoring"
            + (f"; {micro_payment_note}" if micro_payment_note else ""),
        }
    )
    rows.append(
        {
            "Scenario": "Full system deployment",
            "Architecture": "Monolith",
            "Components Changed": "Whole application",
            "Services Redeployed": "1 whole application",
            "Deployment Time Seconds": mono_full_time,
            "Manual Steps Required": "Build and restart one deployable unit",
            "Rollback Complexity": "Redeploy previous monolith artifact",
            "Monitoring Complexity": "Low, one runtime unit",
            "Notes": "Simpler deployment model but less granular"
            + (f"; {mono_full_note}" if mono_full_note else ""),
        }
    )
    rows.append(
        {
            "Scenario": "Full system deployment",
            "Architecture": "Microservices",
            "Components Changed": "Multiple services",
            "Services Redeployed": "5 services",
            "Deployment Time Seconds": micro_full_time,
            "Manual Steps Required": "Build and start Docker Compose stack",
            "Rollback Complexity": "More complex because multiple service versions may be involved",
            "Monitoring Complexity": "High, five runtime services",
            "Notes": "More flexible service-level deployment but higher orchestration complexity"
            + (f"; {micro_full_note}" if micro_full_note else ""),
        }
    )

    OUTPUT_FILE.parent.mkdir(parents=True, exist_ok=True)
    with OUTPUT_FILE.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=FIELDNAMES)
        writer.writeheader()
        writer.writerows(rows)

    print(f"Wrote {OUTPUT_FILE}")


if __name__ == "__main__":
    main()
