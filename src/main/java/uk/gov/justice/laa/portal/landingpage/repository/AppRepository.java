package uk.gov.justice.laa.portal.landingpage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.portal.landingpage.entity.App;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppRepository extends JpaRepository<App, UUID> {
    Optional<App> findByName(String name);

    Optional<App> findByEntraAppIdOrName(String entraAppOid, String name);
}
