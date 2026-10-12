package uk.gov.justice.laa.portal.landingpage.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.datauserapi.contracts.domain.DeactivateUserReason;
import uk.gov.justice.laa.datauserapi.contracts.dto.FirmView;
import uk.gov.justice.laa.datauserapi.contracts.dto.OfficeView;
import uk.gov.justice.laa.datauserapi.contracts.request.BulkDeactivateFirmUsersRequest;
import uk.gov.justice.laa.datauserapi.contracts.response.CommandResult;
import uk.gov.justice.laa.datauserapi.contracts.response.FirmPageResponse;
import uk.gov.justice.laa.datauserapi.contracts.response.FirmResponse;
import uk.gov.justice.laa.portal.landingpage.client.UserDataApiClient;
import uk.gov.justice.laa.portal.landingpage.client.UserDataApiClientException;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDirectoryDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.OfficeDto;
import uk.gov.justice.laa.portal.landingpage.dto.PaginatedFirmDirectory;
import uk.gov.justice.laa.portal.landingpage.dto.PaginatedOffices;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;

import java.util.List;
import java.util.UUID;

/**
 * Firm Directory data access via laa-data-user-api, used instead of
 * {@link FirmService} / {@link OfficeService} when the firm directory data API feature flag is on.
 */
@Service
public class FirmDirectoryApiService {

    private final UserDataApiClient userDataApiClient;
    private final OAuth2AuthorizedClientRepository authorizedClientRepository;
    private final HttpServletRequest request;

    public FirmDirectoryApiService(UserDataApiClient userDataApiClient,
                                   OAuth2AuthorizedClientRepository authorizedClientRepository,
                                   HttpServletRequest request) {
        this.userDataApiClient = userDataApiClient;
        this.authorizedClientRepository = authorizedClientRepository;
        this.request = request;
    }

    /**
     * @param page 1-based page number as used by the UI
     */
    public PaginatedFirmDirectory getFirmsPage(String firmSearch, int page, int pageSize) {
        FirmPageResponse response = call((token, oid, cid) ->
                userDataApiClient.queryFirms(token, oid, cid, firmSearch, page - 1, pageSize));

        List<FirmDirectoryDto> firms = response.items().stream().map(this::toDirectoryDto).toList();
        int totalPages = Math.max(1, response.page().totalPages());
        return PaginatedFirmDirectory.builder()
                .firmDirectories(firms)
                .totalElements(response.page().totalElements())
                .totalPages(totalPages)
                .currentPage(Math.max(1, Math.min(page, totalPages)))
                .pageSize(pageSize)
                .build();
    }

    public FirmDto getFirm(UUID firmId) {
        FirmResponse firm = call((token, oid, cid) -> userDataApiClient.queryFirmById(token, oid, cid, firmId));
        return FirmDto.builder()
                .id(UUID.fromString(firm.firmId()))
                .name(firm.name())
                .type(firm.type() == null ? null : FirmType.valueOf(firm.type().name()))
                .enabled(firm.enabled())
                .build();
    }

    /**
     * The API returns all offices for a firm, so paging is applied here.
     *
     * @param page 1-based page number as used by the UI
     */
    public PaginatedOffices getOfficesPage(UUID firmId, int page, int pageSize) {
        List<OfficeDto> all = call((token, oid, cid) -> userDataApiClient.queryFirmOffices(token, oid, cid, firmId))
                .items().stream().map(this::toOfficeDto).toList();

        int totalPages = Math.max(1, (int) Math.ceil((double) all.size() / pageSize));
        int currentPage = Math.max(1, Math.min(page, totalPages));
        int from = Math.min((currentPage - 1) * pageSize, all.size());
        int to = Math.min(from + pageSize, all.size());
        return PaginatedOffices.builder()
                .offices(all.subList(from, to))
                .totalElements(all.size())
                .totalPages(totalPages)
                .currentPage(currentPage)
                .pageSize(pageSize)
                .build();
    }

    public void bulkDeactivateFirmUsers(UUID firmId, DeactivateUserReason reason) {
        CommandResult result = call((token, oid, cid) -> userDataApiClient.bulkDeactivateFirmUsers(token, oid, cid,
                new BulkDeactivateFirmUsersRequest(firmId, reason)));
        if (result == null || !result.success()) {
            throw new UserDataApiClientException("Bulk deactivation was not reported as successful", 502);
        }
    }

    private FirmDirectoryDto toDirectoryDto(FirmView firm) {
        return FirmDirectoryDto.builder()
                .firmId(UUID.fromString(firm.firmId()))
                .firmName(firm.name())
                .firmType(firm.type() == null ? null : firm.type().getValue())
                .enabled(firm.enabled())
                .build();
    }

    private OfficeDto toOfficeDto(OfficeView office) {
        return OfficeDto.builder()
                .id(UUID.fromString(office.officeId()))
                .address(OfficeDto.AddressDto.builder()
                        .addressLine1(office.addressLine1())
                        .addressLine2(office.addressLine2())
                        .addressLine3(office.addressLine3())
                        .city(office.city() == null ? "" : office.city())
                        .postcode(office.postCode())
                        .build())
                .status(null)
                .build();
    }

    private <T> T call(ApiCall<T> apiCall) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            throw new UserDataApiClientException("No OAuth2 authentication available for data API call", 401);
        }
        String registrationId = oauthToken.getAuthorizedClientRegistrationId();
        OAuth2AuthorizedClient client = authorizedClientRepository.loadAuthorizedClient(
                registrationId, oauthToken, request);
        if (client == null || client.getAccessToken() == null) {
            throw new ClientAuthorizationRequiredException(registrationId);
        }
        String oid = oauthToken.getPrincipal().getAttribute("oid");
        try {
            return apiCall.apply(client.getAccessToken().getTokenValue(), oid, resolveCorrelationId());
        } catch (UserDataApiClientException e) {
            if (e.getStatusCode() == 401) {
                // Handled by GlobalControllerAdvice: redirects the user to re-authenticate
                throw new ClientAuthorizationRequiredException(registrationId);
            }
            throw e;
        }
    }

    private String resolveCorrelationId() {
        String header = request.getHeader("X-Correlation-ID");
        return header != null && !header.isBlank() ? header : UUID.randomUUID().toString();
    }

    @FunctionalInterface
    private interface ApiCall<T> {
        T apply(String token, String oid, String correlationId);
    }
}
