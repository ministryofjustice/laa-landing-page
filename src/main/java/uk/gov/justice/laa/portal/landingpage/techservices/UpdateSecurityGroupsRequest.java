package uk.gov.justice.laa.portal.landingpage.techservices;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSecurityGroupsRequest {
    private UUID userId;
    private String firstName;
    private String lastName;
    private String email;
    private Set<String> securityGroups;
}
