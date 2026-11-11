package uk.gov.justice.laa.portal.landingpage.utils;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import uk.gov.justice.laa.portal.landingpage.entity.SilasAccountStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfileSilasStatus;
import uk.gov.justice.laa.portal.landingpage.model.ReactivationRequestStatus;

@Component("statusTagClass")
public class StatusTagClassUtil {

    public Map<String, String> userProfileStatusClasses() {
        return Arrays.stream(UserProfileSilasStatus.values())
                .collect(Collectors.toMap(Enum::name, this::userProfileSilasStatusClass));
    }

    public Map<String, String> accountStatusClasses() {
        return Arrays.stream(SilasAccountStatus.values())
                .collect(Collectors.toMap(Enum::name, this::silasAccountStatusClass));
    }

    public Map<String, String> reactivationRequestStatusClasses() {
        return Arrays.stream(ReactivationRequestStatus.values())
                .collect(Collectors.toMap(Enum::name, this::reactivationRequestStatusClass));
    }

    public String userProfileSilasStatusClass(UserProfileSilasStatus status) {
        return switch (status) {
            case ACTIVATION_REQUIRED, UNKNOWN -> "govuk-tag--grey";
            case COMPLETE -> "govuk-tag--green";
            case NO_ACCESS_ASSIGNED -> "govuk-tag--yellow";
            case NO_FIRMS_LINKED -> "govuk-tag--orange";
        };
    }

    public String silasAccountStatusClass(SilasAccountStatus status) {
        return switch (status) {
            case ACTIVE -> "govuk-tag--green";
            case DEACTIVATED -> "govuk-tag--red";
            case ACTIVATION_REQUIRED, UNKNOWN -> "govuk-tag--grey";
        };
    }

    public String reactivationRequestStatusClass(ReactivationRequestStatus status) {
        return switch (status) {
            case IN_REVIEW -> "govuk-tag--yellow";
            case INFORMATION_REQUIRED -> "govuk-tag--blue";
            case APPROVED -> "govuk-tag--green";
            case REJECTED -> "govuk-tag--red";
            case EXPIRED -> "govuk-tag--grey";
            case FIRM_APPROVAL_REQUIRED -> "govuk-tag--purple";
        };
    }

}