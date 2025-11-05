package uk.gov.justice.laa.portal.landingpage.model;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class OfficeModelTest {

    @Test
    void testFormattedAddress_AllFieldsPresent() {
        // Given
        OfficeModel.Address address = OfficeModel.Address.builder()
                .addressLine1("123 Main Street")
                .addressLine2("Suite 200")
                .addressLine3("Greater London")
                .city("London")
                .postcode("SW1A 1AA")
                .build();

        // When
        String formatted = address.getFormattedAddress();

        // Then
        assertThat(formatted).isEqualTo("123 Main Street, Suite 200, Greater London, London, SW1A 1AA");
    }

    @Test
    void testFormattedAddress_MissingAddressLine2() {
        // Given
        OfficeModel.Address address = OfficeModel.Address.builder()
                .addressLine1("123 Main Street")
                .addressLine2(null)
                .addressLine3("Greater London")
                .city("London")
                .postcode("SW1A 1AA")
                .build();

        // When
        String formatted = address.getFormattedAddress();

        // Then
        assertThat(formatted).isEqualTo("123 Main Street, Greater London, London, SW1A 1AA");
    }

    @Test
    void testFormattedAddress_EmptyAddressLine2() {
        // Given
        OfficeModel.Address address = OfficeModel.Address.builder()
                .addressLine1("123 Main Street")
                .addressLine2("")
                .addressLine3("Greater London")
                .city("London")
                .postcode("SW1A 1AA")
                .build();

        // When
        String formatted = address.getFormattedAddress();

        // Then
        assertThat(formatted).isEqualTo("123 Main Street, London, Greater London, SW1A 1AA");
    }

    @Test
    void testFormattedAddress_MissingAddressLine3() {
        // Given
        OfficeModel.Address address = OfficeModel.Address.builder()
                .addressLine1("123 Main Street")
                .addressLine2("Suite 200")
                .addressLine3(null)
                .city("London")
                .postcode("SW1A 1AA")
                .build();

        // When
        String formatted = address.getFormattedAddress();

        // Then
        assertThat(formatted).isEqualTo("123 Main Street, Suite 200, London, SW1A 1AA");
    }

    @Test
    void testFormattedAddress_EmptyAddressLine3() {
        // Given
        OfficeModel.Address address = OfficeModel.Address.builder()
                .addressLine1("123 Main Street")
                .addressLine2("Suite 200")
                .addressLine3("")
                .city("London")
                .postcode("SW1A 1AA")
                .build();

        // When
        String formatted = address.getFormattedAddress();

        // Then
        assertThat(formatted).isEqualTo("123 Main Street, Suite 200, London, SW1A 1AA");
    }
}
