package uk.gov.justice.laa.portal.landingpage.techservices;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeAccountEnabledRequest {

    AccountEnabledBody accountEnabled;

    public ChangeAccountEnabledRequest(boolean enabled, String disabledReason) {
        this.accountEnabled = new AccountEnabledBody(enabled, disabledReason);
    }

    public ChangeAccountEnabledRequest(boolean enabled) {
        this.accountEnabled = new AccountEnabledBody(enabled, null);
    }

    @Getter
    @Setter
    @AllArgsConstructor
    static class AccountEnabledBody {
        private boolean enabled;
        private String disabledReason;
    }
}
