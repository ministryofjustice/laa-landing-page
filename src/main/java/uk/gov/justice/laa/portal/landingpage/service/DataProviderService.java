package uk.gov.justice.laa.portal.landingpage.service;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PreDestroy;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.json.JsonReadOptions;
import uk.gov.justice.laa.portal.landingpage.config.DataProviderConfig;
import uk.gov.justice.laa.portal.landingpage.dto.ComparisonResultDto;
import uk.gov.justice.laa.portal.landingpage.dto.PdaFirmData;
import uk.gov.justice.laa.portal.landingpage.dto.PdaOfficeData;
import uk.gov.justice.laa.portal.landingpage.dto.PdaSyncResultDto;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;
import uk.gov.justice.laa.portal.landingpage.repository.OfficeRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;
import uk.gov.justice.laa.portal.landingpage.service.pda.command.CreateFirmCommand;
import uk.gov.justice.laa.portal.landingpage.service.pda.command.CreateOfficeCommand;
import uk.gov.justice.laa.portal.landingpage.service.pda.command.DisableFirmCommand;
import uk.gov.justice.laa.portal.landingpage.service.pda.command.UpdateFirmCommand;
import uk.gov.justice.laa.portal.landingpage.service.pda.command.UpdateOfficeCommand;

/**
 * Service for calling PDA (Provider Data API) endpoints and matching with local database.
 */
@Slf4j
@Service
public class DataProviderService {

    private final RestClient dataProviderRestClient;
    private final ObjectMapper objectMapper;
    private final FirmRepository firmRepository;
    private final OfficeRepository officeRepository;
    private final UserProfileRepository userProfileRepository;
    private final TransactionTemplate transactionTemplate;
    private final DataProviderConfig dataProviderConfig;

    public DataProviderService(
            @Qualifier("dataProviderRestClient") RestClient dataProviderRestClient,
            ObjectMapper objectMapper,
            FirmRepository firmRepository,
            OfficeRepository officeRepository,
            UserProfileRepository userProfileRepository,
            TransactionTemplate transactionTemplate,
            DataProviderConfig dataProviderConfig) {
        this.dataProviderRestClient = dataProviderRestClient;
        this.objectMapper = objectMapper;
        this.firmRepository = firmRepository;
        this.officeRepository = officeRepository;
        this.userProfileRepository = userProfileRepository;
        this.transactionTemplate = transactionTemplate;
        this.dataProviderConfig = dataProviderConfig;
    }

    @PersistenceContext
    private EntityManager entityManager;

