#!/bin/bash

# Test the OPA policy for canResendActivationForAuditUser
# Requires OPA running via: docker compose up

BASE_URL="http://localhost:8181/v1/data/authz/can_resend_activation_for_audit_user"

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

pass=0
fail=0

check_result() {
    local test_name="$1"
    local expected="$2"
    local response="$3"

    # Extract the result field (true/false), default to false if missing
    actual=$(echo "$response" | grep -o '"result":[a-z]*' | cut -d: -f2)
    if [ -z "$actual" ]; then
        actual="false"
    fi

    if [ "$actual" == "$expected" ]; then
        echo -e "  ${GREEN}✓ PASS${NC} - result: $actual"
        pass=$((pass + 1))
    else
        echo -e "  ${RED}✗ FAIL${NC} - expected: $expected, got: $actual"
        echo "  Response: $response"
        fail=$((fail + 1))
    fi
}

echo "=== OPA POC: canResendActivationForAuditUser ==="
echo ""

# Test 1: Should ALLOW - internal actor with CREATE_EXTERNAL_USER
echo "Test 1: Internal actor, CREATE_EXTERNAL_USER permission, valid target"
response=$(curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "input": {
      "actor": { "is_internal": true, "permissions": ["CREATE_EXTERNAL_USER"] },
      "target": { "is_internal": false, "is_enabled": true, "invitation_status": "PENDING" }
    }
  }')
check_result "Test 1" "true" "$response"

# Test 2: Should ALLOW - internal actor with EDIT_EXTERNAL_USER
echo "Test 2: Internal actor, EDIT_EXTERNAL_USER permission, valid target"
response=$(curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "input": {
      "actor": { "is_internal": true, "permissions": ["EDIT_EXTERNAL_USER", "VIEW_EXTERNAL_USER"] },
      "target": { "is_internal": false, "is_enabled": true, "invitation_status": "VERIFICATION_SENT" }
    }
  }')
check_result "Test 2" "true" "$response"

# Test 3: Should DENY - external actor
echo "Test 3: External actor (should be DENIED)"
response=$(curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "input": {
      "actor": { "is_internal": false, "permissions": ["CREATE_EXTERNAL_USER"] },
      "target": { "is_internal": false, "is_enabled": true, "invitation_status": "PENDING" }
    }
  }')
check_result "Test 3" "false" "$response"

# Test 4: Should DENY - target is internal
echo "Test 4: Target is internal (should be DENIED)"
response=$(curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "input": {
      "actor": { "is_internal": true, "permissions": ["CREATE_EXTERNAL_USER"] },
      "target": { "is_internal": true, "is_enabled": true, "invitation_status": "PENDING" }
    }
  }')
check_result "Test 4" "false" "$response"

# Test 5: Should DENY - target already verified
echo "Test 5: Target already verified (should be DENIED)"
response=$(curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "input": {
      "actor": { "is_internal": true, "permissions": ["CREATE_EXTERNAL_USER"] },
      "target": { "is_internal": false, "is_enabled": true, "invitation_status": "VERIFICATION_SUCCESS" }
    }
  }')
check_result "Test 5" "false" "$response"

# Test 6: Should DENY - target is disabled
echo "Test 6: Target is disabled (should be DENIED)"
response=$(curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "input": {
      "actor": { "is_internal": true, "permissions": ["CREATE_EXTERNAL_USER"] },
      "target": { "is_internal": false, "is_enabled": false, "invitation_status": "PENDING" }
    }
  }')
check_result "Test 6" "false" "$response"

# Test 7: Should DENY - actor has wrong permissions
echo "Test 7: Actor has wrong permissions (should be DENIED)"
response=$(curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "input": {
      "actor": { "is_internal": true, "permissions": ["VIEW_EXTERNAL_USER", "DELETE_EXTERNAL_USER"] },
      "target": { "is_internal": false, "is_enabled": true, "invitation_status": "PENDING" }
    }
  }')
check_result "Test 7" "false" "$response"

echo ""
echo "=== Results: $pass passed, $fail failed ==="
