package uk.gov.justice.laa.portal.landingpage.dto;

import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.forms.FirmSearchForm;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserSearchCriteriaTest {

    private static final FirmSearchForm firmSearch = FirmSearchForm.builder().build();

    @Test
    void testDefaultConstructor() {
        // When
        UserSearchCriteria criteria = new UserSearchCriteria();

        // Then
        assertThat(criteria).isNotNull();
        assertThat(criteria.getSearchTerm()).isNull();
        assertThat(criteria.getFirmSearch()).isNull();
        assertThat(criteria.getUserType()).isNull();
        assertThat(criteria.isShowFirmAdmins()).isFalse();
    }

    @Test
    void testAllArgsConstructor() {
        // Given
        String searchTerm = "john doe";
        FirmSearchForm firmSearch = FirmSearchForm.builder().selectedFirmId(UUID.randomUUID()).build();
        UserType userType = UserType.EXTERNAL;
        boolean showFirmAdmins = true;

        // When
        UserSearchCriteria criteria = new UserSearchCriteria(searchTerm, firmSearch, userType, showFirmAdmins);

        // Then
        assertThat(criteria.getSearchTerm()).isEqualTo(searchTerm);
        assertThat(criteria.getFirmSearch()).isEqualTo(firmSearch);
        assertThat(criteria.getUserType()).isEqualTo(userType);
        assertThat(criteria.isShowFirmAdmins()).isEqualTo(showFirmAdmins);
    }

    @Test
    void testSettersAndGetters() {
        // Given
        UserSearchCriteria criteria = new UserSearchCriteria();
        String searchTerm = "jane smith";
        FirmSearchForm firmSearch = FirmSearchForm.builder().selectedFirmId(UUID.randomUUID()).build();
        UserType userType = UserType.INTERNAL;
        boolean showFirmAdmins = false;

        // When
        criteria.setSearchTerm(searchTerm);
        criteria.setFirmSearch(firmSearch);
        criteria.setUserType(userType);
        criteria.setShowFirmAdmins(showFirmAdmins);

        // Then
        assertThat(criteria.getSearchTerm()).isEqualTo(searchTerm);
        assertThat(criteria.getFirmSearch()).isEqualTo(firmSearch);
        assertThat(criteria.getUserType()).isEqualTo(userType);
        assertThat(criteria.isShowFirmAdmins()).isEqualTo(showFirmAdmins);
    }

    @Test
    void testWithNullValues() {
        // Given
        UserSearchCriteria criteria = new UserSearchCriteria();

        // When
        criteria.setSearchTerm(null);
        criteria.setFirmSearch(null);
        criteria.setUserType(null);
        criteria.setShowFirmAdmins(false);

        // Then
        assertThat(criteria.getSearchTerm()).isNull();
        assertThat(criteria.getFirmSearch()).isNull();
        assertThat(criteria.getUserType()).isNull();
        assertThat(criteria.isShowFirmAdmins()).isFalse();
    }

    @Test
    void testWithEmptyStringValues() {
        // Given
        UserSearchCriteria criteria = new UserSearchCriteria();

        // When
        criteria.setSearchTerm("");
        criteria.setFirmSearch(null);
        criteria.setUserType(null);

        // Then
        assertThat(criteria.getSearchTerm()).isEmpty();
        assertThat(criteria.getFirmSearch()).isNull();
        assertThat(criteria.getUserType()).isNull();
    }

    @Test
    void testWithWhitespaceValues() {
        // Given
        UserSearchCriteria criteria = new UserSearchCriteria();

        // When
        criteria.setSearchTerm("   ");

        // Then
        assertThat(criteria.getSearchTerm()).isEqualTo("   ");
    }

    @Test
    void testWithAllUserTypes() {
        // Test with all available user types
        for (UserType userType : UserType.values()) {
            // When
            UserSearchCriteria criteria = new UserSearchCriteria("test", null, userType, true);
            
            // Then
            assertThat(criteria.getUserType()).isEqualTo(userType);
        }
    }

    @Test
    void testWithMultipleUserTypes() {
        // Given
        UserType userType = UserType.EXTERNAL;

        // When
        UserSearchCriteria criteria = new UserSearchCriteria("search", null, userType, true);

        // Then
        assertThat(criteria.getUserType()).isEqualTo(UserType.EXTERNAL);
    }

    @Test
    void testWithExternalUserTypes() {
        // Given
        UserType userType = UserType.EXTERNAL;

        // When
        UserSearchCriteria criteria = new UserSearchCriteria("test", firmSearch, userType, false);

        // Then
        assertThat(criteria.getUserType()).isEqualTo(userType);
    }

    @Test
    void testWithInternalUserTypes() {
        // Given
        UserType userType = UserType.INTERNAL;

        // When
        UserSearchCriteria criteria = new UserSearchCriteria("test", firmSearch, userType, true);

        // Then
        assertThat(criteria.getUserType()).isEqualTo(userType);
    }

    @Test
    void testShowFirmAdminsFlagTrue() {
        // Given & When
        UserSearchCriteria criteria = new UserSearchCriteria("test", firmSearch, UserType.INTERNAL, true);

        // Then
        assertThat(criteria.isShowFirmAdmins()).isTrue();
    }

    @Test
    void testShowFirmAdminsFlagFalse() {
        // Given & When
        UserSearchCriteria criteria = new UserSearchCriteria("test", firmSearch, UserType.INTERNAL, false);

        // Then
        assertThat(criteria.isShowFirmAdmins()).isFalse();
    }

    @Test
    void testToString() {
        // Given
        String searchTerm = "john doe";
        UUID firmSearchId = UUID.randomUUID();
        FirmSearchForm firmSearchForm = FirmSearchForm.builder().firmSearch("firm-123").selectedFirmId(firmSearchId).build();
        UserType userType = UserType.EXTERNAL;
        boolean showFirmAdmins = true;

        UserSearchCriteria criteria = new UserSearchCriteria(searchTerm, firmSearchForm, userType, showFirmAdmins);

        // When
        String toString = criteria.toString();

        // Then
        assertThat(toString).contains("UserSearchCriteria");
        assertThat(toString).contains("searchTerm='john doe'");
        assertThat(toString).contains("firmSearch='FirmSearchForm(firmSearch=firm-123, selectedFirmId=" + firmSearchId + ")'");
        assertThat(toString).contains("userType=EXTERNAL");
        assertThat(toString).contains("showFirmAdmins=true");
    }

    @Test
    void testToStringWithNullValues() {
        // Given
        UserSearchCriteria criteria = new UserSearchCriteria(null, null, null, false);

        // When
        String toString = criteria.toString();

        // Then
        assertThat(toString).contains("UserSearchCriteria");
        assertThat(toString).contains("searchTerm='null'");
        assertThat(toString).contains("firmSearch='null'");
        assertThat(toString).contains("userType=null");
        assertThat(toString).contains("showFirmAdmins=false");
    }

    @Test
    void testToStringWithEmptyValues() {
        // Given
        UserSearchCriteria criteria = new UserSearchCriteria("", firmSearch, null, false);

        // When
        String toString = criteria.toString();

        // Then
        assertThat(toString).contains("UserSearchCriteria");
        assertThat(toString).contains("searchTerm=''");
        assertThat(toString).contains("firmSearch='FirmSearchForm(firmSearch=null, selectedFirmId=null)'");
        assertThat(toString).contains("userType=null");
        assertThat(toString).contains("showFirmAdmins=false");
    }

    @Test
    void testWithValidUuidFirmSearch() {
        // Given
        UUID validUuid = UUID.randomUUID();
        FirmSearchForm firmSearch = FirmSearchForm.builder().selectedFirmId(validUuid).build();

        // When
        UserSearchCriteria criteria = new UserSearchCriteria("test", firmSearch, UserType.INTERNAL, false);

        // Verify it's a valid UUID format
        assertThat(criteria.getFirmSearch().getSelectedFirmId()).isNotNull();
        assertThat(criteria.getFirmSearch().getSelectedFirmId()).isEqualTo(validUuid);
    }

    @Test
    void testWithLongSearchTerm() {
        // Given
        String longSearchTerm = "This is a very long search term that contains multiple words and should be handled properly by the UserSearchCriteria class";

        // When
        UserSearchCriteria criteria = new UserSearchCriteria(longSearchTerm, firmSearch, UserType.INTERNAL, true);

        // Then
        assertThat(criteria.getSearchTerm()).isEqualTo(longSearchTerm);
    }

    @Test
    void testWithSpecialCharactersInSearchTerm() {
        // Given
        String specialCharSearchTerm = "john@doe.com & jane's-name (test) [user] {search} 100%";

        // When
        UserSearchCriteria criteria = new UserSearchCriteria(specialCharSearchTerm, firmSearch, UserType.INTERNAL, false);

        // Then
        assertThat(criteria.getSearchTerm()).isEqualTo(specialCharSearchTerm);
    }

    @Test
    void testWithUnicodeCharactersInSearchTerm() {
        // Given
        String unicodeSearchTerm = "José García-López 李小明 Ñiño Müller";

        // When
        UserSearchCriteria criteria = new UserSearchCriteria(unicodeSearchTerm, firmSearch, UserType.INTERNAL, true);

        // Then
        assertThat(criteria.getSearchTerm()).isEqualTo(unicodeSearchTerm);
    }

    @Test
    void testConstructorWithAllParametersNull() {
        // When
        UserSearchCriteria criteria = new UserSearchCriteria(null, null, null, false);

        // Then
        assertThat(criteria.getSearchTerm()).isNull();
        assertThat(criteria.getFirmSearch()).isNull();
        assertThat(criteria.getUserType()).isNull();
        assertThat(criteria.isShowFirmAdmins()).isFalse();
    }

    @Test
    void testConstructorWithMixedNullAndValidValues() {
        // Given
        UserType userType = UserType.EXTERNAL;
        FirmSearchForm firmSearchForm = FirmSearchForm.builder().firmSearch("firm-123").build();

        // When
        UserSearchCriteria criteria = new UserSearchCriteria(null, firmSearchForm, userType, true);

        // Then
        assertThat(criteria.getSearchTerm()).isNull();
        assertThat(criteria.getFirmSearch().getFirmSearch()).isNotNull();
        assertThat(criteria.getFirmSearch().getFirmSearch()).isEqualTo("firm-123");
        assertThat(criteria.getUserType()).isEqualTo(userType);
        assertThat(criteria.isShowFirmAdmins()).isTrue();
    }

    @Test
    void testModifyingUserTypesAfterConstruction() {
        // Given
        UserSearchCriteria criteria = new UserSearchCriteria("test", firmSearch, null, false);
        
        // When
        criteria.setUserType(UserType.INTERNAL);

        // Then
        assertThat(criteria.getUserType()).isEqualTo(UserType.INTERNAL);
    }
}
