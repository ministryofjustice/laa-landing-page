#!/usr/bin/env python3
"""
STB-4126: One-Off Manual Validation of SiLAS and Entra User Sync After Fix

Compares all user records between SiLAS (PostgreSQL) and Entra (MS Graph API).
Identifies mismatches in: enabled status, email, and records present in only one system.
Also flags users where the 'User already exists in Entra' fix was triggered
(fingerprint: audit ENABLED event recorded at creation time).

Usage:
    python validate_silas_entra_sync.py --env staging
    python validate_silas_entra_sync.py --env prod

Prerequisites:
    1. Port-forward to the target DB:
       - Staging (NLE): kubectl -n laa-landing-page-test port-forward ... 5433:5433
       - Prod:          kubectl -n laa-landing-page-prod port-forward ... 5435:5432
       (See scripts/setup-port-forwards.sh for the full commands)

    2. Export DB credentials (from scripts/get-db-credentials.sh):
       export POSTGRES_DB_ADDRESS=localhost
       export POSTGRES_DB_NAME=<db_name>
       export POSTGRES_USERNAME=<username>
       export POSTGRES_PASSWORD=<password>

    3. Export Entra credentials (service principal with User.Read.All):
       export ENTRA_TENANT_ID=<tenant_id>
       export ENTRA_CLIENT_ID=<client_id>
       export ENTRA_CLIENT_SECRET=<client_secret>

    4. Install dependencies:
       pip install -r STB-4126/requirements.txt
"""

import argparse
import csv
import os
import sys
from datetime import datetime

try:
    import psycopg2
    from psycopg2.extras import RealDictCursor
except ImportError:
    print("Error: psycopg2 is not installed. Run: pip install psycopg2-binary")
    sys.exit(1)

try:
    import requests
except ImportError:
    print("Error: requests is not installed. Run: pip install requests")
    sys.exit(1)

try:
    import pandas as pd
except ImportError:
    print("Error: pandas is not installed. Run: pip install pandas")
    sys.exit(1)


# ── Environment configuration ─────────────────────────────────────────────────

ENV_DB_PORTS = {
    "staging": 5433,
    "prod": 5435,
}


# ── Database ──────────────────────────────────────────────────────────────────

def get_db_connection(env: str):
    port = ENV_DB_PORTS[env]
    try:
        conn = psycopg2.connect(
            host=os.getenv("POSTGRES_DB_ADDRESS", "localhost"),
            port=os.getenv("POSTGRES_DB_PORT", str(port)),
            database=os.getenv("POSTGRES_DB_NAME", "portal-database"),
            user=os.getenv("POSTGRES_USERNAME", "laa"),
            password=os.getenv("POSTGRES_PASSWORD", "laa"),
        )
        return conn
    except psycopg2.Error as e:
        print(f"[DB] Connection failed: {e}")
        print("\nMake sure:")
        print(f"  1. Port-forward is active on port {port}")
        print("  2. DB credentials are exported as environment variables")
        print("     (run scripts/get-db-credentials.sh to retrieve them)")
        sys.exit(1)


SILAS_QUERY = """
SELECT
    eu.entra_oid,
    eu.email,
    eu.first_name,
    eu.last_name,
    eu.enabled,
    eu.status,
    eu.invitation_status,
    eu.disable_type,
    eu.disabled_by::text                        AS disabled_by,
    eu.last_synced_on,
    eu.created_date,
    eu.last_modified_date,
    eu.created_by,
    -- Most recent audit event for this user
    (
        SELECT a.status_change
        FROM user_account_status_audit a
        WHERE a.entra_user_id = eu.id
        ORDER BY a.status_changed_date DESC
        LIMIT 1
    )                                           AS latest_audit_status,
    -- Fingerprint of enableUserOnRecreate:
    -- an ENABLED audit event whose status_changed_by matches created_by,
    -- recorded within 60 seconds of the user's created_date.
    EXISTS (
        SELECT 1
        FROM user_account_status_audit a
        WHERE a.entra_user_id = eu.id
          AND a.status_change  = 'ENABLED'
          AND a.status_changed_by::text = eu.created_by::text
          AND a.status_changed_date BETWEEN eu.created_date
                                        AND eu.created_date + INTERVAL '60 seconds'
    )                                           AS fix_candidate
FROM entra_user eu
ORDER BY eu.email;
"""


