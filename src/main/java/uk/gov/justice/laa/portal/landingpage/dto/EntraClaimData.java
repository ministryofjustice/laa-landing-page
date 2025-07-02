package uk.gov.justice.laa.portal.landingpage.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

import java.util.List; 

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EntraClaimData {
    @JsonProperty("@odata.type")
    private String odataType;
    private String tenantId;
    private String authenticationEventListenerId;
    private String customAuthenticationExtensionId;
    private EntraAuthenticationContext authenticationContext;
    private List<EntraClaimAction> actions;
}