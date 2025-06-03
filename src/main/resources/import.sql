CREATE UNIQUE INDEX one_default_profile_per_user ON laa_user_profile(entra_user_id) WHERE default_profile = true;
CREATE UNIQUE INDEX one_profile_per_non_multi_firm_user ON laa_user_profile(entra_user_id) WHERE user_type != 'EXTERNAL_MULTI_FIRM';
CREATE UNIQUE INDEX one_profile_per_firm_for_multi_firm_user ON laa_user_profile(entra_user_id, firm_id) WHERE user_type = 'EXTERNAL_MULTI_FIRM';
ALTER TABLE laa_user_profile ADD CONSTRAINT firm_not_null_for_non_internal_users_only CHECK (firm_id IS NOT NULL OR user_type = 'INTERNAL');