def extract_silas_users(conn) -> list[dict]:
    with conn.cursor(cursor_factory=RealDictCursor) as cur:
        cur.execute(SILAS_QUERY)
        rows = [dict(row) for row in cur.fetchall()]
    print(f"[SiLAS] Extracted {len(rows)} user records")
    return rows


# ── Entra / MS Graph ──────────────────────────────────────────────────────────

GRAPH_TOKEN_URL = "https://login.microsoftonline.com/{tenant_id}/oauth2/v2.0/token"
GRAPH_USERS_URL = (
    "https://graph.microsoft.com/v1.0/users"
    "?$select=id,mail,accountEnabled,identities,givenName,surname,createdDateTime"
    "&$top=999"
)


def get_graph_token(tenant_id: str, client_id: str, client_secret: str) -> str:
    url = GRAPH_TOKEN_URL.format(tenant_id=tenant_id)
    resp = requests.post(
        url,
        data={
            "grant_type": "client_credentials",
            "client_id": client_id,
            "client_secret": client_secret,
            "scope": "https://graph.microsoft.com/.default",
        },
        timeout=30,
    )
    resp.raise_for_status()
    token = resp.json().get("access_token")
    if not token:
        print("[Entra] Failed to obtain access token")
        sys.exit(1)
    return token


def _resolve_email(user: dict) -> str | None:
    """
    B2C users may have their email in 'mail' or inside 'identities'.
    Prefer 'mail', fall back to the issuerAssignedId of the first
    emailAddress-type identity.
    """
    if user.get("mail"):
        return user["mail"].lower().strip()
    for identity in user.get("identities") or []:
        if identity.get("signInType") == "emailAddress":
            assigned = identity.get("issuerAssignedId")
            if assigned:
                return assigned.lower().strip()
    return None


def extract_entra_users(token: str) -> dict[str, dict]:
    """Return a dict keyed on Entra object ID."""
    headers = {"Authorization": f"Bearer {token}"}
    users: dict[str, dict] = {}
    url: str | None = GRAPH_USERS_URL
    page = 0

    while url:
        resp = requests.get(url, headers=headers, timeout=30)
        resp.raise_for_status()
        data = resp.json()
        page_users = data.get("value", [])
        for u in page_users:
            users[u["id"]] = {
                "entra_id": u["id"],
                "entra_mail": _resolve_email(u),
                "entra_account_enabled": u.get("accountEnabled"),
                "entra_given_name": u.get("givenName"),
                "entra_surname": u.get("surname"),
                "entra_created_date_time": u.get("createdDateTime"),
            }
        page += 1
        url = data.get("@odata.nextLink")
        if url:
            print(f"[Entra] Fetched page {page} ({len(users)} users so far) — paginating...")

    print(f"[Entra] Extracted {len(users)} user records across {page} page(s)")
    return users


# ── Comparison ────────────────────────────────────────────────────────────────

