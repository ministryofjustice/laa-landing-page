# STB-4126: One-Off SiLAS / Entra User Sync Validation

One-time validation script to confirm SiLAS and Entra user records are in sync following the fix for handling users who already exist in Entra at the point of SiLAS user creation.

**No automated jobs, scheduled tasks, or recurring processes are introduced by this script.**

---

## Background

The fix (`UserService.createUser()`) added handling for when Entra returns HTTP 200 / "User already exists" during registration. Before the fix, users re-created in SiLAS after already existing in Entra could have had misaligned account state — particularly: disabled in Entra, not re-enabled. This script identifies any residual discrepancies.

---

## Prerequisites

### 1. Python dependencies

```bash
pip install -r STB-4126/requirements.txt
```

### 2. Port-forward to the target database

Use `scripts/setup-port-forwards.sh` or set up manually:

| Environment | Namespace                   | Local port |
|-------------|-----------------------------|------------|
| Staging     | `laa-landing-page-test`     | `5433`     |
| Production  | `laa-landing-page-prod`     | `5435`     |

```bash
kubectl -n laa-landing-page-test port-forward <pod> 5433:5432
```

### 3. Export database credentials

Use `scripts/get-db-credentials.sh` to retrieve values, then export:

```bash
export POSTGRES_DB_ADDRESS=localhost
export POSTGRES_DB_NAME=<db_name>
export POSTGRES_USERNAME=<username>
export POSTGRES_PASSWORD=<password>
```

### 4. Export Entra credentials

Service principal with `User.Read.All` on MS Graph:

```bash
export ENTRA_TENANT_ID=<tenant_id>
export ENTRA_CLIENT_ID=<client_id>
export ENTRA_CLIENT_SECRET=<client_secret>
```

---

## Usage

```bash
# Staging
python3 STB-4126/validate_silas_entra_sync.py --env staging

# Production
python3 STB-4126/validate_silas_entra_sync.py --env prod
```

---

## Output

| Output | Description |
|--------|-------------|
| Console summary | Totals per system, mismatch counts by type, table of fix-candidate users |
| `silas_entra_comparison_{env}_{timestamp}.csv` | All records joined on Entra OID, with a `mismatch_flags` column |
| `silas_entra_mismatches_{env}_{timestamp}.csv` | Rows with at least one mismatch flag (not written if none found) |

CSVs are written to the current working directory.

---

## Mismatch flags

| Flag | Meaning |
|------|---------|
| `enabled_mismatch` | SiLAS `enabled` differs from Entra `accountEnabled` |
| `email_mismatch` | SiLAS `email` differs from Entra `mail` on the same OID |
| `only_in_silas` | OID present in SiLAS but not found in Entra |
| `only_in_entra` | OID present in Entra but not found in SiLAS |

## Fix candidates

Rows where `fix_candidate = True` are users where the `enableUserOnRecreate` code path was triggered — detected by an `ENABLED` audit event recorded within 60 seconds of `created_date` by the same actor who created the user. These are the highest-priority records to review.

---

## Acceptance criteria

| AC | How satisfied |
|----|---------------|
| AC1 – Validation completed | Run script against staging and prod |
| AC2 – Discrepancies identified | Review mismatch CSV and console summary |
| AC3 – Outcome recorded | Record result in Jira ticket (no action / manual fix / follow-up story) |
| AC4 – No automated process introduced | Script is run once manually; no jobs or schedules created |
