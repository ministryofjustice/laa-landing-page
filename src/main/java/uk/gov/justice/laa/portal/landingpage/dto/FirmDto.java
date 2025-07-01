package uk.gov.justice.laa.portal.landingpage.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FirmDto {
    private UUID id;
    private String name;
    private String code;
}
