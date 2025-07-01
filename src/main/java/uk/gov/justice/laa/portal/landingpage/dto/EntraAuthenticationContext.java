package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntraAuthenticationContext {
    private List<EntraClaim> claims;
}
