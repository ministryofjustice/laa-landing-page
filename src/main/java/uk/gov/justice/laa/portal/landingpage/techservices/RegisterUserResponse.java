package uk.gov.justice.laa.portal.landingpage.techservices;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegisterUserResponse implements Serializable {
    @JsonProperty("success")
    private boolean success;
    @JsonProperty("message")
    private String message;

    // Legacy compatibility: some TS responses still return 'entraObject'
    @JsonProperty("entraObject")
    private CreatedUser createdUser;

    // Preferred enhanced shape: minimal fields required by business logic
    @JsonProperty("user")
    private User user;

    @JsonProperty("verification")
    private Verification verification;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CreatedUser implements Serializable {
        @JsonProperty("id")
        private String id;
        @JsonProperty("mail")
        private String mail;
        @JsonProperty("accountEnabled")
        private boolean accountEnabled;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class User implements Serializable {
        @JsonProperty("id")
        private String id;
        @JsonProperty("email")
        private String email;
        @JsonProperty("accountEnabled")
        private boolean accountEnabled;
        @JsonProperty("customSecurityAttributes")
        private CustomSecurityAttributes customSecurityAttributes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CustomSecurityAttributes implements Serializable {
        @JsonProperty("GuestUserStatus")
        private GuestUserStatus guestUserStatus;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GuestUserStatus implements Serializable {
        @JsonProperty("DisabledReason")
        private String disabledReason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Verification implements Serializable {
        @JsonProperty("status")
        private String status;
    }
}