def compare(silas_rows: list[dict], entra_users: dict[str, dict]) -> tuple[list[dict], list[dict]]:
    silas_by_oid = {row["entra_oid"]: row for row in silas_rows if row["entra_oid"]}
    entra_oids = set(entra_users.keys())
    silas_oids = set(silas_by_oid.keys())

    all_rows: list[dict] = []
    mismatch_rows: list[dict] = []

    # ── Records present in both systems ───────────────────────────────────────
    for oid in silas_oids & entra_oids:
        s = silas_by_oid[oid]
        e = entra_users[oid]

        flags = []

        if s["enabled"] != e["entra_account_enabled"]:
            flags.append("enabled_mismatch")

        silas_email = (s["email"] or "").lower().strip()
        entra_email = e["entra_mail"] or ""
        if silas_email and entra_email and silas_email != entra_email:
            flags.append("email_mismatch")

        row = {
            "entra_oid": oid,
            "silas_email": s["email"],
            "entra_email": e["entra_mail"],
            "silas_enabled": s["enabled"],
            "entra_account_enabled": e["entra_account_enabled"],
            "silas_status": s["status"],
            "silas_invitation_status": s["invitation_status"],
            "silas_disable_type": s["disable_type"],
            "silas_disabled_by": s["disabled_by"],
            "silas_last_synced_on": s["last_synced_on"],
            "silas_created_date": s["created_date"],
            "silas_last_modified_date": s["last_modified_date"],
            "latest_audit_status": s["latest_audit_status"],
            "fix_candidate": s["fix_candidate"],
            "present_in": "both",
            "mismatch_flags": "|".join(flags) if flags else "",
        }
        all_rows.append(row)
        if flags:
            mismatch_rows.append(row)

    # ── Only in SiLAS ─────────────────────────────────────────────────────────
    for oid in silas_oids - entra_oids:
        s = silas_by_oid[oid]
        row = {
            "entra_oid": oid,
            "silas_email": s["email"],
            "entra_email": None,
            "silas_enabled": s["enabled"],
            "entra_account_enabled": None,
            "silas_status": s["status"],
            "silas_invitation_status": s["invitation_status"],
            "silas_disable_type": s["disable_type"],
            "silas_disabled_by": s["disabled_by"],
            "silas_last_synced_on": s["last_synced_on"],
            "silas_created_date": s["created_date"],
            "silas_last_modified_date": s["last_modified_date"],
            "latest_audit_status": s["latest_audit_status"],
            "fix_candidate": s["fix_candidate"],
            "present_in": "silas_only",
            "mismatch_flags": "only_in_silas",
        }
        all_rows.append(row)
        mismatch_rows.append(row)

    # ── Only in Entra ─────────────────────────────────────────────────────────
    for oid in entra_oids - silas_oids:
        e = entra_users[oid]
        row = {
            "entra_oid": oid,
            "silas_email": None,
            "entra_email": e["entra_mail"],
            "silas_enabled": None,
            "entra_account_enabled": e["entra_account_enabled"],
            "silas_status": None,
            "silas_invitation_status": None,
            "silas_disable_type": None,
            "silas_disabled_by": None,
            "silas_last_synced_on": None,
            "silas_created_date": None,
            "silas_last_modified_date": None,
            "latest_audit_status": None,
            "fix_candidate": False,
            "present_in": "entra_only",
            "mismatch_flags": "only_in_entra",
        }
        all_rows.append(row)
        mismatch_rows.append(row)

    return all_rows, mismatch_rows


# ── Reporting ─────────────────────────────────────────────────────────────────

def print_summary(env: str, silas_rows: list[dict], entra_users: dict, all_rows: list[dict], mismatch_rows: list[dict]):
    silas_oids = {r["entra_oid"] for r in silas_rows if r["entra_oid"]}
    entra_oids = set(entra_users.keys())

    enabled_mismatches  = [r for r in mismatch_rows if "enabled_mismatch"  in r["mismatch_flags"]]
    email_mismatches    = [r for r in mismatch_rows if "email_mismatch"    in r["mismatch_flags"]]
    only_in_silas       = [r for r in mismatch_rows if r["present_in"] == "silas_only"]
    only_in_entra       = [r for r in mismatch_rows if r["present_in"] == "entra_only"]
    fix_candidates      = [r for r in all_rows if r["fix_candidate"]]

    bar = "=" * 60
    print(f"\n{bar}")
    print(f"  SiLAS / Entra Sync Validation — {env.upper()}")
    print(bar)
    print(f"  SiLAS users checked       : {len(silas_rows)}")
    print(f"  Entra users checked       : {len(entra_users)}")
    print(f"  Matched on OID            : {len(silas_oids & entra_oids)}")
    print(f"  Enabled status mismatches : {len(enabled_mismatches)}")
    print(f"  Email mismatches          : {len(email_mismatches)}")
    print(f"  Only in SiLAS             : {len(only_in_silas)}")
    print(f"  Only in Entra             : {len(only_in_entra)}")
    print(f"  Total mismatches          : {len(mismatch_rows)}")
    print(f"  Fix candidates (STB-4126) : {len(fix_candidates)}")
    print(bar)

    if fix_candidates:
        print("\n  Fix candidates (users where enableUserOnRecreate was triggered):")
        print(f"  {'OID':<38}  {'Email':<40}  SiLAS enabled  Entra enabled  Flags")
        print("  " + "-" * 120)
        for r in fix_candidates:
            print(
                f"  {str(r['entra_oid']):<38}  "
                f"{str(r['silas_email'] or ''):<40}  "
                f"{str(r['silas_enabled']):<14} "
                f"{str(r['entra_account_enabled']):<14} "
                f"{r['mismatch_flags'] or 'none'}"
            )
    else:
        print("\n  No fix candidates detected.")

    if enabled_mismatches:
        print("\n  Enabled status mismatches:")
        print(f"  {'OID':<38}  {'Email':<40}  SiLAS  Entra")
        print("  " + "-" * 100)
        for r in enabled_mismatches:
            print(
                f"  {str(r['entra_oid']):<38}  "
                f"{str(r['silas_email'] or ''):<40}  "
                f"{str(r['silas_enabled']):<6} "
                f"{str(r['entra_account_enabled'])}"
            )
    else:
        print("\n  No enabled-status mismatches found.")

    if len(mismatch_rows) == 0:
        print("\n  OUTCOME: No discrepancies found. No action required.")
    else:
        print(f"\n  OUTCOME: {len(mismatch_rows)} discrepancy/ies found. Review the mismatch CSV.")

    print(bar + "\n")


