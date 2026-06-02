package uk.gov.justice.laa.portal.landingpage.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class EntraUserRepositoryCustomAuditSearchImpl implements EntraUserRepositoryCustomAuditSearch {

    @PersistenceContext
    private EntityManager entityManager;

    public Page<Object[]> findAuditUsersWithDynamicProjection(String sortType, String searchTerm, UUID firmId, String silasRole,
                                                              UUID appId, String userType, Boolean multiFirm, Boolean inactiveSinceDateFlag,
                                                              Boolean neverActivated, Pageable pageable) {

        // 1. Build the Dynamic Projection (SELECT) and GROUP BY segments
        String selectBlock;
        String groupByBlock = "";

        switch (sortType != null ? sortType.toUpperCase() : "NAME") {
            case "PROFILE_COUNT":
                selectBlock = "SELECT u.id AS userId, COUNT(DISTINCT up.id) AS predictionValue ";
                groupByBlock = " GROUP BY u.id ";
                break;
            case "FIRM_NAME":
                selectBlock = "SELECT u.id AS userId, MIN(f.name) AS predictionValue ";
                groupByBlock = " GROUP BY u.id ";
                break;
            case "STATUS_RANK":
                selectBlock = "SELECT u.id AS userId, eff.silas_status AS silasStatus, "
                        + "eff.silasStatusRank AS predictionValue ";
                groupByBlock = " GROUP BY u.id, eff.silas_status, eff.silasStatusRank ";
                break;
            case "MULTI_FIRM":
                selectBlock = "SELECT u.id AS userId, " + "CASE WHEN COALESCE(LOWER(up.user_type), 'external') = 'external' "
                        + "AND COALESCE(u.multi_firm_user, false) = false THEN 3 "
                        + "     WHEN COALESCE(LOWER(up.user_type), 'external') = 'external' "
                        + "AND COALESCE(u.multi_firm_user, false) = true THEN 2 "
                        + "     ELSE 1 END AS predictionValue ";
                groupByBlock = " GROUP BY u.id, u.multi_firm_user, up.user_type ";
                break;
            case "USER_TYPE_RANK":
                selectBlock = "SELECT u.id AS userId, " + "CASE WHEN COALESCE(LOWER(up.user_type), 'external') = 'external' "
                        + "AND COALESCE(u.multi_firm_user, false) = false THEN 1 "
                        + "     WHEN COALESCE(LOWER(up.user_type), 'external') = 'external' "
                        + "AND COALESCE(u.multi_firm_user, false) = true THEN 2 "
                        + "     ELSE 3 END AS predictionValue ";
                groupByBlock = " GROUP BY u.id, u.multi_firm_user, up.user_type ";
                break;
            case "EMAIL":
                selectBlock = "SELECT u.id AS userId, LOWER(u.email) AS predictionValue ";
                groupByBlock = " GROUP BY u.id, u.email ";
                break;
            default:
                selectBlock = "SELECT u.id AS userId, LOWER(CONCAT(u.first_name, ' ', u.last_name)) "
                        + "AS predictionValue ";
                groupByBlock = " GROUP BY u.id, u.first_name, u.last_name ";
                break;
        }

        // 2. Assemble the main data query string
        StringBuilder dataQueryStr = new StringBuilder(selectBlock);
        appendCoreQueryBody(dataQueryStr, sortType);
        dataQueryStr.append(groupByBlock);

        // Handle Dynamic Pagination Sorting
        if (pageable.getSort().isSorted()) {
            String direction = pageable.getSort().iterator().next().isDescending() ? "DESC" : "ASC";
            dataQueryStr.append(" ORDER BY predictionValue ")
                    .append(direction);
        } else {
            dataQueryStr.append(" ORDER BY predictionValue ASC ");
        }

        // 3. Assemble the count query string using the exact same shared body
        StringBuilder countQueryStr = new StringBuilder("SELECT COUNT(DISTINCT u.id) ");
        appendCoreQueryBody(countQueryStr, sortType);

        // 4. Instantiate Query Objects
        Query dataQuery = entityManager.createNativeQuery(dataQueryStr.toString());
        Query countQuery = entityManager.createNativeQuery(countQueryStr.toString());

        // 5. Apply parameter bindings uniformly to both queries
        bindParameters(dataQuery, searchTerm, firmId, silasRole, appId, userType, multiFirm, inactiveSinceDateFlag, neverActivated);
        bindParameters(countQuery, searchTerm, firmId, silasRole, appId, userType, multiFirm, inactiveSinceDateFlag, neverActivated);

        // 6. Execute Count Query
        long totalCount = ((Number) countQuery.getSingleResult()).longValue();

        // 7. Apply Pagination Limits to Data Query and Execute
        dataQuery.setFirstResult((int) pageable.getOffset());
        dataQuery.setMaxResults(pageable.getPageSize());

        @SuppressWarnings("unchecked") List<Object[]> results = dataQuery.getResultList();

        return new PageImpl<>(results, pageable, totalCount);
    }

    /**
     * Centralised Query Body containing all target tables, joins, lateral sorting blocks, and the
     * entire business logic filter set with safe parameter type casting.
     */
    private void appendCoreQueryBody(StringBuilder sb, String sortType) {
        sb.append(" FROM entra_user u ");

        if ("USER_TYPE_RANK".equalsIgnoreCase(sortType)
                || "MULTI_FIRM".equalsIgnoreCase(sortType)
                || "FIRM_NAME".equalsIgnoreCase(sortType)
                || "PROFILE_COUNT".equalsIgnoreCase(sortType)) {
            sb.append(" LEFT JOIN user_profile up ON up.entra_user_id = u.id ");
        }

        // Append conditional structural tables based on sort requirements
        if ("FIRM_NAME".equalsIgnoreCase(sortType)) {
            sb.append(" LEFT JOIN firm f ON f.id = up.firm_id ");
        }

        if ("STATUS_RANK".equalsIgnoreCase(sortType)) {
            sb.append(" LEFT JOIN LATERAL ( ")
                    .append("   SELECT up_lat.silas_status, ")
                    .append("   CASE WHEN up_lat.silas_status = 'INCOMPLETE' THEN 1 ")
                    .append("        WHEN up_lat.silas_status = 'ACTIVATION_PENDING' THEN 2 ")
                    .append("        WHEN up_lat.silas_status = 'DISABLED' THEN 3 ")
                    .append("        WHEN up_lat.silas_status = 'NO_ROLES_ASSIGNED' THEN 4 ")
                    .append("        WHEN up_lat.silas_status = 'COMPLETE' THEN 5 ")
                    .append("        ELSE 6 END AS silasStatusRank ")
                    .append("   FROM user_profile up_lat WHERE up_lat.entra_user_id = u.id ")
                    .append("   UNION ALL ")
                    .append("   SELECT NULL, CASE WHEN u.enabled = FALSE THEN 3 ")
                    .append("                     WHEN u.invitation_status IS NULL ")
                    .append("                     OR u.invitation_status <> ")
                    .append("'VERIFICATION_SUCCESS' THEN 1 ")
                    .append("                     ELSE 4 END ")
                    .append("   WHERE NOT EXISTS (SELECT 1 FROM user_profile upx ")
                    .append("WHERE upx.entra_user_id = u.id) ")
                    .append("   ORDER BY silasStatusRank LIMIT 1 ")
                    .append(" ) eff ON TRUE ");
        }

        // Immutable master filtering block with complete explicit type hinting
        sb.append(" WHERE (CAST(:searchTerm AS varchar) IS NULL ")
                .append("       OR CAST(:searchTerm AS varchar) = '' ")
                .append("       OR LOWER(CONCAT(u.first_name, ' ', u.last_name)) ")
                .append("           LIKE LOWER(CONCAT('%', CAST(:searchTerm AS varchar), '%')) ")
                .append("       OR LOWER(u.email) ")
                .append("           LIKE LOWER(CONCAT('%', CAST(:searchTerm AS varchar), '%')) ")
                .append("     ) ")
                .append("   AND (CAST(:firmId AS varchar) IS NULL ")
                .append("        OR EXISTS ( ")
                .append("            SELECT 1 ")
                .append("            FROM user_profile upf ")
                .append("            WHERE upf.entra_user_id = u.id ")
                .append("              AND upf.firm_id = CAST(:firmId AS UUID) ")
                .append("        ) ")
                .append("   ) ")
                .append("   AND (CAST(:silasRole AS varchar) IS NULL ")
                .append("        OR CAST(:silasRole AS varchar) = '' ")
                .append("        OR EXISTS ( ")
                .append("            SELECT 1 ")
                .append("            FROM user_profile up2 ")
                .append("            JOIN user_profile_app_role upar ")
                .append("              ON upar.user_profile_id = up2.id ")
                .append("            JOIN app_role ar ")
                .append("              ON ar.id = upar.app_role_id ")
                .append("            WHERE up2.entra_user_id = u.id ")
                .append("              AND ar.authz_role = TRUE ")
                .append("              AND ar.name = CAST(:silasRole AS varchar) ")
                .append("        ) ")
                .append("   ) ")
                .append("   AND (CAST(:appId AS varchar) IS NULL ")
                .append("        OR EXISTS ( ")
                .append("            SELECT 1 ")
                .append("            FROM user_profile up3 ")
                .append("            JOIN user_profile_app_role upar ")
                .append("              ON upar.user_profile_id = up3.id ")
                .append("            JOIN app_role ar ")
                .append("              ON ar.id = upar.app_role_id ")
                .append("            WHERE up3.entra_user_id = u.id ")
                .append("              AND ar.app_id = CAST(:appId AS uuid) ")
                .append("        ) ")
                .append("   ) ")
                .append("   AND (CAST(:userType AS varchar) IS NULL ")
                .append("        OR (CAST(:userType AS varchar) = 'EXTERNAL' ")
                .append("            AND NOT EXISTS ( ")
                .append("                SELECT 1 ")
                .append("                FROM user_profile upt ")
                .append("                WHERE upt.entra_user_id = u.id ")
                .append("            ) ")
                .append("        ) ")
                .append("        OR EXISTS ( ")
                .append("            SELECT 1 ")
                .append("            FROM user_profile up4 ")
                .append("            WHERE up4.entra_user_id = u.id ")
                .append("              AND up4.user_type = CAST(:userType AS varchar) ")
                .append("        ) ")
                .append("   ) ")
                .append("   AND (CAST(:multiFirm AS boolean) IS NULL ")
                .append("        OR u.multi_firm_user = CAST(:multiFirm AS boolean) ")
                .append("   ) ")
                .append("   AND ( ")
                .append("        (CAST(:inactiveSinceDateFlag AS boolean) IS NULL ")
                .append("         AND CAST(:neverActivated AS boolean) IS NULL) ")
                .append("        OR (CAST(:neverActivated AS boolean) IS NOT NULL ")
                .append("            AND u.invitation_status != 'VERIFICATION_SUCCESS' ")
                .append("            AND NOT EXISTS ( ")
                .append("                SELECT 1 ")
                .append("                FROM user_profile up5 ")
                .append("                WHERE up5.entra_user_id = u.id ")
                .append("                  AND up5.user_type = 'INTERNAL' ")
                .append("            ) ")
                .append("        ) ")
                .append("   ) ");
    }

    /**
     * Binds parameters to a native query safely, handling UUID conversion conversions.
     */
    private void bindParameters(Query query, String searchTerm, UUID firmId, String silasRole, UUID appId,
                                String userType, Boolean multiFirm, Boolean inactiveSinceDateFlag, Boolean neverActivated) {
        query.setParameter("searchTerm", searchTerm);
        query.setParameter("firmId", firmId != null ? firmId.toString() : null);
        query.setParameter("silasRole", silasRole);
        query.setParameter("appId", appId != null ? appId.toString() : null);
        query.setParameter("userType", userType);
        query.setParameter("multiFirm", multiFirm);
        query.setParameter("inactiveSinceDateFlag", inactiveSinceDateFlag);
        query.setParameter("neverActivated", neverActivated);
    }
}
