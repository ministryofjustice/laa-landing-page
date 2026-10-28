package uk.gov.justice.laa.portal.landingpage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.datauserapi.contracts.response.UserAccountSummaryPage;
import uk.gov.justice.laa.datauserapi.contracts.response.UserAccountSummaryView;
import uk.gov.justice.laa.portal.landingpage.client.CreateExternalUserRequest;
import uk.gov.justice.laa.portal.landingpage.client.CreateExternalUserResult;
import uk.gov.justice.laa.portal.landingpage.client.UserDataApiClient;
import uk.gov.justice.laa.portal.landingpage.client.UserDataApiClientException;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.exception.CreateUserDetailsIncompleteException;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExternalUserCreationApiService {

    private final UserDataApiClient client;
    private final UserDataApiRequestExecutor executor;
    private final FirmDirectoryApiService firms;

    public Optional<UserAccountSummaryView> findByEmail(String email) {
        int pageNumber = 0;
        int totalPages;
        do {
            int currentPage = pageNumber;
            UserAccountSummaryPage response = executor.call((token, oid, cid) ->
                    client.queryUsersByEmail(token, oid, cid, email.trim(), currentPage));
            if (response == null || response.items() == null || response.page() == null
                    || response.page().pageNumber() != currentPage || response.page().totalPages() < 0
                    || response.items().stream().anyMatch(account -> account == null || account.email() == null)) {
                log.error("Invalid user search response on page {}", currentPage);
                throw new UserDataApiClientException("Invalid user search response", 502);
            }
            Optional<UserAccountSummaryView> match = response.items().stream()
                    .filter(account -> email.trim().equalsIgnoreCase(account.email().trim())).findFirst();
            if (match.isPresent()) {
                return match;
            }
            totalPages = response.page().totalPages();
            pageNumber++;
        } while (pageNumber < totalPages);
        return Optional.empty();
    }

    public List<FirmDto> searchFirms(String query, int limit) {
        return firms.searchFirms(query.trim(), Math.max(10, Math.min(limit, 100)));
    }

    public Optional<FirmDto> selectFirm(String code, String query) {
        boolean hasCode = code != null && !code.isBlank();
        return searchFirms(hasCode ? code : query, 100).stream()
                .filter(firm -> hasCode ? code.equals(firm.getCode())
                        : FirmComparatorByRelevance.relevance(firm, query) > 0)
                .sorted((left, right) -> Integer.compare(FirmComparatorByRelevance.relevance(right, query),
                        FirmComparatorByRelevance.relevance(left, query)))
                .findFirst();
    }

    public CreateExternalUserResult create(EntraUserDto user, FirmDto firm, Boolean multiFirmUser) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()
                || user.getFirstName() == null || user.getFirstName().isBlank()
                || user.getLastName() == null || user.getLastName().isBlank() || multiFirmUser == null
                || (!multiFirmUser && (firm == null || firm.getId() != null
                || firm.getCode() == null || firm.getCode().isBlank()))) {
            throw new CreateUserDetailsIncompleteException();
        }
        CreateExternalUserRequest request = new CreateExternalUserRequest(user.getEmail().trim(),
                user.getFirstName().trim(), user.getLastName().trim(), UserType.EXTERNAL,
                multiFirmUser ? null : firm.getCode(), multiFirmUser);
        return executor.call((token, oid, cid) -> client.createExternalUser(token, oid, cid, request));
    }
}
