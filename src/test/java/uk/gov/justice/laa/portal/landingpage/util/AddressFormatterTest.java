package uk.gov.justice.laa.portal.landingpage.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class AddressFormatterTest {

    @Test
    void formatAddress_allFieldsPresent_returnsCommaSeparatedAddress() {
        String result = AddressFormatter.formatAddress("123 Main St", "Apt 4", "London", "SW1A 1AA");
        assertEquals("123 Main St, Apt 4, London, SW1A 1AA", result);
    }

    @Test
    void formatAddress_someFieldsNull_skipsNullFields() {
        String result = AddressFormatter.formatAddress("123 Main St", null, "London", "SW1A 1AA");
        assertEquals("123 Main St, London, SW1A 1AA", result);
    }

    @Test
    void formatAddress_someFieldsEmpty_skipsEmptyFields() {
        String result = AddressFormatter.formatAddress("123 Main St", "", "London", "SW1A 1AA");
        assertEquals("123 Main St, London, SW1A 1AA", result);
    }

    @Test
    void formatAddress_allFieldsNull_returnsEmptyString() {
        String result = AddressFormatter.formatAddress(null, null, null, null);
        assertEquals("", result);
    }

    @Test
    void formatAddress_allFieldsEmpty_returnsEmptyString() {
        String result = AddressFormatter.formatAddress("", " ", "   ", "");
        assertEquals("", result);
    }

    @Test
    void formatAddress_onlyOneFieldPresent_returnsSingleField() {
        String result = AddressFormatter.formatAddress(null, null, "London", null);
        assertEquals("London", result);
    }

    @Test
    void formatAddress_fieldsWithWhitespaceOnly_skipsWhitespaceFields() {
        String result = AddressFormatter.formatAddress("  ", "Apt 4", " ", "SW1A 1AA");
        assertEquals("Apt 4, SW1A 1AA", result);
    }
}