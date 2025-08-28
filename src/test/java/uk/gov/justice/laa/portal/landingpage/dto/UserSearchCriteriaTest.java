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
        assertThat(criteria.getUserTypes()).isNull();
        assertThat(criteria.isShowFirmAdmins()).isFalse();
    }

    @Test
    void testAllArgsConstructor() {
        // Given
        String searchTerm = "john doe";
        FirmSearchForm firmSearch = FirmSearchForm.builder().selectedFirmId(UUID.randomUUID()).build();
        List<UserType> userTypes = List.of(UserType.EXTERNAL_SINGLE_FIRM, UserType.EXTERNAL_SINGLE_FIRM_ADMIN);
        boolean showFirmAdmins = true;

        // When
        UserSearchCriteria criteria = new UserSearchCriteria(searchTerm, firmSearch, userTypes, showFirmAdmins);

        // Then
        assertThat(criteria.getSearchTerm()).isEqualTo(searchTerm);
        assertThat(criteria.getFirmSearch()).isEqualTo(firmSearch);
        assertThat(criteria.getUserTypes()).isEqualTo(userTypes);
        assertThat(criteria.isShowFirmAdmins()).isEqualTo(showFirmAdmins);
    }

    @Test
    void testSettersAndGetters() {
        // Given
        UserSearchCriteria criteria = new UserSearchCriteria();
        String searchTerm = "jane smith";
        FirmSearchForm firmSearch = FirmSearchForm.builder().selectedFirmId(UUID.randomUUID()).build();
        List<UserType> userTypes = List.of(UserType.INTERNAL);
        boolean showFirmAdmins = false;

        // When
        criteria.setSearchTerm(searchTerm);
        criteria.setFirmSearch(firmSearch);
        criteria.setUserTypes(userTypes);
        criteria.setShowFirmAdmins(showFirmAdmins);

        // Then
        assertThat(criteria.getSearchTerm()).isEqualTo(searchTerm);
        assertThat(criteria.getFirmSearch()).isEqualTo(firmSearch);
        assertThat(criteria.getUserTypes()).isEqualTo(userTypes);
        assertThat(criteria.isShowFirmAdmins()).isEqualTo(showFirmAdmins);
    }

    @Test
    void testWithNullValues() {
        // Given
        UserSearchCriteria criteria = new UserSearchCriteria();

        // When
        criteria.setSearchTerm(null);
        criteria.setFirmSearch(null);
        criteria.setUserTypes(null);
        criteria.setShowFirmAdmins(false);

        // Then
        assertThat(criteria.getSearchTerm()).isNull();
        assertThat(criteria.getFirmSearch()).isNull();
        assertThat(criteria.getUserTypes()).isNull();
        assertThat(criteria.isShowFirmAdmins()).isFalse();
    }

    @Test
    void testWithEmptyStringValues() {
        // Given
        UserSearchCriteria criteria = new UserSearchCriteria();

        // When
        criteria.setSearchTerm("");
        criteria.setFirmSearch(null);
        criteria.setUserTypes(new ArrayList<>());

        // Then
        assertThat(criteria.getSearchTerm()).isEmpty();
        assertThat(criteria.getFirmSearch()).isNull();
        assertThat(criteria.getUserTypes()).isEmpty();
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
            // Given
            List<UserType> userTypes = List.of(userType);
            
            // When
            UserSearchCriteria criteria = new UserSearchCriteria("test", null, userTypes, true);
            
            // Then
            assertThat(criteria.getUserTypes()).hasSize(1);
            assertThat(criteria.getUserTypes()).contains(userType);
        }
    }

    @Test
    void testWithMultipleUserTypes() {
        // Given
        List<UserType> userTypes = List.of(
                UserType.EXTERNAL_SINGLE_FIRM,
                UserType.EXTERNAL_SINGLE_FIRM_ADMIN,
                UserType.EXTERNAL_MULTI_FIRM
        );

        // When
        UserSearchCriteria criteria = new UserSearchCriteria("search", null, userTypes, true);

        // Then
        assertThat(criteria.getUserTypes()).hasSize(3);
        assertThat(criteria.getUserTypes()).containsExactlyInAnyOrder(
                UserType.EXTERNAL_SINGLE_FIRM,
                UserType.EXTERNAL_SINGLE_FIRM_ADMIN,
                UserType.EXTERNAL_MULTI_FIRM
        );
    }

    @Test
    void testWithExternalUserTypes() {
        // Given
        List<UserType> externalTypes = UserType.EXTERNAL_TYPES;

        // When
        UserSearchCriteria criteria = new UserSearchCriteria("test", firmSearch, externalTypes, false);

        // Then
        assertThat(criteria.getUserTypes()).isEqualTo(UserType.EXTERNAL_TYPES);
        assertThat(criteria.getUserTypes()).contains(
                UserType.EXTERNAL_SINGLE_FIRM,
                UserType.EXTERNAL_SINGLE_FIRM_ADMIN,
                UserType.EXTERNAL_MULTI_FIRM
        );
        assertThat(criteria.getUserTypes()).doesNotContain(UserType.INTERNAL);
    }

    @Test
    void testWithInternalUserTypes() {
        // Given
        List<UserType> internalTypes = UserType.INTERNAL_TYPES;

        // When
        UserSearchCriteria criteria = new UserSearchCriteria("test", firmSearch, internalTypes, true);

        // Then
        assertThat(criteria.getUserTypes()).isEqualTo(UserType.INTERNAL_TYPES);
        assertThat(criteria.getUserTypes()).contains(UserType.INTERNAL);
        assertThat(criteria.getUserTypes()).doesNotContain(
                UserType.EXTERNAL_SINGLE_FIRM,
                UserType.EXTERNAL_SINGLE_FIRM_ADMIN,
                UserType.EXTERNAL_MULTI_FIRM
        );
    }

    @Test
    void testShowFirmAdminsFlagTrue() {
        // Given & When
        UserSearchCriteria criteria = new UserSearchCriteria("test", firmSearch, List.of(UserType.INTERNAL), true);

        // Then
        assertThat(criteria.isShowFirmAdmins()).isTrue();
    }

    @Test
    void testShowFirmAdminsFlagFalse() {
        // Given & When
        UserSearchCriteria criteria = new UserSearchCriteria("test", firmSearch, List.of(UserType.INTERNAL), false);

        // Then
        assertThat(criteria.isShowFirmAdmins()).isFalse();
    }

    @Test
    void testToString() {
        // Given
        String searchTerm = "john doe";
        UUID firmSearchId = UUID.randomUUID();
        FirmSearchForm firmSearchForm = FirmSearchForm.builder().firmSearch("firm-123").selectedFirmId(firmSearchId).build();
        List<UserType> userTypes = List.of(UserType.EXTERNAL_SINGLE_FIRM, UserType.INTERNAL);
        boolean showFirmAdmins = true;

        UserSearchCriteria criteria = new UserSearchCriteria(searchTerm, firmSearchForm, userTypes, showFirmAdmins);

        // When
        String toString = criteria.toString();

        // Then
        assertThat(toString).contains("UserSearchCriteria");
        assertThat(toString).contains("searchTerm='john doe'");
        assertThat(toString).contains("firmSearch='FirmSearchForm(firmSearch=firm-123, selectedFirmId=" + firmSearchId + ")'");
        assertThat(toString).contains("userTypes=[EXTERNAL_SINGLE_FIRM, INTERNAL]");
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
        assertThat(toString).contains("userTypes=null");
        assertThat(toString).contains("showFirmAdmins=false");
    }

    @Test
    void testToStringWithEmptyValues() {
        // Given
        UserSearchCriteria criteria = new UserSearchCriteria("", firmSearch, new ArrayList<>(), false);

        // When
        String toString = criteria.toString();

        // Then
        assertThat(toString).contains("UserSearchCriteria");
        assertThat(toString).contains("searchTerm=''");
        assertThat(toString).contains("firmSearch='FirmSearchForm(firmSearch=null, selectedFirmId=null)'");
        assertThat(toString).contains("userTypes=[]");
        assertThat(toString).contains("showFirmAdmins=false");
    }

    @Test
    void testWithValidUuidFirmSearch() {
        // Given
        UUID validUuid = UUID.randomUUID();
        FirmSearchForm firmSearch = FirmSearchForm.builder().selectedFirmId(validUuid).build();

        // When
        UserSearchCriteria criteria = new UserSearchCriteria("test", firmSearch, List.of(UserType.INTERNAL), false);

        // Verify it's a valid UUID format
        assertThat(criteria.getFirmSearch().getSelectedFirmId()).isNotNull();
        assertThat(criteria.getFirmSearch().getSelectedFirmId()).isEqualTo(validUuid);
    }

    @Test
    void testWithLongSearchTerm() {
        // Given
        String longSearchTerm = "This is a very long search term that contains multiple words and should be handled properly by the UserSearchCriteria class";

        // When
        UserSearchCriteria criteria = new UserSearchCriteria(longSearchTerm, firmSearch, List.of(UserType.INTERNAL), true);

        // Then
        assertThat(criteria.getSearchTerm()).isEqualTo(longSearchTerm);
    }

    @Test
    void testWithSpecialCharactersInSearchTerm() {
        // Given
        String specialCharSearchTerm = "john@doe.com & jane's-name (test) [user] {search} 100%";

        // When
        UserSearchCriteria criteria = new UserSearchCriteria(specialCharSearchTerm, firmSearch, List.of(UserType.INTERNAL), false);

        // Then
        assertThat(criteria.getSearchTerm()).isEqualTo(specialCharSearchTerm);
    }

    @Test
    void testWithUnicodeCharactersInSearchTerm() {
        // Given
        String unicodeSearchTerm = "José García-López 李小明 Ñiño Müller";

        // When
        UserSearchCriteria criteria = new UserSearchCriteria(unicodeSearchTerm, firmSearch, List.of(UserType.INTERNAL), true);

        // Then
        assertThat(criteria.getSearchTerm()).isEqualTo(unicodeSearchTerm);
    }

    @Test
    void testMutableUserTypesList() {
        // Given
        List<UserType> userTypes = new ArrayList<>();
        userTypes.add(UserType.INTERNAL);
        UserSearchCriteria criteria = new UserSearchCriteria("test", firmSearch, userTypes, false);

        // When - modify the original list
        userTypes.add(UserType.EXTERNAL_SINGLE_FIRM);

        // Then - the criteria should reflect the change (since it's the same reference)
        assertThat(criteria.getUserTypes()).hasSize(2);
        assertThat(criteria.getUserTypes()).contains(UserType.INTERNAL, UserType.EXTERNAL_SINGLE_FIRM);
    }

    @Test
    void testImmutableUserTypesList() {
        // Given
        List<UserType> userTypes = List.of(UserType.INTERNAL); // Immutable list
        UserSearchCriteria criteria = new UserSearchCriteria("test", firmSearch, userTypes, false);

        // Then
        assertThat(criteria.getUserTypes()).hasSize(1);
        assertThat(criteria.getUserTypes()).contains(UserType.INTERNAL);
        
        // Verify the list is the same reference
        assertThat(criteria.getUserTypes()).isSameAs(userTypes);
    }

    @Test
    void testConstructorWithAllParametersNull() {
        // When
        UserSearchCriteria criteria = new UserSearchCriteria(null, null, null, false);

        // Then
        assertThat(criteria.getSearchTerm()).isNull();
        assertThat(criteria.getFirmSearch()).isNull();
        assertThat(criteria.getUserTypes()).isNull();
        assertThat(criteria.isShowFirmAdmins()).isFalse();
    }

    @Test
    void testConstructorWithMixedNullAndValidValues() {
        // Given
        List<UserType> userTypes = List.of(UserType.EXTERNAL_SINGLE_FIRM_ADMIN);
        FirmSearchForm firmSearchForm = FirmSearchForm.builder().firmSearch("firm-123").build();

        // When
        UserSearchCriteria criteria = new UserSearchCriteria(null, firmSearchForm, userTypes, true);

        // Then
        assertThat(criteria.getSearchTerm()).isNull();
        assertThat(criteria.getFirmSearch().getFirmSearch()).isNotNull();
        assertThat(criteria.getFirmSearch().getFirmSearch()).isEqualTo("firm-123");
        assertThat(criteria.getUserTypes()).isEqualTo(userTypes);
        assertThat(criteria.isShowFirmAdmins()).isTrue();
    }

    @Test
    void testModifyingUserTypesAfterConstruction() {
        // Given
        UserSearchCriteria criteria = new UserSearchCriteria("test", firmSearch, new ArrayList<>(), false);
        
        // When
        criteria.getUserTypes().add(UserType.INTERNAL);
        criteria.getUserTypes().add(UserType.EXTERNAL_SINGLE_FIRM);

        // Then
        assertThat(criteria.getUserTypes()).hasSize(2);
        assertThat(criteria.getUserTypes()).contains(UserType.INTERNAL, UserType.EXTERNAL_SINGLE_FIRM);
    }

    @Test
    void testReplacingUserTypesList() {
        // Given
        UserSearchCriteria criteria = new UserSearchCriteria("test", firmSearch, List.of(UserType.INTERNAL), false);
        List<UserType> newUserTypes = List.of(UserType.EXTERNAL_SINGLE_FIRM, UserType.EXTERNAL_MULTI_FIRM);

        // When
        criteria.setUserTypes(newUserTypes);

        // Then
        assertThat(criteria.getUserTypes()).isEqualTo(newUserTypes);
        assertThat(criteria.getUserTypes()).hasSize(2);
        assertThat(criteria.getUserTypes()).contains(UserType.EXTERNAL_SINGLE_FIRM, UserType.EXTERNAL_MULTI_FIRM);
        assertThat(criteria.getUserTypes()).doesNotContain(UserType.INTERNAL);
    }
}
