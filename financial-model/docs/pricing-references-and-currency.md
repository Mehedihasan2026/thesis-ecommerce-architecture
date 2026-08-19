# Pricing References And Currency

## Currency Used

The editable values in:

- `financial-model/input/cost-assumptions.csv`
- `financial-model/input/labor-assumptions.csv`
- `financial-model/input/downtime-assumptions.csv`

are currently modeled in **USD (United States dollars)**.

This means:

- monthly infrastructure costs are interpreted as `USD/month`
- hourly labor rates are interpreted as `USD/hour`
- revenue-loss assumptions are interpreted as `USD/hour`

## What These Prices Represent

The current financial model values are **estimate inputs for relative comparison**, not live cloud quotes or actual invoices. They were kept intentionally simple so the model remains transparent and editable for thesis use.

## Official Pricing References

These are the official vendor pricing tools/pages that should be used to validate or replace the default assumptions before final thesis submission:

- AWS Pricing Calculator: https://aws.amazon.com/pricing/
- AWS Pricing Calculator documentation: https://docs.aws.amazon.com/cost-management/latest/userguide/pricing-calculator.html
- Microsoft Azure Pricing: https://azure.microsoft.com/en-us/pricing
- Google Cloud Pricing Calculator: https://cloud.google.com/products/calculator
- Google Cloud Pricing Overview: https://cloud.google.com/pricing

## How To Use These References

For a more defensible final thesis version:

1. Choose one provider or define a neutral reference deployment.
2. Map the prototype to comparable services:
   - virtual machine or container runtime
   - managed database
   - storage
   - network egress
   - monitoring/logging
   - backup
3. Recalculate the values in `financial-model/input/cost-assumptions.csv`.
4. Keep the same currency throughout the model.

## Thesis Note

In the current repository state, the financial assumptions should be described as:

- **USD-denominated estimate inputs**
- **based on simple comparative modeling**
- **intended for relative monolith vs microservices analysis**
- **to be validated against official cloud pricing calculators before final submission**
