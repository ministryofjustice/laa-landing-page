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
     * Compares PDA data with local database firms and offices.
     * Adds comparison columns to indicate matches.
     *
     * @return TableSaw Table with additional columns showing database match status
     */
    public Table compareWithDatabase() {
        log.info("Comparing PDA data with local database");

        Table pdaTable = getProviderOfficesSnapshot();

        // Get all firms and offices from database
        List<Firm> allFirms = firmRepository.findAll();
        List<Office> allOffices = officeRepository.findAll();

        // Build lookup maps for O(1) performance instead of O(n) streams
        Map<String, Firm> firmsByCode = allFirms.stream()
            .filter(f -> f.getCode() != null)
            .collect(Collectors.toMap(Firm::getCode, f -> f, (f1, f2) -> f1));

        Map<String, Office> officesByCode = allOffices.stream()
            .filter(o -> o.getCode() != null)
            .collect(Collectors.toMap(Office::getCode, o -> o, (o1, o2) -> o1));

        log.info("Built lookup maps: {} firms, {} offices", firmsByCode.size(), officesByCode.size());

        // Create new columns for match indicators
        StringColumn firmMatchColumn = StringColumn.create("db_firm_match");
        StringColumn officeMatchColumn = StringColumn.create("db_office_match");
        StringColumn firmIdColumn = StringColumn.create("db_firm_id");
        StringColumn officeIdColumn = StringColumn.create("db_office_id");

        // For each row in PDA data, try to find matches in database
        for (int i = 0; i < pdaTable.rowCount(); i++) {
            String pdaFirmNumber = String.valueOf(pdaTable.column("firmNumber").get(i));
            String pdaOfficeAccountNo = String.valueOf(pdaTable.column("officeAccountNo").get(i));

            // Try to match firm by code (firmNumber in PDA maps to code in DB)
            Firm matchedFirm = firmsByCode.get(pdaFirmNumber);

            if (matchedFirm != null) {
                firmMatchColumn.append("MATCHED");
                firmIdColumn.append(matchedFirm.getId().toString());

                // Try to match office by code (officeAccountNo in PDA maps to code in DB)
                Office matchedOffice = officesByCode.get(pdaOfficeAccountNo);

                if (matchedOffice != null && matchedOffice.getFirm().getId().equals(matchedFirm.getId())) {
                    officeMatchColumn.append("MATCHED");
                    officeIdColumn.append(matchedOffice.getId().toString());
                } else {
                    officeMatchColumn.append("NOT_FOUND");
                    officeIdColumn.append("");
                }
            } else {
                firmMatchColumn.append("NOT_FOUND");
                officeMatchColumn.append("N/A");
                firmIdColumn.append("");
                officeIdColumn.append("");
            }
        }

        // Add the comparison columns to the table
        pdaTable.addColumns(firmMatchColumn, officeMatchColumn, firmIdColumn, officeIdColumn);

        log.info("Comparison complete. {} total rows, {} firms matched, {} offices matched",
            pdaTable.rowCount(),
            firmMatchColumn.countOccurrences("MATCHED"),
            officeMatchColumn.countOccurrences("MATCHED"));

        return pdaTable;
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
    public PdaSyncResultDto synchronizeWithPda() {
        log.info("=== Starting PDA synchronization ===");
        logMemoryUsage("Start");
        PdaSyncResultDto result = PdaSyncResultDto.builder().build();

        try {
            log.info("Fetching PDA data snapshot...");
            Table pdaTable = getProviderOfficesSnapshot();
            log.info("Retrieved {} rows from PDA", pdaTable.rowCount());
            logMemoryUsage("After fetching PDA data");

            // Build maps of PDA data
            log.info("Building PDA data maps...");
            Map<String, PdaFirmData> pdaFirms = buildPdaFirmsMap(pdaTable);
            Map<String, PdaOfficeData> pdaOffices = buildPdaOfficesMap(pdaTable);
            log.info("Found {} unique firms and {} offices in PDA data", pdaFirms.size(), pdaOffices.size());

            // Perform data integrity checks (matching Python script logic)
            log.info("=== Data Integrity Checks ===");
            checkDataIntegrity(pdaFirms, pdaOffices, result);
            log.info("After integrity checks: {} firms, {} offices", pdaFirms.size(), pdaOffices.size());
            logMemoryUsage("After building PDA maps");

            // Get current database state
            log.info("Loading firms from database...");
            Map<String, Firm> dbFirms = new HashMap<>();
            firmRepository.findAll().forEach(f -> {
                if (f.getCode() != null) {
                    dbFirms.put(f.getCode(), f);
                }
            });
            log.info("Found {} firms in database", dbFirms.size());

            // Track processed codes
            Set<String> processedFirmCodes = new HashSet<>();

            // Process firms first
            log.info("=== Processing Firms ===");
            for (Map.Entry<String, PdaFirmData> entry : pdaFirms.entrySet()) {
                String firmCode = entry.getKey();
                PdaFirmData pdaFirm = entry.getValue();
                processedFirmCodes.add(firmCode);

                Firm dbFirm = dbFirms.get(firmCode);

                if (dbFirm == null) {
                    // New firm - create it
                    createFirm(pdaFirm, result);
                } else {
                    // Update existing firm
                    updateFirm(dbFirm, pdaFirm, result);
                }
            }

            // Deactivate firms not in PDA (skip for now - can implement deletion if needed)
            log.info("Checking for firms to deactivate...");
            for (String firmCode : dbFirms.keySet()) {
                if (!processedFirmCodes.contains(firmCode)) {
                    log.info("Firm {} not in PDA data (would deactivate/delete)", firmCode);
                    result.setFirmsDeactivated(result.getFirmsDeactivated() + 1);
                }
            }
            log.info("Firms processing complete: {} created, {} reactivated, {} updated, {} deactivated",
                result.getFirmsCreated(), result.getFirmsReactivated(), result.getFirmsUpdated(), result.getFirmsDeactivated());
            logMemoryUsage("After processing firms");

            // Reload firms after changes
            log.info("Reloading firms from database...");
            dbFirms.clear();
            firmRepository.findAll().forEach(f -> {
                if (f.getCode() != null) {
                    dbFirms.put(f.getCode(), f);
                }
            });

            // Get DB offices
            log.info("Loading offices from database...");
            Map<String, Office> dbOffices = new HashMap<>();
            officeRepository.findAll().forEach(o -> {
                if (o.getCode() != null) {
                    dbOffices.put(o.getCode(), o);
                }
            });
            log.info("Found {} offices in database", dbOffices.size());

            final Set<String> processedOfficeCodes = new HashSet<>();

            // Process offices
            log.info("=== Processing Offices ===");
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
                    // New office - create it
                    createOffice(pdaOffice, parentFirm, result);
                } else {
                    // Update existing office
                    updateOffice(dbOffice, pdaOffice, parentFirm, result);
                }
            }

            // Deactivate offices not in PDA (skip for now - can implement deletion if needed)
            log.info("Checking for offices to deactivate...");
            for (String officeCode : dbOffices.keySet()) {
                if (!processedOfficeCodes.contains(officeCode)) {
                    log.info("Office {} not in PDA data (would deactivate/delete)", officeCode);
                    result.setOfficesDeactivated(result.getOfficesDeactivated() + 1);
                }
            }
            log.info("Offices processing complete: {} created, {} reactivated, {} updated, {} deactivated",
                result.getOfficesCreated(), result.getOfficesReactivated(), result.getOfficesUpdated(), result.getOfficesDeactivated());
            logMemoryUsage("After processing offices");

            log.info("=== PDA synchronization complete ===");
            log.info("Final results: {}", result);
            logMemoryUsage("End");

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
        log.info("Initial PDA data integrity check ---------------");

        // Check for duplicate firm codes (should not happen if map is built correctly)
        Set<String> firmCodes = new HashSet<>();
        int duplicateFirms = 0;
        for (String code : pdaFirms.keySet()) {
            if (!firmCodes.add(code)) {
                duplicateFirms++;
            }
        }
        log.info("DUPLICATE FIRMS: {}", duplicateFirms);
        if (duplicateFirms > 0) {
            result.addWarning("Found " + duplicateFirms + " duplicate firm codes in PDA data");
        }

        // Check for duplicate office codes
        Set<String> officeCodes = new HashSet<>();
        int duplicateOffices = 0;
        for (String code : pdaOffices.keySet()) {
            if (!officeCodes.add(code)) {
                duplicateOffices++;
            }
        }
        log.info("DUPLICATE OFFICES: {}", duplicateOffices);
        if (duplicateOffices > 0) {
            result.addWarning("Found " + duplicateOffices + " duplicate office codes in PDA data");
        }

        // RULE 1: Find and remove orphan offices (offices with no relatable firm)
        Set<String> orphanOfficeCodes = new HashSet<>();
        for (Map.Entry<String, PdaOfficeData> entry : pdaOffices.entrySet()) {
            String officeCode = entry.getKey();
            PdaOfficeData office = entry.getValue();
            if (!pdaFirms.containsKey(office.getFirmNumber())) {
                orphanOfficeCodes.add(officeCode);
            }
        }

        if (!orphanOfficeCodes.isEmpty()) {
            log.warn("Found {} office(s) with no relatable firm. These will be removed", orphanOfficeCodes.size());
            result.addWarning("Removed " + orphanOfficeCodes.size() + " orphan offices (no relatable firm)");
            orphanOfficeCodes.forEach(pdaOffices::remove);
        } else {
            log.info("All offices have a valid, relatable firm.");
        }

        // RULE 2: Find and remove firms without offices
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
            log.warn("Found {} firm(s) with no offices. These will be removed.", firmsWithoutOffices.size());
            result.addWarning("Removed " + firmsWithoutOffices.size() + " firms with no offices");
            firmsWithoutOffices.forEach(pdaFirms::remove);
        } else {
            log.info("All firms have at least one office.");
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
