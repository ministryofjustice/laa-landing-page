package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.datauserapi.contracts.dto.PageMetadata;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalUserCreationApiServiceTest {

    @Mock private UserDataApiClient client;
    @Mock private UserDataApiRequestExecutor executor;
    @Mock private FirmDirectoryApiService firms;
    private ExternalUserCreationApiService service;

    @BeforeEach
    void setUp() {
        service = new ExternalUserCreationApiService(client, executor, firms);
        lenient().when(executor.call(any())).thenAnswer(invocation -> {
            UserDataApiRequestExecutor.ApiCall<?> call = invocation.getArgument(0);
            return call.apply("token", "actor", "cid");
        });
    }

    @Test
    void checksAllPagesAndMatchesOnlyTheExactEmailIgnoringCaseAndWhitespace() {
        when(client.queryUsersByEmail("token", "actor", "cid", "Test@example.org", 0))
                .thenReturn(new UserAccountSummaryPage(List.of(account("prefixTest@example.org", false)),
                        new PageMetadata(0, 100, 101L, 2)));
        when(client.queryUsersByEmail("token", "actor", "cid", "Test@example.org", 1))
                .thenReturn(new UserAccountSummaryPage(List.of(account("test@example.org", true)),
                        new PageMetadata(1, 100, 101L, 2)));
        assertThat(service.findByEmail(" Test@example.org ")).hasValueSatisfying(account ->
                assertThat(account.multiFirmUser()).isTrue());
        verify(client).queryUsersByEmail("token", "actor", "cid", "Test@example.org", 1);
    }

    @Test
    void emptySearchDoesNotClaimGlobalUniqueness() {
        when(client.queryUsersByEmail("token", "actor", "cid", "test@example.org", 0))
                .thenReturn(new UserAccountSummaryPage(List.of(), new PageMetadata(0, 100, 0L, 0)));
        assertThat(service.findByEmail("test@example.org")).isEmpty();
    }

    @Test
    void missingSearchResponseIsNotAnEmptySuccess() {
        assertThatThrownBy(() -> service.findByEmail("test@example.org"))
                .isInstanceOf(UserDataApiClientException.class);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void buildsExternalRequestWithFirmOnlyForSingleFirmUsers(boolean multiFirm) {
        EntraUserDto user = EntraUserDto.builder().email(" test@example.org ")
                .firstName(" Test ").lastName(" User ").build();
        FirmDto firm = FirmDto.builder().code("00123").name("Firm").build();
        CreateExternalUserResult result = new CreateExternalUserResult(true, UUID.randomUUID(), null);
        when(client.createExternalUser(eq("token"), eq("actor"), eq("cid"), any())).thenReturn(result);
        assertThat(service.create(user, firm, multiFirm)).isSameAs(result);
        ArgumentCaptor<CreateExternalUserRequest> request = ArgumentCaptor.forClass(CreateExternalUserRequest.class);
        verify(client).createExternalUser(eq("token"), eq("actor"), eq("cid"), request.capture());
        assertThat(request.getValue()).isEqualTo(new CreateExternalUserRequest("test@example.org", "Test",
                "User", UserType.EXTERNAL, multiFirm ? null : "00123", multiFirm));
        verifyNoInteractions(firms);
    }

    @Test
    void refusesLegacyFirmAndIncompleteSessionData() {
        EntraUserDto user = EntraUserDto.builder().email("test@example.org")
                .firstName("Test").lastName("User").build();
        assertThatThrownBy(() -> service.create(user, FirmDto.builder().id(UUID.randomUUID()).code("123").build(), false))
                .isInstanceOf(CreateUserDetailsIncompleteException.class);
        assertThatThrownBy(() -> service.create(user, null, false))
                .isInstanceOf(CreateUserDetailsIncompleteException.class);
        assertThatThrownBy(() -> service.create(user, null, null))
                .isInstanceOf(CreateUserDetailsIncompleteException.class);
        verifyNoInteractions(client);
    }

    @Test
    void selectionValidatesExactApiCodeNotSubstring() {
        FirmDto exact = FirmDto.builder().code("00123").name("Firm").build();
        when(firms.searchFirms("00123", 100)).thenReturn(List.of(
                FirmDto.builder().code("001234").name("Other").build(), exact));
        assertThat(service.selectFirm("00123", "Firm")).contains(exact);
    }

    @Test
    void searchRetainsExistingResultLimits() {
        service.searchFirms(" firm ", 500);
        service.searchFirms(" firm ", 1);
        verify(firms).searchFirms("firm", 100);
        verify(firms).searchFirms("firm", 10);
    }

    private UserAccountSummaryView account(String email, boolean multiFirm) {
        return new UserAccountSummaryView(UUID.randomUUID().toString(), email, "Test", "User",
                null, multiFirm, false, null, null, null, 0);
    }
}
