# POC: Create User Flow — CQRS + OPA + SiLAS

This document describes the proof-of-concept (POC) that replaces the monolithic create user flow
with a CQRS-style architecture, OPA-based authorisation, and a dedicated orchestration service (SiLAS).

---

## Architecture Overview

```
Browser
  │
  ▼
laa-landing-page  (port 8080)   ← Spring MVC UI + legacy User API
  │   UserController / FirmSearchController
  │   [when feature.flag.silas.create.user=true]
  │
  │  HTTP  (silasRestClient → localhost:8081)
  ▼
laa-silas  (port 8081)          ← Orchestrator: CQRS + OPA
  │   CreateUserController
  │   CreateUserService
  │         │
  │         ├── OpaClient ──────────────────────► OPA  (port 8181)
  │         │                                     Evaluates Rego policy
  │         │
  │         └── UserApiClient ──────────────────► laa-landing-page (port 8080)
  │                                               /api/create-user/query/*  (read)
  │                                               /api/create-user/command/* (write)
  ▼
laa-landing-page  (port 8080)   ← CQRS API endpoints
    CreateUserQueryController   ← read: email check, firm search
    CreateUserCommandController ← write: TechServices + DB persistence
```

### Key design principles

- **OPA is the sole authority** on whether a user is permitted to create another user.
  The Rego policy (`authz.can_create_external_user`) only needs two facts: whether the
  actor is internal, and their permission set. No DB calls happen in OPA.
- **SiLAS is stateless**. It never accesses the database. All reads/writes go via the
  User API (laa-landing-page) CQRS endpoints.
- **Feature flag controls the entire flow.** When `feature.flag.silas.create.user=false`
  the landing page uses its original monolithic path; when `true` every step routes through
  SiLAS.

---

## Modules and Their Roles

### `laa-landing-page` (port 8080)

**UI Controllers** (Thymeleaf MVC, browser-facing):

| Class | Purpose |
|---|---|
| `UserController` | Multi-step create user wizard (details → firm → check answers). Branches on feature flag. |
| `FirmSearchController` | AJAX firm autocomplete endpoint. Delegates to SiLAS when flag is on. |

**CQRS API Controllers** (REST, called by SiLAS only):

| Class | Endpoint prefix | Purpose |
|---|---|---|
| `CreateUserQueryController` | `/api/create-user/query` | Email availability check, firm search |
| `CreateUserCommandController` | `/api/create-user/command` | Persist user to DB + register with TechServices (Entra ID) |

**Service / Client beans (landing page side)**:

| Class | Purpose |
|---|---|
| `SilasCreateUserClient` | HTTP client that calls SiLAS `/api/create-user/*`. Used by UI controllers when flag is on. |
| `AccessControlService` | Supplies `getAuthenticatedUserPermissions()` and `isAuthenticatedUserInternal()` — actor context passed to SiLAS. |
| `OpaConfig` | Spring `@Configuration` that creates `silasRestClient` and `opaRestClient` beans. |

---

### `laa-silas` (port 8081)

| Class | Purpose |
|---|---|
| `CreateUserController` | REST controller: `/api/create-user/{authorize,validate-email,search-firms,execute}` |
| `CreateUserService` | Orchestrates the four steps in order: authorize → validate → (firm search) → execute |
| `OpaClient` | Calls OPA REST API; evaluates `authz/can_create_external_user` policy |
| `UserApiClient` | HTTP client for laa-landing-page CQRS endpoints (query + command) |

---

### `user-api-dto-library` (shared Gradle submodule)

Shared DTOs used across all three processes:

| DTO | Used for |
|---|---|
| `CreateUserCommand` | Payload sent from SiLAS → User API command endpoint |
| `CreateUserResult` | Response from User API → SiLAS → landing page |
| `EmailCheckResult` | Response from User API email query |
| `FirmSummaryDto` | Lightweight firm object for query results |

---

### OPA (`opa-poc/`)

| File | Purpose |
|---|---|
| `policies/authz.rego` | Rego policies: `can_create_external_user`, `can_resend_activation_for_audit_user` |
| `policies/authz_test.rego` | Inline OPA unit tests |
| `docker-compose.yml` | Runs OPA server with the policies directory mounted |

---

## Step-by-step Create User Flow (flag ON)

