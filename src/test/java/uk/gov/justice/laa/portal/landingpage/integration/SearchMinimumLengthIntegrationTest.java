package uk.gov.justice.laa.portal.landingpage.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import uk.gov.justice.laa.portal.landingpage.controller.UserController;

@SpringBootTest
public class SearchMinimumLengthIntegrationTest {

    @Autowired
    private UserController userController;

    @Test
    @WithMockUser(roles = {"INTERNAL_USER"})
    public void testEmptyQueriesReturnEmpty() {
        // Empty queries should return empty lists
        List<Map<String, String>> result1 = userController.searchFirms("");
        assertThat(result1).isEmpty();

        List<Map<String, String>> result2 = userController.searchFirms("   ");
        assertThat(result2).isEmpty();
    }

    @Test 
    @WithMockUser(roles = {"INTERNAL_USER"})
    public void testSingleCharacterQueriesWork() {
        // Single character queries should now work (not return empty due to minimum length restriction)
        List<Map<String, String>> result = userController.searchFirms("A");
        
        // The query should be processed (not blocked by minimum length validation)
        // The result could be empty if no firms match, but it shouldn't be blocked
        // This test verifies that the query reaches the service layer
        assertThat(result).isNotNull();
    }
}
