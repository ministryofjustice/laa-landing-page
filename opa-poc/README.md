# OPA POC - AccessControlService Policy

Proof-of-concept demonstrating `canResendActivationForAuditUser` from `AccessControlService.java` implemented as an OPA (Open Policy Agent) Rego policy.

## What it does

Translates the Java logic:

```java
public boolean canResendActivationForAuditUser(String entraUserId) {
    return userService.isInternal(authenticatedUser.getId())  // actor is internal
            && !isAccessedUserInternal                        // target is NOT internal
            && accessedUser.isEnabled()                       // target is enabled
            && !VERIFICATION_SUCCESS.equals(invitationStatus) // target not yet verified
            && userHasAnyGivenPermissions(authenticatedUser,
                Permission.CREATE_EXTERNAL_USER,
                Permission.EDIT_EXTERNAL_USER);               // actor has required permission
}
```

Into a Rego policy that OPA evaluates via REST API.

## Quick start

### 1. Start OPA

```bash
cd opa-poc
docker compose up
```

OPA is now running at `http://localhost:8181`.

### 2. Run the Rego unit tests (no Docker needed if OPA CLI installed)

```bash
# With OPA CLI installed:
opa test ./policies -v

# Or via Docker:
docker run --rm -v $(pwd)/policies:/policies openpolicyagent/opa:latest test /policies -v
```

### 3. Run integration tests against the running OPA server

```bash
chmod +x test-requests.sh
./test-requests.sh
```

### 4. Try a single request manually

```bash
curl -X POST http://localhost:8181/v1/data/authz/can_resend_activation_for_audit_user \
  -H "Content-Type: application/json" \
  -d '{
    "input": {
      "actor": {
        "is_internal": true,
        "permissions": ["CREATE_EXTERNAL_USER"]
      },
      "target": {
        "is_internal": false,
        "is_enabled": true,
        "invitation_status": "PENDING"
      }
    }
  }'
```

**Response** (allowed):
```json
{"result": true}
```

**Response** (denied - empty result means `false`):
```json
{}
```

## How it maps to the Java code

| Java concept | OPA input field |
|---|---|
| `userService.isInternal(authenticatedUser.getId())` | `input.actor.is_internal` |
| `userService.isInternal(entraUserId)` | `input.target.is_internal` |
| `accessedUser.isEnabled()` | `input.target.is_enabled` |
| `accessedUser.getInvitationStatus()` | `input.target.invitation_status` |
| `userHasAnyGivenPermissions(...)` | `input.actor.permissions` (set intersection) |

## Key differences from Java

1. **No DB calls** - the caller (Spring app) loads context from DB and passes it as `input`
2. **Stateless** - OPA doesn't know about users, it only evaluates the input it receives
3. **Testable offline** - `opa test` runs tests in milliseconds without Spring context
4. **Hot-reloadable** - change a `.rego` file, OPA picks it up immediately (no redeploy)

## Next steps

- Add more policies (`canEditUser`, `canDeleteUser`, etc.)
- Wire up a Spring `OpaAuthzClient` that calls OPA instead of evaluating Java logic
- Add decision logging for audit trail
- Deploy as sidecar in K8s pod
