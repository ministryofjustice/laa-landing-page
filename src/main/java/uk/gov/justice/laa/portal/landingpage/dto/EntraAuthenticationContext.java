package uk.gov.justice.laa.portal.landingpage.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EntraAuthenticationContext implements Serializable {
    private String correlationId;
    private EntraClientDto client;
    private String protocol;
    private EntraServicePrincipalDto clientServicePrincipal;
    private EntraServicePrincipalDto resourceServicePrincipal;
    private EntraUserPayloadDto user;
}
