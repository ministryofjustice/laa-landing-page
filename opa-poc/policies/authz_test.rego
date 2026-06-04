package authz

import rego.v1

# ===== can_create_external_user TESTS =====

test_allow_create_user_internal_with_permission if {
    can_create_external_user with input as {
        "actor": {
            "is_internal": true,
            "permissions": ["CREATE_EXTERNAL_USER", "VIEW_EXTERNAL_USER"]
        }
    }
}

test_deny_create_user_external_actor if {
    not can_create_external_user with input as {
        "actor": {
            "is_internal": false,
            "permissions": ["CREATE_EXTERNAL_USER"]
        }
    }
}

test_deny_create_user_internal_without_permission if {
    not can_create_external_user with input as {
        "actor": {
            "is_internal": true,
            "permissions": ["VIEW_EXTERNAL_USER", "EDIT_EXTERNAL_USER"]
        }
    }
}

# ===== can_resend_activation_for_audit_user: ALLOWED CASES =====

test_allow_internal_actor_with_create_permission if {
    can_resend_activation_for_audit_user with input as {
        "actor": {
            "is_internal": true,
            "permissions": {"CREATE_EXTERNAL_USER"}
        },
        "target": {
            "is_internal": false,
            "is_enabled": true,
            "invitation_status": "PENDING"
        }
    }
}

test_allow_internal_actor_with_edit_permission if {
    can_resend_activation_for_audit_user with input as {
        "actor": {
            "is_internal": true,
            "permissions": {"EDIT_EXTERNAL_USER"}
        },
        "target": {
            "is_internal": false,
            "is_enabled": true,
            "invitation_status": "VERIFICATION_SENT"
        }
    }
}

test_allow_internal_actor_with_both_permissions if {
    can_resend_activation_for_audit_user with input as {
        "actor": {
            "is_internal": true,
            "permissions": {"CREATE_EXTERNAL_USER", "EDIT_EXTERNAL_USER", "VIEW_EXTERNAL_USER"}
        },
        "target": {
            "is_internal": false,
            "is_enabled": true,
            "invitation_status": "VERIFICATION_SENT"
        }
    }
}

# ===== DENIED CASES =====

test_deny_external_actor if {
    not can_resend_activation_for_audit_user with input as {
        "actor": {
            "is_internal": false,
            "permissions": {"CREATE_EXTERNAL_USER"}
        },
        "target": {
            "is_internal": false,
            "is_enabled": true,
            "invitation_status": "PENDING"
        }
    }
}

test_deny_target_is_internal if {
    not can_resend_activation_for_audit_user with input as {
        "actor": {
            "is_internal": true,
            "permissions": {"CREATE_EXTERNAL_USER"}
        },
        "target": {
            "is_internal": true,
            "is_enabled": true,
            "invitation_status": "PENDING"
        }
    }
}

test_deny_target_is_disabled if {
    not can_resend_activation_for_audit_user with input as {
        "actor": {
            "is_internal": true,
            "permissions": {"CREATE_EXTERNAL_USER"}
        },
        "target": {
            "is_internal": false,
            "is_enabled": false,
            "invitation_status": "PENDING"
        }
    }
}

test_deny_target_already_verified if {
    not can_resend_activation_for_audit_user with input as {
        "actor": {
            "is_internal": true,
            "permissions": {"CREATE_EXTERNAL_USER"}
        },
        "target": {
            "is_internal": false,
            "is_enabled": true,
            "invitation_status": "VERIFICATION_SUCCESS"
        }
    }
}

test_deny_actor_lacks_required_permissions if {
    not can_resend_activation_for_audit_user with input as {
        "actor": {
            "is_internal": true,
            "permissions": {"VIEW_EXTERNAL_USER", "DELETE_EXTERNAL_USER"}
        },
        "target": {
            "is_internal": false,
            "is_enabled": true,
            "invitation_status": "PENDING"
        }
    }
}

test_deny_actor_has_no_permissions if {
    not can_resend_activation_for_audit_user with input as {
        "actor": {
            "is_internal": true,
            "permissions": set()
        },
        "target": {
            "is_internal": false,
            "is_enabled": true,
            "invitation_status": "PENDING"
        }
    }
}
