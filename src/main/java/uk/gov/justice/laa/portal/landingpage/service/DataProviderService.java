package uk.gov.justice.laa.portal.landingpage.service;

import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

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
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;
import uk.gov.justice.laa.portal.landingpage.repository.OfficeRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;

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

        // Create new columns for match indicators
        StringColumn firmMatchColumn = StringColumn.create("db_firm_match");
        StringColumn officeMatchColumn = StringColumn.create("db_office_match");
        StringColumn firmIdColumn = StringColumn.create("db_firm_id");
        StringColumn officeIdColumn = StringColumn.create("db_office_id");

        // For each row in PDA data, try to find matches in database
        for (int i = 0; i < pdaTable.rowCount(); i++) {
            String pdaFirmNumber = pdaTable.stringColumn("firmNumber").get(i);
            String pdaOfficeAccountNo = pdaTable.stringColumn("officeAccountNo").get(i);

            // Try to match firm by code (firmNumber in PDA maps to code in DB)
            Firm matchedFirm = allFirms.stream()
                .filter(f -> f.getCode() != null && f.getCode().equals(pdaFirmNumber))
                .findFirst()
                .orElse(null);

            if (matchedFirm != null) {
                firmMatchColumn.append("MATCHED");
                firmIdColumn.append(matchedFirm.getId().toString());

                // Try to match office by code (officeAccountNo in PDA maps to code in DB)
                Office matchedOffice = allOffices.stream()
                    .filter(o -> o.getFirm().getId().equals(matchedFirm.getId())
                              && o.getCode() != null
                              && o.getCode().equals(pdaOfficeAccountNo))
                    .findFirst()
                    .orElse(null);

                if (matchedOffice != null) {
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
            final Map<String, PdaFirmData> pdaFirms = buildPdaFirmsMap(pdaTable);
            log.info("Found {} unique firms in PDA data", pdaFirms.size());
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

            // Build PDA offices map and get DB offices
            log.info("Building PDA offices map...");
            final Map<String, PdaOfficeData> pdaOffices = buildPdaOfficesMap(pdaTable);
            log.info("Found {} offices in PDA data", pdaOffices.size());

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
                Firm parentFirm = dbFirms.get(pdaOffice.firmNumber);

                if (parentFirm == null) {
                    result.addError("Cannot process office " + officeCode + ": firm " + pdaOffice.firmNumber + " not found");
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
                PdaFirmData firmData = new PdaFirmData();
                firmData.firmNumber = firmNumber;
                firmData.firmName = getStringValue(pdaTable, "firmName", i);
                firmData.firmType = getStringValue(pdaTable, "firmType", i);
                firmData.parentFirmNumber = getStringValue(pdaTable, "parentFirmNumber", i);
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
            PdaOfficeData officeData = new PdaOfficeData();
            officeData.officeAccountNo = getStringValue(pdaTable, "officeAccountNo", i);
            officeData.firmNumber = getStringValue(pdaTable, "firmNumber", i);
            officeData.addressLine1 = getStringValue(pdaTable, "officeAddressLine1", i);
            officeData.addressLine2 = getStringValue(pdaTable, "officeAddressLine2", i);
            officeData.addressLine3 = getStringValue(pdaTable, "officeAddressLine3", i);
            officeData.city = getStringValue(pdaTable, "officeAddressCity", i);
            officeData.postcode = getStringValue(pdaTable, "officeAddressPostcode", i);
            offices.put(officeData.officeAccountNo, officeData);
        }
        return offices;
    }

    private void createFirm(PdaFirmData pdaFirm, PdaSyncResultDto result) {
        try {
            Firm firm = Firm.builder()
                .code(pdaFirm.firmNumber)
                .name(pdaFirm.firmName)
                .type(FirmType.valueOf(pdaFirm.firmType.toUpperCase().replace(" ", "_")))
                .build();

            // Handle parent firm if exists
            if (pdaFirm.parentFirmNumber != null && !pdaFirm.parentFirmNumber.isEmpty()) {
                Firm parentFirm = firmRepository.findByCode(pdaFirm.parentFirmNumber);
                if (parentFirm != null) {
                    firm.setParentFirm(parentFirm);
                }
            }

            // firmRepository.save(firm);  // COMMENTED OUT FOR TESTING
            result.setFirmsCreated(result.getFirmsCreated() + 1);
            log.info("Would create firm: {} (name: {}, type: {})", pdaFirm.firmNumber, pdaFirm.firmName, pdaFirm.firmType);
        } catch (Exception e) {
            log.error("Failed to create firm {}: {}", pdaFirm.firmNumber, e.getMessage());
            result.addError("Failed to create firm " + pdaFirm.firmNumber + ": " + e.getMessage());
        }
    }

    private void reactivateFirm(Firm firm, PdaFirmData pdaFirm, PdaSyncResultDto result) {
        try {
            firm.setName(pdaFirm.firmName);
            firm.setType(FirmType.valueOf(pdaFirm.firmType.toUpperCase().replace(" ", "_")));

            if (pdaFirm.parentFirmNumber != null && !pdaFirm.parentFirmNumber.isEmpty()) {
                Firm parentFirm = firmRepository.findByCode(pdaFirm.parentFirmNumber);
                firm.setParentFirm(parentFirm);
            } else {
                firm.setParentFirm(null);
            }

            // firmRepository.save(firm);  // COMMENTED OUT FOR TESTING
            result.setFirmsReactivated(result.getFirmsReactivated() + 1);
            log.info("Would reactivate firm: {} (name: {}, type: {})", pdaFirm.firmNumber, pdaFirm.firmName, pdaFirm.firmType);
        } catch (Exception e) {
            log.error("Failed to reactivate firm {}: {}", pdaFirm.firmNumber, e.getMessage());
            result.addError("Failed to reactivate firm " + pdaFirm.firmNumber + ": " + e.getMessage());
        }
    }

    private void updateFirm(Firm firm, PdaFirmData pdaFirm, PdaSyncResultDto result) {
        try {
            boolean updated = false;

            // Check firmType - reject if changed
            FirmType newType = FirmType.valueOf(pdaFirm.firmType.toUpperCase().replace(" ", "_"));
            if (!firm.getType().equals(newType)) {
                result.addWarning("Firm " + pdaFirm.firmNumber + " type change rejected: " + firm.getType() + " -> " + newType);
                return;
            }

            // Update name if changed
            if (!firm.getName().equals(pdaFirm.firmName)) {
                firm.setName(pdaFirm.firmName);
                updated = true;
            }

            // Update parent if changed
            String currentParentCode = firm.getParentFirm() != null ? firm.getParentFirm().getCode() : null;
            if ((pdaFirm.parentFirmNumber == null && currentParentCode != null)
                || (pdaFirm.parentFirmNumber != null && !pdaFirm.parentFirmNumber.equals(currentParentCode))) {

                if (pdaFirm.parentFirmNumber != null && !pdaFirm.parentFirmNumber.isEmpty()) {
                    Firm parentFirm = firmRepository.findByCode(pdaFirm.parentFirmNumber);
                    firm.setParentFirm(parentFirm);
                } else {
                    firm.setParentFirm(null);
                }
                updated = true;
            }

            if (updated) {
                // firmRepository.save(firm);  // COMMENTED OUT FOR TESTING
                result.setFirmsUpdated(result.getFirmsUpdated() + 1);
                log.info("Would update firm: {} (name: {})", pdaFirm.firmNumber, pdaFirm.firmName);
            }
        } catch (Exception e) {
            log.error("Failed to update firm {}: {}", pdaFirm.firmNumber, e.getMessage());
            result.addError("Failed to update firm " + pdaFirm.firmNumber + ": " + e.getMessage());
        }
    }

    private void deactivateFirm(Firm firm, PdaSyncResultDto result) {
        try {
            // firmRepository.delete(firm);  // COMMENTED OUT FOR TESTING - could delete instead
            result.setFirmsDeactivated(result.getFirmsDeactivated() + 1);
            log.info("Would deactivate/delete firm: {} (name: {})", firm.getCode(), firm.getName());
        } catch (Exception e) {
            log.error("Failed to deactivate firm {}: {}", firm.getCode(), e.getMessage());
            result.addError("Failed to deactivate firm " + firm.getCode() + ": " + e.getMessage());
        }
    }

    private void createOffice(PdaOfficeData pdaOffice, Firm firm, PdaSyncResultDto result) {
        try {
            Office office = Office.builder()
                .code(pdaOffice.officeAccountNo)
                .firm(firm)
                .address(Office.Address.builder()
                    .addressLine1(pdaOffice.addressLine1)
                    .addressLine2(pdaOffice.addressLine2)
                    .addressLine3(pdaOffice.addressLine3)
                    .city(pdaOffice.city)
                    .postcode(pdaOffice.postcode)
                    .build())
                .build();

            // officeRepository.save(office);  // COMMENTED OUT FOR TESTING
            result.setOfficesCreated(result.getOfficesCreated() + 1);
            log.info("Would create office: {} for firm {} (address: {}, {})",
                pdaOffice.officeAccountNo, firm.getCode(), pdaOffice.addressLine1, pdaOffice.city);
        } catch (Exception e) {
            log.error("Failed to create office {}: {}", pdaOffice.officeAccountNo, e.getMessage());
            result.addError("Failed to create office " + pdaOffice.officeAccountNo + ": " + e.getMessage());
        }
    }

    private void reactivateOffice(Office office, PdaOfficeData pdaOffice, Firm firm, PdaSyncResultDto result) {
        try {
            office.setFirm(firm);
            office.setAddress(Office.Address.builder()
                .addressLine1(pdaOffice.addressLine1)
                .addressLine2(pdaOffice.addressLine2)
                .addressLine3(pdaOffice.addressLine3)
                .city(pdaOffice.city)
                .postcode(pdaOffice.postcode)
                .build());

            // officeRepository.save(office);  // COMMENTED OUT FOR TESTING
            result.setOfficesReactivated(result.getOfficesReactivated() + 1);
            log.info("Would reactivate office: {} for firm {} (address: {}, {})",
                pdaOffice.officeAccountNo, firm.getCode(), pdaOffice.addressLine1, pdaOffice.city);
        } catch (Exception e) {
            log.error("Failed to reactivate office {}: {}", pdaOffice.officeAccountNo, e.getMessage());
            result.addError("Failed to reactivate office " + pdaOffice.officeAccountNo + ": " + e.getMessage());
        }
    }

    private void updateOffice(Office office, PdaOfficeData pdaOffice, Firm firm, PdaSyncResultDto result) {
        try {
            boolean updated = false;

            // Check if firm changed - if so, remove user_profile_office associations
            if (!office.getFirm().getId().equals(firm.getId())) {
                // removeUserProfileOfficeAssociations(office);  // COMMENTED OUT FOR TESTING
                log.info("Firm changed for office {}: {} -> {}", office.getCode(), office.getFirm().getCode(), firm.getCode());
                log.info("Would remove user profile associations for office: {}", office.getCode());
                office.setFirm(firm);
                updated = true;
            }

            // Update address if changed
            Office.Address currentAddress = office.getAddress();
            if (currentAddress == null
                || !equals(currentAddress.getAddressLine1(), pdaOffice.addressLine1)
                || !equals(currentAddress.getAddressLine2(), pdaOffice.addressLine2)
                || !equals(currentAddress.getAddressLine3(), pdaOffice.addressLine3)
                || !equals(currentAddress.getCity(), pdaOffice.city)
                || !equals(currentAddress.getPostcode(), pdaOffice.postcode)) {

                office.setAddress(Office.Address.builder()
                    .addressLine1(pdaOffice.addressLine1)
                    .addressLine2(pdaOffice.addressLine2)
                    .addressLine3(pdaOffice.addressLine3)
                    .city(pdaOffice.city)
                    .postcode(pdaOffice.postcode)
                    .build());
                updated = true;
            }

            if (updated) {
                // officeRepository.save(office);  // COMMENTED OUT FOR TESTING
                result.setOfficesUpdated(result.getOfficesUpdated() + 1);
                log.info("Would update office: {} (firm: {}, address: {})",
                    pdaOffice.officeAccountNo, firm.getCode(), pdaOffice.addressLine1);
            }
        } catch (Exception e) {
            log.error("Failed to update office {}: {}", pdaOffice.officeAccountNo, e.getMessage());
            result.addError("Failed to update office " + pdaOffice.officeAccountNo + ": " + e.getMessage());
        }
    }

    private void deactivateOffice(Office office, PdaSyncResultDto result) {
        try {
            // removeUserProfileOfficeAssociations(office);  // COMMENTED OUT FOR TESTING
            log.info("Would remove user profile associations for office: {}", office.getCode());
            // officeRepository.delete(office);  // COMMENTED OUT FOR TESTING - could delete instead
            result.setOfficesDeactivated(result.getOfficesDeactivated() + 1);
            log.info("Would deactivate/delete office: {} (firm: {})", office.getCode(), office.getFirm().getCode());
        } catch (Exception e) {
            log.error("Failed to deactivate office {}: {}", office.getCode(), e.getMessage());
            result.addError("Failed to deactivate office " + office.getCode() + ": " + e.getMessage());
        }
    }

    private void removeUserProfileOfficeAssociations(Office office) {
        List<UserProfile> profiles = userProfileRepository.findAll();
        for (UserProfile profile : profiles) {
            if (profile.getOffices() != null && profile.getOffices().contains(office)) {
                // profile.getOffices().remove(office);  // COMMENTED OUT FOR TESTING
                // userProfileRepository.save(profile);  // COMMENTED OUT FOR TESTING
                log.info("Would remove office {} from user profile {}", office.getCode(), profile.getId());
            }
        }
    }

    private boolean equals(String s1, String s2) {
        if (s1 == null && s2 == null) {
            return true;
        }
        if (s1 == null || s2 == null) {
            return false;
        }
        return s1.equals(s2);
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

    // Inner classes for PDA data
    private static class PdaFirmData {
        String firmNumber;
        String firmName;
        String firmType;
        String parentFirmNumber;
    }

    private static class PdaOfficeData {
        String officeAccountNo;
        String firmNumber;
        String addressLine1;
        String addressLine2;
        String addressLine3;
        String city;
        String postcode;
    }
}
