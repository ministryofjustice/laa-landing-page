package uk.gov.justice.laa.portal.landingpage.techservices;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSecurityGroupsResponse implements Serializable {
    private boolean success;
    private String message;
    List<UUID> groupsAdded;
    List<UUID> groupsRemoved;
}
