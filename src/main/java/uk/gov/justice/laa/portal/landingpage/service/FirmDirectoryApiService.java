package uk.gov.justice.laa.portal.landingpage.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;
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

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * Firm Directory data access via laa-data-user-api, used instead of
 * {@link FirmService} / {@link OfficeService} when the firm directory data API feature flag is on.
 */
@Service
public class FirmDirectoryApiService {

    private static final long TOKEN_EXPIRY_BUFFER_SECONDS = 30;

    private final UserDataApiClient userDataApiClient;
    private final OAuth2AuthorizedClientRepository authorizedClientRepository;
    private final ObjectProvider<HttpServletRequest> requestProvider;

    public FirmDirectoryApiService(UserDataApiClient userDataApiClient,
                                   OAuth2AuthorizedClientRepository authorizedClientRepository,
                                   ObjectProvider<HttpServletRequest> requestProvider) {
        this.userDataApiClient = userDataApiClient;
        this.authorizedClientRepository = authorizedClientRepository;
        this.requestProvider = requestProvider;
    }

    /**
     * @param page 1-based page number as used by the UI
     * @param firmType the {@link FirmType} enum name to filter by, or null for no filtering
     */
    public PaginatedFirmDirectory getFirmsPage(String firmSearch, String firmType, int page, int pageSize,
                                               String sort, String direction) {
        FirmPageResponse response = call((token, oid, cid) ->
                userDataApiClient.queryFirms(token, oid, cid, firmSearch, firmType, page - 1, pageSize,
                        sort + "," + direction));

        int lastPage = Math.max(1, response.page().totalPages());
        if (page > lastPage) {
            return getFirmsPage(firmSearch, firmType, lastPage, pageSize, sort, direction);
        }
        return PaginatedFirmDirectory.builder()
                .firmDirectories(response.items().stream().map(this::toDirectoryDto).toList())
                .totalElements(response.page().totalElements())
                .totalPages(lastPage)
                .currentPage(page)
                .pageSize(pageSize)
                .build();
    }

    public List<FirmDto> searchFirms(String query, int limit) {
        return call((token, oid, cid) -> userDataApiClient.queryFirmsSearch(token, oid, cid, query, limit))
                .items().stream()
                .map(firm -> FirmDto.builder().code(firm.firmId()).name(firm.name()).build())
                .toList();
    }

    public FirmDto getFirm(String firmId) {
        FirmResponse firm = call((token, oid, cid) -> userDataApiClient.queryFirmById(token, oid, cid, firmId));
        return FirmDto.builder()
                .code(firm.firmId())
                .name(firm.name())
                .type(firm.type() == null ? null : FirmType.valueOf(firm.type().name()))
                .enabled(firm.enabled())
                .build();
    }

    /**
     * The API returns all offices for a firm, so ordering and paging are applied here.
     *
     * @param page 1-based page number as used by the UI
     * @param sort the office property to order by (unsupported values fall back to the office code)
     */
    public PaginatedOffices getOfficesPage(String firmId, int page, int pageSize, String sort, String direction) {
        if (pageSize < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page size must be positive");
        }
        List<OfficeDto> all = call((token, oid, cid) -> userDataApiClient.queryFirmOffices(token, oid, cid, firmId))
                .items().stream().sorted(officeComparator(sort, direction)).map(this::toOfficeDto).toList();

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

    public void bulkDeactivateFirmUsers(String firmCode, DeactivateUserReason reason) {
        CommandResult result = call((token, oid, cid) -> userDataApiClient.bulkDeactivateFirmUsers(token, oid, cid,
                new BulkDeactivateFirmUsersRequest(firmCode, reason)));
        if (result == null || !result.success()) {
            throw new UserDataApiClientException("Bulk deactivation was not reported as successful", 502);
        }
    }

    private Comparator<OfficeView> officeComparator(String sort, String direction) {
        Function<OfficeView, String> key = switch (sort == null ? "code" : sort) {
            case "address.addressLine1" -> OfficeView::addressLine1;
            case "address.city" -> OfficeView::city;
            case "address.postcode" -> OfficeView::postCode;
            default -> OfficeView::officeId;
        };
        Comparator<String> order = "desc".equalsIgnoreCase(direction)
                ? String.CASE_INSENSITIVE_ORDER.reversed()
                : String.CASE_INSENSITIVE_ORDER;
        return Comparator.comparing(key, Comparator.nullsLast(order));
    }

    private FirmDirectoryDto toDirectoryDto(FirmView firm) {
        return FirmDirectoryDto.builder()
                .firmId(firm.firmId())
                .firmCode(firm.firmId())
                .firmName(firm.name())
                .firmType(firm.type() == null ? null : firm.type().getValue())
                .enabled(firm.enabled())
                .build();
    }

    private OfficeDto toOfficeDto(OfficeView office) {
        return OfficeDto.builder()
                .code(office.officeId())
                .address(OfficeDto.AddressDto.builder()
                        .addressLine1(office.addressLine1())
                        .addressLine2(office.addressLine2())
                        .addressLine3(office.addressLine3())
                        .city(office.city() == null ? "" : office.city())
                        .postcode(office.postCode())
                        .build())
                .build();
    }

    private <T> T call(ApiCall<T> apiCall) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            throw new UserDataApiClientException("No OAuth2 authentication available for data API call", 401);
        }
        String registrationId = oauthToken.getAuthorizedClientRegistrationId();
        HttpServletRequest request = requestProvider.getObject();
        OAuth2AuthorizedClient client = authorizedClientRepository.loadAuthorizedClient(
                registrationId, oauthToken, request);
        if (client == null || client.getAccessToken() == null || isExpiringSoon(client.getAccessToken())) {
            throw new ClientAuthorizationRequiredException(registrationId);
        }
        String oid = oauthToken.getPrincipal().getAttribute("oid");
        try {
            return apiCall.apply(client.getAccessToken().getTokenValue(), oid, resolveCorrelationId(request));
        } catch (UserDataApiClientException e) {
            if (e.getStatusCode() == 401) {
                // Handled by GlobalControllerAdvice: redirects the user to re-authenticate
                throw new ClientAuthorizationRequiredException(registrationId);
            }
            throw e;
        } catch (HttpClientErrorException e) {
            // The OBO token exchange rejected the user's access token (e.g. invalid_grant)
            int status = e.getStatusCode().value();
            if (status == 400 || status == 401) {
                throw new ClientAuthorizationRequiredException(registrationId);
            }
            throw e;
        }
    }

    private boolean isExpiringSoon(OAuth2AccessToken accessToken) {
        Instant expiresAt = accessToken.getExpiresAt();
        return expiresAt != null && !expiresAt.isAfter(Instant.now().plusSeconds(TOKEN_EXPIRY_BUFFER_SECONDS));
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        String header = request.getHeader("X-Correlation-ID");
        return header != null && !header.isBlank() ? header : UUID.randomUUID().toString();
    }

    @FunctionalInterface
    private interface ApiCall<T> {
        T apply(String token, String oid, String correlationId);
    }
}
