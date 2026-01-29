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
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("user")
    private List<TechServicesUser> user;
    
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
        
        @JsonProperty("CustomSecurityAttributeSet")
        private Object customSecurityAttributeSet;
        
        @JsonProperty("isMailOnly")
        private boolean isMailOnly;
        
        @JsonProperty("deleted")
        private boolean deleted;
    }
}
