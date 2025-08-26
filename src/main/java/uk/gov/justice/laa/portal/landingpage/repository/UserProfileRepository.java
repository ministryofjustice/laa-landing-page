package uk.gov.justice.laa.portal.landingpage.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import uk.gov.justice.laa.portal.landingpage.dto.UserSearchCriteria;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

        @Query("""
                        SELECT ups FROM UserProfile ups
                        JOIN FETCH ups.entraUser u
                        LEFT JOIN FETCH ups.firm f
                        WHERE ups.userType IN (:userTypes)
                        AND (LOWER(u.firstName) LIKE LOWER(CONCAT('%', :firstName, '%'))
                        OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :lastName, '%'))
                        OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%')))
                            """)
        Page<UserProfile> findByNameEmailAndUserTypes(@Param("firstName") String firstName,
                        @Param("lastName") String lastName, @Param("email") String email,
                        @Param("userTypes") List<UserType> userTypes, Pageable pageable);

        @Query("""
                        SELECT ups FROM UserProfile ups
                        JOIN FETCH ups.firm f
                        JOIN FETCH ups.entraUser u
                        WHERE ups.userType IN (:userTypes)
                        AND f.id IN (:firmIds)
                        AND (LOWER(u.firstName) LIKE LOWER(CONCAT('%', :firstName, '%'))
                        OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :lastName, '%'))
                        OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%')))
                            """)
        Page<UserProfile> findByNameEmailAndUserTypesFirms(@Param("firstName") String firstName,
                        @Param("lastName") String lastName, @Param("email") String email,
                        @Param("userTypes") List<UserType> userTypes, @Param("firmIds") List<UUID> firmIds,
                        Pageable pageable);

        @Query("""
                        SELECT ups FROM UserProfile ups
                        JOIN FETCH ups.firm f
                        JOIN FETCH ups.entraUser u
                        WHERE ups.userType IN (:userTypes)
                        AND f.id IN (:firmIds)
                        """)
        Page<UserProfile> findByUserTypesFirms(@Param("userTypes") List<UserType> userTypes,
                        @Param("firmIds") List<UUID> firmIds, Pageable pageable);

        @Query("SELECT ups FROM UserProfile ups "
                        + "LEFT JOIN FETCH ups.firm f "
                        + "JOIN FETCH ups.entraUser u "
                        + "WHERE ups.userType IN (:userTypes)")
        Page<UserProfile> findByUserTypes(List<UserType> userTypes, Pageable pageable);

        @Query("""
                        SELECT DISTINCT u.entraOid FROM EntraUser u
                        JOIN u.userProfiles ups
                        WHERE ups.userType IN (:userType)
                        """)
        List<UUID> findByUserTypes(@Param("userType") UserType userType);

        @Query("""
                        SELECT ups FROM UserProfile ups
                        JOIN FETCH ups.firm f
                        JOIN FETCH ups.entraUser u
                        WHERE ups.userType IN (:userTypes)
                        AND f.id IN (:firmIds)
                        """)
        Page<UserProfile> findByUserTypesAndFirms(@Param("userTypes") List<UserType> userTypes,
                        @Param("firmIds") List<UUID> firmIds, Pageable pageable);

        @Query("""
                        SELECT ups FROM UserProfile ups
                                    JOIN FETCH ups.entraUser u
                        WHERE (:firmId IS NULL OR ups.firm.id = :firmId)
                        AND (:userTypes IS NULL OR ups.userType IN :userTypes)
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
                        @Param("firmId") UUID firmId, @Param("userTypes") List<UserType> userTypes,
                        @Param("showFirmAdmins") boolean showFirmAdmins, Pageable pageable);

        @Query("""
                        SELECT ups FROM UserProfile ups
                                    JOIN FETCH ups.entraUser u
                                    LEFT JOIN FETCH ups.firm f
                        WHERE (:firmId IS NULL OR ups.firm.id = :firmId)
                        AND (:#{#criteria.userTypes} IS NULL OR ups.userType IN :#{#criteria.userTypes})
                        AND (
                            (:#{#criteria.searchTerm} IS NULL OR :#{#criteria.searchTerm} = '') OR
                            EXISTS (
                                SELECT 1 FROM ups.entraUser u
                                WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :#{#criteria.searchTerm}, '%'))
                                OR LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :#{#criteria.searchTerm}, '%'))
                            )
                        )
                        AND (
                            (:#{#criteria.firmSearch} IS NULL OR :#{#criteria.firmSearch} = '') OR
                            (ups.firm IS NOT NULL AND
                             (LOWER(ups.firm.name) LIKE LOWER(CONCAT('%', :#{#criteria.firmSearch}, '%')) OR
                              LOWER(ups.firm.code) LIKE LOWER(CONCAT('%', :#{#criteria.firmSearch}, '%'))))
                        )
                        AND (:#{#criteria.showFirmAdmins} = false OR EXISTS (
                            SELECT 1 FROM ups.appRoles ar
                            WHERE ar.authzRole = true
                            AND ar.name = 'External User Manager'
                        ))
                        """)
        Page<UserProfile> findBySearchParams(@Param("firmId") UUID firmId,
                        @Param("criteria") UserSearchCriteria criteria, Pageable pageable);
}
