package uk.gov.justice.laa.portal.landingpage.viewmodel;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class AppRoleViewModel implements Comparable<AppRoleViewModel>{
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
