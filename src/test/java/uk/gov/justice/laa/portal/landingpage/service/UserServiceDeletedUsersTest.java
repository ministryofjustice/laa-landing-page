package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import uk.gov.justice.laa.portal.landingpage.dto.DeletedUserAuditDto;
import uk.gov.justice.laa.portal.landingpage.dto.PaginatedDeletedUsers;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserAccountStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserAccountStatusAudit;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.repository.UserAccountStatusAuditRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for deleted users functionality in UserService
 */
@ExtendWith(MockitoExtension.class)
class UserServiceDeletedUsersTest {

    @Mock
    private UserAccountStatusAuditRepository userAccountStatusAuditRepository;

    @InjectMocks
    private UserService userService;

    private UserAccountStatusAudit deletedAudit1;
    private UserAccountStatusAudit deletedAudit2;
    private UserAccountStatusAudit deletedAuditInternal;

    @BeforeEach
    void setUp() {
        // Create test deleted user audit records for external users
        deletedAudit1 = UserAccountStatusAudit.builder()
                .userEmail("deleted.user1@example.com")
                .userName("Deleted User One")
                .statusChange(UserAccountStatus.DELETED)
                .statusChangedBy("John Doe")
                .statusChangedDate(LocalDateTime.of(2024, 1, 15, 10, 30))
                .entraUser(null) // No EntraUser reference after deletion
                .build();

        deletedAudit2 = UserAccountStatusAudit.builder()
                .userEmail("deleted.user2@example.com")
                .userName("Deleted User Two")
                .statusChange(UserAccountStatus.DELETED)
                .statusChangedBy("Jane Smith")
                .statusChangedDate(LocalDateTime.of(2024, 1, 16, 14, 45))
                .entraUser(null) // No EntraUser reference after deletion
                .build();

        // Create test deleted user audit record for an INTERNAL user
        // This represents the edge case where an INTERNAL user somehow appears in deleted records
        EntraUser internalEntraUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .entraOid(UUID.randomUUID().toString())
                .email("internal.deleted@example.com")
                .firstName("Internal")
                .lastName("User")
                .build();

        UserProfile internalProfile = UserProfile.builder()
                .id(UUID.randomUUID())
                .userType(UserType.INTERNAL)
                .activeProfile(true)
                .build();

        Set<UserProfile> internalProfiles = new HashSet<>();
        internalProfiles.add(internalProfile);
        internalEntraUser.setUserProfiles(internalProfiles);

        deletedAuditInternal = UserAccountStatusAudit.builder()
                .userEmail("internal.deleted@example.com")
                .userName("Internal Deleted User")
                .statusChange(UserAccountStatus.DELETED)
                .statusChangedBy("Admin")
                .statusChangedDate(LocalDateTime.of(2024, 1, 17, 9, 15))
                .entraUser(internalEntraUser) // EntraUser still exists with INTERNAL profile
                .build();
    }

    @Test
    void getDeletedUsers_shouldReturnPaginatedResults() {
        // Arrange
        List<UserAccountStatusAudit> auditList = Arrays.asList(deletedAudit1, deletedAudit2);
        Page<UserAccountStatusAudit> auditPage = new PageImpl<>(auditList, PageRequest.of(0, 10), 2);

        when(userAccountStatusAuditRepository.findDeletedUsers(
                isNull(),
                any(Pageable.class)))
                .thenReturn(auditPage);

        // Act
        PaginatedDeletedUsers result = userService.getDeletedUsers(null, 1, 10, "statusChangedDate", "desc");

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getDeletedUsers().size());
        assertEquals(2, result.getTotalDeletedUsers());
        assertEquals(1, result.getTotalPages());
        assertEquals(1, result.getCurrentPage());
        assertEquals(10, result.getPageSize());

