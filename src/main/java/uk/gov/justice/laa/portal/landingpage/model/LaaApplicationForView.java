package uk.gov.justice.laa.portal.landingpage.model;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.entity.AppType;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaaApplicationForView implements Comparable<LaaApplicationForView>, Serializable {

    private String id;
    private String name;
    private String title;
    private String description;
    private String url;
    private int ordinal;
    private boolean specialHandling;
    private AppType appGroup;
    private String otherAssignedAppIdForAltDesc;

    public LaaApplicationForView(AppDto appDto) {
        this.id = appDto.getId();
        this.name = appDto.getName();
        this.title = appDto.getTitle();
        this.description = appDto.getDescription();
        this.url = appDto.getUrl();
        this.ordinal = appDto.getOrdinal();
        this.specialHandling = appDto.getAlternativeAppDescription() != null
                && appDto.getAlternativeAppDescription().getAssignedAppId() != null
                && StringUtils.isNotEmpty(appDto.getAlternativeAppDescription().getAlternativeDescription());
        this.otherAssignedAppIdForAltDesc =
                specialHandling ? appDto.getAlternativeAppDescription().getAssignedAppId() : null;
        this.appGroup = appDto.getAppType();
    }

    @Override
    public int compareTo(@NotNull LaaApplicationForView o) {
        int cmp = ordinal - o.ordinal;

        if (cmp == 0) {
            return name.compareToIgnoreCase(o.name);
        }

        return cmp;
    }
}
