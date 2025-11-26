package uk.gov.justice.laa.portal.landingpage.repository.projection;

/**
 * Projection for audit table query sorted by account status
 */
public interface UserAuditAccountStatusProjection extends UserAuditProjection {
    String getAccountStatus();
}