        DeletedUserAuditDto firstUser = result.getDeletedUsers().get(0);
        assertEquals("deleted.user1@example.com", firstUser.getUserEmail());
        assertEquals("John Doe", firstUser.getDeletedBy());
        assertNotNull(firstUser.getDeletedDate());
    }

    @Test
    void getDeletedUsers_shouldFilterByEmailSearchTerm() {
        // Arrange
        List<UserAccountStatusAudit> filteredList = Arrays.asList(deletedAudit1);
        Page<UserAccountStatusAudit> auditPage = new PageImpl<>(filteredList, PageRequest.of(0, 10), 1);

        when(userAccountStatusAuditRepository.findDeletedUsers(
                eq("user1"),
                any(Pageable.class)))
                .thenReturn(auditPage);

        // Act
        PaginatedDeletedUsers result = userService.getDeletedUsers("user1", 1, 10, "statusChangedDate", "desc");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getDeletedUsers().size());
        assertEquals(1, result.getTotalDeletedUsers());
        assertEquals("deleted.user1@example.com", result.getDeletedUsers().getFirst().getUserEmail());
    }

    @Test
    void getDeletedUsers_shouldHandleEmptyResults() {
        // Arrange
        Page<UserAccountStatusAudit> emptyPage = new PageImpl<>(Arrays.asList(), PageRequest.of(0, 10), 0);

        when(userAccountStatusAuditRepository.findDeletedUsers(
                isNull(),
                any(Pageable.class)))
                .thenReturn(emptyPage);

        // Act
        PaginatedDeletedUsers result = userService.getDeletedUsers(null, 1, 10, "statusChangedDate", "desc");

        // Assert
        assertNotNull(result);
        assertTrue(result.getDeletedUsers().isEmpty());
        assertEquals(0, result.getTotalDeletedUsers());
        assertEquals(0, result.getTotalPages());
    }

    @Test
    void getDeletedUsers_shouldSortByUserEmail() {
        // Arrange
        List<UserAccountStatusAudit> auditList = Arrays.asList(deletedAudit1, deletedAudit2);
        Page<UserAccountStatusAudit> auditPage = new PageImpl<>(auditList, PageRequest.of(0, 10), 2);

        when(userAccountStatusAuditRepository.findDeletedUsers(
                isNull(),
                any(Pageable.class)))
                .thenReturn(auditPage);

        // Act
        PaginatedDeletedUsers result = userService.getDeletedUsers(null, 1, 10, "email", "asc");

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getDeletedUsers().size());

        // Verify the repository was called with correct sort field
        verify(userAccountStatusAuditRepository, org.mockito.Mockito.atLeastOnce()).findDeletedUsers(
                isNull(),
                argThat(pageable ->
                    pageable != null && pageable.getSort().getOrderFor("userEmail") != null
                )
        );
    }

    @Test
    void getDeletedUsers_shouldSortByDeletedBy() {
        // Arrange
        List<UserAccountStatusAudit> auditList = Arrays.asList(deletedAudit1, deletedAudit2);
        Page<UserAccountStatusAudit> auditPage = new PageImpl<>(auditList, PageRequest.of(0, 10), 2);

        when(userAccountStatusAuditRepository.findDeletedUsers(
                isNull(),
                any(Pageable.class)))
                .thenReturn(auditPage);

        // Act
        PaginatedDeletedUsers result = userService.getDeletedUsers(null, 1, 10, "deletedBy", "asc");

        // Assert
        assertNotNull(result);

        // Verify the repository was called with correct sort field
        verify(userAccountStatusAuditRepository, org.mockito.Mockito.atLeastOnce()).findDeletedUsers(
                isNull(),
                argThat(pageable ->
                    pageable != null && pageable.getSort().getOrderFor("statusChangedBy") != null
                )
        );
    }

    @Test
    void getDeletedUsers_shouldHandleSecondPage() {

        List<UserAccountStatusAudit> page2List = Arrays.asList(deletedAudit1);
        Page<UserAccountStatusAudit> page2 = new PageImpl<>(page2List, PageRequest.of(1, 10), 12);

        List<UserAccountStatusAudit> allAuditsList =
                Arrays.asList(deletedAudit1, deletedAudit2);
        Page<UserAccountStatusAudit> allPages = new PageImpl<>(allAuditsList, PageRequest.of(0, 12), 12);

        when(userAccountStatusAuditRepository.findDeletedUsers(
                isNull(),
                argThat(pageable ->
                        pageable != null
                                && pageable.getPageNumber() == 1
                                && pageable.getPageSize() == 10)))
                .thenReturn(page2);

        when(userAccountStatusAuditRepository.findDeletedUsers(
                isNull(),
                argThat(pageable ->
                        pageable != null
                                && pageable.getPageNumber() == 0)))
                .thenReturn(allPages);


        // Act
        PaginatedDeletedUsers result = userService.getDeletedUsers(null, 2, 10, "statusChangedDate", "desc");

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getCurrentPage());
        assertEquals(2, result.getTotalPages()); // 12 total / 10 per page = 2 pages
    }

    @Test
    void getDeletedUsers_shouldFilterOutInternalUsers() {
        List<UserAccountStatusAudit> auditList = Arrays.asList(deletedAudit1, deletedAuditInternal, deletedAudit2);
        Page<UserAccountStatusAudit> auditPage = new PageImpl<>(auditList, PageRequest.of(0, 10), 3);

        when(userAccountStatusAuditRepository.findDeletedUsers(
                isNull(),
                any(Pageable.class)))
                .thenReturn(auditPage);

        PaginatedDeletedUsers result = userService.getDeletedUsers(null, 1, 10, "statusChangedDate", "desc");

        assertNotNull(result);
        assertEquals(2, result.getDeletedUsers().size());
        assertEquals(2, result.getTotalDeletedUsers());
        assertEquals(1, result.getTotalPages());

        boolean hasInternalUser = result.getDeletedUsers().stream()
                .anyMatch(user -> user.getUserEmail().equals("internal.deleted@example.com"));
        assertFalse(hasInternalUser, "Internal users should be filtered out");

        boolean hasExternalUser1 = result.getDeletedUsers().stream()
                .anyMatch(user -> user.getUserEmail().equals("deleted.user1@example.com"));
        boolean hasExternalUser2 = result.getDeletedUsers().stream()
                .anyMatch(user -> user.getUserEmail().equals("deleted.user2@example.com"));
        assertTrue(hasExternalUser1, "External user 1 should be present");
        assertTrue(hasExternalUser2, "External user 2 should be present");
    }

    @Test
    void getDeletedUsers_shouldOnlyReturnInternalUsersWhenAllDeleted() {
        List<UserAccountStatusAudit> auditList = Arrays.asList(deletedAuditInternal);
        Page<UserAccountStatusAudit> auditPage = new PageImpl<>(auditList, PageRequest.of(0, 10), 1);

        when(userAccountStatusAuditRepository.findDeletedUsers(
                isNull(),
                any(Pageable.class)))
                .thenReturn(auditPage);

        PaginatedDeletedUsers result = userService.getDeletedUsers(null, 1, 10, "statusChangedDate", "desc");

        assertNotNull(result);
        assertTrue(result.getDeletedUsers().isEmpty(), "All internal users should be filtered out");
        assertEquals(0, result.getTotalDeletedUsers());
        assertEquals(0, result.getTotalPages());
    }

    @Test
    void getDeletedUsers_shouldFilterExternalUsersWithoutEntraReference() {

        List<UserAccountStatusAudit> auditList = Arrays.asList(deletedAudit1, deletedAudit2);
        Page<UserAccountStatusAudit> auditPage = new PageImpl<>(auditList, PageRequest.of(0, 10), 2);

        when(userAccountStatusAuditRepository.findDeletedUsers(
                isNull(),
                any(Pageable.class)))
                .thenReturn(auditPage);

        PaginatedDeletedUsers result = userService.getDeletedUsers(null, 1, 10, "statusChangedDate", "desc");

        assertNotNull(result);
        assertEquals(2, result.getDeletedUsers().size());
        assertEquals(2, result.getTotalDeletedUsers());
    }
}

