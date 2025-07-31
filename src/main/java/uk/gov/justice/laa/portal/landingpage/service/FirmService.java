package uk.gov.justice.laa.portal.landingpage.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;

/**
 * FirmService
 */
@Service
@RequiredArgsConstructor
public class FirmService {

    private final FirmRepository firmRepository;
    private final UserProfileRepository userProfileRepository;
    private final ModelMapper mapper;

    public List<FirmDto> getFirms() {
        return firmRepository.findAll()
                .stream()
                .map(firm -> mapper.map(firm, FirmDto.class))
                .collect(Collectors.toList());
    }

    public FirmDto getFirm(String id) {
        return mapper.map(firmRepository.getReferenceById(UUID.fromString(id)), FirmDto.class);
    }

    public Optional<FirmDto> getUserFirm(EntraUser entraUser) {
        return entraUser.getUserProfiles().stream()
                .filter(UserProfile::isActiveProfile)
                .findFirst()
                .map(UserProfile::getFirm)
                .map(firm -> mapper.map(firm, FirmDto.class));
    }

    public List<FirmDto> getUserFirms(EntraUser entraUser) {
        return entraUser.getUserProfiles().stream()
                .filter(UserProfile::isActiveProfile)
                .map(userProfile -> mapper.map(userProfile.getFirm(), FirmDto.class)).toList();
    }

    public List<FirmDto> getUserAllFirms(EntraUser entraUser) {
        return entraUser.getUserProfiles().stream()
                .map(userProfile -> mapper.map(userProfile.getFirm(), FirmDto.class)).toList();
    }

    /**
     * Get firms associated with a user by their ID
     * 
     * @param userId The ID of the user
     * @return List of FirmDto objects associated with the user
     */
    public List<FirmDto> getUserFirmsByUserId(String userId) {
        return userProfileRepository.findById(UUID.fromString(userId))
                .map(userProfile -> {
                    UserProfileDto userProfileDto = mapper.map(userProfile, UserProfileDto.class);
                    return userProfileDto.getFirm() != null ? List.of(userProfileDto.getFirm()) : List.<FirmDto>of();
                })
                .orElse(List.of());
    }
}
