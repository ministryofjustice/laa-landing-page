package uk.gov.justice.laa.portal.landingpage.service;

import org.springframework.stereotype.Service;
import uk.gov.justice.laa.datauserapi.contracts.domain.UserType;
import uk.gov.justice.laa.datauserapi.contracts.request.CreateUserAccountRequest;
import uk.gov.justice.laa.datauserapi.contracts.response.CreateUserAccountResponse;
import uk.gov.justice.laa.datauserapi.contracts.response.UserAccountSummaryPage;
import uk.gov.justice.laa.datauserapi.contracts.response.UserAccountSummaryView;
import uk.gov.justice.laa.portal.landingpage.client.UserDataApiClient;
import uk.gov.justice.laa.portal.landingpage.client.UserDataApiClientException;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * External user creation via laa-data-user-api, used instead of {@link UserService#createUser}
 * when the create user data API feature flag is on.
 */
@Service
public class CreateUserApiService {

    private static final int EMAIL_LOOKUP_PAGE_SIZE = 100;
    private static final int MAX_EMAIL_LOOKUP_PAGES = 10;

    private final UserDataApiClient userDataApiClient;
    private final DataApiCaller dataApiCaller;
    private final FirmRepository firmRepository;

    public CreateUserApiService(UserDataApiClient userDataApiClient, DataApiCaller dataApiCaller,
                                FirmRepository firmRepository) {
        this.userDataApiClient = userDataApiClient;
        this.dataApiCaller = dataApiCaller;
        this.firmRepository = firmRepository;
    }

    /**
     * The API matches {@code email_query} as a substring, so pages are followed until an exact
     * (case-insensitive) match is found. The API only returns accounts that have a profile and that the
     * signed-in user is allowed to view, so an existing account outside that set is not found here and is
     * instead rejected with a 409 when the user is created.
     */
    public Optional<UserAccountSummaryView> findUserByEmail(String email) {
        for (int pageNumber = 0; pageNumber < MAX_EMAIL_LOOKUP_PAGES; pageNumber++) {
            final int requestedPage = pageNumber;
            UserAccountSummaryPage page = dataApiCaller.call((token, oid, cid) ->
                    userDataApiClient.queryUsersByEmail(token, oid, cid, email, requestedPage,
                            EMAIL_LOOKUP_PAGE_SIZE));
            if (page == null || page.items() == null) {
                return Optional.empty();
            }
            Optional<UserAccountSummaryView> match = page.items().stream()
                    .filter(user -> user.email() != null && user.email().equalsIgnoreCase(email))
                    .findFirst();
            if (match.isPresent()) {
                return match;
            }
            if (page.page() == null || pageNumber + 1 >= page.page().totalPages()) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    /**
     * Searches firms through the API. The search result identifies a firm by its code, whereas the
     * create request needs the firm id, so each result is resolved to its enabled firm; any
     * result that cannot be resolved cannot be used to create a user and is left out.
     */
    public List<FirmDto> searchFirms(String query, int limit) {
        var response = dataApiCaller.call((token, oid, cid) ->
                userDataApiClient.queryFirmsSearch(token, oid, cid, query, limit));
        if (response == null || response.items() == null || response.items().isEmpty()) {
            return List.of();
        }
        Map<String, Firm> firmsByCode = firmRepository
                .findByCodeInAndEnabledTrue(response.items().stream().map(item -> item.firmId()).toList())
                .stream()
                .collect(Collectors.toMap(Firm::getCode, Function.identity(), (first, second) -> first));
        return response.items().stream()
                .filter(item -> firmsByCode.containsKey(item.firmId()))
                .map(item -> FirmDto.builder()
                        .id(firmsByCode.get(item.firmId()).getId())
                        .code(item.firmId())
                        .name(item.name())
                        .enabled(true)
                        .build())
                .toList();
    }

    /**
     * Creates an external user with an initial profile for the firm. Roles and offices are assigned
     * separately through Grant Access.
     *
     * @throws UserDataApiClientException when the API rejects the request, e.g. 409 for an existing account
     */
    public CreateUserAccountResponse createUser(EntraUserDto user, FirmDto firm, boolean multiFirmUser) {
        if (firm == null || firm.getId() == null) {
            throw new IllegalArgumentException("A firm is required to create an external user");
        }
        CreateUserAccountRequest request = new CreateUserAccountRequest(
                user.getEmail().trim(),
                user.getFirstName().trim(),
                user.getLastName().trim(),
                UserType.EXTERNAL,
                multiFirmUser,
                firm.getId().toString());
        CreateUserAccountResponse response = dataApiCaller.call((token, oid, cid) ->
                userDataApiClient.createUser(token, oid, cid, request));
        if (response == null || !response.success()) {
            throw new UserDataApiClientException("User creation was not reported as successful", 502);
        }
        return response;
    }
}
