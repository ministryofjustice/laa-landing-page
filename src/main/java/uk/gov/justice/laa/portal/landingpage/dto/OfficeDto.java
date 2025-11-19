package uk.gov.justice.laa.portal.landingpage.dto;

import java.io.Serializable;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.laa.portal.landingpage.utils.AddressFormatter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfficeDto implements Serializable {
    private UUID id;
    private String code;
    private AddressDto address;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressDto implements Serializable {
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
