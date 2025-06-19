package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Builder
@Data
public class EntraAuthenticationContext {
    private List<EntraClaim> claims;
}
