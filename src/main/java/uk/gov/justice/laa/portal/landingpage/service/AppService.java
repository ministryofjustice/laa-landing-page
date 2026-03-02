package uk.gov.justice.laa.portal.landingpage.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppSynchronizationAuditEvent;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppType;
import uk.gov.justice.laa.portal.landingpage.forms.AppsOrderForm;
import uk.gov.justice.laa.portal.landingpage.repository.AppRepository;
import uk.gov.justice.laa.portal.landingpage.techservices.GetAllApplicationsResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.TechServicesApiResponse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppService {

    private final AppRepository appRepository;

    private final TechServicesClient techServicesClient;

    private final ModelMapper mapper;
    private final EventService eventService;

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

    public List<AppDto> getAllActiveLaaApps() {
        return appRepository.findAppsByAppTypeAndEnabled(AppType.LAA, true)
                .stream()
                .map(app -> mapper.map(app, AppDto.class))
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

    @Transactional
    public List<AppDto> synchronizeAndGetApplicationsFromTechServices(CurrentUserDto currentUserDto, UserProfileDto userProfile) {
        log.info("Synchronizing applications from Tech Services...");

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
                .collect(Collectors.toMap(GetAllApplicationsResponse.TechServicesApplication::getId,
                        a -> a, (a, b) -> a));

        Map<String, App> localById = localApps.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(App::getEntraAppId, a -> a, (a, b) -> a));

        Set<String> allIds = new HashSet<>();
        allIds.addAll(remoteById.keySet());
        allIds.addAll(localById.keySet());

        int totalProcessed = 0;
        int noChanges = 0;
        int newApps = 0;
        int updatedApps = 0;
        int deletedApps = 0;

        List<AppDto> result = new ArrayList<>(allIds.size());
        List<App> modifiedApps = new ArrayList<>();

        for (String id : allIds) {
            GetAllApplicationsResponse.TechServicesApplication remote = remoteById.get(id);
            App local = localById.get(id);
            AppDto syncedApp;

            if (remote != null && local != null) {
                AppDto.ChangeType changeType = getChangeType(remote, local);
                switch (changeType) {
                    case REVIEW:
                        applyRemoteFieldsToLocal(remote, local);
                        modifiedApps.add(local);
                        syncedApp = toDtoWithChangeType(local, changeType);
                        updatedApps++;
                        log.info("REVIEW: Updated local metadata (id={}, name={})", id, safe(remote.getName()));
                        break;

                    case UPDATED:
                        applyRemoteFieldsToLocal(remote, local);
                        modifiedApps.add(local);
                        syncedApp = toDtoWithChangeType(local, changeType);
                        updatedApps++;
                        log.info("UPDATED: Applied remote updates (id={}, name={})", id, safe(remote.getName()));
                        break;

                    default:
                        syncedApp = toDtoWithChangeType(local, changeType);
                        noChanges++;
                        log.info("NONE: No changes for app (id={}, name={})", id, safe(local.getName()));
                        break;
                }
                totalProcessed++;

            } else if (remote != null) {
                App newApp = createLocalFromRemote(remote);
                modifiedApps.add(newApp);
                syncedApp = toDtoWithChangeType(newApp, AppDto.ChangeType.ADDED);
                newApps++;
                totalProcessed++;
                log.info("ADDED: New app added to DB (id={}, name={})", remote.getId(), safe(remote.getName()));

            } else {
                local.setEnabled(false);
                modifiedApps.add(local);
                syncedApp = toDtoWithChangeType(local, AppDto.ChangeType.DELETED);
                deletedApps++;
                totalProcessed++;
                log.info("DELETED: App missing from remote; disabled locally (id={}, name={})", id, safe(local.getName()));
            }

            result.add(syncedApp);
        }

        appRepository.saveAll(modifiedApps);

        log.info("Finished synchronization. Total: {}, No changes: {}, New: {}, Updated: {}, Deleted: {}",
                totalProcessed, noChanges, newApps, updatedApps, deletedApps);

        String auditMessage = String.format(
                "Total apps processed: %s, No changes: %s, New apps: %s, Updated apps: %s, Deleted apps: %s",
                totalProcessed, noChanges, newApps, updatedApps, deletedApps
        );
        AppSynchronizationAuditEvent auditEvent =
                new AppSynchronizationAuditEvent(currentUserDto, userProfile.getId(), auditMessage);
        eventService.logEvent(auditEvent);

        return result.stream().sorted().toList();
    }

    /**
     * Copies fields from remote to local with null safety for security group fields.
     */
    private void applyRemoteFieldsToLocal(GetAllApplicationsResponse.TechServicesApplication remote, App local) {
        local.setName(remote.getName());
        local.setUrl(remote.getUrl());

        // Null-safe extraction of first security group
        Optional.ofNullable(remote.getSecurityGroups())
                .filter(list -> !list.isEmpty())
                .map(list -> list.get(0))
                .ifPresentOrElse(
                        sg -> {
                            local.setSecurityGroupOid(sg.getId());
                            local.setSecurityGroupName(sg.getName());
                        },
                        () -> {
                            local.setSecurityGroupOid(null);
                            local.setSecurityGroupName(null);
                        });
    }

    /**
     * Creates a new local App from a remote application; new entries start disabled.
     */
    private App createLocalFromRemote(GetAllApplicationsResponse.TechServicesApplication remote) {
        String sgId = null;
        String sgName = null;

        if (remote.getSecurityGroups() != null && !remote.getSecurityGroups().isEmpty()) {
            var sg = remote.getSecurityGroups().get(0);
            sgId = sg.getId();
            sgName = sg.getName();
        }

        return App.builder()
                .entraAppId(remote.getId())
                .name(remote.getName())
                .url(remote.getUrl())
                .securityGroupOid(sgId)
                .securityGroupName(sgName)
                .appType(AppType.LAA)
                .enabled(false)
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
                && (!remote.getName().equals(local.getName())
                || !remote.getUrl().equals(local.getUrl())
                || !areSecurityGroupsEqual(remote.getSecurityGroups(), local))) {
            return AppDto.ChangeType.UPDATED;
        }

        return AppDto.ChangeType.NONE;
    }

    private boolean areSecurityGroupsEqual(List<GetAllApplicationsResponse.TechServicesApplication.AppSecurityGroup> remoteSecGroups, App local) {
        String remoteSecGroupName = remoteSecGroups == null || remoteSecGroups.isEmpty() ? null : remoteSecGroups.getFirst().getName();
        String remoteSecGroupId = remoteSecGroups == null || remoteSecGroups.isEmpty() ? null : remoteSecGroups.getFirst().getId();

        return Objects.equals(remoteSecGroupName, local.getSecurityGroupName()) && Objects.equals(remoteSecGroupId, local.getSecurityGroupOid());

    }

}
