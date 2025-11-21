package uk.gov.justice.laa.portal.landingpage.repository.projection;

/**
 * Projection for audit table query sorted by firm name
 */
public interface UserAuditFirmProjection extends UserAuditProjection {
    String getFirmName();
}
