package uk.gov.justice.laa.portal.landingpage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;

import java.util.UUID;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    // Paginated search by entra user's first name, last name or email (case-insensitive)
    @Query("""
            SELECT p FROM UserProfile p
                JOIN p.entraUser u
            WHERE upper(u.firstName) LIKE upper(concat('%', :term, '%'))
               OR upper(u.lastName)  LIKE upper(concat('%', :term, '%'))
               OR upper(u.email)    LIKE upper(concat('%', :term, '%'))
            """)
    Page<UserProfile> searchProfiles(@Param("term") String term, Pageable pageable);
}
