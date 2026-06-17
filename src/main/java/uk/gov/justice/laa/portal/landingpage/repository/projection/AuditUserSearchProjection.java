package uk.gov.justice.laa.portal.landingpage.repository.projection;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
public class AuditUserSearchProjection {
    private UUID userId;
    private Object predictionValue;
    private String silasStatus;

    public AuditUserSearchProjection(UUID userId, Object predictionValue) {
        this.userId = userId;
        this.predictionValue = predictionValue;
    }

    public AuditUserSearchProjection(UUID userId, String silasStatus, Object predictionValue) {
        this.userId = userId;
        this.silasStatus = silasStatus;
        this.predictionValue = predictionValue;
    }

}
