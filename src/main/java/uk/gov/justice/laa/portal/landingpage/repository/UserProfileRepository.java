package uk.gov.justice.laa.portal.landingpage.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.portal.landingpage.dto.UserSearchCriteria;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    @Query("""
                        SELECT DISTINCT u.entraOid FROM EntraUser u
                        JOIN u.userProfiles ups
                        WHERE ups.userType IN (:userType)
                        """)
    List<UUID> findByUserTypes(@Param("userType") UserType userType);

    @Query("""
                        SELECT ups FROM UserProfile ups
                                    JOIN FETCH ups.entraUser u
                        WHERE (:firmId IS NULL OR ups.firm.id = :firmId)
                        AND (:userType IS NULL OR ups.userType = :userType)
                        AND (
                            :search = '' OR
                            EXISTS (
                                SELECT 1 FROM ups.entraUser u
                                WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
                                OR LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :search, '%'))
                            )
                        )
                        AND (:showFirmAdmins = false OR EXISTS (
                            SELECT 1 FROM ups.appRoles ar
                            WHERE ar.authzRole = true
                            AND ar.name = 'External User Manager'
                        ))
                        """)
    Page<UserProfile> findByNameOrEmailAndPermissionsAndFirm(@Param("search") String search,
                    @Param("firmId") UUID firmId, @Param("userType") UserType userType,
                    @Param("showFirmAdmins") boolean showFirmAdmins, Pageable pageable);

    @Query("""
                        SELECT ups FROM UserProfile ups
                                    JOIN FETCH ups.entraUser u
                                    LEFT JOIN FETCH ups.firm f
            WHERE (
                (:#{#criteria.firmSearch.selectedFirmId} IS NULL AND
                    (
                        :#{#criteria.firmSearch.firmSearch} IS NULL
                        OR :#{#criteria.firmSearch.firmSearch} = ''
                        OR (LOWER(f.name) LIKE LOWER(CONCAT(:#{#criteria.firmSearch.firmSearch}, '%')))
                    )
                )
                OR ups.firm.id = :#{#criteria.firmSearch.selectedFirmId}
            )
                        AND (:#{#criteria.userType} IS NULL OR ups.userType = :#{#criteria.userType})
                        AND (
                            (:#{#criteria.searchTerm} IS NULL OR :#{#criteria.searchTerm} = '') OR
                            EXISTS (
                                SELECT 1 FROM ups.entraUser u
                                WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :#{#criteria.searchTerm}, '%'))
                                OR LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :#{#criteria.searchTerm}, '%'))
                            )
                        )
                        AND (
                (:#{#criteria.firmSearch.selectedFirmId} IS NULL) OR
                (ups.firm IS NOT NULL AND ups.firm.id = :#{#criteria.firmSearch.selectedFirmId})
                        )
                        AND (:#{#criteria.showFirmAdmins} = false OR EXISTS (
                            SELECT 1 FROM ups.appRoles ar
                            WHERE ar.authzRole = true
                            AND (ar.name = 'External User Manager' OR ar.name = 'Firm User Manager')
                        ))
                        """)
    Page<UserProfile> findBySearchParams(@Param("criteria") UserSearchCriteria criteria, Pageable pageable);

    @Query("""
            SELECT ups FROM UserProfile ups
                        JOIN FETCH ups.firm f
                        JOIN FETCH ups.entraUser u
            WHERE ups.firm.id = :firmId
            AND EXISTS (
                SELECT 1 FROM ups.appRoles ar 
                WHERE ar.authzRole = true 
                AND ar.name = :role
            )
            """)
    Page<UserProfile> findFirmUserByAuthzRoleAndFirm(@Param("firmId") UUID firmId, @Param("role") String role, Pageable pageable);

    @Query("""
            SELECT ups FROM UserProfile ups
                        JOIN FETCH ups.entraUser u
            WHERE ups.firm IS NULL
            AND EXISTS (
                SELECT 1 FROM ups.appRoles ar 
                WHERE ar.authzRole = true 
                AND ar.name = :role
            )
            """)
    Page<UserProfile> findInternalUserByAuthzRole(@Param("role") String role, Pageable pageable);
}
