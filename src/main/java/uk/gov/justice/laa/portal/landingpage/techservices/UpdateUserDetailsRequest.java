package uk.gov.justice.laa.portal.landingpage.techservices;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Getter
@Setter
@ToString
public class UpdateUserDetailsRequest implements Serializable {
    UpdateUserDetailsBody userDetails;

    public UpdateUserDetailsRequest(String givenName, String surname, String mail ) {
        this.userDetails = new UpdateUserDetailsBody(givenName, surname, mail);
    }


    @Getter
    @Setter
    @AllArgsConstructor
    @ToString
    static class UpdateUserDetailsBody {
        private String givenName;
        private String surname;
        private String mail;

    }
}
