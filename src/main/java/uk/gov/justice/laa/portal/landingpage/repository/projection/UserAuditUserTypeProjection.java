package uk.gov.justice.laa.portal.landingpage.repository.projection;

/**
 * Projection for audit table query sorted by user type rank
 */
public interface UserAuditUserTypeProjection extends UserAuditProjection {
    int getUserTypeRank();
}
