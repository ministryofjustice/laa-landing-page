package uk.gov.justice.laa.portal.landingpage.utils;

public final class AddressFormatter {
    
    private AddressFormatter() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Formats address components into a comma-separated string.
     * Null or empty components are skipped.
     * 
     * @param addressLine1 First line of the address
     * @param addressLine2 Second line of the address
     * @param city City name
     * @param postcode Postal code
     * @return Formatted address string with components separated by commas
     */
    public static String formatAddress(String addressLine1, String addressLine2, String city, String postcode) {
        StringBuilder sb = new StringBuilder();
        
        if (addressLine1 != null && !addressLine1.trim().isEmpty()) {
            sb.append(addressLine1);
        }
        
        if (addressLine2 != null && !addressLine2.trim().isEmpty()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(addressLine2);
        }
        
        if (city != null && !city.trim().isEmpty()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(city);
        }
        
        if (postcode != null && !postcode.trim().isEmpty()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(postcode);
        }
        
        return sb.toString();
    }
}
