package uk.gov.justice.laa.portal.landingpage.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * Model class representing Laa Applications
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LaaApplication implements Comparable<LaaApplication> {
    private String name;
    private String oid;
    private String oidGroupName;
    private String title;
    private String description;
    private String url;
    private int ordinal;

    @Override
    public int compareTo(@NotNull LaaApplication o) {
        return ordinal - o.ordinal;
    }
}
