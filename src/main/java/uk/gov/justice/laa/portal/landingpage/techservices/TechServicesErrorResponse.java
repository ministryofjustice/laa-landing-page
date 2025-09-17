package uk.gov.justice.laa.portal.landingpage.techservices;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TechServicesErrorResponse {
    private boolean success;
    private String code;
    private String message;
    private String[] errors;
}
