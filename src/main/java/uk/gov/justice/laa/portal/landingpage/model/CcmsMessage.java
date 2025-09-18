package uk.gov.justice.laa.portal.landingpage.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Builder(toBuilder = true)
@AllArgsConstructor
@Data
public class CcmsMessage implements Serializable {

    @JsonProperty("userName")
    private final String userName;
    @JsonProperty("vendorNumber")
    private final String vendorNumber;
    @JsonProperty("firstName")
    private final String firstName;
    @JsonProperty("lastName")
    private final String lastName;
    @JsonProperty("timestampNow")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private final LocalDateTime timestamp;
    @JsonProperty("emailAddress")
    private final String email;
    @JsonProperty("responsibilityKey")
    private final List<String> responsibilityKey;
}
