package uk.gov.justice.laa.portal.landingpage.service;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import uk.gov.justice.laa.datauserapi.contracts.domain.DeactivateUserReason;
import uk.gov.justice.laa.datauserapi.contracts.domain.FirmType;
import uk.gov.justice.laa.datauserapi.contracts.dto.FirmView;
import uk.gov.justice.laa.datauserapi.contracts.dto.OfficeView;
import uk.gov.justice.laa.datauserapi.contracts.dto.PageMetadata;
import uk.gov.justice.laa.datauserapi.contracts.request.BulkDeactivateFirmUsersRequest;
import uk.gov.justice.laa.datauserapi.contracts.response.CommandResult;
import uk.gov.justice.laa.datauserapi.contracts.response.FirmOfficesResponse;
import uk.gov.justice.laa.datauserapi.contracts.response.FirmPageResponse;
import uk.gov.justice.laa.datauserapi.contracts.dto.FirmSearchView;
import uk.gov.justice.laa.datauserapi.contracts.response.FirmSearchResponse;
import uk.gov.justice.laa.datauserapi.contracts.response.FirmResponse;
import uk.gov.justice.laa.portal.landingpage.client.UserDataApiClient;
import uk.gov.justice.laa.portal.landingpage.client.UserDataApiClientException;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.PaginatedFirmDirectory;
import uk.gov.justice.laa.portal.landingpage.dto.PaginatedOffices;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FirmDirectoryApiServiceTest {

    private static final String OID = "11111111-1111-1111-1111-111111111111";

    @Mock
    private UserDataApiClient client;
    @Mock
    private OAuth2AuthorizedClientRepository authorizedClientRepository;
    @Mock
    private HttpServletRequest request;
    @Mock
    private OAuth2AuthorizedClient authorizedClient;
    @Mock
    private OAuth2User principal;

    private FirmDirectoryApiService service;

    @BeforeEach
    void setUp() {
        service = new FirmDirectoryApiService(client, authorizedClientRepository, request);
        OAuth2AuthenticationToken token = new OAuth2AuthenticationToken(
                principal, Set.of(), "azure");
        SecurityContextHolder.setContext(new org.springframework.security.core.context.SecurityContextImpl(token));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void stubAuthorizedClient() {
        when(principal.getAttribute("oid")).thenReturn(OID);
        when(authorizedClientRepository.loadAuthorizedClient(eq("azure"), any(), any())).thenReturn(authorizedClient);
        when(authorizedClient.getAccessToken()).thenReturn(new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "user-token", Instant.now(), Instant.now().plusSeconds(60)));
    }

    @Test
    void getFirmsPage_convertsToZeroIndexedPageAndMapsResponse() {
        stubAuthorizedClient();
        String firmId = "25966";
        FirmPageResponse response = new FirmPageResponse(
                List.of(new FirmView(firmId, "Firm A", FirmType.CHAMBERS, null, true)),
                new PageMetadata(1, 10, 25L, 3));
        when(client.queryFirms(eq("user-token"), eq(OID), anyString(), eq("abc"), eq(null), eq(1), eq(10),
                eq("type,desc"))).thenReturn(response);

        PaginatedFirmDirectory result = service.getFirmsPage("abc", null, 2, 10, "type", "desc");

        assertThat(result.getTotalElements()).isEqualTo(25L);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.getCurrentPage()).isEqualTo(2);
        assertThat(result.getFirmDirectories()).singleElement().satisfies(f -> {
            assertThat(f.getFirmId()).isEqualTo(firmId);
            assertThat(f.getFirmName()).isEqualTo("Firm A");
            assertThat(f.getFirmType()).isEqualTo("Chambers");
            assertThat(f.isEnabled()).isTrue();
        });
    }

    @Test
    void getFirmsPage_passesFirmTypeFilterThrough() {
        stubAuthorizedClient();
        String firmId = "25966";
        FirmPageResponse response = new FirmPageResponse(
                List.of(new FirmView(firmId, "Firm A", FirmType.CHAMBERS, null, true)),
                new PageMetadata(0, 10, 1L, 1));
        when(client.queryFirms(eq("user-token"), eq(OID), anyString(), eq(null), eq("CHAMBERS"), eq(0), eq(10),
                eq("name,asc"))).thenReturn(response);

        PaginatedFirmDirectory result = service.getFirmsPage(null, "CHAMBERS", 1, 10, "name", "asc");

        assertThat(result.getFirmDirectories()).singleElement()
                .satisfies(f -> assertThat(f.getFirmType()).isEqualTo("Chambers"));
    }

    @Test
    void getFirmsPage_normalizesEmptyResponsePagination() {
        stubAuthorizedClient();
        when(client.queryFirms(eq("user-token"), eq(OID), anyString(), eq(null), eq(null), anyInt(), eq(10),
                eq("name,asc")))
                .thenReturn(new FirmPageResponse(List.of(), new PageMetadata(0, 10, 0L, 0)));

        PaginatedFirmDirectory result = service.getFirmsPage(null, null, 6, 10, "name", "asc");

        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.getCurrentPage()).isEqualTo(1);
    }

    @Test
    void getFirmsPage_refetchesLastPageWhenRequestedPageIsOutOfRange() {
        stubAuthorizedClient();
        when(client.queryFirms(eq("user-token"), eq(OID), anyString(), eq(null), eq(null), eq(5), eq(10),
                eq("name,asc")))
                .thenReturn(new FirmPageResponse(List.of(), new PageMetadata(5, 10, 15L, 2)));
        when(client.queryFirms(eq("user-token"), eq(OID), anyString(), eq(null), eq(null), eq(1), eq(10),
                eq("name,asc")))
                .thenReturn(new FirmPageResponse(
                        List.of(new FirmView("25966", "Firm A", FirmType.CHAMBERS, null, true)),
                        new PageMetadata(1, 10, 15L, 2)));

        PaginatedFirmDirectory result = service.getFirmsPage(null, null, 6, 10, "name", "asc");

        assertThat(result.getCurrentPage()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getFirmDirectories()).extracting(f -> f.getFirmId()).containsExactly("25966");
    }

    @Test
    void searchFirms_mapsSuggestionsToFirmDtos() {
        stubAuthorizedClient();
        when(client.queryFirmsSearch(eq("user-token"), eq(OID), anyString(), eq("abc"), eq(10)))
                .thenReturn(new FirmSearchResponse(List.of(new FirmSearchView("25966", "Firm A"))));

        List<FirmDto> result = service.searchFirms("abc", 10);

        assertThat(result).singleElement().satisfies(f -> {
            assertThat(f.getCode()).isEqualTo("25966");
            assertThat(f.getName()).isEqualTo("Firm A");
        });
    }

    @Test
    void getFirm_mapsResponse() {
        stubAuthorizedClient();
        String firmId = "25966";
        when(client.queryFirmById(eq("user-token"), eq(OID), anyString(), eq(firmId)))
                .thenReturn(new FirmResponse(firmId, "Firm A", FirmType.ADVOCATE, null, true));

        FirmDto firm = service.getFirm(firmId);

        assertThat(firm.getCode()).isEqualTo(firmId);
        assertThat(firm.getName()).isEqualTo("Firm A");
        assertThat(firm.getType()).isEqualTo(uk.gov.justice.laa.portal.landingpage.entity.FirmType.ADVOCATE);
        assertThat(firm.isEnabled()).isTrue();
    }

    @Test
    void getOfficesPage_pagesInMemory() {
        stubAuthorizedClient();
        String firmId = "25966";
        List<OfficeView> offices = IntStream.range(0, 12)
                .mapToObj(i -> new OfficeView(UUID.randomUUID().toString(), firmId, "PC" + i,
                        "Line " + i, null, null, null))
                .toList();
        when(client.queryFirmOffices(eq("user-token"), eq(OID), anyString(), eq(firmId)))
                .thenReturn(new FirmOfficesResponse(offices));

        PaginatedOffices result = service.getOfficesPage(firmId, 2, 10, "code", "asc");

        assertThat(result.getOffices()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(12);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getOffices().get(0).getAddress().getCity()).isEmpty();
        assertThat(result.getOffices().get(0).getStatus()).isNull();
    }

    @Test
    void getOfficesPage_ordersWholeListBeforePaging() {
        stubAuthorizedClient();
        String firmId = "25966";
        List<OfficeView> offices = List.of("C", "A", "E", "B", "D").stream()
                .map(code -> new OfficeView(code, firmId, "PC", "Line", null, null, "City"))
                .toList();
        when(client.queryFirmOffices(eq("user-token"), eq(OID), anyString(), eq(firmId)))
                .thenReturn(new FirmOfficesResponse(offices));

        PaginatedOffices ascPage2 = service.getOfficesPage(firmId, 2, 2, "code", "asc");
        PaginatedOffices descPage1 = service.getOfficesPage(firmId, 1, 2, "code", "desc");

        assertThat(ascPage2.getOffices()).extracting(o -> o.getCode()).containsExactly("C", "D");
        assertThat(descPage1.getOffices()).extracting(o -> o.getCode()).containsExactly("E", "D");
    }

    @Test
    void getOfficesPage_rejectsNonPositivePageSize() {
        assertThatThrownBy(() -> service.getOfficesPage("25966", 1, 0, "code", "asc"))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.getOfficesPage("25966", 1, -1, "code", "asc"))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(client);
    }

    @Test
    void getOfficesPage_normalizesEmptyResponsePagination() {
        stubAuthorizedClient();
        String firmId = "25966";
        when(client.queryFirmOffices(eq("user-token"), eq(OID), anyString(), eq(firmId)))
                .thenReturn(new FirmOfficesResponse(List.of()));

        PaginatedOffices result = service.getOfficesPage(firmId, 2, 10, "code", "asc");

        assertThat(result.getOffices()).isEmpty();
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.getCurrentPage()).isEqualTo(1);
    }

    @Test
    void bulkDeactivateFirmUsers_sendsFirmAndReason() {
        stubAuthorizedClient();
        String firmId = "25966";
        BulkDeactivateFirmUsersRequest expected =
                new BulkDeactivateFirmUsersRequest(firmId, DeactivateUserReason.CyberRisk);
        when(client.bulkDeactivateFirmUsers(eq("user-token"), eq(OID), anyString(), eq(expected)))
                .thenReturn(CommandResult.success("ok"));

        service.bulkDeactivateFirmUsers(firmId, DeactivateUserReason.CyberRisk);
    }

    @Test
    void bulkDeactivateFirmUsers_failsWhenResultNotSuccessful() {
        stubAuthorizedClient();
        String firmId = "25966";
        when(client.bulkDeactivateFirmUsers(anyString(), anyString(), anyString(), any()))
                .thenReturn(new CommandResult(false, "nope"));

        assertThatThrownBy(() -> service.bulkDeactivateFirmUsers(firmId, DeactivateUserReason.CyberRisk))
                .isInstanceOf(UserDataApiClientException.class);
    }

    @Test
    void apiUnauthorized_triggersReauthentication() {
        stubAuthorizedClient();
        String firmId = "25966";
        when(client.queryFirmById(anyString(), anyString(), anyString(), eq(firmId)))
                .thenThrow(new UserDataApiClientException("x", 401));

        assertThatThrownBy(() -> service.getFirm(firmId))
                .isInstanceOf(ClientAuthorizationRequiredException.class);
    }

    @Test
    void otherApiErrors_arePropagatedWithStatus() {
        stubAuthorizedClient();
        String firmId = "25966";
        when(client.queryFirmById(anyString(), anyString(), anyString(), eq(firmId)))
                .thenThrow(new UserDataApiClientException("x", 404));

        assertThatThrownBy(() -> service.getFirm(firmId))
                .isInstanceOfSatisfying(UserDataApiClientException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(404));
    }

    @Test
    void missingAuthorizedClient_triggersReauthentication() {
        when(authorizedClientRepository.loadAuthorizedClient(eq("azure"), any(), any())).thenReturn(null);

        assertThatThrownBy(() -> service.getFirm("25966"))
                .isInstanceOf(ClientAuthorizationRequiredException.class);
    }

    @Test
    void expiredAccessToken_triggersReauthenticationWithoutCallingApi() {
        when(authorizedClientRepository.loadAuthorizedClient(eq("azure"), any(), any())).thenReturn(authorizedClient);
        when(authorizedClient.getAccessToken()).thenReturn(new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "user-token",
                Instant.now().minusSeconds(3600), Instant.now().minusSeconds(60)));

        assertThatThrownBy(() -> service.getFirm("25966"))
                .isInstanceOf(ClientAuthorizationRequiredException.class);
        verifyNoInteractions(client);
    }

    @Test
    void rejectedOboTokenExchange_triggersReauthentication() {
        stubAuthorizedClient();
        when(client.queryFirmById(eq("user-token"), eq(OID), anyString(), eq("25966")))
                .thenThrow(HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request",
                        HttpHeaders.EMPTY, new byte[0], null));

        assertThatThrownBy(() -> service.getFirm("25966"))
                .isInstanceOf(ClientAuthorizationRequiredException.class);
    }

    @Test
    void otherTokenExchangeClientErrors_arePropagated() {
        stubAuthorizedClient();
        when(client.queryFirmById(eq("user-token"), eq(OID), anyString(), eq("25966")))
                .thenThrow(HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests",
                        HttpHeaders.EMPTY, new byte[0], null));

        assertThatThrownBy(() -> service.getFirm("25966"))
                .isInstanceOf(HttpClientErrorException.class);
    }
}
