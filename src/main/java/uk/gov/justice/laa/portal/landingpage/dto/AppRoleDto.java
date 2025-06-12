package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.Data;

import java.util.Objects;

@Data
public class AppRoleDto {
    private String id;
    private String name;
    private AppDto app;

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AppRoleDto appRoleDto) {
            return Objects.equals(id, appRoleDto.id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
