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
import uk.gov.justice.laa.portal.landingpage.entity.UserAccountStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserAccountStatusAudit;
import uk.gov.justice.laa.portal.landingpage.repository.UserAccountStatusAuditRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @BeforeEach
    void setUp() {
        // Create test deleted user audit records
        deletedAudit1 = UserAccountStatusAudit.builder()
                .userEmail("deleted.user1@example.com")
                .statusChange(UserAccountStatus.DELETED)
                .statusChangedBy("John Doe")
                .statusChangedDate(LocalDateTime.of(2024, 1, 15, 10, 30))
                .build();

        deletedAudit2 = UserAccountStatusAudit.builder()
                .userEmail("deleted.user2@example.com")
                .statusChange(UserAccountStatus.DELETED)
                .statusChangedBy("Jane Smith")
                .statusChangedDate(LocalDateTime.of(2024, 1, 16, 14, 45))
                .build();
    }

    @Test
    void getDeletedUsers_shouldReturnPaginatedResults() {
        // Arrange
        List<UserAccountStatusAudit> auditList = Arrays.asList(deletedAudit1, deletedAudit2);
        Page<UserAccountStatusAudit> auditPage = new PageImpl<>(auditList, PageRequest.of(0, 10), 2);

        when(userAccountStatusAuditRepository.findDeletedUsers(
                eq(UserAccountStatus.DELETED),
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
                eq(UserAccountStatus.DELETED),
                eq("user1"),
                any(Pageable.class)))
                .thenReturn(auditPage);

        // Act
        PaginatedDeletedUsers result = userService.getDeletedUsers("user1", 1, 10, "statusChangedDate", "desc");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getDeletedUsers().size());
        assertEquals(1, result.getTotalDeletedUsers());
        assertEquals("deleted.user1@example.com", result.getDeletedUsers().get(0).getUserEmail());
    }

    @Test
    void getDeletedUsers_shouldHandleEmptyResults() {
        // Arrange
        Page<UserAccountStatusAudit> emptyPage = new PageImpl<>(Arrays.asList(), PageRequest.of(0, 10), 0);

        when(userAccountStatusAuditRepository.findDeletedUsers(
                eq(UserAccountStatus.DELETED),
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
                eq(UserAccountStatus.DELETED),
                isNull(),
                any(Pageable.class)))
                .thenReturn(auditPage);

        // Act
        PaginatedDeletedUsers result = userService.getDeletedUsers(null, 1, 10, "email", "asc");

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getDeletedUsers().size());

        // Verify the repository was called with correct sort field
        verify(userAccountStatusAuditRepository).findDeletedUsers(
                eq(UserAccountStatus.DELETED),
                isNull(),
                argThat(pageable ->
                    pageable.getSort().getOrderFor("userEmail") != null
                )
        );
    }

    @Test
    void getDeletedUsers_shouldSortByDeletedBy() {
        // Arrange
        List<UserAccountStatusAudit> auditList = Arrays.asList(deletedAudit1, deletedAudit2);
        Page<UserAccountStatusAudit> auditPage = new PageImpl<>(auditList, PageRequest.of(0, 10), 2);

        when(userAccountStatusAuditRepository.findDeletedUsers(
                eq(UserAccountStatus.DELETED),
                isNull(),
                any(Pageable.class)))
                .thenReturn(auditPage);

        // Act
        PaginatedDeletedUsers result = userService.getDeletedUsers(null, 1, 10, "deletedBy", "asc");

        // Assert
        assertNotNull(result);

        // Verify the repository was called with correct sort field
        verify(userAccountStatusAuditRepository).findDeletedUsers(
                eq(UserAccountStatus.DELETED),
                isNull(),
                argThat(pageable ->
                    pageable.getSort().getOrderFor("statusChangedBy") != null
                )
        );
    }

    @Test
    void getDeletedUsers_shouldHandleSecondPage() {
        // Arrange
        List<UserAccountStatusAudit> auditList = Arrays.asList(deletedAudit1);
        Page<UserAccountStatusAudit> auditPage = new PageImpl<>(auditList, PageRequest.of(1, 10), 12);

        when(userAccountStatusAuditRepository.findDeletedUsers(
                eq(UserAccountStatus.DELETED),
                isNull(),
                any(Pageable.class)))
                .thenReturn(auditPage);

        // Act
        PaginatedDeletedUsers result = userService.getDeletedUsers(null, 2, 10, "statusChangedDate", "desc");

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getCurrentPage());
        assertEquals(2, result.getTotalPages());

        // Verify pagination
        verify(userAccountStatusAuditRepository).findDeletedUsers(
                eq(UserAccountStatus.DELETED),
                isNull(),
                argThat(pageable -> pageable.getPageNumber() == 1) // 0-based index
        );
    }
}

