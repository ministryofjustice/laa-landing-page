package uk.gov.justice.laa.portal.landingpage.model;

import com.microsoft.graph.models.ServicePrincipal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServicePrincipalModel implements Serializable {
    private ServicePrincipal  servicePrincipal;
    private boolean selected = false;
}
