package uk.gov.justice.laa.portal.landingpage.viewmodel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AppRoleViewModel implements Comparable<AppRoleViewModel>, Serializable {
    private String id;
    private int ordinal;
    private String name;
    private String appName;
    private int appOrdinal;
    private String description;
    private boolean selected;

    @Override
    public int compareTo(@NotNull AppRoleViewModel o) {
        int appOrdinalComparison = Integer.compare(this.appOrdinal, o.appOrdinal);
        if (appOrdinalComparison != 0) {
            return appOrdinalComparison;
        }
        return Integer.compare(this.ordinal, o.ordinal);
    }

}
