package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.datauserapi.contracts.domain.UserType;
import uk.gov.justice.laa.datauserapi.contracts.dto.FirmSearchView;
import uk.gov.justice.laa.datauserapi.contracts.dto.PageMetadata;
import uk.gov.justice.laa.datauserapi.contracts.request.CreateUserAccountRequest;
import uk.gov.justice.laa.datauserapi.contracts.response.CreateUserAccountResponse;
import uk.gov.justice.laa.datauserapi.contracts.response.FirmSearchResponse;
import uk.gov.justice.laa.datauserapi.contracts.response.UserAccountSummaryPage;
import uk.gov.justice.laa.datauserapi.contracts.response.UserAccountSummaryView;
import uk.gov.justice.laa.portal.landingpage.client.UserDataApiClient;
import uk.gov.justice.laa.portal.landingpage.client.UserDataApiClientException;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateUserApiServiceTest {

    @Mock
    private UserDataApiClient client;
    @Mock
    private DataApiCaller dataApiCaller;
    @Mock
    private FirmRepository firmRepository;

    private CreateUserApiService service;

    @BeforeEach
    void setUp() {
        service = new CreateUserApiService(client, dataApiCaller, firmRepository);
        lenient().when(dataApiCaller.call(any())).thenAnswer(invocation ->
                ((DataApiCaller.ApiCall<?>) invocation.getArgument(0)).apply("token", "oid", "cid"));
    }

    @Test
    void findUserByEmail_ignoresSubstringMatches() {
        UserAccountSummaryView substring = summary("other.a@b.com", false);
        UserAccountSummaryView exact = summary("A@B.com", true);
        when(client.queryUsersByEmail("token", "oid", "cid", "a@b.com", 0, 100))
                .thenReturn(new UserAccountSummaryPage(List.of(substring, exact), null));

        assertThat(service.findUserByEmail("a@b.com")).contains(exact);
    }

    @Test
    void findUserByEmail_followsPagesUntilExactMatchFound() {
        UserAccountSummaryView exact = summary("a@b.com", true);
        when(client.queryUsersByEmail("token", "oid", "cid", "a@b.com", 0, 100)).thenReturn(
                new UserAccountSummaryPage(List.of(summary("other.a@b.com", false)), new PageMetadata(0, 100, 101, 2)));
        when(client.queryUsersByEmail("token", "oid", "cid", "a@b.com", 1, 100)).thenReturn(
                new UserAccountSummaryPage(List.of(exact), new PageMetadata(1, 100, 101, 2)));

        assertThat(service.findUserByEmail("a@b.com")).contains(exact);
    }

    @Test
    void findUserByEmail_stopsAfterLastPage() {
        when(client.queryUsersByEmail("token", "oid", "cid", "a@b.com", 0, 100)).thenReturn(
                new UserAccountSummaryPage(List.of(summary("other.a@b.com", false)), new PageMetadata(0, 100, 1, 1)));

        assertThat(service.findUserByEmail("a@b.com")).isEmpty();
        verify(client, never()).queryUsersByEmail("token", "oid", "cid", "a@b.com", 1, 100);
    }

    @Test
    void findUserByEmail_returnsEmptyWhenNoExactMatch() {
        when(client.queryUsersByEmail(anyString(), anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(new UserAccountSummaryPage(List.of(summary("other.a@b.com", false)), null));

        assertThat(service.findUserByEmail("a@b.com")).isEmpty();
    }

    @Test
    void searchFirms_resolvesFirmIdAndDropsUnknownFirms() {
        UUID firmId = UUID.randomUUID();
        Firm known = Firm.builder().id(firmId).code("111").name("Known").enabled(true).build();
        when(client.queryFirmsSearch("token", "oid", "cid", "kn", 10)).thenReturn(new FirmSearchResponse(List.of(
                new FirmSearchView("111", "Known"), new FirmSearchView("222", "Not synced locally"))));
        when(firmRepository.findByCodeInAndEnabledTrue(List.of("111", "222"))).thenReturn(List.of(known));

        List<FirmDto> result = service.searchFirms("kn", 10);

        assertThat(result).singleElement().satisfies(firm -> {
            assertThat(firm.getId()).isEqualTo(firmId);
            assertThat(firm.getCode()).isEqualTo("111");
            assertThat(firm.getName()).isEqualTo("Known");
        });
    }

    @Test
    void searchFirms_withNoResultsDoesNotQueryLocalFirms() {
        when(client.queryFirmsSearch(anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(new FirmSearchResponse(List.of()));

        assertThat(service.searchFirms("zzz", 10)).isEmpty();
        verifyNoInteractions(firmRepository);
    }

    @Test
    void createUser_sendsExternalUserWithFirmAndMultiFirmOnly() {
        UUID firmId = UUID.randomUUID();
        UUID entraOid = UUID.randomUUID();
        EntraUserDto user = EntraUserDto.builder().email(" a@b.com ").firstName(" First ").lastName("Last ").build();
        FirmDto firm = FirmDto.builder().id(firmId).build();
        CreateUserAccountResponse response =
                new CreateUserAccountResponse(true, "ok", entraOid.toString(), UUID.randomUUID());
        when(client.createUser(eq("token"), eq("oid"), eq("cid"), any())).thenReturn(response);

        assertThat(service.createUser(user, firm, true)).isEqualTo(response);

        ArgumentCaptor<CreateUserAccountRequest> captor = ArgumentCaptor.forClass(CreateUserAccountRequest.class);
        verify(client).createUser(eq("token"), eq("oid"), eq("cid"), captor.capture());
        assertThat(captor.getValue()).isEqualTo(new CreateUserAccountRequest(
                "a@b.com", "First", "Last", UserType.EXTERNAL, true, firmId.toString()));
    }

    @Test
    void createUser_failsWhenApiDoesNotReportSuccess() {
        EntraUserDto user = EntraUserDto.builder().email("a@b.com").firstName("First").lastName("Last").build();
        when(client.createUser(anyString(), anyString(), anyString(), any()))
                .thenReturn(new CreateUserAccountResponse(false, "no", null, null));

        assertThatThrownBy(() -> service.createUser(user, FirmDto.builder().id(UUID.randomUUID()).build(), false))
                .isInstanceOfSatisfying(UserDataApiClientException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(502));
    }

    @Test
    void createUser_requiresFirm() {
        EntraUserDto user = EntraUserDto.builder().email("a@b.com").firstName("First").lastName("Last").build();

        assertThatThrownBy(() -> service.createUser(user, new FirmDto(), false))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(client);
    }

    private static UserAccountSummaryView summary(String email, boolean multiFirm) {
        return new UserAccountSummaryView(UUID.randomUUID().toString(), email, "F", "L", null, multiFirm,
                false, null, null, null, 1);
    }
}
