package uk.gov.justice.laa.portal.landingpage.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.laa.portal.landingpage.entity.AppType;

import java.io.Serializable;
import java.util.Objects;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppDto implements Comparable<AppDto>, Serializable {
    private String id;
    private String name;
    private String title;
    private String description;
    private String url;
    private int ordinal;
    private boolean selected;
    private AppType appType;
    private AlternativeAppDescriptionDto alternativeAppDescription;

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AppDto appDto) {
            return Objects.equals(id, appDto.id) && Objects.equals(name, appDto.name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public int compareTo(@NotNull AppDto o) {
        int cmp = ordinal - o.ordinal;

        if (cmp == 0) {
            return o.name.compareToIgnoreCase(name);
        }

        return cmp;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AlternativeAppDescriptionDto implements Serializable{
        private String assignedAppId;
        private String alternativeDescription;
    }
}
