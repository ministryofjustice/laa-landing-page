package uk.gov.justice.laa.portal.landingpage.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.ReactivationRequestsPageData;
import uk.gov.justice.laa.portal.landingpage.entity.AuthzRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.model.PaginatedReactivationRequests;
import uk.gov.justice.laa.portal.landingpage.model.ReactivationRequestListItem;
import uk.gov.justice.laa.portal.landingpage.model.ReactivationRequestPageMode;
import uk.gov.justice.laa.portal.landingpage.model.ReactivationRequestStatus;
import uk.gov.justice.laa.portal.landingpage.model.ReactivationRequestUserType;

@Service
@RequiredArgsConstructor
public class ReactivationRequestService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final List<String> FIRST_NAMES = List.of(
            "Alice", "Samantha", "Jacob", "Steve", "Jamie", "Ben", "Daniel", "Finn", "Taylor", "Morgan");
    private static final List<String> LAST_NAMES = List.of(
            "Turner", "Springer", "Nolan", "Stevenson", "Helles", "Walker", "Hughes", "Roberts", "Clarke", "Patel");

    private final LoginService loginService;
    private final FirmService firmService;

    public ReactivationRequestsPageData getPage(
            Authentication authentication,
            String search,
            List<ReactivationRequestUserType> selectedUserTypes,
            List<ReactivationRequestStatus> selectedStatuses,
            int page,
            int size,
            String sort,
            String direction) {

        EntraUser currentUser = loginService.getCurrentEntraUser(authentication);
        ReactivationRequestPageMode pageMode = resolvePageMode(currentUser);
        List<ReactivationRequestStatus> effectiveStatuses = selectedStatuses == null
            ? List.of()
            : List.copyOf(selectedStatuses);

        List<ReactivationRequestListItem> requests = filterAndSortRequests(
                buildMockRequests(currentUser, pageMode),
                normalizeSearch(search),
                selectedUserTypes,
                effectiveStatuses,
                sort,
                direction);

        PaginatedReactivationRequests paginated = paginate(requests, page, size);
        return new ReactivationRequestsPageData(pageMode, effectiveStatuses, paginated);
    }

    public ReactivationRequestPageMode getPageMode(Authentication authentication) {
        return resolvePageMode(loginService.getCurrentEntraUser(authentication));
    }

    private ReactivationRequestPageMode resolvePageMode(EntraUser currentUser) {
        if (currentUser == null) {
            return ReactivationRequestPageMode.MANAGE;
        }

        boolean isManageRole = AccessControlService.userHasAuthzRole(currentUser, AuthzRole.EXTERNAL_USER_ADMIN.getRoleName())
                || AccessControlService.userHasAuthzRole(currentUser, AuthzRole.GLOBAL_ADMIN.getRoleName())
                || AccessControlService.userHasAuthzRole(currentUser, AuthzRole.SECURITY_RESPONSE.getRoleName());

        boolean isProviderAdminOnly = AccessControlService.userHasAuthzRole(currentUser, AuthzRole.FIRM_USER_MANAGER.getRoleName())
                && !isManageRole;

        return isProviderAdminOnly ? ReactivationRequestPageMode.TRACK : ReactivationRequestPageMode.MANAGE;
    }

    private List<ReactivationRequestListItem> buildMockRequests(EntraUser currentUser, ReactivationRequestPageMode pageMode) {
        List<FirmDto> userActiveFirms = currentUser == null
                ? List.of()
                : firmService.getUserActiveAllFirms(currentUser);

        List<FirmDto> allFirms = new ArrayList<>(firmService.getAllFirmsFromCache());
        if (allFirms.isEmpty()) {
            allFirms = new ArrayList<>(userActiveFirms);
        }

        if (allFirms.isEmpty()) {
            allFirms.add(FirmDto.builder().id(UUID.randomUUID()).code("MOCK").name("Mock Firm").build());
        }

        List<FirmDto> firmsForData = new ArrayList<>();
        Set<UUID> seenFirmIds = new HashSet<>();
        for (FirmDto firm : userActiveFirms) {
            if (firm != null && firm.getId() != null && seenFirmIds.add(firm.getId())) {
                firmsForData.add(firm);
            }
        }
        for (FirmDto firm : allFirms) {
            if (firm != null && firm.getId() != null && seenFirmIds.add(firm.getId())) {
                firmsForData.add(firm);
            }
        }
        firmsForData = firmsForData.stream().limit(4).toList();

        List<ReactivationRequestListItem> mockData = new ArrayList<>();

        LocalDate baseDate = LocalDate.now().minusDays(20);
        int index = 0;
        for (FirmDto firm : firmsForData) {
            for (ReactivationRequestStatus status : ReactivationRequestStatus.values()) {
                ReactivationRequestUserType userType = ReactivationRequestUserType.values()[index
                        % ReactivationRequestUserType.values().length];
                String firstName = FIRST_NAMES.get(index % FIRST_NAMES.size());
                String lastName = LAST_NAMES.get(index % LAST_NAMES.size());
                LocalDate dateSubmitted = baseDate.plusDays(index);
                LocalDate lastActivity = dateSubmitted.plusDays(index % 3);

                mockData.add(new ReactivationRequestListItem(
                        UUID.nameUUIDFromBytes((firm.getCode() + "-" + index).getBytes()),
                        firstName + " " + lastName,
                        (firstName.charAt(0) + "." + lastName + index + "@clashlaw.com").toLowerCase(Locale.UK),
                        dateSubmitted,
                        lastActivity,
                        userType,
                        status,
                        firm.getId()));
                index++;
            }
        }

        if (pageMode == ReactivationRequestPageMode.TRACK && currentUser != null) {
            Set<UUID> allowedFirmIds = userActiveFirms.stream()
                    .map(FirmDto::getId)
                    .collect(Collectors.toSet());

            if (allowedFirmIds.isEmpty()) {
                return List.of();
            }

            return mockData.stream()
                    .filter(item -> item.firmId() != null && allowedFirmIds.contains(item.firmId()))
                    .toList();
        }

        return mockData;
    }

    private List<ReactivationRequestListItem> filterAndSortRequests(
            List<ReactivationRequestListItem> requests,
            String search,
            List<ReactivationRequestUserType> selectedUserTypes,
            List<ReactivationRequestStatus> selectedStatuses,
            String sort,
            String direction) {

        Set<ReactivationRequestUserType> userTypeFilter = selectedUserTypes == null
                ? Set.of()
                : new HashSet<>(selectedUserTypes);
        Set<ReactivationRequestStatus> statusFilter = selectedStatuses == null
                ? Set.of()
                : new HashSet<>(selectedStatuses);

        Comparator<ReactivationRequestListItem> comparator = resolveComparator(sort);
        if (!"asc".equalsIgnoreCase(direction)) {
            comparator = comparator.reversed();
        }

        return requests.stream()
                .filter(item -> search.isBlank()
                        || item.name().toLowerCase(Locale.UK).contains(search)
                        || item.email().toLowerCase(Locale.UK).contains(search))
                .filter(item -> userTypeFilter.isEmpty() || userTypeFilter.contains(item.userType()))
                .filter(item -> statusFilter.isEmpty() || statusFilter.contains(item.requestStatus()))
                .sorted(comparator)
                .toList();
    }

    private PaginatedReactivationRequests paginate(List<ReactivationRequestListItem> requests, int page, int requestedSize) {
        int pageSize = requestedSize > 0 ? requestedSize : DEFAULT_PAGE_SIZE;
        int safePage = Math.max(1, page);

        int totalItems = requests.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / pageSize));
        int boundedPage = Math.min(safePage, totalPages);
        int startIndex = (boundedPage - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalItems);

        List<ReactivationRequestListItem> pageItems = startIndex >= totalItems
                ? List.of()
                : requests.subList(startIndex, endIndex);

        PaginatedReactivationRequests paginated = new PaginatedReactivationRequests();
        paginated.setRequests(pageItems);
        paginated.setTotalRequests(totalItems);
        paginated.setTotalPages(totalPages);
        paginated.setCurrentPage(boundedPage);
        paginated.setPageSize(pageSize);
        return paginated;
    }

    private Comparator<ReactivationRequestListItem> resolveComparator(String sort) {
        if (sort == null) {
            return Comparator.comparing(ReactivationRequestListItem::dateSubmitted, Comparator.nullsLast(LocalDate::compareTo));
        }

        return switch (sort) {
            case "name" -> Comparator.comparing(ReactivationRequestListItem::name, String.CASE_INSENSITIVE_ORDER);
            case "email" -> Comparator.comparing(ReactivationRequestListItem::email, String.CASE_INSENSITIVE_ORDER);
            case "lastActivity" -> Comparator.comparing(ReactivationRequestListItem::lastActivity,
                    Comparator.nullsLast(LocalDate::compareTo));
            case "userType" -> Comparator.comparing(item -> item.userType().getTableLabel(), String.CASE_INSENSITIVE_ORDER);
            case "requestStatus" -> Comparator.comparing(item -> item.requestStatus().getLabel(), String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(ReactivationRequestListItem::dateSubmitted,
                    Comparator.nullsLast(LocalDate::compareTo));
        };
    }

    private String normalizeSearch(String search) {
        return search == null ? "" : search.trim().toLowerCase(Locale.UK);
    }
}
