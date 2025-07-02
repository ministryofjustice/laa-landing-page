package uk.gov.justice.laa.portal.landingpage.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

@Repository
public interface EntraUserRepository extends JpaRepository<EntraUser, UUID> {
    @Query("SELECT u from EntraUser u JOIN FETCH u.userProfiles where u.entraUserId = ?1")
    Optional<EntraUser> findByEntraUserId(String entraId);

    @Query(
        """
        SELECT DISTINCT u FROM EntraUser u
        JOIN FETCH u.userProfiles ups
        WHERE (LOWER(u.firstName) LIKE LOWER(CONCAT('%', :firstName, '%'))
        OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :lastName, '%'))
        OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%')))
        AND ups.userType IN (:userTypes)
        """)
    Page<EntraUser> findByNameEmailAndUserTypes(@Param("firstName") String firstName, @Param("lastName") String lastName, @Param("email") String email,
                                                @Param("userTypes") List<UserType> userTypes, Pageable pageable);

    @Query(
            """
            SELECT DISTINCT u FROM EntraUser u
            JOIN FETCH u.userProfiles ups
            WHERE ups.userType IN (:userTypes)
            """)
    Page<EntraUser> findByUserTypes(@Param("userTypes") List<UserType> userTypes, Pageable pageable);

    @Query(
            """
            SELECT DISTINCT u FROM EntraUser u
            JOIN FETCH u.userProfiles ups
            WHERE (LOWER(u.firstName) LIKE LOWER(CONCAT('%', :firstName, '%'))
            OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :lastName, '%'))
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%')))
            AND ups.userType IN (:userTypes)
            AND ups.firm.id IN (:firmIds)
            """)
    Page<EntraUser> findByNameEmailAndUserTypesFirms(@Param("firstName") String firstName, @Param("lastName") String lastName, @Param("email") String email,
                                                @Param("userTypes") List<UserType> userTypes, @Param("firmIds") List<UUID> firmIds, Pageable pageable);

    @Query(
            """
            SELECT DISTINCT u FROM EntraUser u
            JOIN FETCH u.userProfiles ups
            WHERE ups.userType IN (:userTypes)
            AND ups.firm.id IN (:firmIds)
            """)
    Page<EntraUser> findByUserTypesAndFirms(@Param("userTypes") List<UserType> userTypes, @Param("firmIds") List<UUID> firmIds, Pageable pageable);

    @Query("SELECT u from EntraUser u where u.email = ?1")
    Optional<EntraUser> findByEmailIgnoreCase(String email);
}
