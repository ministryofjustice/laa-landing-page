package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimEnrichmentResponse {
    private boolean success;
    private String message;
    private String correlationId;
    @SuppressWarnings("checkstyle:MemberName")
    private String user_name;
    @SuppressWarnings("checkstyle:MemberName")
    private String user_email;
    @SuppressWarnings("checkstyle:MemberName")
    private List<String> laa_app_roles;
    @SuppressWarnings("checkstyle:MemberName")
    private List<String> laa_accounts;
}