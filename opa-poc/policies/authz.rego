package authz

import rego.v1

# ============================================================
# Policy: can_create_external_user
# Used by SiLAS to check if the actor has permission to create
# an external user via the create user flow.
# ============================================================
default can_create_external_user := false

can_create_external_user if {
    input.actor.is_internal == true
    actor_permissions := {p | p := input.actor.permissions[_]}
    actor_permissions["CREATE_EXTERNAL_USER"]
}

# ============================================================
# Policy: can_resend_activation_for_audit_user
# ============================================================
default can_resend_activation_for_audit_user := false

# Actor must be internal AND target must be external AND enabled AND not verified
# AND actor must have CREATE_EXTERNAL_USER or EDIT_EXTERNAL_USER permission
can_resend_activation_for_audit_user if {
    # Actor is internal
    input.actor.is_internal == true

    # Target is NOT internal
    input.target.is_internal == false

    # Target is enabled
    input.target.is_enabled == true

    # Target invitation status is NOT VERIFICATION_SUCCESS
    input.target.invitation_status != "VERIFICATION_SUCCESS"

    # Actor has at least one of the required permissions
    required_permissions := {"CREATE_EXTERNAL_USER", "EDIT_EXTERNAL_USER"}
    actor_permissions := {p | p := input.actor.permissions[_]}
    count(actor_permissions & required_permissions) > 0
}
