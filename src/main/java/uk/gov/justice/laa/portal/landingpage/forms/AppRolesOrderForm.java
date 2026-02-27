package uk.gov.justice.laa.portal.landingpage.forms;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Range;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleAdminDto;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppRolesOrderForm implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Valid
    @NotNull
    private List<AppRolesOrderDetailsForm> appRoles;

    @AssertTrue(message = "Two or more app roles have the same ordinal. Each ordinal must be unique.")
    public boolean isOrdinalsUnique() {
        if (appRoles == null || appRoles.isEmpty()) {
            return true;
        }

        List<Integer> ordinals = appRoles.stream()
                .map(AppRolesOrderDetailsForm::getOrdinal)
                .toList();

        return ordinals.size() == new HashSet<>(ordinals).size();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AppRolesOrderDetailsForm implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotBlank
        private String appRoleId;
        private String name;
        @Range(min = 0, max = Integer.MAX_VALUE)
        private int ordinal;

        public AppRolesOrderDetailsForm(AppRoleAdminDto appRoleDto) {
            this.appRoleId = appRoleDto.getId();
            this.name = appRoleDto.getName();
            this.ordinal = appRoleDto.getOrdinal();
        }

    }
}
