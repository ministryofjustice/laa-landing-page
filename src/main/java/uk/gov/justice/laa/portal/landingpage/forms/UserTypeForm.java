package uk.gov.justice.laa.portal.landingpage.forms;

import lombok.Getter;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

@Getter
public enum UserTypeForm {
    ALL(null, null),
    INTERNAL(false, UserType.INTERNAL),
    EXTERNAL(false, UserType.EXTERNAL),
    MULTI_FIRM(true, null);

    private final Boolean multiFirm;
    private final UserType userType;

    UserTypeForm(Boolean multiFirm, UserType userType) {
        this.multiFirm = multiFirm;
        this.userType = userType;
    }

}
