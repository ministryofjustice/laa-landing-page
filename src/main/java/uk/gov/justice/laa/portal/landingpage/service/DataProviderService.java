package uk.gov.justice.laa.portal.landingpage.service;

import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.json.JsonReadOptions;
import uk.gov.justice.laa.portal.landingpage.dto.ComparisonResultDto;
import uk.gov.justice.laa.portal.landingpage.dto.PdaSyncResultDto;
import uk.gov.justice.laa.portal.landingpage.dto.PdaFirmData;
import uk.gov.justice.laa.portal.landingpage.dto.PdaOfficeData;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;
import uk.gov.justice.laa.portal.landingpage.repository.OfficeRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;
import uk.gov.justice.laa.portal.landingpage.service.pda.command.CreateFirmCommand;
import uk.gov.justice.laa.portal.landingpage.service.pda.command.CreateOfficeCommand;
import uk.gov.justice.laa.portal.landingpage.service.pda.command.DeactivateFirmCommand;
import uk.gov.justice.laa.portal.landingpage.service.pda.command.DeactivateOfficeCommand;
import uk.gov.justice.laa.portal.landingpage.service.pda.command.UpdateFirmCommand;
import uk.gov.justice.laa.portal.landingpage.service.pda.command.UpdateOfficeCommand;

