package uk.gov.justice.laa.portal.landingpage.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppType;
import uk.gov.justice.laa.portal.landingpage.forms.AppsOrderForm;
import uk.gov.justice.laa.portal.landingpage.repository.AppRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppService {

    private final AppRepository appRepository;

    private final ModelMapper mapper;

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
}
