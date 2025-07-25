package uk.gov.justice.laa.portal.landingpage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.portal.landingpage.entity.Permission;

import java.util.UUID;

@Repository
public interface PermissionRepository  extends JpaRepository<Permission, UUID> {
}
