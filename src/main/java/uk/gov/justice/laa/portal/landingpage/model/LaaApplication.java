package uk.gov.justice.laa.portal.landingpage.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model class representing Laa Applications
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LaaApplication {
    @JsonProperty(index = 0)
    private String id;
    @JsonProperty(index = 1)
    private String oidGroupName;
    @JsonProperty(index = 2)
    private String title;
    @JsonProperty(index = 3)
    private String description;
    @JsonProperty(index = 4)
    private String url;
}
