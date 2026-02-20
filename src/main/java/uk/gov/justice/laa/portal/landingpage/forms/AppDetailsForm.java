package uk.gov.justice.laa.portal.landingpage.forms;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppDetailsForm implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String appId;
    private boolean enabled;
    private String name;
    @NotBlank(message = "Application description cannot be empty")
    @Size(min = 1, max = 1000, message = "Application description cannot exceed 1000 characters")
    private String description;
    private String code;
    private int ordinal;
}
