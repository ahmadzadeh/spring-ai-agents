---
name: supplier-synthesis
description: Decision framework for synthesizing supplier evaluation findings into an onboarding recommendation
---

# Supplier Synthesis Skill

## Decision logic

Apply the following rules in order:

1. **REJECT** if any single risk dimension scores 5 (Critical)
2. **REJECT** if Overall Risk Score > 4.0
3. **APPROVE WITH CONDITIONS** if Overall Risk Score is 3.0–4.0
4. **APPROVE WITH CONDITIONS** if 2 or more dimensions score Amber (3)
5. **APPROVE** if Overall Risk Score < 3.0 and no single dimension scores > 3

If multiple rules apply, use the most restrictive outcome.

## Conditions catalogue

When issuing "APPROVE WITH CONDITIONS", select from these standard conditions:

- **C1**: Supplier must provide verified copies of [certification] within 30 days
- **C2**: Annual security audit to be conducted by a mutually agreed third party
- **C3**: Contract must include data processing agreement (DPA) with GDPR clauses
- **C4**: Supplier must provide audited financial statements for the last 2 years
- **C5**: Business continuity plan to be reviewed and approved before contract signing
- **C6**: Quarterly performance reviews for first 12 months
- **C7**: Pilot engagement limited to non-critical workloads for 6 months

## Follow-up checklist rules

Always include the following follow-up items:
- Verify all claimed certifications directly with issuing bodies
- Confirm legal entity registration with national registry
- Obtain bank reference or credit check
- Review supplier's subcontractor / fourth-party risk policy

Add dimension-specific follow-ups for each Amber/Red dimension.
