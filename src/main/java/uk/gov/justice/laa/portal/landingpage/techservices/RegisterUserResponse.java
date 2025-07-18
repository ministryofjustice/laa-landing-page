package uk.gov.justice.laa.portal.landingpage.techservices;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterUserResponse {
    @JsonProperty("success")
    private boolean success;
    @JsonProperty("message")
    private String message;
    @JsonProperty("entraObject")
    private CreatedUser createdUser;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreatedUser {
        @JsonProperty("id")
        private String id;
        @JsonProperty("displayName")
        private String displayName;
        @JsonProperty("mail")
        private String mail;
        @JsonProperty("accountEnabled")
        private boolean accountEnabled;
        @JsonProperty("createdDateTime")
        private Date createdDateTime;
    }
}
