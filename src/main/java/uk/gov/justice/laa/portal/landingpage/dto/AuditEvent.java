package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.Getter;
import uk.gov.justice.laa.portal.landingpage.entity.EventType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public abstract class AuditEvent {
    @Getter
    protected UUID userId;//modifier id
    @Getter
    protected String userName;
    private final LocalDateTime createdDate;

    public abstract EventType getEventType();

    public abstract String getDescription();

    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    public AuditEvent() {
        createdDate = LocalDateTime.now();
    }

    public String getCreatedDate() {
        return createdDate.format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT));
    }

}
