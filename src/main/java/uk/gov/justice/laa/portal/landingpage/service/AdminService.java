package uk.gov.justice.laa.portal.landingpage.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.dto.AdminAppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppAdminDto;
import uk.gov.justice.laa.portal.landingpage.entity.AdminApp;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.repository.AdminAppRepository;
import uk.gov.justice.laa.portal.landingpage.repository.AppRepository;

/**
 * Service for SiLAS Administration functionality
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AdminService {

    private final AppRepository appRepository;
    private final AdminAppRepository adminAppRepository;

    /**
     * Get all admin apps for administration display
     */
    public List<AdminAppDto> getAllAdminApps() {
        return adminAppRepository.findAll().stream()
                .filter(AdminApp::isEnabled)
                .map(this::mapToAdminAppDto)
                .sorted((a, b) -> Integer.compare(a.getOrdinal(), b.getOrdinal()))
                .collect(Collectors.toList());
    }

    /**
     * Get all apps for administration display
     */
    public List<AppAdminDto> getAllApps() {
        return appRepository.findAll().stream()
                .map(this::mapToAppAdminDto)
                .sorted((a, b) -> Integer.compare(a.getOrdinal(), b.getOrdinal()))
                .collect(Collectors.toList());
    }


    /**
     * Map App entity to AppAdminDto
     */
    private AppAdminDto mapToAppAdminDto(App app) {
        return AppAdminDto.builder()
                .id(app.getId().toString())
                .name(app.getName())
                .description(app.getDescription())
                .ordinal(app.getOrdinal())
                .url(app.getUrl())
                .enabled(app.isEnabled())
                .appType(app.getAppType() != null ? app.getAppType().name() : "")
                .build();
    }

    /**
     * Map AdminApp entity to AdminAppDto
     */
    private AdminAppDto mapToAdminAppDto(AdminApp adminApp) {
        return AdminAppDto.builder()
                .id(adminApp.getId().toString())
                .name(adminApp.getName())
                .description(adminApp.getDescription())
                .ordinal(adminApp.getOrdinal())
                .enabled(adminApp.isEnabled())
                .build();
    }

}
