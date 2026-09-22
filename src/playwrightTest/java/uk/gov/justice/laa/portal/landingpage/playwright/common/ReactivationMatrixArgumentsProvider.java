package uk.gov.justice.laa.portal.landingpage.playwright.common;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.util.stream.Stream;

public class ReactivationMatrixArgumentsProvider
        implements ArgumentsProvider {

    public enum ReactivationOutcome {
        DIRECT_REACTIVATION,
        RAISE_REQUEST,
        NOT_ALLOWED
    }

    public record ReactivationMatrixCase(
            String deactivatedBy,
            String deactivateType,
            String externalUserEmail,
            TestUser deactivatedByActor,
            TestUser actor,
            ReactivationOutcome expectedOutcome
    ) {

        @Override
        public String toString() {
            return "Deactivated by: "
                    + deactivatedBy
                    + " | Actor: "
                    + actor
                    + " | Expected: "
                    + expectedOutcome;
        }
    }

    @Override
    public Stream<? extends Arguments> provideArguments(
            ExtensionContext context
    ) {

        return Stream.of(

                /*
                 * =====================================================
                 * DEACTIVATED BY: FIRM USER MANAGER
                 * Deactivation Type: Firm
                 * =====================================================
                 */

                Arguments.of(new ReactivationMatrixCase(
                        "Firm User Manager",
                        "Firm",
                        "playwright-matrix-fum-fum@playwrighttest.com",
                        TestUser.FIRM_USER_MANAGER,
                        TestUser.FIRM_USER_MANAGER,
                        ReactivationOutcome.DIRECT_REACTIVATION
                )),

                Arguments.of(new ReactivationMatrixCase(
                        "Firm User Manager",
                        "Firm",
                        "playwright-matrix-fum-eua@playwrighttest.com",
                        TestUser.FIRM_USER_MANAGER,
                        TestUser.EXTERNAL_USER_ADMIN,
                        ReactivationOutcome.DIRECT_REACTIVATION
                )),

                Arguments.of(new ReactivationMatrixCase(
                        "Firm User Manager",
                        "Firm",
                        "playwright-matrix-fum-eum@playwrighttest.com",
                        TestUser.FIRM_USER_MANAGER,
                        TestUser.EXTERNAL_USER_MANAGER,
                        ReactivationOutcome.RAISE_REQUEST
                )),

                Arguments.of(new ReactivationMatrixCase(
                        "Firm User Manager",
                        "Firm",
                        "playwright-matrix-fum-ga@playwrighttest.com",
                        TestUser.FIRM_USER_MANAGER,
                        TestUser.GLOBAL_ADMIN,
                        ReactivationOutcome.DIRECT_REACTIVATION
                )),


                /*
                 * =====================================================
                 * DEACTIVATED BY: EXTERNAL USER MANAGER
                 * Deactivation Type: LAA
                 * =====================================================
                 */

                Arguments.of(new ReactivationMatrixCase(
                        "External User Manager",
                        "LAA",
                        "playwright-matrix-eum-fum@playwrighttest.com",
                        TestUser.EXTERNAL_USER_MANAGER,
                        TestUser.FIRM_USER_MANAGER,
                        ReactivationOutcome.RAISE_REQUEST
                )),

                Arguments.of(new ReactivationMatrixCase(
                        "External User Manager",
                        "LAA",
                        "playwright-matrix-eum-eua@playwrighttest.com",
                        TestUser.EXTERNAL_USER_MANAGER,
                        TestUser.EXTERNAL_USER_ADMIN,
                        ReactivationOutcome.DIRECT_REACTIVATION
                )),

                Arguments.of(new ReactivationMatrixCase(
                        "External User Manager",
                        "LAA",
                        "playwright-matrix-eum-eum@playwrighttest.com",
                        TestUser.EXTERNAL_USER_MANAGER,
                        TestUser.EXTERNAL_USER_MANAGER,
                        ReactivationOutcome.RAISE_REQUEST
                )),

                Arguments.of(new ReactivationMatrixCase(
                        "External User Manager",
                        "LAA",
                        "playwright-matrix-eum-ga@playwrighttest.com",
                        TestUser.EXTERNAL_USER_MANAGER,
                        TestUser.GLOBAL_ADMIN,
                        ReactivationOutcome.DIRECT_REACTIVATION
                )),


                /*
                 * =====================================================
                 * DEACTIVATED BY: EXTERNAL USER ADMIN
                 * Deactivation Type: LAA
                 * =====================================================
                 */

                Arguments.of(new ReactivationMatrixCase(
                        "External User Admin",
                        "LAA",
                        "playwright-matrix-eua-fum@playwrighttest.com",
                        TestUser.EXTERNAL_USER_ADMIN,
                        TestUser.FIRM_USER_MANAGER,
                        ReactivationOutcome.RAISE_REQUEST
                )),

                Arguments.of(new ReactivationMatrixCase(
                        "External User Admin",
                        "LAA",
                        "playwright-matrix-eua-eua@playwrighttest.com",
                        TestUser.EXTERNAL_USER_ADMIN,
                        TestUser.EXTERNAL_USER_ADMIN,
                        ReactivationOutcome.DIRECT_REACTIVATION
                )),

                Arguments.of(new ReactivationMatrixCase(
                        "External User Admin",
                        "LAA",
                        "playwright-matrix-eua-eum@playwrighttest.com",
                        TestUser.EXTERNAL_USER_ADMIN,
                        TestUser.EXTERNAL_USER_MANAGER,
                        ReactivationOutcome.RAISE_REQUEST
                )),

                Arguments.of(new ReactivationMatrixCase(
                        "External User Admin",
                        "LAA",
                        "playwright-matrix-eua-ga@playwrighttest.com",
                        TestUser.EXTERNAL_USER_ADMIN,
                        TestUser.GLOBAL_ADMIN,
                        ReactivationOutcome.DIRECT_REACTIVATION
                )),


                /*
                 * =====================================================
                 * DEACTIVATED BY: GLOBAL ADMIN
                 * Deactivation Type: Privileged
                 * =====================================================
                 */

                Arguments.of(new ReactivationMatrixCase(
                        "Global Admin",
                        "Privileged",
                        "playwright-matrix-ga-fum@playwrighttest.com",
                        TestUser.GLOBAL_ADMIN,
                        TestUser.FIRM_USER_MANAGER,
                        ReactivationOutcome.NOT_ALLOWED
                )),

                Arguments.of(new ReactivationMatrixCase(
                        "Global Admin",
                        "Privileged",
                        "playwright-matrix-ga-eua@playwrighttest.com",
                        TestUser.GLOBAL_ADMIN,
                        TestUser.EXTERNAL_USER_ADMIN,
                        ReactivationOutcome.NOT_ALLOWED
                )),

                Arguments.of(new ReactivationMatrixCase(
                        "Global Admin",
                        "Privileged",
                        "playwright-matrix-ga-eum@playwrighttest.com",
                        TestUser.GLOBAL_ADMIN,
                        TestUser.EXTERNAL_USER_MANAGER,
                        ReactivationOutcome.NOT_ALLOWED
                )),

                Arguments.of(new ReactivationMatrixCase(
                        "Global Admin",
                        "Privileged",
                        "playwright-matrix-ga-ga@playwrighttest.com",
                        TestUser.GLOBAL_ADMIN,
                        TestUser.GLOBAL_ADMIN,
                        ReactivationOutcome.DIRECT_REACTIVATION
                ))
        );
    }
}