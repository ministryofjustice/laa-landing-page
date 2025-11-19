package uk.gov.justice.laa.portal.landingpage.repository.projection;

/**
 * Projection for audit table query sorted by profile count
 */
public interface UserAuditProfileCountProjection extends UserAuditProjection {
    Long getProfileCount();
}
