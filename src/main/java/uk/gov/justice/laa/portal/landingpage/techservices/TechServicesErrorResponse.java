package uk.gov.justice.laa.portal.landingpage.techservices;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechServicesErrorResponse {
    private boolean success;
    private String code;
    private String message;
}