    // Flag to detect application shutdown and prevent database access
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);

    @PreDestroy
    public void onShutdown() {
        log.info("DataProviderService shutdown initiated - flagging sync operations to abort");
        shuttingDown.set(true);
    }

    /**
     * Enables the bypass flag for firm-office constraint checking during PDA sync.
     * Sets a session variable that the trigger function checks to skip expensive validation.
     * Uses regular SET (not SET LOCAL) so it persists through COMMIT when the constraint trigger fires.
     */
    private void enableFirmOfficeCheckBypass() {
        try {
            entityManager.createNativeQuery("SET app.internal.pda_sync_bypass_constraint_check = 'true'")
                .executeUpdate();
            log.info("Enabled firm-office constraint bypass for PDA sync (persists through COMMIT)");
        } catch (Exception e) {
            log.error("Failed to enable firm-office constraint bypass: {}", e.getMessage(), e);
            throw new RuntimeException("Cannot enable constraint bypass", e);
        }
    }

    /**
     * Resets the firm-office constraint bypass flag after PDA sync completes.
     */
    private void resetFirmOfficeCheckBypass() {
        try {
            entityManager.createNativeQuery("RESET app.internal.pda_sync_bypass_constraint_check")
                .executeUpdate();
            log.info("Reset firm-office constraint bypass after PDA sync");
        } catch (Exception e) {
            log.error("Failed to reset firm-office constraint bypass: {}", e.getMessage(), e);
            // Don't throw - we want to continue even if reset fails
        }
    }

    /**
     * Fetches provider offices snapshot from PDA and returns as TableSaw dataframe.
     *
     * @return TableSaw Table containing provider offices snapshot data
     */
    public Table getProviderOfficesSnapshot() {
        log.debug("Fetching provider offices snapshot from {}",
            dataProviderConfig.isUseLocalFile() ? "local file: " + dataProviderConfig.getLocalFilePath() : "PDA API");

        try {
            String response;

            if (dataProviderConfig.isUseLocalFile()) {
                // Read from local file
                response = Files.readString(
                    Paths.get(dataProviderConfig.getLocalFilePath())
                );
                log.debug("Successfully loaded data from local file");
            } else {
                // Fetch from API
                response = dataProviderRestClient.get()
                        .uri("/provider-offices/snapshot")
                        .retrieve()
                        .body(String.class);
                log.debug("Successfully fetched provider offices snapshot from PDA API");
            }

            log.debug("Converting to dataframe");

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

            log.debug("Dataframe created with {} rows and {} columns",
                table.rowCount(), table.columnCount());

            return table;
        } catch (Exception e) {
            log.error("Error fetching or parsing provider offices snapshot from PDA: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch provider offices snapshot from PDA", e);
        }
    }

    /**
     * Returns structured comparison showing created, updated, deleted, and matched items.
     * IMPORTANT: This comparison mirrors the actual sync logic including business rules:
     * - Firms without offices are skipped (database constraint)
     * - Firm name updates are skipped if duplicate name exists
     * - Parent firm validation rules apply
     *
     * @return ComparisonResultDto with categorized items matching actual sync behavior
     */
    public ComparisonResultDto compareWithDatabase() {
        log.debug("Comparing PDA data with local database (mirroring sync business rules)");
        log.debug("Initial CWA data integrity check ---------------\n");

        Table pdaTable = getProviderOfficesSnapshot();

        // Get all firms and offices from database (optimized with fetch joins)
        List<Firm> allFirms = firmRepository.findAllWithParentFirm();
        List<Office> allOffices = officeRepository.findAllWithFirm();

        // Build lookup maps for O(1) performance
        Map<String, Firm> firmsByCode = allFirms.stream()
            .filter(f -> f.getCode() != null)
            .collect(Collectors.toMap(Firm::getCode, f -> f, (f1, f2) -> f1));

        final Map<String, Firm> firmsByName = allFirms.stream()
            .filter(f -> f.getName() != null)
            .collect(Collectors.toMap(Firm::getName, f -> f, (f1, f2) -> f1));

        Map<String, Office> officesByCode = allOffices.stream()
            .filter(o -> o.getCode() != null)
            .collect(Collectors.toMap(Office::getCode, o -> o, (o1, o2) -> o1));

        // Count firms with NULL codes (filtered out from comparison)
        final long firmsWithNullCode = allFirms.stream()
            .filter(f -> f.getCode() == null)
            .count();

        // Count offices with NULL codes (filtered out from comparison)
        long officesWithNullCode = allOffices.stream()
            .filter(o -> o.getCode() == null)
            .count();

        log.debug("Built lookup maps: {} firms, {} offices", firmsByCode.size(), officesByCode.size());

        // Build PDA data maps
        Map<String, PdaFirmData> pdaFirms = buildPdaFirmsMap(pdaTable);
        Map<String, PdaOfficeData> pdaOffices = buildPdaOfficesMap(pdaTable);

        // Determine which firms have offices (sync skips firms without offices)
        Set<String> firmsWithOffices = new HashSet<>();
        for (PdaOfficeData office : pdaOffices.values()) {
            if (office.getFirmNumber() != null) {
                firmsWithOffices.add(office.getFirmNumber());
            }
        }

        // Count firms without offices
        int firmsWithoutOffices = (int) pdaFirms.keySet().stream()
            .filter(code -> !firmsWithOffices.contains(code))
            .count();
        log.debug("\nFound {} firm(s) with no offices. These will be removed.", firmsWithoutOffices);

        // Track separate counts
        int firmCreates = 0;
        int firmUpdates = 0;
        int firmUpdatesNameOnly = 0;
        int firmUpdatesParentOnly = 0;
        int firmUpdatesNameAndParent = 0;
        int firmUpdatesNameSkipped = 0;  // Name changes skipped due to duplicates

        // Parent firm change breakdowns
        int firmUpdatesParentSet = 0;      // null -> parent
        int firmUpdatesParentCleared = 0;  // parent -> null
        int firmUpdatesParentChanged = 0;  // parent A -> parent B

        int firmDisables = 0;
        int firmExists = 0;
        int officeCreates = 0;
        int officeUpdates = 0;
        int officeUpdatesAddressOnly = 0;
        int officeUpdatesFirmOnly = 0;
        int officeUpdatesBoth = 0;

        // Address field change details
        int officeUpdatesAddressLine1 = 0;
        int officeUpdatesAddressLine2 = 0;
        int officeUpdatesAddressLine3 = 0;
        int officeUpdatesCity = 0;
        int officeUpdatesPostcode = 0;

        int officeExists = 0;
        int officeCreatesWithParentFirm = 0;  // Offices that can be created immediately (parent firm exists)
        int officeUpdatesSkippedNoParentFirm = 0;  // Existing offices with updates but parent firm doesn't exist
        int officesSwitchedFirm = 0;
        Set<UUID> officeIdsWithFirmSwitch = new HashSet<>();

        ComparisonResultDto result = ComparisonResultDto.builder().build();

        // Compare firms - MIRRORING SYNC LOGIC
        Set<String> processedFirmCodes = new HashSet<>();
        for (Map.Entry<String, PdaFirmData> entry : pdaFirms.entrySet()) {
            String firmCode = entry.getKey();
            PdaFirmData pdaFirm = entry.getValue();

            // SYNC RULE: Skip firms without offices (database constraint)
            if (!firmsWithOffices.contains(firmCode)) {
                log.debug("Skipping firm {} in comparison - no offices (sync would skip this)", firmCode);
                continue;
            }

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
                // Check if firm needs updating (name or parent firm)
                // UpdateFirmCommand handles name changes in Pass 1
                // Pass 2 handles parent firm references via direct repository save
                boolean nameChanged = !pdaFirm.getFirmName().equals(dbFirm.getName());
                boolean parentChanged = false;
                boolean needsUpdate = false;
                boolean nameUpdateSkipped = false;

                // Check parent firm changes - apply PASS 2 sync validation rules
                String currentParentCode = dbFirm.getParentFirm() != null ? dbFirm.getParentFirm().getCode() : null;
                String rawNewParentCode = (pdaFirm.getParentFirmNumber() != null
                    && !pdaFirm.getParentFirmNumber().trim().isEmpty()
                    && !pdaFirm.getParentFirmNumber().trim().equalsIgnoreCase("null"))
                    ? pdaFirm.getParentFirmNumber().trim() : null;

                // Apply PASS 2 sync validation rules to get effective parent code
                // This mirrors what synchronizeWithPda() will actually do
                String effectiveNewParentCode = rawNewParentCode;
                if (rawNewParentCode != null) {
                    // In the real sync, PASS 1 can create a parent firm from PDA data
                    // if it exists in pdaFirms and passes the "has offices" rule.
                    // If such a firm will be created, treat the parent as valid here
                    boolean parentWillBeCreatedFromPda = pdaFirms.containsKey(rawNewParentCode)
                        && firmsWithOffices.contains(rawNewParentCode);

                    Firm parentFirm = firmsByCode.get(rawNewParentCode);
                    if (parentFirm == null && !parentWillBeCreatedFromPda) {
                        // SYNC RULE: Parent firm not found (and not due to be created) -> cleared to null
                        effectiveNewParentCode = null;
                        log.debug("Firm {} raw parent {} not found in DB or PDA-creatable firms - sync will clear parent to null", firmCode, rawNewParentCode);
                    } else if (parentFirm != null && parentFirm.getType() == FirmType.ADVOCATE) {
                        // SYNC RULE: ADVOCATE firms cannot be parents -> cleared to null
                        effectiveNewParentCode = null;
                        log.debug("Firm {} raw parent {} is ADVOCATE type - sync will clear parent to null", firmCode, rawNewParentCode);
                    } else if (parentFirm != null && parentFirm.getParentFirm() != null) {
                        // SYNC RULE: Multi-level hierarchy not allowed -> cleared to null
                        effectiveNewParentCode = null;
                        log.debug("Firm {} raw parent {} has a parent - sync will clear parent to null (multi-level not allowed)", firmCode, rawNewParentCode);
                    } else if (parentFirm == null && parentWillBeCreatedFromPda) {
                        // Parent will be created from PDA - check if that PDA parent itself has a parent
                        PdaFirmData pdaParent = pdaFirms.get(rawNewParentCode);
                        if (pdaParent != null
                            && pdaParent.getParentFirmNumber() != null
                            && !pdaParent.getParentFirmNumber().trim().isEmpty()
                            && !pdaParent.getParentFirmNumber().trim().equalsIgnoreCase("null")
                            && !pdaParent.getParentFirmNumber().trim().equalsIgnoreCase("ADVOCATE")) {
                            // SYNC RULE: Multi-level hierarchy not allowed (parent-to-be-created has a parent)
                            effectiveNewParentCode = null;
                            log.debug("Firm {} raw parent {} will be created with a parent ({}) - sync will clear parent to null (multi-level not allowed)",
                                firmCode, rawNewParentCode, pdaParent.getParentFirmNumber());
                        }
                        if (pdaParent != null
                            && pdaParent.getFirmType() != null
                            && pdaParent.getFirmType().trim().equalsIgnoreCase("ADVOCATE")) {
                            // SYNC RULE: ADVOCATE firms cannot be parents
                            effectiveNewParentCode = null;
                            log.debug("Firm {} raw parent {} will be created as ADVOCATE type - sync will clear parent to null",
                                firmCode, rawNewParentCode);
                        }
                    }
                }

                // Treat parent as changed whenever the effective new parent code (after PASS 2 rules)
                // differs from the current parent, even if PDA omitted/blanked the parent value.
                if ((currentParentCode == null && effectiveNewParentCode != null)
                    || (currentParentCode != null && !currentParentCode.equals(effectiveNewParentCode))) {
                    parentChanged = true;
                }

                if (nameChanged) {
                    // SYNC RULE: Skip name update if duplicate name exists (UpdateFirmCommand behavior)
                    Firm existingFirmWithName = firmsByName.get(pdaFirm.getFirmName());
                    if (existingFirmWithName != null && !existingFirmWithName.getId().equals(dbFirm.getId())) {
                        log.debug("Firm {} name change would be skipped - duplicate name exists", firmCode);
                        nameUpdateSkipped = true;
                        firmUpdatesNameSkipped++;  // Track all name conflicts detected
                    } else {
                        log.debug("COMPARE: Firm {} needs name update: '{}' -> '{}'", firmCode, dbFirm.getName(), pdaFirm.getFirmName());
                        needsUpdate = true;
                    }
                }

                if (parentChanged) {
                    log.debug("COMPARE: Firm {} needs parent firm update: '{}' -> '{}'", firmCode, currentParentCode, effectiveNewParentCode);
                    needsUpdate = true;

                    // Track granular parent change types using effective parent code
                    if (currentParentCode == null && effectiveNewParentCode != null) {
                        firmUpdatesParentSet++;  // Setting parent
                    } else if (currentParentCode != null && effectiveNewParentCode == null) {
                        firmUpdatesParentCleared++;  // Clearing parent
                    } else if (currentParentCode != null && effectiveNewParentCode != null) {
                        firmUpdatesParentChanged++;  // Changing parent
                    }
                }

                if (needsUpdate) {
                    result.getUpdated().add(ComparisonResultDto.ItemInfo.builder()
                        .type("firm")
                        .code(firmCode)
                        .name(pdaFirm.getFirmName())
                        .dbId(dbFirm.getId())
                        .build());
                    firmUpdates++;

                    // Track granular update types
                    boolean nameWillUpdate = nameChanged && !nameUpdateSkipped;
                    if (nameWillUpdate && parentChanged) {
                        firmUpdatesNameAndParent++;
                    } else if (nameWillUpdate) {
                        firmUpdatesNameOnly++;
                    } else if (parentChanged) {
                        firmUpdatesParentOnly++;
                    }
                } else if (nameUpdateSkipped && !parentChanged) {
                    // Firm with ONLY a skipped name change (no other changes)
                    // Treat as an existing firm, since sync will not update/modify it
                    result.getExists().add(ComparisonResultDto.ItemInfo.builder()
                        .type("firm")
                        .code(firmCode)
                        .name(pdaFirm.getFirmName())
                        .dbId(dbFirm.getId())
                        .build());
                    firmExists++;
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

        // Find deleted firms - MIRRORING SYNC LOGIC
        // Sync deactivates firms that are:
        // 1. Not in PDA data anymore, OR
        // 2. In PDA but don't have any offices (violates database constraint)
        for (Map.Entry<String, Firm> entry : firmsByCode.entrySet()) {
            String firmCode = entry.getKey();
            if (!processedFirmCodes.contains(firmCode) || !firmsWithOffices.contains(firmCode)) {
                Firm firm = entry.getValue();
                result.getDeleted().add(ComparisonResultDto.ItemInfo.builder()
                    .type("firm")
                    .code(firmCode)
                    .name(firm.getName())
                    .dbId(firm.getId())
                    .build());
                firmDisables++;
            }
        }

        // Compare offices
        Set<String> processedOfficeCodes = new HashSet<>();
        for (Map.Entry<String, PdaOfficeData> entry : pdaOffices.entrySet()) {
            String officeCode = entry.getKey();
            PdaOfficeData pdaOffice = entry.getValue();

            Office dbOffice = officesByCode.get(officeCode);
            Firm parentFirm = firmsByCode.get(pdaOffice.getFirmNumber());

            String officeName = pdaOffice.getAddressLine1() != null ? pdaOffice.getAddressLine1() : officeCode;

            // Check if office exists in database first (for accurate counting)
            if (dbOffice == null) {
                // New office to be created (count it even if parent firm doesn't exist yet)
                result.getCreated().add(ComparisonResultDto.ItemInfo.builder()
                    .type("office")
                    .code(officeCode)
                    .name(officeName + " (firm: " + pdaOffice.getFirmNumber() + ")")
                    .build());
                officeCreates++;

                // Track if parent firm exists (can be created immediately)
                if (parentFirm != null) {
                    officeCreatesWithParentFirm++;
                }
            }

            // Skip orphan offices for sync operations (parent firm must exist)
            if (parentFirm == null) {
                // Check if this is an existing office with updates that we're skipping
                if (dbOffice != null) {
                    // Office exists but we can't update it without parent firm
                    // Check if dbOffice has a firm (handle orphaned offices in DB)
                    boolean firmChanged = dbOffice.getFirm() != null
                        && !dbOffice.getFirm().getCode().equals(pdaOffice.getFirmNumber());
                    boolean addressChanged = !isSameAddress(dbOffice, pdaOffice);
                    if (firmChanged || addressChanged) {
                        officeUpdatesSkippedNoParentFirm++;
                    }
                }
                continue; // Don't mark as processed since sync won't process them
            }

            processedOfficeCodes.add(officeCode);

            if (dbOffice != null) {
                // Existing office - check for updates
                // Check if office switched firm
                boolean firmChanged = !dbOffice.getFirm().getId().equals(parentFirm.getId());
                boolean addressChanged = !isSameAddress(dbOffice, pdaOffice);

                if (firmChanged) {
                    officesSwitchedFirm++;
                    officeIdsWithFirmSwitch.add(dbOffice.getId());
                }

                // Check if office needs updating
                boolean needsUpdate = firmChanged || addressChanged;

                if (needsUpdate) {
                    result.getUpdated().add(ComparisonResultDto.ItemInfo.builder()
                        .type("office")
                        .code(officeCode)
                        .name(officeName)
                        .dbId(dbOffice.getId())
                        .build());
                    officeUpdates++;
                    log.debug("Office {} marked for update - firmChanged: {}, addressChanged: {}",
                        officeCode, firmChanged, addressChanged);

                    // Track granular office update types
                    if (addressChanged && firmChanged) {
                        officeUpdatesBoth++;
                    } else if (addressChanged) {
                        officeUpdatesAddressOnly++;

                        // Track specific address field changes
                        if (dbOffice.getAddress() != null) {
                            // Compare each field with DB
                            if (!equals(dbOffice.getAddress().getAddressLine1(), emptyToNull(pdaOffice.getAddressLine1()))) {
                                officeUpdatesAddressLine1++;
                            }
                            if (!equals(dbOffice.getAddress().getAddressLine2(), emptyToNull(pdaOffice.getAddressLine2()))) {
                                officeUpdatesAddressLine2++;
                            }
                            if (!equals(dbOffice.getAddress().getAddressLine3(), emptyToNull(pdaOffice.getAddressLine3()))) {
                                officeUpdatesAddressLine3++;
                            }
                            if (!equals(dbOffice.getAddress().getCity(), emptyToNull(pdaOffice.getCity()))) {
                                officeUpdatesCity++;
                            }
                            if (!equals(dbOffice.getAddress().getPostcode(), emptyToNull(pdaOffice.getPostcode()))) {
                                officeUpdatesPostcode++;
                            }
                        } else {
                            // DB address is null - count all non-null PDA fields as changes
                            if (emptyToNull(pdaOffice.getAddressLine1()) != null) {
                                officeUpdatesAddressLine1++;
                            }
                            if (emptyToNull(pdaOffice.getAddressLine2()) != null) {
                                officeUpdatesAddressLine2++;
                            }
                            if (emptyToNull(pdaOffice.getAddressLine3()) != null) {
                                officeUpdatesAddressLine3++;
                            }
                            if (emptyToNull(pdaOffice.getCity()) != null) {
                                officeUpdatesCity++;
                            }
                            if (emptyToNull(pdaOffice.getPostcode()) != null) {
                                officeUpdatesPostcode++;
                            }
                        }
                    } else if (firmChanged) {
                        officeUpdatesFirmOnly++;
                    }
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

        // Batch query for user associations affected by firm switches
        int userAssociationsDeletedFirmSwitch = 0;
        if (!officeIdsWithFirmSwitch.isEmpty()) {
            log.debug("Batch querying user associations for {} offices that switched firms", officeIdsWithFirmSwitch.size());
            List<Object[]> associationCounts = userProfileRepository.countAssociationsByOfficeIds(new ArrayList<>(officeIdsWithFirmSwitch));

            for (Object[] row : associationCounts) {
                Number count = (Number) row[1];
                userAssociationsDeletedFirmSwitch += count.intValue();
            }
            log.debug("Found {} user associations that would be deleted due to firm switches", userAssociationsDeletedFirmSwitch);
        }

        // Find deleted offices
        int officeDeletes = 0;
        int userAssociationsDeletedOfficeDeleted = 0;
        Set<UUID> officeIdsToDelete = new HashSet<>();
        for (Map.Entry<String, Office> entry : officesByCode.entrySet()) {
            String officeCode = entry.getKey();
            if (!processedOfficeCodes.contains(officeCode)) {
                Office office = entry.getValue();
                officeIdsToDelete.add(office.getId());

                String officeName = office.getAddress() != null && office.getAddress().getAddressLine1() != null
                    ? office.getAddress().getAddressLine1() : officeCode;
                log.debug("COMPARE: Office {} needs deletion (firm: {}, not in PDA data)", officeCode,
                    office.getFirm() != null ? office.getFirm().getCode() : "null");
                result.getDeleted().add(ComparisonResultDto.ItemInfo.builder()
                    .type("office")
                    .code(officeCode)
                    .name(officeName)
                    .dbId(office.getId())
                    .build());
                officeDeletes++;
            }
        }

        // Batch query for user associations affected by office deletions
        if (!officeIdsToDelete.isEmpty()) {
            log.debug("Batch querying user associations for {} offices to be deleted", officeIdsToDelete.size());
            List<Object[]> associationCounts = userProfileRepository.countAssociationsByOfficeIds(new ArrayList<>(officeIdsToDelete));

            for (Object[] row : associationCounts) {
                Number count = (Number) row[1];
                userAssociationsDeletedOfficeDeleted += count.intValue();
            }
            log.debug("Found {} user associations that would be deleted due to office deletions", userAssociationsDeletedOfficeDeleted);
        }

        StringBuilder summary = new StringBuilder();
        summary.append("\n--------------------------------------")
            .append("\nDelta Analysis -----------------------\n")
            .append("\nNo. of firms updated: ").append(firmUpdates);

        if (firmUpdates > 0) {
            summary.append("\n    -> ").append(firmUpdatesNameOnly).append(" with name changes only")
                .append("\n    -> ").append(firmUpdatesParentOnly).append(" with parent firm changes only")
                .append("\n    -> ").append(firmUpdatesNameAndParent).append(" with both name and parent changes");
            if (firmUpdatesParentSet + firmUpdatesParentCleared + firmUpdatesParentChanged > 0) {
                summary.append("\n        * ").append(firmUpdatesParentSet).append(" setting parent (null -> parent)")
                    .append("\n        * ").append(firmUpdatesParentCleared).append(" clearing parent (parent -> null)")
                    .append("\n        * ").append(firmUpdatesParentChanged).append(" changing parent (parent A -> parent B)");
            }
            if (firmUpdatesNameSkipped > 0) {
                summary.append("\n    -> ").append(firmUpdatesNameSkipped).append(" with name changes skipped (duplicate name exists)");
            }
        }

        summary.append("\nNo. of new firms: ").append(firmCreates)
            .append("\nNo. of removed firms: ").append(firmDisables);

        if (firmsWithNullCode > 0) {
            summary.append("\n    -> ").append(firmDisables).append(" with valid codes")
                .append("\n    -> ").append(firmsWithNullCode).append(" with NULL codes (skipped from comparison)");
        }

        summary.append("\n")
            .append("\nNo. of new offices: ").append(officeCreates);

        if (officeCreates != officeCreatesWithParentFirm) {
            summary.append("\n    -> ").append(officeCreatesWithParentFirm).append(" can be created immediately (parent firm exists)")
                .append("\n    -> ").append(officeCreates - officeCreatesWithParentFirm).append(" require parent firm creation first");
        }

        int totalOfficesUpdated = officeUpdates + officeUpdatesSkippedNoParentFirm;
        summary.append("\nTotal No. of offices updated: ").append(totalOfficesUpdated);

        if (officeUpdatesSkippedNoParentFirm > 0) {
            summary.append("\n    -> ").append(officeUpdates).append(" with valid parent firms")
                .append("\n    -> ").append(officeUpdatesSkippedNoParentFirm).append(" skipped (parent firm doesn't exist in DB)");
        }

        summary.append("\nNo. of offices updated, with no change to firm: ").append(officeUpdates - officesSwitchedFirm);

        if (officeUpdates > 0) {
            summary.append("\n    -> ").append(officeUpdatesAddressOnly).append(" with address changes only");
            if (officeUpdatesAddressOnly > 0) {
                summary.append("\n        * ").append(officeUpdatesAddressLine1).append(" address line 1 changes")
                    .append("\n        * ").append(officeUpdatesAddressLine2).append(" address line 2 changes")
                    .append("\n        * ").append(officeUpdatesAddressLine3).append(" address line 3 changes")
                    .append("\n        * ").append(officeUpdatesCity).append(" city changes")
                    .append("\n        * ").append(officeUpdatesPostcode).append(" postcode changes");
            }
            summary.append("\n    -> ").append(officeUpdatesFirmOnly).append(" with firm changes only")
                .append("\n    -> ").append(officeUpdatesBoth).append(" with both address and firm changes");
        }

        summary.append("\nNo. of offices that switched firm: ").append(officesSwitchedFirm)
            .append("\n    -> No of user_profile/office associations deleted due to firm switch: ").append(userAssociationsDeletedFirmSwitch)
            .append("\nNo. of removed offices: ").append(officeDeletes)
            .append("\n    -> No of user_profile/office associations deleted due to deleted offices: ").append(userAssociationsDeletedOfficeDeleted);

        log.info(summary.toString());

        // Set the separate counts in the result
        result.setFirmCreates(firmCreates);
        result.setFirmUpdates(firmUpdates);
        result.setFirmDisables(firmDisables);
        result.setFirmExists(firmExists);
        result.setOfficeCreates(officeCreates);
        result.setOfficeUpdates(officeUpdates);
        result.setOfficeDeletes(officeDeletes);
        result.setOfficeExists(officeExists);

        return result;
    }

    private boolean isSameAddress(Office office, PdaOfficeData pdaOffice) {
        if (office.getAddress() == null) {
            return false;
        }

        // Use emptyToNull normalization to match UpdateOfficeCommand behavior
        return equals(office.getAddress().getAddressLine1(), emptyToNull(pdaOffice.getAddressLine1()))
               && equals(office.getAddress().getAddressLine2(), emptyToNull(pdaOffice.getAddressLine2()))
               && equals(office.getAddress().getAddressLine3(), emptyToNull(pdaOffice.getAddressLine3()))
               && equals(office.getAddress().getCity(), emptyToNull(pdaOffice.getCity()))
               && equals(office.getAddress().getPostcode(), emptyToNull(pdaOffice.getPostcode()));
    }

    private boolean equals(String s1, String s2) {
        if (s1 == null) {
            return s2 == null;
        }
        return s1.equals(s2);
    }

    private String emptyToNull(String value) {
        return (value == null || value.trim().isEmpty()) ? null : value;
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

        // Check if application is shutting down before starting any work
        if (shuttingDown.get()) {
            log.warn("PDA sync aborted - application is shutting down");
            PdaSyncResultDto shutdownResult = PdaSyncResultDto.builder().build();
            shutdownResult.addWarning("Sync aborted - application is shutting down");
            return CompletableFuture.completedFuture(shutdownResult);
        }

        try {
            // Use TransactionTemplate to ensure proper transaction boundary
            // @Transactional doesn't work when called from same class due to proxy bypass
            PdaSyncResultDto result = transactionTemplate.execute(status -> {
                // Set bypass flag inside transaction - using SET (not SET LOCAL) so it persists through COMMIT
                // Constraint triggers fire during COMMIT and will see this setting
                enableFirmOfficeCheckBypass();

                // Re-check shutdown flag inside transaction
                if (shuttingDown.get()) {
                    log.warn("PDA sync aborted during transaction - application is shutting down");
                    status.setRollbackOnly();
                    PdaSyncResultDto shutdownResult = PdaSyncResultDto.builder().build();
                    shutdownResult.addWarning("Sync aborted during transaction - application is shutting down");
                    return shutdownResult;
                }

                try {
                    return synchronizeWithPda();
                } catch (Exception e) {
                    // Check if error is due to shutdown (connection closed, etc.)
                    String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                    if (shuttingDown.get() || errorMsg.contains("connection") && errorMsg.contains("closed")) {
                        log.warn("PDA sync interrupted by shutdown: {}", e.getMessage());
                        status.setRollbackOnly();
                        PdaSyncResultDto shutdownResult = PdaSyncResultDto.builder().build();
                        shutdownResult.addWarning("Sync interrupted by application shutdown");
                        return shutdownResult;
                    }

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
                log.info("Async PDA synchronization completed - Firms: {} created, {} updated, {} reactivated, {} disabled | Offices: {} created, {} updated, {} deleted",
                    result.getFirmsCreated(), result.getFirmsUpdated(), result.getFirmsReactivated(), result.getFirmsDisabled(),
                    result.getOfficesCreated(), result.getOfficesUpdated(), result.getOfficesDeleted());
            }

            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("Async PDA synchronization failed with exception", e);
            PdaSyncResultDto errorResult = PdaSyncResultDto.builder().build();
            errorResult.addError("Async synchronization failed: " + e.getMessage());
            return CompletableFuture.completedFuture(errorResult);
        } finally {
            // Always reset the bypass flag after sync completes in its own transaction
            try {
                transactionTemplate.execute(status -> {
                    resetFirmOfficeCheckBypass();
                    return null;
                });
            } catch (Exception e) {
                log.error("Failed to reset bypass flag: {}", e.getMessage());
            }
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
        PdaSyncResultDto result = PdaSyncResultDto.builder().build();

        // Detailed delta tracking counters
        int firmUpdatesNameOnly = 0;
        int firmUpdatesParentOnly = 0;
        int firmUpdatesNameAndParent = 0;
        int firmUpdatesNameSkipped = 0;
        int firmUpdatesParentSet = 0;
        int firmUpdatesParentCleared = 0;
        int firmUpdatesParentChanged = 0;
        int officeUpdatesAddressOnly = 0;
        int officeUpdatesFirmOnly = 0;
        int officeUpdatesBoth = 0;
        int officeUpdatesAddressLine1 = 0;
        int officeUpdatesAddressLine2 = 0;
        int officeUpdatesAddressLine3 = 0;
        int officeUpdatesCity = 0;
        int officeUpdatesPostcode = 0;
        int officesSwitchedFirm = 0;
        int userAssociationsDeletedFirmSwitch = 0;
        int userAssociationsDeletedOfficeDeleted = 0;
        Set<UUID> officeIdsWithFirmSwitch = new HashSet<>();

        try {
            // Defer constraint checking to allow firms to be created before their offices
            // The check_firms_have_office constraint will be checked at transaction commit
            // Use EntityManager to ensure it's on the same connection as the transaction
            entityManager.createNativeQuery("SET CONSTRAINTS ALL DEFERRED").executeUpdate();

            // Fetch and build PDA data maps
            Table pdaTable = getProviderOfficesSnapshot();
            Map<String, PdaFirmData> pdaFirms = buildPdaFirmsMap(pdaTable);
            Map<String, PdaOfficeData> pdaOffices = buildPdaOfficesMap(pdaTable);

            // Perform data integrity checks
            checkDataIntegrity(pdaFirms, pdaOffices, result);

            // Get current database state (optimized with fetch join)
            Map<String, Firm> dbFirms = new HashMap<>();
            firmRepository.findAllWithParentFirm().forEach(f -> {
                if (f.getCode() != null) {
                    dbFirms.put(f.getCode(), f);
                }
            });

            // Track processed codes
            Set<String> processedFirmCodes = new HashSet<>();

            // Determine which firms have offices (database constraint requires this)
            Set<String> firmsWithOffices = new HashSet<>();
            for (PdaOfficeData office : pdaOffices.values()) {
                if (office.getFirmNumber() != null) {
                    firmsWithOffices.add(office.getFirmNumber());
                }
            }

            // Count firms without offices
            int firmsWithoutOffices = (int) pdaFirms.keySet().stream()
                .filter(code -> !firmsWithOffices.contains(code))
                .count();

            // PASS 1: Process firms - create or update (without parent references for new firms)
            // ONLY process firms that have at least one office (database constraint requirement)
            for (Map.Entry<String, PdaFirmData> entry : pdaFirms.entrySet()) {
                // Check for shutdown before processing each firm
                if (shuttingDown.get()) {
                    log.warn("Firm processing aborted - application is shutting down");
                    result.addWarning("Firm processing aborted - application shutdown detected");
                    return result;
                }

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
                    // Track what will be updated before calling updateFirm
                    int beforeUpdates = result.getFirmsUpdated();
                    int beforeReactivated = result.getFirmsReactivated();
                    int beforeWarnings = result.getWarnings().size();

                    boolean nameWillChange = !dbFirm.getName().equals(pdaFirm.getFirmName());
                    String currentParentCode = dbFirm.getParentFirm() != null ? dbFirm.getParentFirm().getCode() : null;
                    String rawNewParentCode = (pdaFirm.getParentFirmNumber() != null
                        && !pdaFirm.getParentFirmNumber().trim().isEmpty()
                        && !pdaFirm.getParentFirmNumber().trim().equalsIgnoreCase("null"))
                        ? pdaFirm.getParentFirmNumber().trim() : null;

                    // Apply same validation rules as UpdateFirmCommand to determine effective parent
                    String effectiveNewParentCode = rawNewParentCode;
                    if (rawNewParentCode != null) {
                        Firm parentFirm = dbFirms.get(rawNewParentCode);
                        if (parentFirm == null || parentFirm.getType() == FirmType.ADVOCATE || parentFirm.getParentFirm() != null) {
                            effectiveNewParentCode = null;
                        }
                    }

                    boolean parentWillChange = !equals(currentParentCode, effectiveNewParentCode);

                    updateFirm(dbFirm, pdaFirm, dbFirms, result);

                    // Analyze results to track detailed breakdowns
                    int afterUpdates = result.getFirmsUpdated();
                    int afterWarnings = result.getWarnings().size();
                    boolean wasUpdated = (afterUpdates > beforeUpdates);
                    boolean wasReactivated = (result.getFirmsReactivated() > beforeReactivated);
                    boolean nameUpdateWasSkipped = false;

                    // Check if name update was skipped (warning added)
                    if (afterWarnings > beforeWarnings && nameWillChange) {
                        String lastWarning = result.getWarnings().get(result.getWarnings().size() - 1);
                        if (lastWarning.contains("Duplicate firm name") && lastWarning.contains(firmCode)) {
                            nameUpdateWasSkipped = true;
                            firmUpdatesNameSkipped++;
                        }
                    }

                    if (wasUpdated && !wasReactivated) {
                        // Track update breakdown
                        boolean nameActuallyUpdated = nameWillChange && !nameUpdateWasSkipped;

                        if (nameActuallyUpdated && parentWillChange) {
                            firmUpdatesNameAndParent++;
                        } else if (nameActuallyUpdated) {
                            firmUpdatesNameOnly++;
                        } else if (parentWillChange) {
                            firmUpdatesParentOnly++;
                        }

                        // Track parent change type
                        if (parentWillChange) {
                            if (currentParentCode == null && effectiveNewParentCode != null) {
                                firmUpdatesParentSet++;
                            } else if (currentParentCode != null && effectiveNewParentCode == null) {
                                firmUpdatesParentCleared++;
                            } else if (currentParentCode != null && effectiveNewParentCode != null) {
                                firmUpdatesParentChanged++;
                            }
                        }
                    }
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
            Set<String> firmsToDeactivate = new HashSet<>();
            for (String firmCode : dbFirms.keySet()) {
                if (!processedFirmCodes.contains(firmCode)) {
                    log.debug("Marking firm {} for deactivation - not in PDA data", firmCode);
                    firmsToDeactivate.add(firmCode);
                } else if (!firmsWithOffices.contains(firmCode)) {
                    log.debug("Marking firm {} for deactivation - no offices in PDA data (database constraint)", firmCode);
                    firmsToDeactivate.add(firmCode);
                }
            }

            // Deactivate/delete firms - only process firms that are currently enabled
            List<Firm> firmsToDisable = firmsToDeactivate.stream()
                .map(dbFirms::get)
                .filter(firm -> firm != null && firm.getEnabled())
                .collect(Collectors.toList());

            if (!firmsToDisable.isEmpty()) {
                log.info("Disabling {} enabled firms (out of {} total to deactivate)",
                    firmsToDisable.size(), firmsToDeactivate.size());
                for (Firm firm : firmsToDisable) {
                    deactivateFirm(firm, result);
                }
            } else {
                log.debug("No enabled firms to deactivate (all {} already disabled)", firmsToDeactivate.size());
            }

            // If errors occurred during deactivation, stop immediately
            if (!result.getErrors().isEmpty()) {
                log.error("Stopping synchronization due to {} errors during firm deactivation", result.getErrors().size());
                return result;
            }

            // Flush Pass 1 changes (firm creation/updates/deactivations) before setting parent references
            entityManager.flush();
            entityManager.clear(); // Clear persistence context to free memory

            // Reload firms after changes (optimized with fetch join)
            dbFirms.clear();
            firmRepository.findAllWithParentFirm().forEach(f -> {
                if (f.getCode() != null) {
                    dbFirms.put(f.getCode(), f);
                }
            });

            // PASS 2: Update parent firm references for newly created firms
            for (Map.Entry<String, PdaFirmData> entry : pdaFirms.entrySet()) {
                String firmCode = entry.getKey();
                PdaFirmData pdaFirm = entry.getValue();

                if (pdaFirm.getParentFirmNumber() != null
                    && !pdaFirm.getParentFirmNumber().isEmpty()
                    && !pdaFirm.getParentFirmNumber().trim().equalsIgnoreCase("null")) {
                    Firm firm = dbFirms.get(firmCode);
                    if (firm != null) {
                        String currentParentCode = firm.getParentFirm() != null ? firm.getParentFirm().getCode() : null;

                        // Only update if parent is missing or different
                        if (!pdaFirm.getParentFirmNumber().equals(currentParentCode)) {
                            Firm parentFirm = dbFirms.get(pdaFirm.getParentFirmNumber());
                            if (parentFirm != null) {
                                // Validate parent firm before setting
                                if (parentFirm.getType() == FirmType.ADVOCATE) {
                                    log.warn("Parent firm {} is ADVOCATE type for firm {} - setting to null (ADVOCATE firms cannot be parents)",
                                        pdaFirm.getParentFirmNumber(), firmCode);
                                    firm.setParentFirm(null);
                                    firmRepository.save(firm);
                                    result.addWarning("Parent firm " + pdaFirm.getParentFirmNumber()
                                        + " is ADVOCATE type and cannot be a parent for firm " + firmCode);
                                } else if (parentFirm.getParentFirm() != null) {
                                    log.warn("Parent firm {} already has parent {} for firm {} - setting to null (multi-level hierarchy not allowed)",
                                        pdaFirm.getParentFirmNumber(), parentFirm.getParentFirm().getCode(), firmCode);
                                    firm.setParentFirm(null);
                                    firmRepository.save(firm);
                                    result.addWarning("Parent firm " + pdaFirm.getParentFirmNumber()
                                        + " already has parent " + parentFirm.getParentFirm().getCode()
                                        + " - multi-level hierarchy not allowed for firm " + firmCode);
                                } else {
                                    try {
                                        firm.setParentFirm(parentFirm);
                                        firmRepository.save(firm);
                                        log.debug("Set parent for firm {}: {} -> {}", firmCode, currentParentCode, pdaFirm.getParentFirmNumber());
                                    } catch (Exception e) {
                                        log.error("Failed to set parent {} for firm {}: {} - clearing parent reference",
                                            pdaFirm.getParentFirmNumber(), firmCode, e.getMessage());
                                        firm.setParentFirm(null);
                                        firmRepository.save(firm);
                                        result.addWarning("Invalid parent firm " + pdaFirm.getParentFirmNumber()
                                            + " for firm " + firmCode + ": " + e.getMessage());
                                    }
                                }
                            } else {
                                log.info("Parent firm {} not found for firm {} - clearing parent reference",
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
            entityManager.flush();
            entityManager.clear(); // Clear persistence context to free memory

            // Get DB offices (optimized with fetch join)
            Map<String, Office> dbOffices = new HashMap<>();
            officeRepository.findAllWithFirm().forEach(o -> {
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

            // Also identify orphaned offices in PDA data (parent firm doesn't exist in DB)
            // These should be deactivated if they exist in DB, or skipped if they don't
            final Set<String> orphanedOfficeCodes = new HashSet<>();
            for (Map.Entry<String, PdaOfficeData> entry : pdaOffices.entrySet()) {
                String officeCode = entry.getKey();
                PdaOfficeData pdaOffice = entry.getValue();
                Firm parentFirm = dbFirms.get(pdaOffice.getFirmNumber());

                if (parentFirm == null) {
                    orphanedOfficeCodes.add(officeCode);
                    // If this office exists in DB, mark it for deactivation
                    if (dbOffices.containsKey(officeCode)) {
                        officesToDeactivate.add(officeCode);
                        log.warn("Office {} is orphaned (parent firm {} not found) - will be deactivated",
                            officeCode, pdaOffice.getFirmNumber());
                        result.addWarning("Office " + officeCode + " orphaned (parent firm "
                            + pdaOffice.getFirmNumber() + " not found) - will be deactivated");
                    } else {
                        log.debug("Office {} is orphaned (parent firm {} not found) and doesn't exist in DB - skipping",
                            officeCode, pdaOffice.getFirmNumber());
                    }
                }
            }

            // PASS 3: Process offices - create new ones and update existing ones (but not those being deactivated)
            for (Map.Entry<String, PdaOfficeData> entry : pdaOffices.entrySet()) {
                // Check for shutdown before processing each office
                if (shuttingDown.get()) {
                    log.warn("Office processing aborted - application is shutting down");
                    result.addWarning("Office processing aborted - application shutdown detected");
                    return result;
                }

                String officeCode = entry.getKey();
                PdaOfficeData pdaOffice = entry.getValue();
                processedOfficeCodes.add(officeCode);

                final Office dbOffice = dbOffices.get(officeCode);
                Firm parentFirm = dbFirms.get(pdaOffice.getFirmNumber());

                // Skip orphaned offices - they're already marked for deactivation
                if (orphanedOfficeCodes.contains(officeCode)) {
                    continue;
                }

                // This should not happen now, but defensive check
                if (parentFirm == null) {
                    log.error("Office {} parent firm {} not found - this should have been caught earlier",
                        officeCode, pdaOffice.getFirmNumber());
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

                    boolean firmWillChange = !dbOffice.getFirm().getId().equals(parentFirm.getId());
                    boolean addressWillChange = !isSameAddress(dbOffice, pdaOffice);

                    // Track address field changes
                    if (addressWillChange && dbOffice.getAddress() != null) {
                        if (!equals(dbOffice.getAddress().getAddressLine1(), emptyToNull(pdaOffice.getAddressLine1()))) {
                            officeUpdatesAddressLine1++;
                        }
                        if (!equals(dbOffice.getAddress().getAddressLine2(), emptyToNull(pdaOffice.getAddressLine2()))) {
                            officeUpdatesAddressLine2++;
                        }
                        if (!equals(dbOffice.getAddress().getAddressLine3(), emptyToNull(pdaOffice.getAddressLine3()))) {
                            officeUpdatesAddressLine3++;
                        }
                        if (!equals(dbOffice.getAddress().getCity(), emptyToNull(pdaOffice.getCity()))) {
                            officeUpdatesCity++;
                        }
                        if (!equals(dbOffice.getAddress().getPostcode(), emptyToNull(pdaOffice.getPostcode()))) {
                            officeUpdatesPostcode++;
                        }
                    }

                    if (firmWillChange) {
                        officesSwitchedFirm++;
                        officeIdsWithFirmSwitch.add(dbOffice.getId());
                    }

                    // Track what will be updated before calling updateOffice
                    int beforeUpdates = result.getOfficesUpdated();
                    updateOffice(dbOffice, pdaOffice, parentFirm, result);

                    // Track update type breakdown
                    int afterUpdates = result.getOfficesUpdated();
                    if (afterUpdates > beforeUpdates) {
                        if (addressWillChange && firmWillChange) {
                            officeUpdatesBoth++;
                        } else if (addressWillChange) {
                            officeUpdatesAddressOnly++;
                        } else if (firmWillChange) {
                            officeUpdatesFirmOnly++;
                        }
                    }
                }

                // Check for errors after each office operation
                if (!result.getErrors().isEmpty()) {
                    log.error("Stopping synchronization due to {} errors during office processing", result.getErrors().size());
                    return result;
                }
            }

            // Batch query for user associations affected by firm switches
            if (!officeIdsWithFirmSwitch.isEmpty()) {
                log.debug("Batch querying user associations for {} offices that switched firms", officeIdsWithFirmSwitch.size());
                List<Object[]> associationCounts = userProfileRepository.countAssociationsByOfficeIds(new ArrayList<>(officeIdsWithFirmSwitch));

                for (Object[] row : associationCounts) {
                    Number count = (Number) row[1];
                    userAssociationsDeletedFirmSwitch += count.intValue();
                }
                log.debug("Deleted {} user associations due to firm switches", userAssociationsDeletedFirmSwitch);
            }

            // Check for errors before deactivating offices
            if (!result.getErrors().isEmpty()) {
                log.error("Stopping synchronization due to {} errors, skipping office deactivation", result.getErrors().size());
                return result;
            }

            // Now deactivate offices that are not in PDA data (batch operation for performance)
            if (!officesToDeactivate.isEmpty()) {
                log.info("Starting batch office deletion for {} offices", officesToDeactivate.size());

                List<Office> officesToDelete = officesToDeactivate.stream()
                    .map(dbOffices::get)
                    .filter(office -> office != null)
                    .collect(Collectors.toList());

                if (!officesToDelete.isEmpty()) {
                    log.debug("Filtered to {} valid office entities for deletion", officesToDelete.size());

                    // Collect all office IDs
                    List<UUID> officeIds = officesToDelete.stream()
                        .map(Office::getId)
                        .collect(Collectors.toList());

                    log.debug("Querying user profiles for {} office IDs using batch query", officeIds.size());

                    // Count associations using native query to avoid N+1 lazy loads
                    List<Object[]> associationCounts = userProfileRepository.countAssociationsByOfficeIds(officeIds);

                    for (Object[] row : associationCounts) {
                        Number count = (Number) row[1];
                        userAssociationsDeletedOfficeDeleted += count.intValue();
                    }

                    // Batch query: Get all user profiles associated with any of these offices in a single query
                    List<UserProfile> affectedProfiles = userProfileRepository.findByOfficeIdIn(officeIds);

                    log.debug("Found {} unique user profiles associated with offices to delete", affectedProfiles.size());

                    // Remove all office associations in memory
                    if (!affectedProfiles.isEmpty()) {
                        log.debug("Removing office associations from {} user profiles", affectedProfiles.size());
                        // Now remove the associations
                        for (UserProfile profile : affectedProfiles) {
                            profile.getOffices().removeAll(officesToDelete);
                        }
                        // Batch save all modified profiles
                        log.debug("Batch saving {} user profiles", affectedProfiles.size());
                        userProfileRepository.saveAll(affectedProfiles);
                        log.info("Removed {} offices from {} user profiles", officesToDelete.size(), affectedProfiles.size());
                    } else {
                        log.debug("No user profiles affected by office deletions");
                    }

                    // Batch delete all offices
                    log.debug("Executing batch delete for {} offices", officesToDelete.size());
                    officeRepository.deleteAll(officesToDelete);
                    result.setOfficesDeleted(result.getOfficesDeleted() + officesToDelete.size());
                    log.info("Successfully batch deleted {} offices", officesToDelete.size());
                }
            }

            // Flush all changes and verify constraint compliance before commit
            entityManager.flush();
            entityManager.clear(); // Clear cache to get fresh data

            // FINAL SAFETY CHECK: Verify no ENABLED firms exist without offices
            // This catches edge cases where office operations failed silently
            // Query already filters for enabled=true firms
            List<Firm> firmsStillWithoutOffices = firmRepository.findFirmsWithoutOffices();

            if (!firmsStillWithoutOffices.isEmpty()) {
                log.info("Found {} ENABLED firms without offices - disabling to prevent constraint violation", firmsStillWithoutOffices.size());

                // Batch disable all firms without offices (double-check they're enabled)
                List<Firm> firmsThatNeedDisabling = firmsStillWithoutOffices.stream()
                    .filter(Firm::getEnabled)
                    .collect(Collectors.toList());

                if (!firmsThatNeedDisabling.isEmpty()) {
                    for (Firm firm : firmsThatNeedDisabling) {
                        firm.setEnabled(false);
                        // Clear parent/child relationships
                        if (firm.getParentFirm() != null) {
                            firm.setParentFirm(null);
                        }
                        if (firm.getChildFirms() != null && !firm.getChildFirms().isEmpty()) {
                            for (Firm childFirm : firm.getChildFirms()) {
                                childFirm.setParentFirm(null);
                            }
                            firm.getChildFirms().clear();
                        }
                    }

                    // Batch save all disabled firms
                    firmRepository.saveAll(firmsThatNeedDisabling);
                    result.setFirmsDisabled(result.getFirmsDisabled() + firmsThatNeedDisabling.size());
                    log.info("Successfully batch disabled {} firms without offices", firmsThatNeedDisabling.size());
                }
            }

            // Print final delta analysis summary with detailed breakdowns
            StringBuilder summary = new StringBuilder();
            summary.append("\n========================================")
                .append("\nPDA Sync Delta Analysis")
                .append("\n========================================")
                .append("\n\nFirms:")
                .append("\n  Created:         ").append(result.getFirmsCreated())
                .append("\n  Reactivated:     ").append(result.getFirmsReactivated())
                .append("\n  Updated:         ").append(result.getFirmsUpdated());

            if (result.getFirmsUpdated() > 0) {
                summary.append("\n    -> ").append(firmUpdatesNameOnly).append(" with name changes only")
                    .append("\n    -> ").append(firmUpdatesParentOnly).append(" with parent firm changes only")
                    .append("\n    -> ").append(firmUpdatesNameAndParent).append(" with both name and parent changes");
                if (firmUpdatesParentSet + firmUpdatesParentCleared + firmUpdatesParentChanged > 0) {
                    summary.append("\n        * ").append(firmUpdatesParentSet).append(" setting parent (null -> parent)")
                        .append("\n        * ").append(firmUpdatesParentCleared).append(" clearing parent (parent -> null)")
                        .append("\n        * ").append(firmUpdatesParentChanged).append(" changing parent (parent A -> parent B)");
                }
                if (firmUpdatesNameSkipped > 0) {
                    summary.append("\n    -> ").append(firmUpdatesNameSkipped).append(" with name changes skipped (duplicate name exists)");
                }
            }

            summary.append("\n  Disabled:        ").append(result.getFirmsDisabled())
                .append("\n\nOffices:")
                .append("\n  Created:         ").append(result.getOfficesCreated())
                .append("\n  Updated:         ").append(result.getOfficesUpdated());

            if (result.getOfficesUpdated() > 0) {
                summary.append("\n    -> ").append(officeUpdatesAddressOnly).append(" with address changes only");
                if (officeUpdatesAddressOnly > 0) {
                    summary.append("\n        * ").append(officeUpdatesAddressLine1).append(" address line 1 changes")
                        .append("\n        * ").append(officeUpdatesAddressLine2).append(" address line 2 changes")
                        .append("\n        * ").append(officeUpdatesAddressLine3).append(" address line 3 changes")
                        .append("\n        * ").append(officeUpdatesCity).append(" city changes")
                        .append("\n        * ").append(officeUpdatesPostcode).append(" postcode changes");
                }
                summary.append("\n    -> ").append(officeUpdatesFirmOnly).append(" with firm changes only")
                    .append("\n    -> ").append(officeUpdatesBoth).append(" with both address and firm changes");
            }

            summary.append("\n  Switched firm:   ").append(officesSwitchedFirm);
            if (officesSwitchedFirm > 0) {
                summary.append("\n    -> ").append(userAssociationsDeletedFirmSwitch).append(" user associations deleted due to firm switch");
            }

            summary.append("\n  Deleted:         ").append(result.getOfficesDeleted());
            if (result.getOfficesDeleted() > 0) {
                summary.append("\n    -> ").append(userAssociationsDeletedOfficeDeleted).append(" user associations deleted due to office deletion");
            }

            log.info(summary.toString());

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
                .officeAccountNo(getStringValue(pdaTable, "officeAccountNumber", i))
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

    private void updateFirm(Firm firm, PdaFirmData pdaFirm, Map<String, Firm> firmsByCode, PdaSyncResultDto result) {
        new UpdateFirmCommand(firmRepository, firm, pdaFirm, firmsByCode).execute(result);
    }

    private void deactivateFirm(Firm firm, PdaSyncResultDto result) {
        new DisableFirmCommand(firmRepository, firm).execute(result);
    }

    private void createOffice(PdaOfficeData pdaOffice, Firm firm, PdaSyncResultDto result) {
        new CreateOfficeCommand(officeRepository, pdaOffice, firm).execute(result);
    }

    private void updateOffice(Office office, PdaOfficeData pdaOffice, Firm firm, PdaSyncResultDto result) {
        new UpdateOfficeCommand(officeRepository, userProfileRepository, office, pdaOffice, firm).execute(result);
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
        final int initialFirms = pdaFirms.size();
        final int initialOffices = pdaOffices.size();

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
            log.debug("Data integrity: removed {} firms, {} offices", removedFirms, removedOffices);
        }
    }
}
