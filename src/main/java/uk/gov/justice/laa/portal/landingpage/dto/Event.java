package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.Data;
import uk.gov.justice.laa.portal.landingpage.entity.EventType;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class Event {
    private UUID userId;
    private String userName;
    private EventType eventType;
    private String description;
    private LocalDateTime createdDate;
}
