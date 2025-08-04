package uk.gov.justice.laa.portal.landingpage.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OfficeModel {
    private String code;
    private Address address;
    private String id;
    private boolean selected;

    @Data
    @SuperBuilder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Address {
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String postcode;
        
        public String getFormattedAddress() {
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
}
