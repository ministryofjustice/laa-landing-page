package uk.gov.justice.laa.portal.landingpage.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import uk.gov.justice.laa.portal.landingpage.utils.AddressFormatter;

import java.io.Serial;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OfficeModel implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String code;
    private Address address;
    private String id;
    private boolean selected;

    @Data
    @SuperBuilder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Address implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String addressLine1;
        private String addressLine2;
        private String addressLine3;
        private String city;
        private String postcode;

        public String getFormattedAddress() {
            String formattedAddress = AddressFormatter.formatAddress(addressLine1, addressLine2, addressLine3, city, postcode);
            return formattedAddress.isBlank() ? "Unknown" : formattedAddress;
        }
    }
}
