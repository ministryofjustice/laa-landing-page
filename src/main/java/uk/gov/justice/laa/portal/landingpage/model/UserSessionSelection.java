package uk.gov.justice.laa.portal.landingpage.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.util.List;
import java.util.Map;
import java.util.Set;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class UserSessionSelection {
    private Set<String> appsSelection;
    private Map<String, List<String>> rolesSelection;
    private Map<String, List<String>> officeSelection;
}



