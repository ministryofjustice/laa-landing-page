package uk.gov.justice.laa.portal.landingpage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    @Query(
            """
            SELECT DISTINCT u.entraOid FROM EntraUser u
            JOIN u.userProfiles ups
            WHERE ups.userType IN (:userType)
            """)
    List<UUID> findByUserTypes(@Param("userType") UserType userType);
}
