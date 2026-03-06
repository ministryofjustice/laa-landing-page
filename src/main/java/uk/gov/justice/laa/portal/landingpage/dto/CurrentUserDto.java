package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Data
public class CurrentUserDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private UUID userId;
    private String name;
    private UUID entraOid;
}
