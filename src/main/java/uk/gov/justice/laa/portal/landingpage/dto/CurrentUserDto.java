package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class CurrentUserDto implements Serializable {
    private UUID userId;
    private String name;
}
