package uk.gov.justice.laa.portal.landingpage.dto;

import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppDto implements Comparable<AppDto>{
    private String id;
    private String name;
    private int ordinal;
    private boolean selected;

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
        return this.ordinal - o.ordinal;
    }
}
