package uk.gov.justice.laa.portal.landingpage.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppSynchronizationAuditEvent;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppType;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
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
    private final LoginService loginService;

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
    public List<AppDto> synchronizeAndGetApplicaitonsFromTechServices(Authentication authentication) {
        log.info("Synchronizing applications from Tech Services...");
        int counter = 0;
        int noChangesCounter = 0;
        int newAppsCounter = 0;
        int updatedAppsCounter = 0;
        int deletedAppsCounter = 0;
        TechServicesApiResponse<GetAllApplicationsResponse> getAllApplicationsResponse = techServicesClient.getAllApplications();
        if (getAllApplicationsResponse.isSuccess()) {
            List<GetAllApplicationsResponse.TechServicesApplication> remoteApps = getAllApplicationsResponse.getData().getApps();
            List<App> localApps = getAllLaaAppEntities();


            Map<String, GetAllApplicationsResponse.TechServicesApplication> remoteMap = remoteApps.stream()
                    .collect(Collectors.toMap(GetAllApplicationsResponse.TechServicesApplication::getId, a -> a));

            Map<String, App> localMap = localApps.stream()
                    .collect(Collectors.toMap(App::getEntraAppId, a -> a));

            Set<String> allIds = new HashSet<>();
            allIds.addAll(remoteMap.keySet());
            allIds.addAll(localMap.keySet());

            List<AppDto> result = new ArrayList<>();

            for (String id : allIds) {
                GetAllApplicationsResponse.TechServicesApplication remote = remoteMap.get(id);
                App local = localMap.get(id);
                AppDto syncedApp = null;

                if (remote != null && local != null) {
                    AppDto.ChangeType changeType = getChangeType(remote, local);
                    counter++;
                    switch (changeType) {
                        case NONE -> {
                            log.info("No changes for app with id: {}", id);
                            syncedApp = mapper.map(local, AppDto.class);
                            syncedApp.setChangeType(changeType);
                            noChangesCounter++;
                        }
                        case REVIEW -> {
                            log.info("App disabled locally for app with id: {}", id);
                            local.setName(remote.getName());
                            local.setUrl(remote.getUrl());
                            local.setSecurityGroupOid(remote.getSecurityGroups().getFirst().getId());
                            local.setSecurityGroupName(remote.getSecurityGroups().getFirst().getName());
                            appRepository.save(local);
                            syncedApp = mapper.map(local, AppDto.class);
                            syncedApp.setChangeType(changeType);
                            updatedAppsCounter++;
                        }
                        case UPDATED -> {
                            log.info("App updated remotely with id: {}", id);
                            local.setName(remote.getName());
                            local.setUrl(remote.getUrl());
                            local.setSecurityGroupOid(remote.getSecurityGroups().getFirst().getId());
                            local.setSecurityGroupName(remote.getSecurityGroups().getFirst().getName());
                            appRepository.save(local);
                            syncedApp = mapper.map(local, AppDto.class);
                            syncedApp.setChangeType(changeType);
                            updatedAppsCounter++;
                        }
                    }
                } else if (remote != null) {
                    // New app from remote → ADDED
                    log.info("New app {} with id {} added to SiLAS DB", remote.getName(), remote.getId());
                    App newApp = App.builder()
                            .entraAppId(remote.getId())
                            .name(remote.getName())
                            .url(remote.getUrl())
                            .securityGroupOid(remote.getSecurityGroups().getFirst().getId())
                            .securityGroupName(remote.getSecurityGroups().getFirst().getName())
                            .appType(AppType.LAA)
                            .enabled(false)
                            .build();
                    appRepository.save(newApp);
                    syncedApp = mapper.map(newApp, AppDto.class);
                    syncedApp.setChangeType(AppDto.ChangeType.ADDED);
                    newAppsCounter++;
                } else {
                    // App deleted remotely → DELETED
                    log.info("App with id {} deleted from Tech Services, so disabling locally", id);
                    local.setEnabled(false);
                    appRepository.save(local);
                    syncedApp = mapper.map(local, AppDto.class);
                    syncedApp.setChangeType(AppDto.ChangeType.DELETED);
                    deletedAppsCounter++;
                }

                result.add(syncedApp);
            }

            log.info("Finished synchronizing applications from Tech Services. Total apps processed: {}, No changes: {}, New apps: {}, Updated apps: {}, Deleted apps: {}",
                    counter, noChangesCounter, newAppsCounter, updatedAppsCounter, deletedAppsCounter);

            CurrentUserDto currentUserDto = loginService.getCurrentUser(authentication);
            UserProfile userProfile = loginService.getCurrentProfile(authentication);
            AppSynchronizationAuditEvent appSynchronizationAuditEvent =
                    new AppSynchronizationAuditEvent(currentUserDto, userProfile.getId(),
                            String.format("Total apps processed: %s, No changes: %s, New apps: %s, Updated apps: %s, Deleted apps: %s",
                                    counter, noChangesCounter, newAppsCounter, updatedAppsCounter, deletedAppsCounter));
            eventService.logEvent(appSynchronizationAuditEvent);

            return result.stream().sorted().toList();
        } else {
            log.error("Error synchronizing applications from Tech Services: {}", getAllApplicationsResponse.getError().getMessage());
            throw new RuntimeException(getAllApplicationsResponse.getError().getMessage());
        }
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
