package uk.gov.justice.laa.portal.landingpage.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;

@Repository
public interface EntraUserRepository extends JpaRepository<EntraUser, UUID> {
    @Query("SELECT u from EntraUser u JOIN FETCH u.userProfiles where u.entraId = ?1")
    Optional<EntraUser> findByEntraId(String entraId);

    Page<EntraUser> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String firstName, String lastName, String email, Pageable pageable);

    @Query("SELECT u from EntraUser u where u.email = ?1")
    Optional<EntraUser> findByEmailIgnoreCase(String email);
}
