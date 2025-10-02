package uk.gov.justice.laa.portal.landingpage.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaaApplicationForView implements Comparable<LaaApplicationForView>, Serializable {

    private String name;
    private String title;
    private String description;
    private String url;
    private int ordinal;
    private boolean specialHandling;

    public LaaApplicationForView(LaaApplication laaApplication) {
        this.name = laaApplication.getName();
        this.title = laaApplication.getTitle();
        this.description = laaApplication.getDescription();
        this.url = laaApplication.getUrl();
        this.ordinal = laaApplication.getOrdinal();
        this.specialHandling = laaApplication.getDescriptionIfAppAssigned() != null
                && StringUtils.isNotEmpty(laaApplication.getDescriptionIfAppAssigned().getAppAssigned())
                && StringUtils.isNotEmpty(laaApplication.getDescriptionIfAppAssigned().getDescription());
    }

    @Override
    public int compareTo(@NotNull LaaApplicationForView o) {
        return ordinal - o.ordinal;
    }
}
