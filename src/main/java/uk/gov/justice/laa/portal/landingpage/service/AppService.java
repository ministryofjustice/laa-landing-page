package uk.gov.justice.laa.portal.landingpage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppType;
import uk.gov.justice.laa.portal.landingpage.repository.AppRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppService {

    private final AppRepository appRepository;

    private final ModelMapper mapper;

    public Optional<App> getById(UUID id) {
        return appRepository.findById(id);
    }

    public List<AppDto> findAll() {
        return appRepository.findAll().stream()
                .map(app -> mapper.map(app, AppDto.class))
                .toList();
    }

    public List<AppDto> getAllActiveLaaApps() {
        return appRepository.findAppsByAppTypeAndEnabled(AppType.LAA, true)
                .stream()
                .map(app -> mapper.map(app, AppDto.class))
                .toList();
    }
}
