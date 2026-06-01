package uk.gov.justice.laa.portal.landingpage.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import uk.gov.justice.laa.portal.landingpage.entity.DeleteUserReason;

@Repository
public interface DeleteUserReasonRepository extends JpaRepository<DeleteUserReason, UUID> {

    Optional<DeleteUserReason> findByCode(String code);

    List<DeleteUserReason> findAllByEditableByInternalUser(boolean editableByInternalUser);

    List<DeleteUserReason> findAllByEditableByExternalUser(boolean editableByExternalUser);

    List<DeleteUserReason> findAllBySystemGenerated(boolean systemGenerated);
}
