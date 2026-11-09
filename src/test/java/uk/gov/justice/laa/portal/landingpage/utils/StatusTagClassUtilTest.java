package uk.gov.justice.laa.portal.landingpage.utils;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.portal.landingpage.entity.SilasAccountStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfileSilasStatus;
import uk.gov.justice.laa.portal.landingpage.model.ReactivationRequestStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StatusTagClassUtilTest {

    private final StatusTagClassUtil statusTagClassUtil = new StatusTagClassUtil();

    @ParameterizedTest
    @CsvSource({
        "ACTIVATION_REQUIRED, govuk-tag--grey",
        "NO_ACCESS_ASSIGNED, govuk-tag--yellow",
        "NO_FIRMS_LINKED, govuk-tag--orange",
        "COMPLETE, govuk-tag--green",
        "UNKNOWN, govuk-tag--grey"
    })
    void shouldReturnCorrectUserProfileSilasStatusClass(UserProfileSilasStatus status, String expectedClass) {
        assertThat(statusTagClassUtil.userProfileSilasStatusClass(status)).isEqualTo(expectedClass);
    }

    @ParameterizedTest
    @CsvSource({
        "ACTIVE, govuk-tag--green",
        "DEACTIVATED, govuk-tag--red",
        "ACTIVATION_REQUIRED, govuk-tag--grey",
        "UNKNOWN, govuk-tag--grey"
    })
    void shouldReturnCorrectSilasAccountStatusClass(SilasAccountStatus status, String expectedClass) {
        assertThat(statusTagClassUtil.silasAccountStatusClass(status)).isEqualTo(expectedClass);
    }

    @ParameterizedTest
    @CsvSource({
        "IN_REVIEW, govuk-tag--yellow",
        "INFORMATION_REQUIRED, govuk-tag--blue",
        "APPROVED, govuk-tag--green",
        "REJECTED, govuk-tag--red",
        "EXPIRED, govuk-tag--grey",
        "FIRM_APPROVAL_REQUIRED, govuk-tag--purple"
    })
    void shouldReturnCorrectReactivationRequestStatusClass(ReactivationRequestStatus status, String expectedClass) {
        assertThat(statusTagClassUtil.reactivationRequestStatusClass(status)).isEqualTo(expectedClass);
    }

    @ParameterizedTest
    @CsvSource({
        "ACTIVATION_REQUIRED, govuk-tag--grey",
        "NO_ACCESS_ASSIGNED, govuk-tag--yellow",
        "NO_FIRMS_LINKED, govuk-tag--orange",
        "COMPLETE, govuk-tag--green",
        "UNKNOWN, govuk-tag--grey"
    })
    void shouldConvertCorrectUserProfileSilasStatusName(String status, String expectedClass) {
        assertThat(statusTagClassUtil.userProfileSilasStatusClassName(status)).isEqualTo(expectedClass);
    }

    @Test
    void shouldRejectInvalidUserProfileSilasStatusName() {
        assertThatThrownBy(() -> statusTagClassUtil.userProfileSilasStatusClassName("INVALID"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldMapAccountStatusClass() {
        assertThat(statusTagClassUtil.activationRequiredClass())
                .isEqualTo(statusTagClassUtil.silasAccountStatusClass(SilasAccountStatus.ACTIVATION_REQUIRED));
        assertThat(statusTagClassUtil.activeClass())
                .isEqualTo(statusTagClassUtil.silasAccountStatusClass(SilasAccountStatus.ACTIVE));
        assertThat(statusTagClassUtil.deactivatedClass())
                .isEqualTo(statusTagClassUtil.silasAccountStatusClass(SilasAccountStatus.DEACTIVATED));
    }
}
