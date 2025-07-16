package uk.gov.justice.laa.portal.landingpage.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EntraUserPayloadDto {
    private String companyName;
    private String createdDateTime;
    private String displayName;
    private String givenName;
    private String id;
    private String mail;
    private String preferredLanguage;
    private String surname;
    private String userPrincipalName;
    private String userType;
}
