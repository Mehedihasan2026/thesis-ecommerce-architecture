# Financial Model

This folder contains a simple financial comparison model for the thesis. The model converts technical benchmarking outputs and editable cost assumptions into monthly cost estimates for `Monolith` and `Microservices`.

## What Each Cost Category Means

- `Infrastructure cost`: compute, database, storage, network, monitoring, backup, and other tooling.
- `Labor cost`: development maintenance time, operations time, deployment effort, and debugging effort.
- `Downtime cost`: estimated business loss from incidents using revenue-per-hour, MTTR, and affected-user share.
- `Cost per request`: total monthly cost divided by estimated monthly requests from measured throughput.
- `Cost per successful checkout`: total monthly cost divided by estimated monthly successful checkout volume from checkout throughput and success rate.

## Formulas Used

Infrastructure monthly cost:

```text
compute_cost = compute_instance_count * compute_cost_per_instance_month
database_cost = database_instance_count * database_cost_per_instance_month
storage_cost = storage_gb * storage_cost_per_gb_month
network_cost = network_gb * network_cost_per_gb
infrastructure_monthly_cost =
  compute_cost + database_cost + storage_cost + network_cost +
  monitoring_cost_month + backup_cost_month + other_tooling_cost_month
```

Labor monthly cost:

```text
development_cost = monthly_development_maintenance_hours * developer_hourly_rate
operations_cost = monthly_operations_hours * operations_hourly_rate
deployment_labor_cost = (average_deployment_minutes / 60) * deployments_per_month * operations_hourly_rate
debugging_labor_cost = (average_debugging_minutes_per_incident / 60) * incidents_per_month * developer_hourly_rate
labor_monthly_cost = development_cost + operations_cost + deployment_labor_cost + debugging_labor_cost
```

Downtime monthly cost:

```text
downtime_hours = incidents_per_month * average_mttr_minutes / 60
downtime_cost = downtime_hours * estimated_revenue_per_hour * percentage_of_users_affected
```

Request-based costs:

```text
average_throughput = average of "Throughput req/s" rows for one architecture
estimated_monthly_requests = average_throughput * 60 * 60 * 24 * 30
cost_per_request = total_monthly_cost / estimated_monthly_requests
```

Checkout-based costs:

```text
checkout_success_rate = average((requests_sent - failed_requests) / requests_sent) across checkout rows
estimated_monthly_successful_checkouts =
  average_checkout_throughput * 60 * 60 * 24 * 30 * checkout_success_rate
cost_per_successful_checkout = total_monthly_cost / estimated_monthly_successful_checkouts
```

## How To Change Assumptions

Edit these CSV files:

- `financial-model/input/cost-assumptions.csv`
- `financial-model/input/labor-assumptions.csv`
- `financial-model/input/downtime-assumptions.csv`

All numbers are plain editable values so the model remains transparent in the thesis.

## How To Run

Run both architectures:

```bash
python financial-model/scripts/calculate-costs.py
```

Run only one architecture:

```bash
python financial-model/scripts/calculate-costs.py --architectures Monolith
python financial-model/scripts/calculate-costs.py --architectures Microservices
```

Use a different processed KPI directory:

```bash
python financial-model/scripts/calculate-costs.py --results-dir results/isolated/comparison/processed
```

## Output Files

- `financial-model/output/architecture-cost-comparison.csv`
- `financial-model/output/monthly-cost-estimate.csv`
- `financial-model/output/cost-per-request.csv`
- `financial-model/output/cost-per-checkout.csv`
- `financial-model/output/failure-cost-comparison.csv`
- `financial-model/output/operational-cost-comparison.csv`

## How To Verify Cloud Prices Manually

This model uses editable placeholder assumptions, not live provider pricing. Before final thesis submission, verify values manually with official calculators:

- AWS Pricing Calculator
- Azure Pricing Calculator
- Google Cloud Pricing Calculator

Use those tools to check compute instance sizes, managed database tiers, storage, network egress, monitoring, and backup services. Then update the CSV assumptions accordingly.

## Limitations

- These are estimated costs, not actual production invoices.
- Local prototype results are used to support relative comparison.
- Cloud prices should be verified using official pricing calculators before final submission.
- Throughput-based monthly volumes are extrapolations from local benchmark measurements.
- Downtime cost is assumption-driven and should be interpreted as scenario modeling, not audited business loss.

## Thesis Use

- Microservices may have higher infrastructure and monitoring cost but may reduce failure impact or allow selective scaling.
- Monolith may have lower infrastructure and operational cost but may scale less selectively and create broader failure impact.
- The financial conclusion should be interpreted together with technical KPIs.

This model supports the thesis decision framework by making the trade-off between technical performance and estimated cost explicit in a form that can be adjusted, reproduced, and defended.
