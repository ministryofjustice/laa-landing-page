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
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppsOrderForm implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Valid
    @NotNull
    private List<AppOrderDetailsForm> apps;

    @AssertTrue(message = "Two or more applications have the same ordinal. Each ordinal must be unique.")
    public boolean isOrdinalsUnique() {
        if (apps == null || apps.isEmpty()) return true;

        List<Integer> ordinals = apps.stream()
                .map(AppOrderDetailsForm::getOrdinal)
                .toList();

        return ordinals.size() == new HashSet<>(ordinals).size();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AppOrderDetailsForm implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotBlank
        private String appId;
        private String name;
        @Range(min = 0, max = Integer.MAX_VALUE)
        private int ordinal;

        public AppOrderDetailsForm(AppDto appDto) {
            this.appId = appDto.getId();
            this.name = appDto.getName();
            this.ordinal = appDto.getOrdinal();
        }

        public AppOrderDetailsForm(String appId, int ordinal) {
            this.appId = appId;
            this.ordinal = ordinal;
        }
    }
}