def write_csvs(env: str, all_rows: list[dict], mismatch_rows: list[dict]) -> tuple[str, str]:
    ts = datetime.now().strftime("%Y%m%d_%H%M%S")
    all_path      = f"silas_entra_comparison_{env}_{ts}.csv"
    mismatch_path = f"silas_entra_mismatches_{env}_{ts}.csv"

    if all_rows:
        pd.DataFrame(all_rows).to_csv(all_path, index=False)
        print(f"[CSV] Full comparison written to: {all_path}")
    else:
        print("[CSV] No rows to write for full comparison.")

    if mismatch_rows:
        pd.DataFrame(mismatch_rows).to_csv(mismatch_path, index=False)
        print(f"[CSV] Mismatches only written to:  {mismatch_path}")
    else:
        print("[CSV] No mismatches — mismatch CSV not written.")
        mismatch_path = None

    return all_path, mismatch_path


# ── Entry point ───────────────────────────────────────────────────────────────

def resolve_env_credentials():
    missing = []
    for var in ("ENTRA_TENANT_ID", "ENTRA_CLIENT_ID", "ENTRA_CLIENT_SECRET"):
        if not os.getenv(var):
            missing.append(var)
    if missing:
        print(f"[Entra] Missing environment variable(s): {', '.join(missing)}")
        print("Export them before running:")
        for m in missing:
            print(f"  export {m}=<value>")
        sys.exit(1)


def main():
    parser = argparse.ArgumentParser(
        description="STB-4126: One-off SiLAS vs Entra user sync validation"
    )
    parser.add_argument(
        "--env",
        required=True,
        choices=["staging", "prod"],
        help="Environment to validate (staging uses port 5433, prod uses port 5435)",
    )
    args = parser.parse_args()
    env = args.env

    print(f"\n[INFO] Starting SiLAS/Entra sync validation for environment: {env.upper()}")
    print(f"[INFO] DB port: {ENV_DB_PORTS[env]}")

    # ── SiLAS extract ──────────────────────────────────────────────────────────
    print("\n[Step 1/4] Connecting to SiLAS database...")
    conn = get_db_connection(env)
    print("[Step 2/4] Extracting SiLAS user records...")
    silas_rows = extract_silas_users(conn)
    conn.close()

    # ── Entra extract ──────────────────────────────────────────────────────────
    resolve_env_credentials()
    tenant_id     = os.environ["ENTRA_TENANT_ID"]
    client_id     = os.environ["ENTRA_CLIENT_ID"]
    client_secret = os.environ["ENTRA_CLIENT_SECRET"]

    print("\n[Step 3/4] Authenticating with MS Graph API...")
    token = get_graph_token(tenant_id, client_id, client_secret)
    print("[Step 3/4] Extracting Entra user records (may paginate)...")
    entra_users = extract_entra_users(token)

    # ── Comparison ─────────────────────────────────────────────────────────────
    print("\n[Step 4/4] Comparing records...")
    all_rows, mismatch_rows = compare(silas_rows, entra_users)

    # ── Output ─────────────────────────────────────────────────────────────────
    print_summary(env, silas_rows, entra_users, all_rows, mismatch_rows)
    write_csvs(env, all_rows, mismatch_rows)


if __name__ == "__main__":
    main()
