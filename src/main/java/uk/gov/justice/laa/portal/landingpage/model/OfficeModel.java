package uk.gov.justice.laa.portal.landingpage.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import uk.gov.justice.laa.portal.landingpage.util.AddressFormatter;

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
            return AddressFormatter.formatAddress(addressLine1, addressLine2, city, postcode);
        }
    }
}
