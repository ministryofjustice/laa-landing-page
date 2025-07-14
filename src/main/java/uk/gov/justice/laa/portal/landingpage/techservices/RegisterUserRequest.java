package uk.gov.justice.laa.portal.landingpage.techservices;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterUserRequest {
    @JsonProperty("givenName")
    private String firstName;
    @JsonProperty("surname")
    private String lastName;
    @JsonProperty("companyName")
    private String companyName;
    @JsonProperty("mail")
    private String email;
    @JsonProperty("verification_method")
    private String verificationMethod;
    @JsonProperty("requiredGroups")
    private Set<String> requiredGroups;
    @JsonProperty("address")
    private Address address;

    @Data
    @Builder
    private static class Address {
        @JsonProperty("line1")
        private String addressLine1;
        @JsonProperty("line2")
        private String addressLine2;
        @JsonProperty("line3")
        private String addressLine3;
        @JsonProperty("line4")
        private String addressLine4;
        @JsonProperty("line5")
        private String addressLine5;
        @JsonProperty("line6")
        private String addressLine6;
        @JsonProperty("line7")
        private String addressLine7;
    }
}