/**
 * Service for calling PDA (Provider Data API) endpoints and matching with local database.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataProviderService {

    @Qualifier("dataProviderRestClient")
    private final RestClient dataProviderRestClient;

    private final ObjectMapper objectMapper;
    private final FirmRepository firmRepository;
    private final OfficeRepository officeRepository;
    private final UserProfileRepository userProfileRepository;

    /**
     * Fetches provider offices snapshot from PDA and returns as TableSaw dataframe.
     *
     * @return TableSaw Table containing provider offices snapshot data
     */
    public Table getProviderOfficesSnapshot() {
        log.info("Fetching provider offices snapshot from PDA");

        try {
            String response = dataProviderRestClient.get()
                    .uri("/api/v1/provider-offices/snapshot")
                    .retrieve()
                    .body(String.class);

            log.info("Successfully fetched provider offices snapshot from PDA, converting to dataframe");

            // Parse JSON to extract the "offices" array
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode officesNode = rootNode.get("offices");

            if (officesNode == null || !officesNode.isArray()) {
                throw new IllegalStateException("Expected 'offices' array in response");
            }

            String officesJson = objectMapper.writeValueAsString(officesNode);

            // Parse JSON array into TableSaw Table
            Table table = Table.read().usingOptions(
                JsonReadOptions.builder(new StringReader(officesJson)).build()
            );

            log.info("Dataframe created with {} rows and {} columns",
                table.rowCount(), table.columnCount());

            return table;
        } catch (Exception e) {
            log.error("Error fetching or parsing provider offices snapshot from PDA: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch provider offices snapshot from PDA", e);
        }
    }

    /**
     * Returns structured comparison showing created, updated, deleted, and matched items.
     *
     * @return ComparisonResultDto with categorized items
     */
    public ComparisonResultDto compareWithDatabase() {
        log.info("Comparing PDA data with local database");

        Table pdaTable = getProviderOfficesSnapshot();

        // Get all firms and offices from database
        List<Firm> allFirms = firmRepository.findAll();
        List<Office> allOffices = officeRepository.findAll();

        // Build lookup maps for O(1) performance
        Map<String, Firm> firmsByCode = allFirms.stream()
            .filter(f -> f.getCode() != null)
            .collect(Collectors.toMap(Firm::getCode, f -> f, (f1, f2) -> f1));

        Map<String, Office> officesByCode = allOffices.stream()
            .filter(o -> o.getCode() != null)
            .collect(Collectors.toMap(Office::getCode, o -> o, (o1, o2) -> o1));

        log.info("Built lookup maps: {} firms, {} offices", firmsByCode.size(), officesByCode.size());

        // Build PDA data maps
        Map<String, PdaFirmData> pdaFirms = buildPdaFirmsMap(pdaTable);
        Map<String, PdaOfficeData> pdaOffices = buildPdaOfficesMap(pdaTable);

        // Track separate counts
        int firmsCreated = 0, firmsUpdated = 0, firmsDeleted = 0, firmsExists = 0;
        int officesCreated = 0, officesUpdated = 0, officesDeleted = 0, officesExists = 0;

        ComparisonResultDto result = ComparisonResultDto.builder().build();

        // Compare firms
        Set<String> processedFirmCodes = new HashSet<>();
        for (Map.Entry<String, PdaFirmData> entry : pdaFirms.entrySet()) {
            String firmCode = entry.getKey();
            PdaFirmData pdaFirm = entry.getValue();
            processedFirmCodes.add(firmCode);

            Firm dbFirm = firmsByCode.get(firmCode);

            if (dbFirm == null) {
                // New firm to be created
                result.getCreated().add(ComparisonResultDto.ItemInfo.builder()
                    .type("firm")
                    .code(firmCode)
                    .name(pdaFirm.getFirmName())
                    .build());
                firmsCreated++;
            } else {
                // Check if firm needs updating
                boolean needsUpdate = !pdaFirm.getFirmName().equals(dbFirm.getName());

                if (needsUpdate) {
                    result.getUpdated().add(ComparisonResultDto.ItemInfo.builder()
                        .type("firm")
                        .code(firmCode)
                        .name(pdaFirm.getFirmName())
                        .dbId(dbFirm.getId())
                        .build());
                    firmsUpdated++;
                } else {
                    result.getExists().add(ComparisonResultDto.ItemInfo.builder()
                        .type("firm")
                        .code(firmCode)
                        .name(pdaFirm.getFirmName())
                        .dbId(dbFirm.getId())
                        .build());
                    firmsExists++;
                }
            }
        }

        // Find deleted firms
        for (Map.Entry<String, Firm> entry : firmsByCode.entrySet()) {
            String firmCode = entry.getKey();
            if (!processedFirmCodes.contains(firmCode)) {
                Firm firm = entry.getValue();
                result.getDeleted().add(ComparisonResultDto.ItemInfo.builder()
                    .type("firm")
                    .code(firmCode)
                    .name(firm.getName())
                    .dbId(firm.getId())
                    .build());
                firmsDeleted++;
            }
        }

        // Compare offices
        Set<String> processedOfficeCodes = new HashSet<>();
        for (Map.Entry<String, PdaOfficeData> entry : pdaOffices.entrySet()) {
            String officeCode = entry.getKey();
            PdaOfficeData pdaOffice = entry.getValue();
            processedOfficeCodes.add(officeCode);

            Office dbOffice = officesByCode.get(officeCode);
            Firm parentFirm = firmsByCode.get(pdaOffice.getFirmNumber());

            if (parentFirm == null) {
                continue; // Skip orphan offices
            }

            String officeName = pdaOffice.getAddressLine1() != null ? pdaOffice.getAddressLine1() : officeCode;

            if (dbOffice == null) {
                // New office to be created
                result.getCreated().add(ComparisonResultDto.ItemInfo.builder()
                    .type("office")
                    .code(officeCode)
                    .name(officeName)
                    .build());
                officesCreated++;
            } else {
                // Check if office needs updating
                boolean needsUpdate = !dbOffice.getFirm().getId().equals(parentFirm.getId()) ||
                    !isSameAddress(dbOffice, pdaOffice);

                if (needsUpdate) {
                    result.getUpdated().add(ComparisonResultDto.ItemInfo.builder()
                        .type("office")
                        .code(officeCode)
                        .name(officeName)
                        .dbId(dbOffice.getId())
                        .build());
                    officesUpdated++;
                } else {
                    result.getExists().add(ComparisonResultDto.ItemInfo.builder()
                        .type("office")
                        .code(officeCode)
                        .name(officeName)
                        .dbId(dbOffice.getId())
                        .build());
                    officesExists++;
                }
            }
        }

        // Find deleted offices
        for (Map.Entry<String, Office> entry : officesByCode.entrySet()) {
            String officeCode = entry.getKey();
            if (!processedOfficeCodes.contains(officeCode)) {
                Office office = entry.getValue();
                String officeName = office.getAddress() != null && office.getAddress().getAddressLine1() != null
                    ? office.getAddress().getAddressLine1() : officeCode;
                result.getDeleted().add(ComparisonResultDto.ItemInfo.builder()
                    .type("office")
                    .code(officeCode)
                    .name(officeName)
                    .dbId(office.getId())
                    .build());
                officesDeleted++;
            }
        }

        // Set the separate counts in the result
        result.setFirmsCreated(firmsCreated);
        result.setFirmsUpdated(firmsUpdated);
        result.setFirmsDeleted(firmsDeleted);
        result.setFirmsExists(firmsExists);
        result.setOfficesCreated(officesCreated);
        result.setOfficesUpdated(officesUpdated);
        result.setOfficesDeleted(officesDeleted);
        result.setOfficesExists(officesExists);

        return result;
    }

    private boolean isSameAddress(Office office, PdaOfficeData pdaOffice) {
        if (office.getAddress() == null) return false;

        return equals(office.getAddress().getAddressLine1(), pdaOffice.getAddressLine1()) &&
               equals(office.getAddress().getAddressLine2(), pdaOffice.getAddressLine2()) &&
               equals(office.getAddress().getAddressLine3(), pdaOffice.getAddressLine3()) &&
               equals(office.getAddress().getCity(), pdaOffice.getCity()) &&
               equals(office.getAddress().getPostcode(), pdaOffice.getPostcode());
    }

    private boolean equals(String s1, String s2) {
        if (s1 == null) return s2 == null;
        return s1.equals(s2);
    }

    /**
     * Asynchronously synchronizes PDA data with local database.
     * Runs in a dedicated thread pool to isolate from main application.
     * All exceptions are caught and logged to prevent application failure.
     *
     * @return CompletableFuture containing PdaSyncResultDto with statistics and errors/warnings
     */
    @Async("pdaSyncExecutor")
    public CompletableFuture<PdaSyncResultDto> synchronizeWithPdaAsync() {
        log.info("Starting async PDA synchronization in thread: {}", Thread.currentThread().getName());

        try {
            PdaSyncResultDto result = synchronizeWithPda();
            log.info("Async PDA synchronization completed successfully");
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("Async PDA synchronization failed with exception", e);
            PdaSyncResultDto errorResult = PdaSyncResultDto.builder().build();
            errorResult.addError("Async synchronization failed: " + e.getMessage());
            return CompletableFuture.completedFuture(errorResult);
        }
    }

    /**
     * Synchronizes PDA data with local database according to the state machine logic.
     * Handles creation, updates, reactivation, and deactivation of firms and offices.
     *
     * @return PdaSyncResultDto containing statistics and any errors/warnings
     */
    @org.springframework.transaction.annotation.Transactional
    public PdaSyncResultDto synchronizeWithPda() {
        log.info("Starting PDA synchronization");
        PdaSyncResultDto result = PdaSyncResultDto.builder().build();

        try {
            // Fetch and build PDA data maps
            Table pdaTable = getProviderOfficesSnapshot();
            Map<String, PdaFirmData> pdaFirms = buildPdaFirmsMap(pdaTable);
            Map<String, PdaOfficeData> pdaOffices = buildPdaOfficesMap(pdaTable);

            // Perform data integrity checks
            checkDataIntegrity(pdaFirms, pdaOffices, result);

            // Get current database state
            Map<String, Firm> dbFirms = new HashMap<>();
            firmRepository.findAll().forEach(f -> {
                if (f.getCode() != null) {
                    dbFirms.put(f.getCode(), f);
                }
            });

            // Track processed codes
            Set<String> processedFirmCodes = new HashSet<>();

            // PASS 1: Process firms - create or update (without parent references for new firms)
            for (Map.Entry<String, PdaFirmData> entry : pdaFirms.entrySet()) {
                String firmCode = entry.getKey();
                PdaFirmData pdaFirm = entry.getValue();
                processedFirmCodes.add(firmCode);

                Firm dbFirm = dbFirms.get(firmCode);

                if (dbFirm == null) {
                    createFirm(pdaFirm, result);
                } else {
                    updateFirm(dbFirm, pdaFirm, result);
                }
            }

            // Check for firms to deactivate
            for (String firmCode : dbFirms.keySet()) {
                if (!processedFirmCodes.contains(firmCode)) {
                    deactivateFirm(dbFirms.get(firmCode), result);
                    result.setFirmsDeactivated(result.getFirmsDeactivated() + 1);
                }
            }

            // Reload firms after changes
            dbFirms.clear();
            firmRepository.findAll().forEach(f -> {
                if (f.getCode() != null) {
                    dbFirms.put(f.getCode(), f);
                }
            });

            // PASS 2: Update parent firm references for newly created firms
            log.info("Updating parent firm references...");
            for (Map.Entry<String, PdaFirmData> entry : pdaFirms.entrySet()) {
                String firmCode = entry.getKey();
                PdaFirmData pdaFirm = entry.getValue();

                if (pdaFirm.getParentFirmNumber() != null && !pdaFirm.getParentFirmNumber().isEmpty()) {
                    Firm firm = dbFirms.get(firmCode);
                    if (firm != null) {
                        String currentParentCode = firm.getParentFirm() != null ? firm.getParentFirm().getCode() : null;

                        // Only update if parent is missing or different
                        if (!pdaFirm.getParentFirmNumber().equals(currentParentCode)) {
                            Firm parentFirm = dbFirms.get(pdaFirm.getParentFirmNumber());
                            if (parentFirm != null) {
                                firm.setParentFirm(parentFirm);
                                firmRepository.save(firm);
                                log.info("Set parent for firm {}: {} -> {}", firmCode, currentParentCode, pdaFirm.getParentFirmNumber());
                            } else {
                                log.warn("Parent firm {} not found for firm {}", pdaFirm.getParentFirmNumber(), firmCode);
                                result.addWarning("Parent firm " + pdaFirm.getParentFirmNumber() + " not found for firm " + firmCode);
                            }
                        }
                    }
                }
            }

            // Get DB offices
            Map<String, Office> dbOffices = new HashMap<>();
            officeRepository.findAll().forEach(o -> {
                if (o.getCode() != null) {
                    dbOffices.put(o.getCode(), o);
                }
            });

            final Set<String> processedOfficeCodes = new HashSet<>();

            // Process offices
            for (Map.Entry<String, PdaOfficeData> entry : pdaOffices.entrySet()) {
                String officeCode = entry.getKey();
                PdaOfficeData pdaOffice = entry.getValue();
                processedOfficeCodes.add(officeCode);

                Office dbOffice = dbOffices.get(officeCode);
                Firm parentFirm = dbFirms.get(pdaOffice.getFirmNumber());

                if (parentFirm == null) {
                    result.addError("Cannot process office " + officeCode + ": firm " + pdaOffice.getFirmNumber() + " not found");
                    continue;
                }

                if (dbOffice == null) {
                    createOffice(pdaOffice, parentFirm, result);
                } else {
                    updateOffice(dbOffice, pdaOffice, parentFirm, result);
                }
            }

            // Check for offices to deactivate
            for (String officeCode : dbOffices.keySet()) {
                if (!processedOfficeCodes.contains(officeCode)) {
                    deactivateOffice(dbOffices.get(officeCode), result);
                    result.setOfficesDeactivated(result.getOfficesDeactivated() + 1);
                }
            }

            log.info("PDA sync complete - Firms: {} created, {} updated, {} deactivated | Offices: {} created, {} updated, {} deactivated",
                result.getFirmsCreated(), result.getFirmsUpdated(), result.getFirmsDeactivated(),
                result.getOfficesCreated(), result.getOfficesUpdated(), result.getOfficesDeactivated());

        } catch (Exception e) {
            log.error("Error during PDA synchronization: {}", e.getMessage(), e);
            result.addError("Synchronization failed: " + e.getMessage());
        }

        return result;
    }

    private Map<String, PdaFirmData> buildPdaFirmsMap(Table pdaTable) {
        Map<String, PdaFirmData> firms = new HashMap<>();
        for (int i = 0; i < pdaTable.rowCount(); i++) {
            String firmNumber = getStringValue(pdaTable, "firmNumber", i);
            if (!firms.containsKey(firmNumber)) {
                PdaFirmData firmData = PdaFirmData.builder()
                    .firmNumber(firmNumber)
                    .firmName(getStringValue(pdaTable, "firmName", i))
                    .firmType(getStringValue(pdaTable, "firmType", i))
                    .parentFirmNumber(getStringValue(pdaTable, "parentFirmNumber", i))
                    .build();
                firms.put(firmNumber, firmData);
            }
        }
        return firms;
    }

    private String getStringValue(Table table, String columnName, int rowIndex) {
        try {
            return table.stringColumn(columnName).get(rowIndex);
        } catch (ClassCastException e) {
            // Column might be an integer, convert it
            return String.valueOf(table.column(columnName).get(rowIndex));
        }
    }

    private Map<String, PdaOfficeData> buildPdaOfficesMap(Table pdaTable) {
        Map<String, PdaOfficeData> offices = new HashMap<>();
        for (int i = 0; i < pdaTable.rowCount(); i++) {
            PdaOfficeData officeData = PdaOfficeData.builder()
                .officeAccountNo(getStringValue(pdaTable, "officeAccountNo", i))
                .firmNumber(getStringValue(pdaTable, "firmNumber", i))
                .addressLine1(getStringValue(pdaTable, "officeAddressLine1", i))
                .addressLine2(getStringValue(pdaTable, "officeAddressLine2", i))
                .addressLine3(getStringValue(pdaTable, "officeAddressLine3", i))
                .city(getStringValue(pdaTable, "officeAddressCity", i))
                .postcode(getStringValue(pdaTable, "officeAddressPostcode", i))
                .build();
            offices.put(officeData.getOfficeAccountNo(), officeData);
        }
        return offices;
    }

    private void createFirm(PdaFirmData pdaFirm, PdaSyncResultDto result) {
        new CreateFirmCommand(firmRepository, pdaFirm).execute(result);
    }

    private void updateFirm(Firm firm, PdaFirmData pdaFirm, PdaSyncResultDto result) {
        new UpdateFirmCommand(firmRepository, firm, pdaFirm).execute(result);
    }

    private void deactivateFirm(Firm firm, PdaSyncResultDto result) {
        new DeactivateFirmCommand(firmRepository, firm).execute(result);
    }

    private void createOffice(PdaOfficeData pdaOffice, Firm firm, PdaSyncResultDto result) {
        new CreateOfficeCommand(officeRepository, pdaOffice, firm).execute(result);
    }

    private void updateOffice(Office office, PdaOfficeData pdaOffice, Firm firm, PdaSyncResultDto result) {
        new UpdateOfficeCommand(officeRepository, userProfileRepository, office, pdaOffice, firm).execute(result);
    }

    private void deactivateOffice(Office office, PdaSyncResultDto result) {
        new DeactivateOfficeCommand(officeRepository, userProfileRepository, office).execute(result);
    }

    /**
     * Validates data integrity of PDA data before processing.
     * Implements Python script rules:
     * 1. Check for duplicate firms/offices
     * 2. Remove orphan offices (offices with no relatable firm)
     * 3. Remove firms without offices
     * This ensures clean data before synchronization.
     */
    private void checkDataIntegrity(Map<String, PdaFirmData> pdaFirms,
                                     Map<String, PdaOfficeData> pdaOffices,
                                     PdaSyncResultDto result) {
        int initialFirms = pdaFirms.size();
        int initialOffices = pdaOffices.size();

        // Check for duplicate codes
        Set<String> firmCodes = new HashSet<>();
        int duplicateFirms = 0;
        for (String code : pdaFirms.keySet()) {
            if (!firmCodes.add(code)) {
                duplicateFirms++;
            }
        }
        if (duplicateFirms > 0) {
            result.addWarning("Found " + duplicateFirms + " duplicate firm codes");
        }

        Set<String> officeCodes = new HashSet<>();
        int duplicateOffices = 0;
        for (String code : pdaOffices.keySet()) {
            if (!officeCodes.add(code)) {
                duplicateOffices++;
            }
        }
        if (duplicateOffices > 0) {
            result.addWarning("Found " + duplicateOffices + " duplicate office codes");
        }

        // Remove orphan offices (offices with no relatable firm)
        Set<String> orphanOfficeCodes = new HashSet<>();
        for (Map.Entry<String, PdaOfficeData> entry : pdaOffices.entrySet()) {
            String officeCode = entry.getKey();
            PdaOfficeData office = entry.getValue();
            if (!pdaFirms.containsKey(office.getFirmNumber())) {
                orphanOfficeCodes.add(officeCode);
            }
        }

        if (!orphanOfficeCodes.isEmpty()) {
            result.addWarning("Removed " + orphanOfficeCodes.size() + " orphan offices");
            orphanOfficeCodes.forEach(pdaOffices::remove);
        }

        // Remove firms without offices
        Set<String> firmCodesWithOffices = new HashSet<>();
        for (PdaOfficeData office : pdaOffices.values()) {
            firmCodesWithOffices.add(office.getFirmNumber());
        }

        Set<String> firmsWithoutOffices = new HashSet<>();
        for (String firmCode : pdaFirms.keySet()) {
            if (!firmCodesWithOffices.contains(firmCode)) {
                firmsWithoutOffices.add(firmCode);
            }
        }

        if (!firmsWithoutOffices.isEmpty()) {
            result.addWarning("Removed " + firmsWithoutOffices.size() + " firms without offices");
            firmsWithoutOffices.forEach(pdaFirms::remove);
        }

        int removedFirms = initialFirms - pdaFirms.size();
        int removedOffices = initialOffices - pdaOffices.size();

        if (removedFirms > 0 || removedOffices > 0) {
            log.info("Data integrity: removed {} firms, {} offices", removedFirms, removedOffices);
        }
    }

    private void logMemoryUsage(String stage) {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();

        log.info("Memory [{}]: Used={} MB, Free={} MB, Total={} MB, Max={} MB",
            stage,
            usedMemory / (1024 * 1024),
            freeMemory / (1024 * 1024),
            totalMemory / (1024 * 1024),
            maxMemory / (1024 * 1024));
    }
}
