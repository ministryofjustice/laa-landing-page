package uk.gov.justice.laa.portal.landingpage.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import uk.gov.justice.laa.portal.landingpage.config.DataProviderConfig;
import uk.gov.justice.laa.portal.landingpage.dto.ComparisonResultDto;
import uk.gov.justice.laa.portal.landingpage.dto.PdaSyncResultDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfileStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;
import uk.gov.justice.laa.portal.landingpage.repository.OfficeRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;

/**
 * Integration tests for DataProviderService using real database and Spring context.
 * Tests the complex orchestration methods compareWithDatabase() and synchronizeWithPda().
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@SuppressWarnings("removal") // MockBean deprecated in Spring Boot 3.4.0+, will migrate when replacement is available
@TestPropertySource(properties = {
    "app.data.provider.use-local-file=true",
    "app.data.provider.local-file-path=/tmp/test-pda-data.json",
        "spring.security.tech.services.credentials.client-id=test-client-id",
        "spring.security.tech.services.credentials.client-secret=test-client-secret",
        "spring.security.tech.services.credentials.tenant-id=test-tenant-id",
        "spring.security.tech.services.credentials.scope=test-scope",
        "spring.security.tech.services.credentials.base-url=https://test.example.com",
  "ccms.user.api.sqs.arn=arn:aws:sqs:eu-west-2:123456789012:test-queue",
  "ccms.user.data.api.base-url=https://test.ccms.example.com",
  "ccms.uda.base-url=https://test.ccms-uda.example.com",
  "ccms.uda.api.key=test-ccms-uda-api-key",
        "notifications.govNotifyApiKey=test-api-key",
        "notifications.portalUrl=http://localhost:8080",
        "notifications.addNewUserEmailTemplate=test-template-1",
        "notifications.delegateFirmAccessEmailTemplate=test-template-2",
        "notifications.revokeFirmAccessEmailTemplate=test-template-3",
        "app.enable.tech.services.call=false"
})
class DataProviderServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("test_db")
        .withUsername("postgres")
        .withPassword("password");

    @Autowired
    private DataProviderService dataProviderService;

    @Autowired
    private FirmRepository firmRepository;

    @Autowired
    private OfficeRepository officeRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private EntraUserRepository entraUserRepository;

    @MockBean
    private DataProviderConfig dataProviderConfig;

    @MockBean(name = "dataProviderRestClient")
    private RestClient restClient;

    @Autowired
    private ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Clean database
        userProfileRepository.deleteAll();
        entraUserRepository.deleteAll();
      List<Firm> firms = firmRepository.findAll();
      firms.forEach(firm -> firm.setEnabled(false));
      firmRepository.saveAll(firms);
        officeRepository.deleteAll();
        firmRepository.deleteAll();
    }

    @Test
    void compareWithDatabase_shouldDetectNewFirm() throws Exception {
        // Given - PDA has a firm that doesn't exist in DB
        String pdaJson = """
            {
              "offices": [
                {
                  "firmNumber": "F001",
                  "firmName": "New Test Firm",
                  "firmType": "LEGAL_SERVICES_PROVIDER",
                  "parentFirmNumber": null,
                  "officeAccountNumber": "O001",
                  "officeAddressLine1": "123 Main St",
                  "officeAddressLine2": null,
                  "officeAddressLine3": null,
                  "officeAddressCity": "London",
                  "officeAddressPostcode": "SW1A 1AA"
                }
              ]
            }
            """;
        Path jsonFile = tempDir.resolve("pda-data.json");
        Files.writeString(jsonFile, pdaJson);

        when(dataProviderConfig.isUseLocalFile()).thenReturn(true);
        when(dataProviderConfig.getLocalFilePath()).thenReturn(jsonFile.toString());

        // When
        ComparisonResultDto result = dataProviderService.compareWithDatabase();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFirmCreates()).isEqualTo(1);
        assertThat(result.getOfficeCreates()).isEqualTo(1);
        assertThat(result.getCreated()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(result.getCreated()).anyMatch(i -> i.getType().equals("firm") && i.getCode().equals("F001"));
    }

    @Test
    void compareWithDatabase_shouldDetectFirmUpdate() throws Exception {
        // Given - firm exists but name changed
        Firm existingFirm = Firm.builder()
            .code("F001")
            .name("Old Firm Name")
            .type(FirmType.LEGAL_SERVICES_PROVIDER)
          .enabled(false)
            .build();
        existingFirm = firmRepository.save(existingFirm);

        Office existingOffice = Office.builder()
            .code("O001")
            .firm(existingFirm)
            .address(Office.Address.builder()
                .addressLine1("123 Main St")
                .city("London")
                .postcode("SW1A 1AA")
                .build())
            .build();
        officeRepository.save(existingOffice);
          existingFirm.setEnabled(true);
          existingFirm = firmRepository.save(existingFirm);

        String pdaJson = """
            {
              "offices": [
                {
                  "firmNumber": "F001",
                  "firmName": "Updated Firm Name",
                  "firmType": "LEGAL_SERVICES_PROVIDER",
                  "parentFirmNumber": null,
                  "officeAccountNumber": "O001",
                  "officeAddressLine1": "123 Main St",
                  "officeAddressLine2": null,
                  "officeAddressLine3": null,
                  "officeAddressCity": "London",
                  "officeAddressPostcode": "SW1A 1AA"
                }
              ]
            }
            """;
        Path jsonFile = tempDir.resolve("pda-data.json");
        Files.writeString(jsonFile, pdaJson);

        when(dataProviderConfig.isUseLocalFile()).thenReturn(true);
        when(dataProviderConfig.getLocalFilePath()).thenReturn(jsonFile.toString());

        // When
        ComparisonResultDto result = dataProviderService.compareWithDatabase();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFirmUpdates()).isEqualTo(1);
        assertThat(result.getUpdated()).anyMatch(i -> i.getType().equals("firm") && i.getCode().equals("F001"));
    }

    @Test
    void compareWithDatabase_shouldDetectDeletedFirm() throws Exception {
        // Given - firm exists in DB but not in PDA
        Firm existingFirm = Firm.builder()
            .code("F001")
            .name("To Be Deleted")
            .type(FirmType.LEGAL_SERVICES_PROVIDER)
          .enabled(false)
            .build();
        existingFirm = firmRepository.save(existingFirm);

        Office existingOffice = Office.builder()
          .code("O001")
          .firm(existingFirm)
          .address(Office.Address.builder()
            .addressLine1("123 Main St")
            .city("London")
            .postcode("SW1A 1AA")
            .build())
          .build();
        officeRepository.save(existingOffice);
        existingFirm.setEnabled(true);
        firmRepository.save(existingFirm);

        String pdaJson = """
            {
              "offices": []
            }
            """;
        Path jsonFile = tempDir.resolve("pda-data.json");
        Files.writeString(jsonFile, pdaJson);

        when(dataProviderConfig.isUseLocalFile()).thenReturn(true);
        when(dataProviderConfig.getLocalFilePath()).thenReturn(jsonFile.toString());

        // When
        ComparisonResultDto result = dataProviderService.compareWithDatabase();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFirmDisables()).isEqualTo(1);
        assertThat(result.getDeleted()).anyMatch(i -> i.getType().equals("firm") && i.getCode().equals("F001"));
    }

    @Test
    void compareWithDatabase_shouldDetectOfficeAddressUpdate() throws Exception {
        // Given - office exists but address changed
        Firm existingFirm = Firm.builder()
            .code("F001")
            .name("Test Firm")
            .type(FirmType.LEGAL_SERVICES_PROVIDER)
          .enabled(false)
            .build();
        existingFirm = firmRepository.save(existingFirm);

        Office existingOffice = Office.builder()
            .code("O001")
            .firm(existingFirm)
            .address(Office.Address.builder()
                .addressLine1("123 Old St")
                .city("London")
                .postcode("SW1A 1AA")
                .build())
            .build();
        officeRepository.save(existingOffice);
          existingFirm.setEnabled(true);
          existingFirm = firmRepository.save(existingFirm);

        String pdaJson = """
            {
              "offices": [
                {
                  "firmNumber": "F001",
                  "firmName": "Test Firm",
                  "firmType": "LEGAL_SERVICES_PROVIDER",
                  "parentFirmNumber": null,
                  "officeAccountNumber": "O001",
                  "officeAddressLine1": "456 New St",
                  "officeAddressLine2": null,
                  "officeAddressLine3": null,
                  "officeAddressCity": "Manchester",
                  "officeAddressPostcode": "M1 1AA"
                }
              ]
            }
            """;
        Path jsonFile = tempDir.resolve("pda-data.json");
        Files.writeString(jsonFile, pdaJson);

        when(dataProviderConfig.isUseLocalFile()).thenReturn(true);
        when(dataProviderConfig.getLocalFilePath()).thenReturn(jsonFile.toString());

        // When
        ComparisonResultDto result = dataProviderService.compareWithDatabase();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOfficeUpdates()).isEqualTo(1);
        assertThat(result.getUpdated()).anyMatch(i -> i.getType().equals("office") && i.getCode().equals("O001"));
    }

    @Test
    void compareWithDatabase_shouldDetectDeletedOffice() throws Exception {
        // Given - office exists in DB but not in PDA
        Firm existingFirm = Firm.builder()
            .code("F001")
            .name("Test Firm")
            .type(FirmType.LEGAL_SERVICES_PROVIDER)
          .enabled(false)
            .build();
        existingFirm = firmRepository.save(existingFirm);

        Office office1 = Office.builder()
            .code("O001")
            .firm(existingFirm)
            .address(Office.Address.builder().addressLine1("To be deleted").build())
            .build();
        officeRepository.save(office1);

        Office office2 = Office.builder()
            .code("O002")
            .firm(existingFirm)
            .address(Office.Address.builder()
                .addressLine1("123 Main St")
                .city("London")
                .postcode("SW1A 1AA")
                .build())
            .build();
        officeRepository.save(office2);
          existingFirm.setEnabled(true);
          existingFirm = firmRepository.save(existingFirm);

        String pdaJson = """
            {
              "offices": [
                {
                  "firmNumber": "F001",
                  "firmName": "Test Firm",
                  "firmType": "LEGAL_SERVICES_PROVIDER",
                  "parentFirmNumber": null,
                  "officeAccountNumber": "O002",
                  "officeAddressLine1": "123 Main St",
                  "officeAddressLine2": null,
                  "officeAddressLine3": null,
                  "officeAddressCity": "London",
                  "officeAddressPostcode": "SW1A 1AA"
                }
              ]
            }
            """;
        Path jsonFile = tempDir.resolve("pda-data.json");
        Files.writeString(jsonFile, pdaJson);

        when(dataProviderConfig.isUseLocalFile()).thenReturn(true);
        when(dataProviderConfig.getLocalFilePath()).thenReturn(jsonFile.toString());

        // When
        ComparisonResultDto result = dataProviderService.compareWithDatabase();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOfficeDeletes()).isEqualTo(1);
        assertThat(result.getDeleted()).anyMatch(i -> i.getType().equals("office") && i.getCode().equals("O001"));
    }

    @Test
    void synchronizeWithPdaAsync_shouldCreateNewFirmAndOffice() throws Exception {
        // Given - PDA has new firm with office
        String pdaJson = """
            {
              "offices": [
                {
                  "firmNumber": "F001",
                  "firmName": "New Firm",
                  "firmType": "LEGAL_SERVICES_PROVIDER",
                  "parentFirmNumber": null,
                  "officeAccountNumber": "O001",
                  "officeAddressLine1": "123 Main St",
                  "officeAddressLine2": null,
                  "officeAddressLine3": null,
                  "officeAddressCity": "London",
                  "officeAddressPostcode": "SW1A 1AA"
                }
              ]
            }
            """;
        Path jsonFile = tempDir.resolve("pda-data.json");
        Files.writeString(jsonFile, pdaJson);

        when(dataProviderConfig.isUseLocalFile()).thenReturn(true);
        when(dataProviderConfig.getLocalFilePath()).thenReturn(jsonFile.toString());

        // When
        CompletableFuture<PdaSyncResultDto> future = dataProviderService.synchronizeWithPdaAsync();
        PdaSyncResultDto result = future.join();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFirmsCreated()).isEqualTo(1);
        assertThat(result.getOfficesCreated()).isEqualTo(1);

        // Verify in database
        List<Firm> firms = firmRepository.findAll();
        assertThat(firms).hasSize(1);
        assertThat(firms.get(0).getCode()).isEqualTo("F001");
        assertThat(firms.get(0).getName()).isEqualTo("New Firm");

        List<Office> offices = officeRepository.findAll();
        assertThat(offices).hasSize(1);
        assertThat(offices.get(0).getCode()).isEqualTo("O001");
    }

    @Test
    void synchronizeWithPdaAsync_shouldUpdateExistingFirm() throws Exception {
        // Given - firm exists with different name
        Firm existingFirm = Firm.builder()
            .code("F001")
            .name("Old Firm Name")
            .type(FirmType.LEGAL_SERVICES_PROVIDER)
          .enabled(false)
            .build();
        existingFirm = firmRepository.save(existingFirm);

        Office existingOffice = Office.builder()
            .code("O001")
            .firm(existingFirm)
            .address(Office.Address.builder()
                .addressLine1("123 Main St")
                .city("London")
                .postcode("SW1A 1AA")
                .build())
            .build();
        officeRepository.save(existingOffice);
          existingFirm.setEnabled(true);
          existingFirm = firmRepository.save(existingFirm);

        String pdaJson = """
            {
              "offices": [
                {
                  "firmNumber": "F001",
                  "firmName": "Updated Firm Name",
                  "firmType": "LEGAL_SERVICES_PROVIDER",
                  "parentFirmNumber": null,
                  "officeAccountNumber": "O001",
                  "officeAddressLine1": "123 Main St",
                  "officeAddressLine2": null,
                  "officeAddressLine3": null,
                  "officeAddressCity": "London",
                  "officeAddressPostcode": "SW1A 1AA"
                }
              ]
            }
            """;
        Path jsonFile = tempDir.resolve("pda-data.json");
        Files.writeString(jsonFile, pdaJson);

        when(dataProviderConfig.isUseLocalFile()).thenReturn(true);
        when(dataProviderConfig.getLocalFilePath()).thenReturn(jsonFile.toString());

        // When
        CompletableFuture<PdaSyncResultDto> future = dataProviderService.synchronizeWithPdaAsync();
        PdaSyncResultDto result = future.join();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFirmsUpdated()).isEqualTo(1);

        // Verify in database
        Firm updatedFirm = firmRepository.findById(existingFirm.getId()).orElseThrow();
        assertThat(updatedFirm.getName()).isEqualTo("Updated Firm Name");
    }

    @Test
    void synchronizeWithPdaAsync_shouldDeactivateFirmNotInPda() throws Exception {
        // Given - firm exists in DB but not in PDA
        Firm existingFirm = Firm.builder()
            .code("F001")
            .name("To Be Disabled")
            .type(FirmType.LEGAL_SERVICES_PROVIDER)
          .enabled(false)
            .build();
        existingFirm = firmRepository.save(existingFirm);

        Office existingOffice = Office.builder()
          .code("O001")
          .firm(existingFirm)
          .address(Office.Address.builder()
            .addressLine1("123 Main St")
            .city("London")
            .postcode("SW1A 1AA")
            .build())
          .build();
        officeRepository.save(existingOffice);
        existingFirm.setEnabled(true);
        existingFirm = firmRepository.save(existingFirm);

        String pdaJson = """
            {
              "offices": []
            }
            """;
        Path jsonFile = tempDir.resolve("pda-data.json");
        Files.writeString(jsonFile, pdaJson);

        when(dataProviderConfig.isUseLocalFile()).thenReturn(true);
        when(dataProviderConfig.getLocalFilePath()).thenReturn(jsonFile.toString());

        // When
        CompletableFuture<PdaSyncResultDto> future = dataProviderService.synchronizeWithPdaAsync();
        PdaSyncResultDto result = future.join();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFirmsDisabled()).isEqualTo(1);

        // Verify in database
        Firm disabledFirm = firmRepository.findById(existingFirm.getId()).orElseThrow();
        assertThat(disabledFirm.getEnabled()).isFalse();
    }

    @Test
    void synchronizeWithPdaAsync_shouldDeleteOfficeNotInPda() throws Exception {
        // Given - office exists in DB but not in PDA
        Firm existingFirm = Firm.builder()
            .code("F001")
            .name("Test Firm")
            .type(FirmType.LEGAL_SERVICES_PROVIDER)
          .enabled(false)
            .build();
        existingFirm = firmRepository.save(existingFirm);

        Office toDelete = Office.builder()
            .code("O001")
            .firm(existingFirm)
            .address(Office.Address.builder().addressLine1("To be deleted").build())
            .build();
        UUID officeToDeleteId = officeRepository.save(toDelete).getId();

        Office toKeep = Office.builder()
            .code("O002")
            .firm(existingFirm)
            .address(Office.Address.builder()
                .addressLine1("123 Main St")
                .city("London")
                .postcode("SW1A 1AA")
                .build())
            .build();
        officeRepository.save(toKeep);
          existingFirm.setEnabled(true);
          existingFirm = firmRepository.save(existingFirm);

        String pdaJson = """
            {
              "offices": [
                {
                  "firmNumber": "F001",
                  "firmName": "Test Firm",
                  "firmType": "LEGAL_SERVICES_PROVIDER",
                  "parentFirmNumber": null,
                  "officeAccountNumber": "O002",
                  "officeAddressLine1": "123 Main St",
                  "officeAddressLine2": null,
                  "officeAddressLine3": null,
                  "officeAddressCity": "London",
                  "officeAddressPostcode": "SW1A 1AA"
                }
              ]
            }
            """;
        Path jsonFile = tempDir.resolve("pda-data.json");
        Files.writeString(jsonFile, pdaJson);

        when(dataProviderConfig.isUseLocalFile()).thenReturn(true);
        when(dataProviderConfig.getLocalFilePath()).thenReturn(jsonFile.toString());

        // When
        CompletableFuture<PdaSyncResultDto> future = dataProviderService.synchronizeWithPdaAsync();
        PdaSyncResultDto result = future.join();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOfficesDeleted()).isEqualTo(1);

        // Verify in database
        assertThat(officeRepository.findById(officeToDeleteId)).isEmpty();
        assertThat(officeRepository.findAll()).hasSize(1);
    }

    @Test
    void synchronizeWithPdaAsync_shouldSetParentFirmReferences() throws Exception {
        // Given - firm with parent firm reference
        String pdaJson = """
            {
              "offices": [
                {
                  "firmNumber": "F001",
                  "firmName": "Child Firm",
                  "firmType": "ADVOCATE",
                  "parentFirmNumber": "F002",
                  "officeAccountNumber": "O001",
                  "officeAddressLine1": "123 Main St",
                  "officeAddressLine2": null,
                  "officeAddressLine3": null,
                  "officeAddressCity": "London",
                  "officeAddressPostcode": "SW1A 1AA"
                },
                {
                  "firmNumber": "F002",
                  "firmName": "Parent Firm",
                  "firmType": "LEGAL_SERVICES_PROVIDER",
                  "parentFirmNumber": null,
                  "officeAccountNumber": "O002",
                  "officeAddressLine1": "456 Parent St",
                  "officeAddressLine2": null,
                  "officeAddressLine3": null,
                  "officeAddressCity": "London",
                  "officeAddressPostcode": "SW1A 2BB"
                }
              ]
            }
            """;
        Path jsonFile = tempDir.resolve("pda-data.json");
        Files.writeString(jsonFile, pdaJson);

        when(dataProviderConfig.isUseLocalFile()).thenReturn(true);
        when(dataProviderConfig.getLocalFilePath()).thenReturn(jsonFile.toString());

        // When
        CompletableFuture<PdaSyncResultDto> future = dataProviderService.synchronizeWithPdaAsync();
        PdaSyncResultDto result = future.join();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFirmsCreated()).isEqualTo(2);

        // Verify parent-child relationship
        Firm childFirm = firmRepository.findByCode("F001");
        Firm parentFirm = firmRepository.findByCode("F002");
        assertThat(childFirm.getParentFirm()).isNotNull();
        assertThat(childFirm.getParentFirm().getId()).isEqualTo(parentFirm.getId());
    }

    @Test
    void synchronizeWithPdaAsync_shouldRemoveUserOfficeAssociationsOnOfficeDelete() throws Exception {
        // Given - office with user association exists in DB but not in PDA
        Firm existingFirm = Firm.builder()
            .code("F001")
            .name("Test Firm")
            .type(FirmType.LEGAL_SERVICES_PROVIDER)
          .enabled(false)
            .build();
        existingFirm = firmRepository.save(existingFirm);

        Office officeToDelete = Office.builder()
            .code("O001")
            .firm(existingFirm)
            .address(Office.Address.builder().addressLine1("To be deleted").build())
            .build();
        officeToDelete = officeRepository.save(officeToDelete);

        Office officeToKeep = Office.builder()
            .code("O002")
            .firm(existingFirm)
            .address(Office.Address.builder()
                .addressLine1("123 Main St")
                .city("London")
                .postcode("SW1A 1AA")
                .build())
            .build();
        officeToKeep = officeRepository.save(officeToKeep);
        existingFirm.setEnabled(true);
        existingFirm = firmRepository.save(existingFirm);

        // Create user with office association
        EntraUser entraUser = EntraUser.builder()
            .email("test@example.com")
            .entraOid(UUID.randomUUID().toString())
            .firstName("Test")
            .lastName("User")
            .userStatus(UserStatus.ACTIVE)
            .createdDate(LocalDateTime.now())
            .createdBy("Test")
            .build();
        entraUser = entraUserRepository.save(entraUser);

        UserProfile userProfile = UserProfile.builder()
            .entraUser(entraUser)
            .firm(existingFirm)
            .offices(new HashSet<>(Arrays.asList(officeToDelete, officeToKeep)))
            .userType(UserType.EXTERNAL)
            .userProfileStatus(UserProfileStatus.COMPLETE)
            .activeProfile(true)
            .unrestrictedOfficeAccess(false)
            .lastCcmsSyncSuccessful(true)
            .build();
        userProfile = userProfileRepository.save(userProfile);

        String pdaJson = """
            {
              "offices": [
                {
                  "firmNumber": "F001",
                  "firmName": "Test Firm",
                  "firmType": "LEGAL_SERVICES_PROVIDER",
                  "parentFirmNumber": null,
                  "officeAccountNumber": "O002",
                  "officeAddressLine1": "123 Main St",
                  "officeAddressLine2": null,
                  "officeAddressLine3": null,
                  "officeAddressCity": "London",
                  "officeAddressPostcode": "SW1A 1AA"
                }
              ]
            }
            """;
        Path jsonFile = tempDir.resolve("pda-data.json");
        Files.writeString(jsonFile, pdaJson);

        when(dataProviderConfig.isUseLocalFile()).thenReturn(true);
        when(dataProviderConfig.getLocalFilePath()).thenReturn(jsonFile.toString());

        // When
        CompletableFuture<PdaSyncResultDto> future = dataProviderService.synchronizeWithPdaAsync();
        PdaSyncResultDto result = future.join();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOfficesDeleted()).isEqualTo(1);

        // Verify office association removed from user
        UserProfile updatedProfile = userProfileRepository.findById(userProfile.getId()).orElseThrow();

        // Query offices directly from database to avoid lazy loading issues
        long officeCount = entityManager.createQuery(
            "SELECT COUNT(o) FROM Office o JOIN o.userProfiles up WHERE up.id = :profileId", Long.class)
            .setParameter("profileId", updatedProfile.getId())
            .getSingleResult();

        assertThat(officeCount).isEqualTo(1);

        // Verify the deleted office is not associated
        List<String> officeCodes = entityManager.createQuery(
            "SELECT o.code FROM Office o JOIN o.userProfiles up WHERE up.id = :profileId", String.class)
            .setParameter("profileId", updatedProfile.getId())
            .getResultList();

        assertThat(officeCodes).doesNotContain("O001");
    }
}
