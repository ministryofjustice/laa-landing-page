package uk.gov.justice.laa.portal.landingpage.service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import uk.gov.justice.laa.portal.landingpage.config.CachingConfig;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;

import java.util.ArrayList;

import static uk.gov.justice.laa.portal.landingpage.service.FirmComparatorByRelevance.relevance;

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

    public Page<Firm> getFirms(Pageable pageable) {
        return firmRepository.findAll(pageable);
    }

    public Page<Firm> getFirmsByName(String name, Pageable pageable) {
        return firmRepository.findAllByName(name, pageable);
    }

    public Page<Firm> getFirmsByType(FirmType firmType, Pageable pageable) {
        return firmRepository.findAllByType(firmType, pageable);
    }

    public Page<Firm> getFirmsById(UUID id, Pageable pageable) {
        return firmRepository.findAllById(id, pageable);
    }

    public FirmDto getFirm(String id) {
        return getFirm(UUID.fromString(id));
    }

    public FirmDto getFirm(UUID id) {
        return mapper.map(firmRepository.getReferenceById(id), FirmDto.class);
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
                .filter(userProfile -> userProfile.getFirm() != null)
                .map(userProfile -> mapper.map(userProfile.getFirm(), FirmDto.class)).toList();
    }

    public List<FirmDto> getUserAllFirms(EntraUser entraUser) {
        return entraUser.getUserProfiles().stream()
                .filter(userProfile -> userProfile.getFirm() != null)
                .map(userProfile -> mapper.map(userProfile.getFirm(), FirmDto.class)).toList();
    }

    public List<FirmDto> getUserActiveAllFirms(EntraUser entraUser) {
        List<FirmDto> userFirms = new ArrayList<>(entraUser.getUserProfiles().stream()
                .filter(UserProfile::isActiveProfile)
                .filter(userProfile -> userProfile.getFirm() != null)
                .map(userProfile -> mapper.map(userProfile.getFirm(), FirmDto.class)).toList());
        List<FirmDto> child = entraUser.getUserProfiles().stream()
                .filter(up -> up.isActiveProfile() && Objects.nonNull(up.getFirm())
                        && Objects.nonNull(up.getFirm().getChildFirms()) && !up.getFirm().getChildFirms().isEmpty())
                .map(userProfile -> userProfile.getFirm().getChildFirms()).flatMap(Collection::stream)
                .map(firm -> mapper.map(firm, FirmDto.class))
                .toList();
        userFirms.addAll(child);
        return userFirms;
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

        return firmRepository.findByNameOrCodeContaining(trimmedSearchTerm)
                .stream()
                .map(firm -> mapper.map(firm, FirmDto.class))
                .sorted((s1, s2) -> Integer.compare(relevance(s2, trimmedSearchTerm), relevance(s1, trimmedSearchTerm)))
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

    public List<FirmDto> getUserAccessibleFirms(EntraUser entraUser, String searchTerm) {

        UserType userType = entraUser.getUserProfiles().stream()
                .filter(UserProfile::isActiveProfile)
                .findFirst()
                .map(UserProfile::getUserType)
                .orElseThrow(() -> new UnsupportedOperationException("User type not found"));

        // If there's a search term, use database query for better performance
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            String trimmedSearchTerm = searchTerm.trim();

            switch (userType) {
                case INTERNAL -> {
                    // Internal users can search all firms via database
                    return firmRepository.findByNameOrCodeContaining(trimmedSearchTerm)
                            .stream()
                            .map(firm -> mapper.map(firm, FirmDto.class))
                            .sorted((s1, s2) -> Integer.compare(relevance(s2, trimmedSearchTerm),
                                    relevance(s1, trimmedSearchTerm)))
                            .collect(Collectors.toList());
                }
                case EXTERNAL -> {
                    // External firm admins can only see their own firms, so filter their accessible
                    // firms
                    List<FirmDto> userAccessibleFirms = getUserAllFirms(entraUser);
                    return userAccessibleFirms
                            .stream()
                            .filter(firm -> (firm.getName().toLowerCase().contains(trimmedSearchTerm.toLowerCase())
                                    || (firm.getCode() != null
                                            && firm.getCode().toLowerCase().contains(trimmedSearchTerm.toLowerCase()))))
                            .sorted((s1, s2) -> Integer.compare(relevance(s2, trimmedSearchTerm),
                                    relevance(s1, trimmedSearchTerm)))
                            .collect(Collectors.toList());
                }
                default -> {
                    throw new UnsupportedOperationException("User type not supported");
                }
            }
        }

        // No search term - return all accessible firms
        switch (userType) {
            case INTERNAL -> {
                return getAllFirmsFromCache();
            }
            case EXTERNAL -> {
                return getUserAllFirms(entraUser);
            }
            default -> {
                throw new UnsupportedOperationException("User type not supported");
            }
        }
    }

    public Firm getById(UUID id) {
        return firmRepository.getReferenceById(id);
    }

    public List<Firm> getFilteredChildFirms(Firm parentFirm, String query) {
        List<Firm> childFirms = parentFirm.getChildFirms() == null
                ? List.of()
                : parentFirm.getChildFirms().stream().toList();
        if (query == null || query.trim().isEmpty()) {
            return childFirms;
        }
        String q = query.trim().toLowerCase();
        return childFirms.stream()
                .filter(f -> firmMatchesQuery(f, q))
                .toList();
    }

    public boolean includeParentFirm(Firm parentFirm, String query) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }
        String q = query.trim().toLowerCase();
        return firmMatchesQuery(parentFirm, q);
    }

    public boolean firmMatchesQuery(Firm firm, String query) {
        if (firm == null || query == null) {
            return false;
        }
        String q = query.trim().toLowerCase();
        if (q.isEmpty()) {
            return false;
        }
        return (firm.getName() != null && firm.getName().toLowerCase().contains(q))
                || (firm.getCode() != null && firm.getCode().toLowerCase().contains(q));
    }
}
