package uk.gov.justice.laa.portal.landingpage.service.pda.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Captor;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.justice.laa.portal.landingpage.dto.PdaSyncResultDto;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.repository.OfficeRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;

/**
 * Tests for DeleteOfficeCommand.
 */
@ExtendWith(MockitoExtension.class)
class DeleteOfficeCommandTest {

    @Mock
    private OfficeRepository officeRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Captor
    private ArgumentCaptor<List<UserProfile>> profilesCaptor;

    private Firm firm;
    private Office office;
    private PdaSyncResultDto result;

    @BeforeEach
    void setUp() {
        firm = Firm.builder()
                .id(UUID.randomUUID())
                .code("12345")
                .name("Test Firm")
                .build();

        office = Office.builder()
                .id(UUID.randomUUID())
                .code("12345-001")
                .firm(firm)
                .address(Office.Address.builder()
                        .addressLine1("123 Test Street")
                        .city("London")
                        .build())
                .build();

        result = PdaSyncResultDto.builder()
                .officesDeleted(0)
                .errors(new ArrayList<>())
                .build();
    }

    @Test
    void shouldDeleteOfficeWithoutUserAssociations() {
        // Given
        when(userProfileRepository.findByOfficeId(office.getId()))
                .thenReturn(Collections.emptyList());

        DeleteOfficeCommand command = new DeleteOfficeCommand(
                officeRepository, userProfileRepository, office);

        // When
        command.execute(result);

        // Then
        verify(officeRepository).delete(office);
        verify(userProfileRepository, never()).saveAll(any());
        assertThat(result.getOfficesDeleted()).isEqualTo(1);
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void shouldRemoveOfficeFromUserProfilesBeforeDeleting() {
        // Given
        Set<Office> offices1 = new HashSet<>(Set.of(office));
        Set<Office> offices2 = new HashSet<>(Set.of(office));

        UserProfile profile1 = UserProfile.builder()
                .id(UUID.randomUUID())
                .activeProfile(true)
                .offices(offices1)
                .build();

        UserProfile profile2 = UserProfile.builder()
                .id(UUID.randomUUID())
                .activeProfile(true)
                .offices(offices2)
                .build();

        when(userProfileRepository.findByOfficeId(office.getId()))
                .thenReturn(List.of(profile1, profile2));

        DeleteOfficeCommand command = new DeleteOfficeCommand(
                officeRepository, userProfileRepository, office);

        // When
        command.execute(result);

        // Then
        verify(userProfileRepository).saveAll(profilesCaptor.capture());
        List<UserProfile> savedProfiles = profilesCaptor.getValue();

        assertThat(savedProfiles).hasSize(2);
        assertThat(savedProfiles.get(0).getOffices()).doesNotContain(office);
        assertThat(savedProfiles.get(1).getOffices()).doesNotContain(office);

        verify(officeRepository).delete(office);
        assertThat(result.getOfficesDeleted()).isEqualTo(1);
    }

    @Test
    void shouldHandleMultipleUserAssociations() {
        // Given
        List<UserProfile> profiles = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Set<Office> offices = new HashSet<>(Set.of(office));
            UserProfile profile = UserProfile.builder()
                    .id(UUID.randomUUID())
                    .activeProfile(true)
                    .offices(offices)
                    .build();
            profiles.add(profile);
        }

        when(userProfileRepository.findByOfficeId(office.getId()))
                .thenReturn(profiles);

        DeleteOfficeCommand command = new DeleteOfficeCommand(
                officeRepository, userProfileRepository, office);

        // When
        command.execute(result);

        // Then
        verify(userProfileRepository).saveAll(profilesCaptor.capture());
        List<UserProfile> savedProfiles = profilesCaptor.getValue();

        assertThat(savedProfiles).hasSize(5);
        savedProfiles.forEach(profile ->
                assertThat(profile.getOffices()).doesNotContain(office));

        verify(officeRepository).delete(office);
        assertThat(result.getOfficesDeleted()).isEqualTo(1);
    }

