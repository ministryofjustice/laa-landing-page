package uk.gov.justice.laa.portal.landingpage.techservices;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegisterUserResponse implements Serializable {
    @JsonProperty("success")
    private boolean success;
    @JsonIgnore
    private ResponseType responseType;
    @JsonProperty("message")
    private String message;
    @JsonProperty("user")
    private TechServicesUser user;
    @JsonProperty("verification")
    private Verification verification;

    public boolean isUserCreated() {
        return ResponseType.CREATED.equals(responseType);
    }

    public boolean isUserFetched() {
        return ResponseType.VERIFIED.equals(responseType);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Verification implements Serializable {
        @JsonProperty("status")
        private String status;
        @JsonProperty("method")
        private String method;
        @JsonProperty("verified_at")
        private String verifiedAt;
    }

    public enum ResponseType {
        CREATED,
        VERIFIED
    }
}
