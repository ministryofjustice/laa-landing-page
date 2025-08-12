package uk.gov.justice.laa.portal.landingpage.service;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.config.CachingConfig;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * FirmService
 */
@Service
@RequiredArgsConstructor
public class FirmService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final FirmRepository firmRepository;
    private final UserProfileRepository userProfileRepository;
    private final ModelMapper mapper;
    private final CacheManager cacheManager;

    private static final String ALL_FIRMS = "all_firms";

    protected List<FirmDto> getFirms() {
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

    /**
     * Search for firms by name or code
     * 
     * @param searchTerm The search term to match against firm name or code
     * @return List of FirmDto objects that match the search term
     */
    public List<FirmDto> searchFirms(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllFirmsFromCache();
        }

        String trimmedSearchTerm = searchTerm.trim();
        
        return getAllFirmsFromCache()
                .parallelStream()
                .filter(firm -> (firm.getName().toLowerCase().contains(trimmedSearchTerm.toLowerCase())
                        || (firm.getCode() != null && firm.getCode().toLowerCase().contains(trimmedSearchTerm.toLowerCase()))))
                .collect(Collectors.toList());
    }

    public List<FirmDto> getAllFirmsFromCache() {
        Cache cache = cacheManager.getCache(CachingConfig.LIST_OF_FIRMS_CACHE);
        if (cache != null) {
            try {
                Cache.ValueWrapper valueWrapper = cache.get(ALL_FIRMS);
                if (valueWrapper != null) {
                    @SuppressWarnings("unchecked")
                    List<FirmDto> cachedFirms = (List<FirmDto>) valueWrapper.get();
                    return cachedFirms;
                }
            } catch (Exception ex) {
                logger.info("Error while getting access token from cache", ex);
            }
        }

        List<FirmDto> allFirms = getFirms();
        if (cache != null) {
            cache.put(ALL_FIRMS, allFirms);
        }
        return allFirms;
    }

}
