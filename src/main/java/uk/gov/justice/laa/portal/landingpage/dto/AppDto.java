package uk.gov.justice.laa.portal.landingpage.dto;

import java.util.Objects;

import lombok.Data;

@Data
public class AppDto {
    private String id;
    private String name;
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
}
