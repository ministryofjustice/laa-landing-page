import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.microsoft.graph.models.User;

import jakarta.servlet.http.HttpSession;
import uk.gov.justice.laa.portal.landingpage.forms.UserDetailsForm;
import uk.gov.justice.laa.portal.landingpage.utils.UserUtils;

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
        User user = new User();
        user.setGivenName("John");
        user.setSurname("Doe");
        user.setMail("john.doe@example.com");
        when(session.getAttribute("isFirmAdmin")).thenReturn(Boolean.TRUE);

        UserDetailsForm result = UserUtils.populateUserDetailsFormWithSession(form, user, session);

        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        assertEquals("john.doe@example.com", result.getEmail());
        assertTrue(result.getIsFirmAdmin());
    }

    @Test
    void testPopulateUserDetailsFormWithNullFields() {
        User user = new User();
        user.setGivenName(null);
        user.setSurname(null);
        user.setMail(null);
        when(session.getAttribute("isFirmAdmin")).thenReturn(Boolean.FALSE);

        UserDetailsForm result = UserUtils.populateUserDetailsFormWithSession(form, user, session);

        assertNull(result.getFirstName());
        assertNull(result.getLastName());
        assertNull(result.getEmail());
        assertFalse(result.getIsFirmAdmin());
    }

    @Test
    void testPopulateUserDetailsFormWithPartialFields() {
        User user = new User();
        user.setGivenName("Alice");
        user.setSurname(null);
        user.setMail("alice@example.com");
        when(session.getAttribute("isFirmAdmin")).thenReturn(null);

        UserDetailsForm result = UserUtils.populateUserDetailsFormWithSession(form, user, session);

        assertEquals("Alice", result.getFirstName());
        assertNull(result.getLastName());
        assertEquals("alice@example.com", result.getEmail());
        assertFalse(result.getIsFirmAdmin());
    }

    @Test
    void testIsFirmAdminAttributeNotBooleanTrue() {
        User user = new User();
        user.setGivenName("Bob");
        when(session.getAttribute("isFirmAdmin")).thenReturn("notBoolean");

        UserDetailsForm result = UserUtils.populateUserDetailsFormWithSession(form, user, session);

        assertFalse(result.getIsFirmAdmin());
    }
}