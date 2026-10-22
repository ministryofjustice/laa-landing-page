package uk.gov.justice.laa.portal.landingpage.playwright.common;

import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.service.TechServicesClient;
import uk.gov.justice.laa.portal.landingpage.techservices.ChangeAccountEnabledResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.GetAllApplicationsResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.GetUserResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.GetUsersResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.RegisterUserResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.SendUserVerificationEmailResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.TechServicesApiResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.TechServicesErrorResponse;

/**
 * Playwright-only override of {@link TechServicesClient}.
 *
 * <p>This bean exists purely to allow Playwright end-to-end tests to simulate
 * a failed response from the Tech Services/Entra API, without ever modifying
 * production code. It is only active under the {@code playwright} Spring
 * profile and is marked {@link Primary} so it is preferred over the standard
 * {@code DoNothingTechServicesClient} stub bean for that profile.</p>
 *
 * <p>For all users except the dedicated fixture below, every call is
 * delegated unchanged to the normal stub:</p>
 * <ul>
 *   <li>{@code playwright-audit-entra-error@playwrighttest.com} - returns a
 *   failed {@link TechServicesApiResponse} ({@code isSuccess() == false}),
 *   replicating what happens if Tech Services/Entra itself is unreachable
 *   or returns an error. {@code AuditController} handles this case
 *   gracefully (it simply skips populating the {@code entraUser} model
 *   attribute), so the Audit detail page should still render normally with
 *   a friendly "problem retrieving Entra Audit Data" banner, rather than
 *   returning a 500.</li>
 * </ul>
 */
@Component
@Profile("playwright")
@Primary
public class PlaywrightTechServicesClientOverride implements TechServicesClient {

    public static final String ENTRA_API_ERROR_EMAIL =
            "playwright-audit-entra-error@playwrighttest.com";

    private final TechServicesClient delegate;
    private final EntraUserRepository entraUserRepository;

    public PlaywrightTechServicesClientOverride(
            @Qualifier("doNothingTechServicesClient") TechServicesClient delegate,
            EntraUserRepository entraUserRepository) {
        this.delegate = delegate;
        this.entraUserRepository = entraUserRepository;
    }

    @Override
    public void updateRoleAssignment(UUID userId) {
        delegate.updateRoleAssignment(userId);
    }

    @Override
    public void deleteRoleAssignment(UUID userId) {
        delegate.deleteRoleAssignment(userId);
    }

    @Override
    public TechServicesApiResponse<RegisterUserResponse> registerNewUser(EntraUserDto user) {
        return delegate.registerNewUser(user);
    }

    @Override
    public TechServicesApiResponse<SendUserVerificationEmailResponse> sendEmailVerification(EntraUserDto user) {
        return delegate.sendEmailVerification(user);
    }

    @Override
    public TechServicesApiResponse<ChangeAccountEnabledResponse> disableUser(EntraUserDto user, String reason) {
        return delegate.disableUser(user, reason);
    }

    @Override
    public TechServicesApiResponse<ChangeAccountEnabledResponse> enableUser(EntraUserDto user) {
        return delegate.enableUser(user);
    }

    @Override
    public TechServicesApiResponse<GetUsersResponse> getUsers(String fromDateTime, String toDateTime) {
        return delegate.getUsers(fromDateTime, toDateTime);
    }

    @Override
    public TechServicesApiResponse<GetUserResponse> getUser(String entraOid) {
        Optional<EntraUser> optionalUser = entraUserRepository.findByEntraOid(entraOid);
        if (optionalUser.isPresent()
                && ENTRA_API_ERROR_EMAIL.equalsIgnoreCase(optionalUser.get().getEmail())) {
            return TechServicesApiResponse.error(TechServicesErrorResponse.builder()
                    .success(false)
                    .code("UPSTREAM_UNAVAILABLE")
                    .message("Simulated Tech Services/Entra API failure for Playwright test")
                    .build());
        }
        return delegate.getUser(entraOid);
    }

    @Override
    public TechServicesApiResponse<GetAllApplicationsResponse> getAllApplications() {
        return delegate.getAllApplications();
    }

    @Override
    public TechServicesApiResponse<ChangeAccountEnabledResponse> updateUserDetails(
            String entraOid, String firstName, String lastName, String email) {
        return delegate.updateUserDetails(entraOid, firstName, lastName, email);
    }
}
