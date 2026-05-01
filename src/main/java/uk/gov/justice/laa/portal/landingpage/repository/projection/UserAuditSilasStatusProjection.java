package uk.gov.justice.laa.portal.landingpage.repository.projection;

/**
 * Projection for audit table query sorted by silas status
 */
public interface UserAuditSilasStatusProjection extends UserAuditProjection {
    String getSilasStatus();

    Integer getSilasStatusRank();
}
