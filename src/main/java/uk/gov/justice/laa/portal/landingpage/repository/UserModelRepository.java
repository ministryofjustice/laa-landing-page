package uk.gov.justice.laa.portal.landingpage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.portal.landingpage.model.UserModel;

@Repository
public interface UserModelRepository extends JpaRepository<UserModel, Long> {
}
