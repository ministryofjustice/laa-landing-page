package uk.gov.justice.laa.portal.landingpage.model;

import com.microsoft.graph.models.ServicePrincipal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServicePrincipalModel {
    private ServicePrincipal  servicePrincipal;
    private boolean selected = false;
}
