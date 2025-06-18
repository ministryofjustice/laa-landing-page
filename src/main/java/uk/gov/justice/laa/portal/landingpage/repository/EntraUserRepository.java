package uk.gov.justice.laa.portal.landingpage.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EntraUserRepository  extends JpaRepository<EntraUser, UUID> {

    @Query("SELECT u from EntraUser u JOIN FETCH u.userProfiles where u.userName = ?1")
    Optional<EntraUser> findByUserName(String userName);

    Page<EntraUser> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String firstName, String lastName, String email, Pageable pageable);

}
