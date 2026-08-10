package uk.gov.justice.laa.portal.landingpage.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppSyncResultDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppSynchronizationAuditEvent;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppType;
import uk.gov.justice.laa.portal.landingpage.forms.AppsOrderForm;
import uk.gov.justice.laa.portal.landingpage.repository.AppRepository;
import uk.gov.justice.laa.portal.landingpage.techservices.GetAllApplicationsResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.TechServicesApiResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppService {

    @Value("${feature.flag.enable.app.sync.from.entra}")
    private String syncAppsFromEntra;

    @Value("${feature.flag.enable.app.updates.sync.from.entra}")
    private String syncAppUpdatesFromEntra;

    private final AppRepository appRepository;

    private final TechServicesClient techServicesClient;

    private final ModelMapper mapper;
    private final EventService eventService;
    private final PlatformTransactionManager transactionManager;

    public Optional<App> getById(UUID id) {
        return appRepository.findById(id);
    }

    public Optional<AppDto> findById(String id) {
        return findById(UUID.fromString(id));
    }

    public Optional<AppDto> findById(UUID id) {
        return appRepository.findById(id).map(app -> mapper.map(app, AppDto.class));
    }

    public List<AppDto> findAll() {
        return appRepository.findAll().stream()
                .map(app -> mapper.map(app, AppDto.class))
                .toList();
    }

    public List<App> getAllLaaAppEntities() {
        return appRepository.findAppsByAppType(AppType.LAA)
                .stream()
                .toList();
    }

    public List<AppDto> getAllLaaApps() {
        return appRepository.findAppsByAppType(AppType.LAA)
                .stream()
                .map(app -> mapper.map(app, AppDto.class))
                .sorted()
                .toList();
    }

    public List<AppDto> getAllAuthzApps() {
        return appRepository.findAppsByAppType(AppType.AUTHZ)
                .stream()
                .map(app -> mapper.map(app, AppDto.class))
                .sorted()
                .toList();
    }

    public List<AppDto> getAllActiveLaaApps() {
        return appRepository.findAppsByAppTypeAndEnabled(AppType.LAA, true)
                .stream()
                .map(app -> mapper.map(app, AppDto.class))
                .toList();
    }

    public List<AppDto> getAllActiveAuthzApps() {
        return appRepository.findAppsByAppTypeAndEnabled(AppType.AUTHZ, true)
                .stream()
                .map(app -> mapper.map(app, AppDto.class))
                .sorted(Comparator.comparingInt(AppDto::getOrdinal))
                .toList();
    }

    @Transactional
    public App save(AppDto appDto) {
        App app = getById(UUID.fromString(appDto.getId()))
                .orElseThrow(() -> new RuntimeException(String.format("App not found for the give app id: %s", appDto.getId())));
        app.setEnabled(appDto.isEnabled());
        app.setDescription(appDto.getDescription());
        return appRepository.save(app);
    }

    public List<AppDto> updateAppsOrder(@Valid @NotNull List<AppsOrderForm.AppOrderDetailsForm> apps) {
        Map<String, Integer> idToOrdinal = apps.stream().collect(Collectors.toMap(AppsOrderForm.AppOrderDetailsForm::getAppId, AppsOrderForm.AppOrderDetailsForm::getOrdinal, (a, b) -> b));
        List<App> appsList = getAllLaaAppEntities();
        appsList.forEach(app -> {
            if (idToOrdinal.containsKey(app.getId().toString())) {
                app.setOrdinal(idToOrdinal.get(app.getId().toString()));
            }
        });
        return appRepository.saveAll(appsList).stream()
                .map(app -> mapper.map(app, AppDto.class))
                .sorted()
                .toList();
    }

    public AppSyncResultDto synchronizeAndGetApplicationsFromTechServices(CurrentUserDto currentUserDto, UserProfileDto userProfile) {
        log.info("Synchronizing applications from Tech Services...");

        if (!Boolean.parseBoolean(syncAppsFromEntra)) {
            log.info("Synchronizing applications has been disabled. App syncing not performed.");
            List<AppDto> result = getAllLaaApps();
            result.forEach(app -> app.setChangeType(AppDto.ChangeType.NONE));
            return AppSyncResultDto.builder().apps(result).build();
        }

        TechServicesApiResponse<GetAllApplicationsResponse> apiResponse = techServicesClient.getAllApplications();
        if (!apiResponse.isSuccess()) {
            String err = apiResponse.getError() != null ? apiResponse.getError().getMessage() : "Unknown error";
            log.error("Error synchronizing applications from Tech Services: {}", err);
            throw new RuntimeException(err);
        }

        List<GetAllApplicationsResponse.TechServicesApplication> remoteApps =
                Optional.ofNullable(apiResponse.getData())
                        .map(GetAllApplicationsResponse::getApps)
                        .orElseGet(List::of);

        List<App> localApps = Optional.ofNullable(getAllLaaAppEntities()).orElseGet(List::of);

        Map<String, GetAllApplicationsResponse.TechServicesApplication> remoteById = remoteApps.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(GetAllApplicationsResponse.TechServicesApplication::getAppId,
                        a -> a, (a, b) -> a));

        Map<String, App> localById = localApps.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(App::getEntraAppId, a -> a, (a, b) -> a));
        int maxOrdinal = localApps.stream().mapToInt(App::getOrdinal).max().orElse(0);

        Set<String> allIds = new HashSet<>();
        allIds.addAll(remoteById.keySet());
        allIds.addAll(localById.keySet());

        // Duplicate counts computed across the whole remote batch, so two clashing apps are both flagged
        Map<String, Long> securityGroupOidCounts = countByRemoteField(remoteApps, this::remoteSecurityGroupOid);
        Map<String, Long> appOidCounts = countByRemoteField(remoteApps, GetAllApplicationsResponse.TechServicesApplication::getAppId);
        Map<String, Long> securityGroupNameCounts = countByRemoteField(remoteApps, this::remoteSecurityGroupName);

        int totalProcessed = 0;
        int noChanges = 0;
        int newApps = 0;
        int updatedApps = 0;
        int deletedApps = 0;

        List<AppDto> result = new ArrayList<>(allIds.size());
        AppSyncResultDto syncResult = AppSyncResultDto.builder().apps(result).build();

        for (String id : allIds) {
            GetAllApplicationsResponse.TechServicesApplication remote = remoteById.get(id);
            App local = localById.get(id);
            AppDto syncedApp;

            if (!Boolean.parseBoolean(syncAppUpdatesFromEntra) && local != null) {
                log.info("Synchronizing app updates has been disabled. Entra app updates not synchronized with local.");
                syncedApp = toDtoWithChangeType(local, AppDto.ChangeType.NONE);
                totalProcessed++;
                noChanges++;
                result.add(syncedApp);
                continue;
            }

            if (remote != null && local != null) {
                AppDto.ChangeType changeType = getChangeType(remote, local);
                switch (changeType) {
                    case REVIEW:
                    case UPDATED:
                        Optional<String> validationError = validateRemoteApp(remote, securityGroupOidCounts, appOidCounts, securityGroupNameCounts);
                        if (validationError.isPresent()) {
                            syncResult.addError(buildErrorMessage(remote, validationError.get()));
                            syncedApp = toDtoWithChangeType(local, AppDto.ChangeType.NONE);
                            log.warn("SKIPPED: Invalid remote app data (id={}, name={}): {}", id, safe(remote.getName()), validationError.get());
                        } else {
                            applyRemoteFieldsToLocal(remote, local);
                            persistApp(local, syncResult, remote);
                            syncedApp = toDtoWithChangeType(local, changeType);
                            updatedApps++;
                            log.info("{}: Applied remote updates (id={}, name={})", changeType, id, safe(remote.getName()));
                        }
                        break;

                    case NONE:
                        syncedApp = toDtoWithChangeType(local, changeType);
                        noChanges++;
                        log.info("NONE: No changes for app (id={}, name={})", id, safe(local.getName()));
                        break;
                    default:
                        throw new RuntimeException("Unknown change type: " + changeType);
                }
                totalProcessed++;

            } else if (remote != null) {
                Optional<String> validationError = validateRemoteApp(remote, securityGroupOidCounts, appOidCounts, securityGroupNameCounts);
                if (validationError.isPresent()) {
                    syncResult.addError(buildErrorMessage(remote, validationError.get()));
                    log.warn("SKIPPED: Invalid new remote app data (app id={}, name={}): {}", remote.getAppId(), safe(remote.getName()), validationError.get());
                    totalProcessed++;
                    continue;
                }
                App newApp = createLocalFromRemote(remote, ++maxOrdinal);
                persistApp(newApp, syncResult, remote);
                syncedApp = toDtoWithChangeType(newApp, AppDto.ChangeType.ADDED);
                newApps++;
                totalProcessed++;
                log.info("ADDED: New app added to DB (oid={}, app id={} and name={})", remote.getId(), remote.getAppId(), safe(remote.getName()));
                result.add(syncedApp);
                continue;

            } else {
                assert local != null;
                if (local.isEnabled()) {
                    local.setEnabled(false);
                    persistApp(local, syncResult, null);
                    syncedApp = toDtoWithChangeType(local, AppDto.ChangeType.DELETED);
                    deletedApps++;
                    log.info("DELETED: App missing from remote; disabled locally (id={}, name={})", id, safe(local.getName()));
                } else {
                    noChanges++;
                    syncedApp = toDtoWithChangeType(local, AppDto.ChangeType.NONE);
                    log.info("Already DELETED: No changes for app (id={}, name={})", id, safe(local.getName()));
                }
                totalProcessed++;
            }

            result.add(syncedApp);
        }

        log.info("Finished synchronization. Total: {}, No changes: {}, New: {}, Updated: {}, Deleted: {}, Errors: {}",
                totalProcessed, noChanges, newApps, updatedApps, deletedApps, syncResult.getErrors().size());

        String auditMessage = String.format(
                "Total apps processed: %s, No changes: %s, New apps: %s, Updated apps: %s, Deleted apps: %s, Errors: %s",
                totalProcessed, noChanges, newApps, updatedApps, deletedApps, syncResult.getErrors().size()
        );
        AppSynchronizationAuditEvent auditEvent =
                new AppSynchronizationAuditEvent(currentUserDto, userProfile.getId(), auditMessage);
        eventService.logEvent(auditEvent);

        syncResult.setApps(result.stream().sorted().toList());
        return syncResult;
    }

    /**
     * Persists a single app change in its own transaction so one bad row cannot roll back others.
     */
    private void persistApp(App app, AppSyncResultDto syncResult, GetAllApplicationsResponse.TechServicesApplication remote) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        try {
            transactionTemplate.executeWithoutResult(status -> appRepository.save(app));
        } catch (DataAccessException e) {
            log.error("Failed to persist app (name={}): {}", safe(app.getName()), e.getMessage());
            syncResult.addError(remote != null
                    ? buildErrorMessage(remote, "Failed to save app: " + e.getMessage())
                    : String.format("Failed to save app '%s': %s", safe(app.getName()), e.getMessage()));
        }
    }

    private String buildErrorMessage(GetAllApplicationsResponse.TechServicesApplication remote, String reason) {
        return String.format("App '%s' (app id: %s): %s", safe(remote.getName()), safe(remote.getAppId()), reason);
    }

    private <T> Map<T, Long> countByRemoteField(List<GetAllApplicationsResponse.TechServicesApplication> remoteApps,
            Function<GetAllApplicationsResponse.TechServicesApplication, T> fieldExtractor) {
        return remoteApps.stream()
                .filter(Objects::nonNull)
                .map(fieldExtractor)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(v -> v, Collectors.counting()));
    }

    private String remoteSecurityGroupOid(GetAllApplicationsResponse.TechServicesApplication remote) {
        var securityGroups = remote.getSecurityGroups();
        return (securityGroups != null && !securityGroups.isEmpty()) ? securityGroups.getFirst().getId() : null;
    }

    private String remoteSecurityGroupName(GetAllApplicationsResponse.TechServicesApplication remote) {
        var securityGroups = remote.getSecurityGroups();
        return (securityGroups != null && !securityGroups.isEmpty()) ? securityGroups.getFirst().getName() : null;
    }

    /**
     * Validates a remote app's data integrity using application logic only (not DB constraints):
     * security group OID, app OID and security group name must each be present, unique across the batch, and OIDs must be valid UUIDs.
     */
    private Optional<String> validateRemoteApp(GetAllApplicationsResponse.TechServicesApplication remote,
            Map<String, Long> securityGroupOidCounts, Map<String, Long> appOidCounts, Map<String, Long> securityGroupNameCounts) {
        String securityGroupOid = remoteSecurityGroupOid(remote);
        String securityGroupName = remoteSecurityGroupName(remote);
        String appOid = remote.getAppId();

        if (StringUtils.isBlank(securityGroupOid)) {
            return Optional.of("Security group OID is missing");
        }
        if (StringUtils.isBlank(appOid)) {
            return Optional.of("App OID is missing");
        }
        if (StringUtils.isBlank(securityGroupName)) {
            return Optional.of("Security group name is missing");
        }
        if (!isValidUuid(securityGroupOid)) {
            return Optional.of("Security group OID is not a valid UUID: " + securityGroupOid);
        }
        if (!isValidUuid(appOid)) {
            return Optional.of("App OID is not a valid UUID: " + appOid);
        }
        if (securityGroupOidCounts.getOrDefault(securityGroupOid, 0L) > 1) {
            return Optional.of("Security group OID is duplicated across apps: " + securityGroupOid);
        }
        if (appOidCounts.getOrDefault(appOid, 0L) > 1) {
            return Optional.of("App OID is duplicated across apps: " + appOid);
        }
        if (securityGroupNameCounts.getOrDefault(securityGroupName, 0L) > 1) {
            return Optional.of("Security group name is duplicated across apps: " + securityGroupName);
        }

        return Optional.empty();
    }

    private boolean isValidUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void applyRemoteFieldsToLocal(GetAllApplicationsResponse.TechServicesApplication remote, App local) {
        local.setName(remote.getName());
        if (StringUtils.isEmpty(remote.getUrl())) {
            local.setUrl("#");
            local.setEnabled(false);
        } else {
            local.setUrl(remote.getUrl());
        }

        var securityGroups = remote.getSecurityGroups();

        var appSecurityGroup = (securityGroups != null && !securityGroups.isEmpty())
                ? securityGroups.getFirst()
                : null;

        if (appSecurityGroup == null) {
            applyDefaultSecurityGroup(local);
            return;
        }

        if (appSecurityGroup.getId() == null) {
            local.setSecurityGroupOid(local.getName());
            local.setEnabled(false);
        } else {
            local.setSecurityGroupOid(appSecurityGroup.getId());
        }

    }

    private void applyDefaultSecurityGroup(App local) {
        local.setSecurityGroupOid(local.getName());
        local.setEnabled(false);
    }

    /**
     * Creates a new local App from a remote application; new entries start disabled.
     */
    private App createLocalFromRemote(GetAllApplicationsResponse.TechServicesApplication remote, int ordinal) {
        String sgId = remote.getName();
        String sgName = remote.getName();
        String url = StringUtils.isEmpty(remote.getUrl()) ? "#" : remote.getUrl();

        if (remote.getSecurityGroups() != null && !remote.getSecurityGroups().isEmpty()) {
            var sg = remote.getSecurityGroups().getFirst();
            sgId = sg.getId();
            sgName = sg.getName();
        }

        return App.builder()
                .entraAppId(remote.getAppId())
                .entraOid(remote.getId())
                .name(remote.getName())
                .description(remote.getName())
                .url(url)
                .securityGroupOid(sgId)
                .appType(AppType.LAA)
                .enabled(false)
                .ordinal(ordinal)
                .build();
    }

    private AppDto toDtoWithChangeType(App entity, AppDto.ChangeType changeType) {
        AppDto dto = mapper.map(entity, AppDto.class);
        dto.setChangeType(changeType);
        return dto;
    }

    private String safe(String s) {
        return s == null ? "(null)" : s;
    }

    private AppDto.ChangeType getChangeType(GetAllApplicationsResponse.TechServicesApplication remote, App local) {
        if (remote == null && local != null && local.isEnabled()) {
            return AppDto.ChangeType.DELETED;
        } else if (local == null && remote != null) {
            return AppDto.ChangeType.ADDED;
        } else if (remote != null && !local.isEnabled()) {
            return AppDto.ChangeType.REVIEW;
        } else if (remote != null
                && (!Strings.CS.equals(remote.getName(), local.getName())
                || (!(remote.getUrl() == null && "#".equals(local.getUrl()))
                    && (!Strings.CS.equals(remote.getUrl(), local.getUrl())))
                || !areSecurityGroupsEqual(remote.getSecurityGroups(), local))) {
            return AppDto.ChangeType.UPDATED;
        }

        return AppDto.ChangeType.NONE;
    }

    private boolean areSecurityGroupsEqual(List<GetAllApplicationsResponse.TechServicesApplication.AppSecurityGroup> remoteSecGroups, App local) {
        String remoteSecGroupId = remoteSecGroups == null || remoteSecGroups.isEmpty() ? null : remoteSecGroups.getFirst().getId();
        return Objects.equals(remoteSecGroupId, local.getSecurityGroupOid());
    }

    public LinkedHashMap<AppType, List<AppDto>> buildGroupedApps(List<AppDto> apps) {
        LinkedHashMap<AppType, List<AppDto>> grouped = new LinkedHashMap<>();
        for (AppType type : AppType.values()) {
            List<AppDto> typeApps = apps.stream()
                    .filter(app -> type.equals(app.getAppType()))
                    .toList();
            if (!typeApps.isEmpty()) {
                grouped.put(type, typeApps);
            }
        }
        return grouped;
    }

}
