package uk.gov.justice.laa.portal.landingpage.techservices;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetUsersResponse implements Serializable {
    
    @JsonProperty("success")
    private boolean success;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("users")
    private List<TechServicesUser> users;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TechServicesUser implements Serializable {
        
        @JsonProperty("id")
        private String id;
        
        @JsonProperty("displayName")
        private String displayName;
        
        @JsonProperty("mail")
        private String mail;
        
        @JsonProperty("accountEnabled")
        private boolean accountEnabled;
        
        @JsonProperty("createdDateTime")
        private LocalDateTime createdDateTime;
        
        @JsonProperty("givenName")
        private String givenName;
        
        @JsonProperty("surname")
        private String surname;
        
        @JsonProperty("alias")
        private List<String> alias;
        
        @JsonProperty("email")
        private String email;
        
        @JsonProperty("lastSignIn")
        private String lastSignIn;
        
        @JsonProperty("customSecurityAttributes")
        private CustomSecurityAttributes customSecurityAttributes;
        
        @JsonProperty("isMailOnly")
        private boolean isMailOnly;
        
        @JsonProperty("deleted")
        private boolean deleted;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CustomSecurityAttributes implements Serializable {
        
        @JsonProperty("GuestUserStatus")
        private GuestUserStatus guestUserStatus;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GuestUserStatus implements Serializable {
        
        @JsonProperty("@odata.type")
        private String odataType;
        
        @JsonProperty("DisabledReason")
        private String disabledReason;
        
        @JsonProperty("InvitationProgress")
        private String invitationProgress;
    }
}
