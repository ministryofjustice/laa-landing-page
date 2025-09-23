package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntraApplicationInfo implements Serializable {
    private String id;
    private String displayName;
}
