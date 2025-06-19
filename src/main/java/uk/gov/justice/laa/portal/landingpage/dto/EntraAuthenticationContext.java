package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.Data;
import java.util.List;

@Data
public class EntraAuthenticationContext {
    private List<EntraClaim> claims;
}
