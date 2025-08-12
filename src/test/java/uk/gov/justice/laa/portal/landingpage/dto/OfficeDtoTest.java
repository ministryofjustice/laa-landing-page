package uk.gov.justice.laa.portal.landingpage.dto;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class OfficeDtoTest {

    @Test
    void testFormattedAddress_AllFieldsPresent() {
        // Given
        OfficeDto.AddressDto address = OfficeDto.AddressDto.builder()
                .addressLine1("123 Main Street")
                .addressLine2("Suite 200")
                .city("London")
                .postcode("SW1A 1AA")
                .build();

        // When
        String formatted = address.getFormattedAddress();

        // Then
        assertThat(formatted).isEqualTo("123 Main Street, Suite 200, London, SW1A 1AA");
    }

    @Test
    void testFormattedAddress_MissingAddressLine2() {
        // Given
        OfficeDto.AddressDto address = OfficeDto.AddressDto.builder()
                .addressLine1("123 Main Street")
                .addressLine2(null)
                .city("London")
                .postcode("SW1A 1AA")
                .build();

        // When
        String formatted = address.getFormattedAddress();

        // Then
        assertThat(formatted).isEqualTo("123 Main Street, London, SW1A 1AA");
    }

    @Test
    void testFormattedAddress_EmptyAddressLine2() {
        // Given
        OfficeDto.AddressDto address = OfficeDto.AddressDto.builder()
                .addressLine1("123 Main Street")
                .addressLine2("")
                .city("London")
                .postcode("SW1A 1AA")
                .build();

        // When
        String formatted = address.getFormattedAddress();

        // Then
        assertThat(formatted).isEqualTo("123 Main Street, London, SW1A 1AA");
    }

    @Test
    void testFormattedAddress_WhitespaceOnlyAddressLine2() {
        // Given
        OfficeDto.AddressDto address = OfficeDto.AddressDto.builder()
                .addressLine1("123 Main Street")
                .addressLine2("   ")
                .city("London")
                .postcode("SW1A 1AA")
                .build();

        // When
        String formatted = address.getFormattedAddress();

        // Then
        assertThat(formatted).isEqualTo("123 Main Street, London, SW1A 1AA");
    }

    @Test
    void testFormattedAddress_OnlyRequiredFields() {
        // Given
        OfficeDto.AddressDto address = OfficeDto.AddressDto.builder()
                .addressLine1("123 Main Street")
                .city("London")
                .postcode("SW1A 1AA")
                .build();

        // When
        String formatted = address.getFormattedAddress();

        // Then
        assertThat(formatted).isEqualTo("123 Main Street, London, SW1A 1AA");
    }

    @Test
    void testFormattedAddress_MissingMultipleFields() {
        // Given
        OfficeDto.AddressDto address = OfficeDto.AddressDto.builder()
                .addressLine1("123 Main Street")
                .addressLine2("")
                .city("")
                .postcode("SW1A 1AA")
                .build();

        // When
        String formatted = address.getFormattedAddress();

        // Then
        assertThat(formatted).isEqualTo("123 Main Street, SW1A 1AA");
    }
}
