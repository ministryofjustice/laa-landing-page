package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppRoleDto implements Comparable<AppRoleDto> {
    private String id;
    private String name;
    private int ordinal;
    private String description;
    private String ccmsCode;
    private AppDto app;
    private UserType[] userTypeRestriction;

    @Override
    public int compareTo(@NotNull AppRoleDto o) {
        int appOrdinalComparison = Integer.compare(this.app.getOrdinal(), o.app.getOrdinal());
        if (appOrdinalComparison != 0) {
            return appOrdinalComparison;
        }
        return Integer.compare(this.ordinal, o.ordinal);
    }
}
