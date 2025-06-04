CREATE UNIQUE INDEX one_default_profile_per_user ON user_profile(entra_user_id) WHERE default_profile = true;
CREATE UNIQUE INDEX one_profile_per_non_multi_firm_user ON user_profile(entra_user_id) WHERE user_type != 'EXTERNAL_MULTI_FIRM';
CREATE UNIQUE INDEX one_profile_per_firm_for_multi_firm_user ON user_profile(entra_user_id, firm_id) WHERE user_type = 'EXTERNAL_MULTI_FIRM';
