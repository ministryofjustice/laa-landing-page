package uk.gov.justice.laa.portal.landingpage.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EntraClaimAction {
    @JsonProperty("@odata.type")
    private String odataType;
    private Map<String, Object> claims; // This will hold the dynamic claims like extension_xxxx_laa_app_roles
}