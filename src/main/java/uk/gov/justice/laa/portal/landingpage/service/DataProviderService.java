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
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.json.JsonReadOptions;
import uk.gov.justice.laa.portal.landingpage.dto.ComparisonResultDto;
import uk.gov.justice.laa.portal.landingpage.dto.PdaFirmData;
import uk.gov.justice.laa.portal.landingpage.dto.PdaOfficeData;
import uk.gov.justice.laa.portal.landingpage.dto.PdaSyncResultDto;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;
import uk.gov.justice.laa.portal.landingpage.repository.OfficeRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;
import uk.gov.justice.laa.portal.landingpage.service.pda.command.CreateFirmCommand;
import uk.gov.justice.laa.portal.landingpage.service.pda.command.CreateOfficeCommand;
import uk.gov.justice.laa.portal.landingpage.service.pda.command.DeactivateFirmCommand;
import uk.gov.justice.laa.portal.landingpage.service.pda.command.DeleteOfficeCommand;
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
    private final TransactionTemplate transactionTemplate;

    @PersistenceContext
    private EntityManager entityManager;

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
        int firmCreates = 0, firmUpdates = 0, firmDeletes = 0, firmExists = 0;
        int officeCreates = 0, officeUpdates = 0, officeDeletes = 0, officeExists = 0;

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
                firmCreates++;
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
                    firmUpdates++;
                } else {
                    result.getExists().add(ComparisonResultDto.ItemInfo.builder()
                        .type("firm")
                        .code(firmCode)
                        .name(pdaFirm.getFirmName())
                        .dbId(dbFirm.getId())
                        .build());
                    firmExists++;
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
                firmDeletes++;
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
                officeCreates++;
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
                    officeUpdates++;
                } else {
                    result.getExists().add(ComparisonResultDto.ItemInfo.builder()
                        .type("office")
                        .code(officeCode)
                        .name(officeName)
                        .dbId(dbOffice.getId())
                        .build());
                    officeExists++;
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
                officeDeletes++;
            }
        }

        // Set the separate counts in the result
        result.setFirmCreates(firmCreates);
        result.setFirmUpdates(firmUpdates);
        result.setFirmDeletes(firmDeletes);
        result.setFirmExists(firmExists);
        result.setOfficeCreates(officeCreates);
        result.setOfficeUpdates(officeUpdates);
        result.setOfficeDeletes(officeDeletes);
        result.setOfficeExists(officeExists);

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
            // Use TransactionTemplate to ensure proper transaction boundary
            // @Transactional doesn't work when called from same class due to proxy bypass
            PdaSyncResultDto result = transactionTemplate.execute(status -> {
                try {
                    return synchronizeWithPda();
                } catch (Exception e) {
                    // Catch any exception that would mark transaction as rollback-only
                    // Log it and return an error result, allowing partial commit
                    log.error("Exception during PDA sync (allowing partial commit): {}", e.getMessage(), e);
                    PdaSyncResultDto errorResult = PdaSyncResultDto.builder().build();
                    errorResult.addError("Sync exception: " + e.getMessage());
                    // Don't set rollback - let partial changes commit
                    return errorResult;
                }
            });

            if (result != null) {
                if (!result.getErrors().isEmpty()) {
                    log.warn("PDA sync completed with {} errors (changes were committed)", result.getErrors().size());
                }
                log.info("Async PDA synchronization completed - Firms: {} created, {} updated, {} deactivated | Offices: {} created, {} updated, {} deactivated",
                    result.getFirmsCreated(), result.getFirmsUpdated(), result.getFirmsDeactivated(),
                    result.getOfficesCreated(), result.getOfficesUpdated(), result.getOfficesDeactivated());
            }

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
     * NOTE: This method should be called within a transaction. The caller (synchronizeWithPdaAsync)
     * uses TransactionTemplate to ensure proper transaction boundary.
     *
     * @return PdaSyncResultDto containing statistics and any errors/warnings
     */
    private PdaSyncResultDto synchronizeWithPda() {
        log.info("Starting PDA synchronization - CODE VERSION 2026-01-21-10:22");
        PdaSyncResultDto result = PdaSyncResultDto.builder().build();

        try {
            // Defer constraint checking to allow firms to be created before their offices
            // The check_firms_have_office constraint will be checked at transaction commit
            // Use EntityManager to ensure it's on the same connection as the transaction
            entityManager.createNativeQuery("SET CONSTRAINTS ALL DEFERRED").executeUpdate();
            log.debug("Deferred constraint checking for PDA sync transaction");

            // Fetch and build PDA data maps
            log.info("DEBUG: Fetching PDA data...");
            Table pdaTable = getProviderOfficesSnapshot();
            log.info("DEBUG: Building PDA maps - firms and offices...");
            Map<String, PdaFirmData> pdaFirms = buildPdaFirmsMap(pdaTable);
            Map<String, PdaOfficeData> pdaOffices = buildPdaOfficesMap(pdaTable);
            log.info("DEBUG: Found {} firms and {} offices in PDA data", pdaFirms.size(), pdaOffices.size());

            // Perform data integrity checks
            log.info("DEBUG: Checking data integrity...");
            checkDataIntegrity(pdaFirms, pdaOffices, result);

            // Get current database state
            log.info("DEBUG: Loading database firms...");
            Map<String, Firm> dbFirms = new HashMap<>();
            firmRepository.findAll().forEach(f -> {
                if (f.getCode() != null) {
                    dbFirms.put(f.getCode(), f);
                }
            });
            log.info("DEBUG: Loaded {} firms from database", dbFirms.size());

            // Track processed codes
            Set<String> processedFirmCodes = new HashSet<>();

            // Determine which firms have offices (database constraint requires this)
            log.info("DEBUG: Building firmsWithOffices set...");
            Set<String> firmsWithOffices = new HashSet<>();
            for (PdaOfficeData office : pdaOffices.values()) {
                if (office.getFirmNumber() != null) {
                    firmsWithOffices.add(office.getFirmNumber());
                }
            }
            log.info("Found {} unique firms with offices in PDA data", firmsWithOffices.size());

            // PASS 1: Process firms - create or update (without parent references for new firms)
            // ONLY process firms that have at least one office (database constraint requirement)
            for (Map.Entry<String, PdaFirmData> entry : pdaFirms.entrySet()) {
                String firmCode = entry.getKey();
                PdaFirmData pdaFirm = entry.getValue();

                if (!firmsWithOffices.contains(firmCode)) {
                    log.warn("Skipping firm {} - no offices found in PDA data (database requires firms to have at least one office)", firmCode);
                    result.addWarning("Skipped firm " + firmCode + " - no offices in PDA data");
                    continue;
                }

                processedFirmCodes.add(firmCode);

                Firm dbFirm = dbFirms.get(firmCode);

                if (dbFirm == null) {
                    createFirm(pdaFirm, result);
                } else {
                    updateFirm(dbFirm, pdaFirm, result);
                }

                // Check if we have critical errors that abort the transaction
                // If so, stop processing to avoid "transaction is aborted" cascading errors
                if (!result.getErrors().isEmpty()) {
                    log.warn("Stopping firm processing due to {} errors to prevent transaction abort cascade",
                        result.getErrors().size());
                    break;
                }
            }

            // If errors occurred during firm processing, stop immediately to avoid cascading failures
            if (!result.getErrors().isEmpty()) {
                log.error("Stopping synchronization due to {} errors during firm processing", result.getErrors().size());
                return result;
            }

            // Check for firms to deactivate
            // Deactivate firms that are either:
            // 1. Not in PDA data anymore
            // 2. In PDA but don't have any offices (violates database constraint)
            for (String firmCode : dbFirms.keySet()) {
                if (!processedFirmCodes.contains(firmCode)) {
                    log.info("Deactivating firm {} - not in PDA data", firmCode);
                    deactivateFirm(dbFirms.get(firmCode), result);
                    result.setFirmsDeactivated(result.getFirmsDeactivated() + 1);
                } else if (!firmsWithOffices.contains(firmCode)) {
                    log.info("Deactivating firm {} - no offices in PDA data (database constraint)", firmCode);
                    deactivateFirm(dbFirms.get(firmCode), result);
                    result.setFirmsDeactivated(result.getFirmsDeactivated() + 1);
                }
            }

            // If errors occurred during deactivation, stop immediately
            if (!result.getErrors().isEmpty()) {
                log.error("Stopping synchronization due to {} errors during firm deactivation", result.getErrors().size());
                return result;
            }

            // Flush Pass 1 changes (firm creation/updates/deactivations) before setting parent references
            log.info("Flushing Pass 1 firm changes to database...");
            entityManager.flush();
            log.info("Pass 1 firm changes flushed successfully");

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
                                try {
                                    firm.setParentFirm(parentFirm);
                                    firmRepository.save(firm);
                                    log.info("Set parent for firm {}: {} -> {}", firmCode, currentParentCode, pdaFirm.getParentFirmNumber());
                                } catch (Exception e) {
                                    log.error("Failed to set parent {} for firm {}: {} - clearing parent reference",
                                        pdaFirm.getParentFirmNumber(), firmCode, e.getMessage());
                                    firm.setParentFirm(null);
                                    firmRepository.save(firm);
                                    result.addWarning("Invalid parent firm " + pdaFirm.getParentFirmNumber() +
                                        " for firm " + firmCode + ": " + e.getMessage());
                                }
                            } else {
                                log.warn("Parent firm {} not found for firm {} - clearing parent reference",
                                    pdaFirm.getParentFirmNumber(), firmCode);
                                firm.setParentFirm(null);
                                firmRepository.save(firm);
                                result.addWarning("Parent firm " + pdaFirm.getParentFirmNumber() + " not found for firm " + firmCode);
                            }
                        }
                    }
                }

                // Check for errors after each parent update attempt
                if (!result.getErrors().isEmpty()) {
                    log.error("Stopping synchronization due to {} errors during parent reference update", result.getErrors().size());
                    return result;
                }
            }

            // CRITICAL: Flush all firm changes before processing offices
            // This ensures offices can reference newly created/updated firms
            log.info("Flushing firm changes to database...");
            entityManager.flush();
            log.info("Firm changes flushed successfully");

            // Get DB offices
            Map<String, Office> dbOffices = new HashMap<>();
            officeRepository.findAll().forEach(o -> {
                if (o.getCode() != null) {
                    dbOffices.put(o.getCode(), o);
                }
            });

            final Set<String> processedOfficeCodes = new HashSet<>();

            // First, identify offices that will be deactivated (not in PDA data)
            final Set<String> officesToDeactivate = new HashSet<>();
            for (String officeCode : dbOffices.keySet()) {
                if (!pdaOffices.containsKey(officeCode)) {
                    officesToDeactivate.add(officeCode);
                }
            }

            // PASS 3: Process offices - create new ones and update existing ones (but not those being deactivated)
            log.info("Processing offices...");
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

                // Skip if this office is being deactivated (should not happen but defensive check)
                if (officesToDeactivate.contains(officeCode)) {
                    log.warn("Office {} is in PDA data but was marked for deactivation - skipping", officeCode);
                    continue;
                }

                if (dbOffice == null) {
                    createOffice(pdaOffice, parentFirm, result);
                } else {
                    // Check if the office entity is still managed (not deleted in this transaction)
                    if (!entityManager.contains(dbOffice)) {
                        log.warn("Office {} entity is detached/deleted, skipping update", officeCode);
                        continue;
                    }
                    updateOffice(dbOffice, pdaOffice, parentFirm, result);
                }

                // Check for errors after each office operation
                if (!result.getErrors().isEmpty()) {
                    log.error("Stopping synchronization due to {} errors during office processing", result.getErrors().size());
                    return result;
                }
            }

            // Check for errors before deactivating offices
            if (!result.getErrors().isEmpty()) {
                log.error("Stopping synchronization due to {} errors, skipping office deactivation", result.getErrors().size());
                return result;
            }

            // Now deactivate offices that are not in PDA data
            for (String officeCode : officesToDeactivate) {
                deactivateOffice(dbOffices.get(officeCode), result);
                result.setOfficesDeactivated(result.getOfficesDeactivated() + 1);
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
        new DeactivateFirmCommand(firmRepository, officeRepository, userProfileRepository, firm).execute(result);
    }

    private void createOffice(PdaOfficeData pdaOffice, Firm firm, PdaSyncResultDto result) {
        new CreateOfficeCommand(officeRepository, pdaOffice, firm).execute(result);
    }

    private void updateOffice(Office office, PdaOfficeData pdaOffice, Firm firm, PdaSyncResultDto result) {
        new UpdateOfficeCommand(officeRepository, office, pdaOffice, firm).execute(result);
    }

    private void deactivateOffice(Office office, PdaSyncResultDto result) {
        new DeleteOfficeCommand(officeRepository, userProfileRepository, office).execute(result);
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