```
1. GET /admin/user/create/details
   └─ UserController extracts actor context from Authentication
   └─ SilasCreateUserClient.isAuthorized() → POST silas/api/create-user/authorize
      └─ CreateUserService.isAuthorized() → OpaClient.canCreateExternalUser()
         └─ OPA evaluates: actor.is_internal && "CREATE_EXTERNAL_USER" ∈ actor.permissions
   └─ 403 thrown if denied; form rendered if allowed

2. POST /admin/user/create/details  (email entered)
   └─ SilasCreateUserClient.validateEmail() → GET silas/api/create-user/validate-email
      └─ CreateUserService.validateEmail() → UserApiClient.checkEmail()
         └─ CreateUserQueryController.checkEmail() → UserService + EmailValidationService

3. GET /admin/user/create/firm  (firm autocomplete AJAX)
   └─ FirmSearchController.searchFirms() → GET silas/api/create-user/search-firms
      └─ CreateUserService.searchFirms() → UserApiClient.searchFirms()
         └─ CreateUserQueryController.searchFirms() → FirmService

4. POST /admin/user/create/firm  (firm selected)
   └─ If selectedFirmId present: build FirmDto from form (no extra call needed)
   └─ If text-only fallback: SilasCreateUserClient.searchFirms() for top-1 match

5. POST /admin/user/create/check-answers  (submit)
   └─ SilasCreateUserClient.executeCreateUser() → POST silas/api/create-user/execute
      └─ CreateUserController re-checks OPA authorization
      └─ CreateUserService.executeCreateUser() → UserApiClient.createUser()
         └─ CreateUserCommandController.createUser()
            └─ UserService.createUser() → TechServices (Entra ID) + DB persist
```

---

## Running Locally

### Prerequisites

- Java 21
- Docker (for OPA)
- Network access to `login.microsoftonline.com` (for Azure AD login)
- Environment variables: `AZURE_CLIENT_ID`, `AZURE_CLIENT_SECRET`, `AZURE_TENANT_ID`,
  `BASE_URL`, `TECH_SERVICES_*`, database connection properties

### 1. Start OPA

```bash
cd opa-poc
docker compose up
```

OPA is now available at `http://localhost:8181`.

Verify:
```bash
curl -X POST http://localhost:8181/v1/data/authz/can_create_external_user \
  -H "Content-Type: application/json" \
  -d '{"input": {"actor": {"is_internal": true, "permissions": ["CREATE_EXTERNAL_USER"]}}}'
# → {"result":true}
```

### 2. Start SiLAS

From the repo root:

```bash
./gradlew :laa-silas:bootRun
```

SiLAS starts on port `8081`. Defaults:
- OPA: `http://localhost:8181`
- User API (landing page): `http://localhost:8080`

These can be overridden with env vars `OPA_BASE_URL` and `USER_API_BASE_URL`.

Verify:
```bash
curl -X POST http://localhost:8081/api/create-user/authorize \
  -H "Content-Type: application/json" \
  -d '{"isActorInternal": true, "permissions": ["CREATE_EXTERNAL_USER"]}'
# → {"authorized":true}
```

### 3. Start laa-landing-page

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

The `local` profile (`application-local.properties`) disables the OIDC discovery network call
that would otherwise fail at startup, and activates the dev JWT decoder which accepts any token.

Key feature flags (defaulted in `application.properties`):

```properties
feature.flag.silas.create.user=true   # routes create user flow through SiLAS
feature.flag.opa.authz=false          # unrelated older POC; leave false
silas.base-url=http://localhost:8081
opa.base-url=http://localhost:8181
```

---

## Running the OPA Unit Tests

No Docker or running server needed:

```bash
# With OPA CLI installed:
opa test opa-poc/policies -v

# Or via Docker:
docker run --rm \
  -v $(pwd)/opa-poc/policies:/policies \
  openpolicyagent/opa:latest test /policies -v
```

---

## Feature Flag Behaviour

| Flag value | Behaviour |
|---|---|
| `feature.flag.silas.create.user=true` | All create user steps route via SiLAS → OPA → User API CQRS |
| `feature.flag.silas.create.user=false` | Original monolithic flow — UserService, FirmService, EmailValidationService used directly |

The flag can also be set at runtime via the env var `FEATURE_FLAG_SILAS_CREATE_USER`.

---

## Shared DTOs

`user-api-dto-library` is a Gradle submodule included in both `laa-landing-page` and `laa-silas`.
This ensures the same types are used on both sides of every HTTP call with no duplication.

```
user-api-dto-library/
└── uk.gov.justice.laa.portal.dto.createuser
    ├── CreateUserCommand   (SiLAS → User API command)
    ├── CreateUserResult    (User API → SiLAS → landing page)
    ├── EmailCheckResult    (User API → SiLAS → landing page)
    └── FirmSummaryDto      (User API → SiLAS → landing page)
```
