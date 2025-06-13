package uk.gov.justice.laa.portal.landingpage.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "event")
public class Event extends BaseEntity{
    @Column(name = "user_id", nullable = false)
    @NotBlank(message = "User ID must be provided")
    private UUID userId;
    @Column(name = "user_name", nullable = false, length = 255)
    @NotBlank(message = "User name must be provided")
    private String userName;
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 255)
    @NotNull(message = "Event type must be provided")
    private EventType eventType;
    @Column(name = "description", nullable = false, length = 500)
    @NotBlank(message = "Description must be provided")
    @Size(min = 1, max = 500, message = "Description must be between 1 and 500 characters")
    private String description;
    @Column(name = "created_date", nullable = false)
    @CreatedDate
    private LocalDateTime createdDate;
}
