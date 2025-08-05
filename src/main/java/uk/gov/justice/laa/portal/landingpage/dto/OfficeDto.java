package uk.gov.justice.laa.portal.landingpage.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.laa.portal.landingpage.util.AddressFormatter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfficeDto {
    private UUID id;
    private String code;
    private AddressDto address;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressDto {
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String postcode;
        
        public String getFormattedAddress() {
            return AddressFormatter.formatAddress(addressLine1, addressLine2, city, postcode);
        }
    }
}
