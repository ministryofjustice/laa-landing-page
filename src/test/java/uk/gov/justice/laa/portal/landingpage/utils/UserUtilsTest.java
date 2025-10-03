package uk.gov.justice.laa.portal.landingpage.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpSession;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.forms.UserDetailsForm;

class UserUtilsTest {

    private UserDetailsForm form;
    private HttpSession session;

    @BeforeEach
    void setUp() {
        form = new UserDetailsForm();
        session = mock(HttpSession.class);
    }

    @Test
    void testPopulateUserDetailsFormWithAllFields() {
        EntraUserDto user = new EntraUserDto();
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@example.com");
        when(session.getAttribute("isFirmAdmin")).thenReturn(Boolean.TRUE);

        UserDetailsForm result = UserUtils.populateUserDetailsFormWithSession(form, user, session);

        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        assertEquals("john.doe@example.com", result.getEmail());
    }

    @Test
    void testPopulateUserDetailsFormWithNullFields() {
        EntraUserDto user = new EntraUserDto();
        when(session.getAttribute("isFirmAdmin")).thenReturn(Boolean.FALSE);

        UserDetailsForm result = UserUtils.populateUserDetailsFormWithSession(form, user, session);

        assertNull(result.getFirstName());
        assertNull(result.getLastName());
        assertNull(result.getEmail());
    }

    @Test
    void testPopulateUserDetailsFormWithPartialFields() {
        EntraUserDto user = new EntraUserDto();
        user.setFirstName("Alice");
        user.setLastName(null);
        user.setEmail("alice@example.com");
        when(session.getAttribute("isFirmAdmin")).thenReturn(null);

        UserDetailsForm result = UserUtils.populateUserDetailsFormWithSession(form, user, session);

        assertEquals("Alice", result.getFirstName());
        assertNull(result.getLastName());
        assertEquals("alice@example.com", result.getEmail());
    }

    @Test
    void testIsFirmAdminAttributeNotBooleanTrue() {
        EntraUserDto user = new EntraUserDto();
        user.setFirstName("Bob");
        when(session.getAttribute("isFirmAdmin")).thenReturn("notBoolean");

        UserDetailsForm result = UserUtils.populateUserDetailsFormWithSession(form, user, session);

    }

    @Test
    void testPopulateUserDetailsFormWithUserManagerTrue() {
        // Given
        EntraUserDto user = new EntraUserDto();
        user.setFirstName("Jane");
        user.setLastName("Smith");
        user.setEmail("jane.smith@example.com");
        when(session.getAttribute("isUserManager")).thenReturn(Boolean.TRUE);

        // When
        UserDetailsForm result = UserUtils.populateUserDetailsFormWithSession(form, user, session);

        // Then
        assertEquals("Jane", result.getFirstName());
        assertEquals("Smith", result.getLastName());
        assertEquals("jane.smith@example.com", result.getEmail());
        assertTrue(result.getUserManager());
    }

    @Test
    void testPopulateUserDetailsFormWithUserManagerFalse() {
        // Given
        EntraUserDto user = new EntraUserDto();
        user.setFirstName("Tom");
        user.setLastName("Jones");
        user.setEmail("tom.jones@example.com");
        when(session.getAttribute("isUserManager")).thenReturn(Boolean.FALSE);

        // When
        UserDetailsForm result = UserUtils.populateUserDetailsFormWithSession(form, user, session);

        // Then
        assertEquals("Tom", result.getFirstName());
        assertEquals("Jones", result.getLastName());
        assertEquals("tom.jones@example.com", result.getEmail());
        assertFalse(result.getUserManager());
    }

    @Test
    void testPopulateUserDetailsFormWithNullUserManager() {
        // Given
        EntraUserDto user = new EntraUserDto();
        user.setFirstName("Sarah");
        user.setLastName("Brown");
        user.setEmail("sarah.brown@example.com");
        when(session.getAttribute("isUserManager")).thenReturn(null);

        // When
        UserDetailsForm result = UserUtils.populateUserDetailsFormWithSession(form, user, session);

        // Then
        assertEquals("Sarah", result.getFirstName());
        assertEquals("Brown", result.getLastName());
        assertEquals("sarah.brown@example.com", result.getEmail());
        assertNull(result.getUserManager());
    }
}