    @Test
    void shouldHandleExceptionDuringDeletion() {
        // Given
        when(userProfileRepository.findByOfficeId(office.getId()))
                .thenReturn(Collections.emptyList());
        doThrow(new RuntimeException("Database error"))
                .when(officeRepository).delete(office);

        DeleteOfficeCommand command = new DeleteOfficeCommand(
                officeRepository, userProfileRepository, office);

        // When
        command.execute(result);

        // Then
        assertThat(result.getOfficesDeleted()).isEqualTo(0);
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0)).contains("Failed to delete office");
        assertThat(result.getErrors().get(0)).contains("12345-001");
    }

    @Test
    void shouldHandleExceptionDuringUserProfileUpdate() {
        // Given
        Set<Office> offices = new HashSet<>(Set.of(office));
        UserProfile profile = UserProfile.builder()
                .id(UUID.randomUUID())
                .activeProfile(true)
                .offices(offices)
                .build();

        when(userProfileRepository.findByOfficeId(office.getId()))
                .thenReturn(List.of(profile));
        when(userProfileRepository.saveAll(any()))
                .thenThrow(new RuntimeException("Profile update error"));

        DeleteOfficeCommand command = new DeleteOfficeCommand(
                officeRepository, userProfileRepository, office);

        // When
        command.execute(result);

        // Then
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0)).contains("Failed to delete office");
        verify(officeRepository, never()).delete(office);
    }

    @Test
    void shouldIncrementOfficesDeletedCounter() {
        // Given
        result = PdaSyncResultDto.builder()
                .officesDeleted(3)
                .errors(new ArrayList<>())
                .build();

        when(userProfileRepository.findByOfficeId(office.getId()))
                .thenReturn(Collections.emptyList());

        DeleteOfficeCommand command = new DeleteOfficeCommand(
                officeRepository, userProfileRepository, office);

        // When
        command.execute(result);

        // Then
        assertThat(result.getOfficesDeleted()).isEqualTo(4);
    }

    @Test
    void shouldHandleUserProfileWithMultipleOffices() {
        // Given
        Office otherOffice = Office.builder()
                .id(UUID.randomUUID())
                .code("12345-002")
                .firm(firm)
                .build();

        Set<Office> offices = new HashSet<>(Set.of(office, otherOffice));
        UserProfile profile = UserProfile.builder()
                .id(UUID.randomUUID())
                .activeProfile(true)
                .offices(offices)
                .build();

        when(userProfileRepository.findByOfficeId(office.getId()))
                .thenReturn(List.of(profile));

        DeleteOfficeCommand command = new DeleteOfficeCommand(
                officeRepository, userProfileRepository, office);

        // When
        command.execute(result);

        // Then
        verify(userProfileRepository).saveAll(profilesCaptor.capture());
        List<UserProfile> savedProfiles = profilesCaptor.getValue();

        assertThat(savedProfiles).hasSize(1);
        assertThat(savedProfiles.get(0).getOffices()).doesNotContain(office);
        assertThat(savedProfiles.get(0).getOffices()).contains(otherOffice);

        verify(officeRepository).delete(office);
    }

    @Test
    void shouldDeleteOfficeEvenIfProfileUpdateSucceeds() {
        // Given
        Set<Office> offices = new HashSet<>(Set.of(office));
        UserProfile profile = UserProfile.builder()
                .id(UUID.randomUUID())
                .activeProfile(true)
                .offices(offices)
                .build();

        when(userProfileRepository.findByOfficeId(office.getId()))
                .thenReturn(List.of(profile));

        DeleteOfficeCommand command = new DeleteOfficeCommand(
                officeRepository, userProfileRepository, office);

        // When
        command.execute(result);

        // Then
        verify(userProfileRepository).saveAll(any());
        verify(officeRepository).delete(office);
        assertThat(result.getOfficesDeleted()).isEqualTo(1);
        assertThat(result.getErrors()).isEmpty();
    }
}
