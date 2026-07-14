package uk.gov.justice.laa.portal.landingpage.techservices;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.laa.portal.landingpage.entity.InvitationStatus;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TechServicesUser implements Serializable {

    @JsonProperty("id")
    private String id;

    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("mail")
    private String mail;

    @JsonProperty("accountEnabled")
    private Boolean accountEnabled;

    @JsonProperty("createdDateTime")
    private Date createdDateTime;

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

    @JsonProperty("groups")
    private Set<String> groups;

    @JsonProperty("customSecurityAttributes")
    private CustomSecurityAttributes customSecurityAttributes;

    @JsonProperty("isMailOnly")
    private Boolean isMailOnly;

    @JsonProperty("deleted")
    private boolean deleted;

    @JsonProperty("verification")
    private VerificationStatus verification;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VerificationStatus implements Serializable {

        @JsonProperty("status")
        private String status;

        @JsonProperty("method")
        private String method;
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
        private InvitationStatus invitationProgress;

        @JsonProperty("ActivationCode")
        private String activationCode;

        @JsonProperty("Invitation")
        private String invitation;
    }
}




