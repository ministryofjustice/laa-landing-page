package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * DTO for AppRole administration display
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppRoleAdminDto implements Comparable<AppRoleAdminDto> {
    private String id;
    private String name;
    private String description;
    private String userTypeRestriction;
    private String parentApp;
    private String parentAppId;
    private int ordinal;
    private boolean authzRole;
    private String ccmsCode;
    private String legacySync;

    @Override
    public int compareTo(@NotNull AppRoleAdminDto o) {
        int appCompare = this.parentApp.compareToIgnoreCase(o.parentApp);
        return appCompare != 0 ? appCompare : this.ordinal - o.ordinal;
    }
}
