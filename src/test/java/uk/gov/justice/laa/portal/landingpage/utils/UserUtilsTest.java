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
}