package uk.gov.justice.laa.portal.landingpage.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class DeletedUser implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private int detachedOfficesCount;
    private int removedRolesCount;
    private UUID deletedUserId;
    private String deletedUserEntraOid;
}
