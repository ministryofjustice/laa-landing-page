package uk.gov.justice.laa.portal.landingpage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimEnrichmentResponse implements Serializable {
    @JsonProperty("data")
    private ResponseData data;
    
    @JsonProperty("success")
    private boolean success;
    
    @JsonProperty("message")
    private String message;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseData implements Serializable {
        @JsonProperty("@odata.type")
        private String odataType;
        
        @JsonProperty("actions")
        private List<ResponseAction> actions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseAction implements Serializable {
        @JsonProperty("@odata.type")
        private String odataType;
        
        @JsonProperty("claims")
        private Map<String, Object> claims;
    }
}
