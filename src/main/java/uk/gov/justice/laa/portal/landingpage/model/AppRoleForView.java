package uk.gov.justice.laa.portal.landingpage.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppRoleForView implements Comparable<AppRoleForView>, Serializable {

    private String id;
    private String name;
    private String description;
    private String userTypeRestriction;
    private String parentApp;
    private String parentAppId;
    private int parentAppOrdinal;
    private String roleGroup;
    private int ordinal;
    private boolean authzRole;
    private String ccmsCode;

    @Override
    public int compareTo(@NotNull AppRoleForView o) {
        int appOrdinalComparison = Integer.compare(this.parentAppOrdinal, o.parentAppOrdinal);
        if (appOrdinalComparison != 0) {
            return appOrdinalComparison;
        }
        return Integer.compare(this.ordinal, o.ordinal);
    }

}
