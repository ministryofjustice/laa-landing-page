package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class CurrentUserDto {
    private UUID userId;
    private String name;
}
