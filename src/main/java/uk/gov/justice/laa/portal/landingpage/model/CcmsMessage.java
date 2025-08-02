package uk.gov.justice.laa.portal.landingpage.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Builder(toBuilder = true)
@AllArgsConstructor
@Data
public class CcmsMessage {

    @JsonProperty("USER_NAME")
    private final String userName;
    @JsonProperty("VENDOR_NUMBER")
    private final String vendorNumber;
    @JsonProperty("FIRST_NAME")
    private final String firstName;
    @JsonProperty("LAST_NAME")
    private final String lastName;
    @JsonProperty("TIMESTAMP")
    private final LocalDateTime timestamp;
    @JsonProperty("EMAIL_ADDRESS")
    private final String email;
    @JsonProperty("RESPONSIBILITY_KEY")
    private final List<String> responsibilityKey;
}
