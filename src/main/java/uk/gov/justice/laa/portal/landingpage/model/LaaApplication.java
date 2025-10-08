package uk.gov.justice.laa.portal.landingpage.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * Model class representing Laa Applications
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LaaApplication implements Comparable<LaaApplication>, Serializable {
    private String name;
    private String oidGroupName;
    private String title;
    private String description;
    private String url;
    private int ordinal;
    private String laaApplicationDetails;
    @JsonIgnore
    private LaaApplicationDetails laaApplicationDetailsObj;
    private DescriptionIfAppAssigned descriptionIfAppAssigned;

    @Override
    public int compareTo(@NotNull LaaApplication o) {
        return ordinal - o.ordinal;
    }

    public LaaApplicationDetails getLaaApplicationDetailsObj() {
        if (laaApplicationDetailsObj == null && laaApplicationDetails != null) {
            String[] values = laaApplicationDetails.split("//");
            String oid = values.length > 0 ? values[0] : "";
            String securityGroupName = values.length > 1 ? values[1] : "";
            String securityGroupOid = values.length > 2 ? values[2] : "";

            laaApplicationDetailsObj = new LaaApplicationDetails(oid, securityGroupName, securityGroupOid);

        }
        return laaApplicationDetailsObj;
    }

    @AllArgsConstructor
    @Data
    public static class LaaApplicationDetails implements Serializable {
        private String oid;
        private String securityGroupName;
        private String securityGroupOid;
    }

    @AllArgsConstructor
    @Data
    @Builder
    public static class DescriptionIfAppAssigned {
        private String appAssigned;
        private String description;
    }
}
