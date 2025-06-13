package uk.gov.justice.laa.portal.landingpage.dto;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FirmDto {
    private UUID id;
    private String name;
}
