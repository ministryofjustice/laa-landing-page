package uk.gov.justice.laa.portal.landingpage.repository.projection;

import java.util.UUID;

/**
 * Base projection for audit table queries
 * Contains the user ID returned by special sorting queries
 */
public interface UserAuditProjection {
    UUID getUserId();
}
