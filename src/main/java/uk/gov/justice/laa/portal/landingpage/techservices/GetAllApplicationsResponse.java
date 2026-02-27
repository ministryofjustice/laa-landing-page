package uk.gov.justice.laa.portal.landingpage.techservices;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetAllApplicationsResponse implements Serializable {

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("message")
    private String message;

    @JsonProperty("users")
    private List<TechServicesApplication> apps;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TechServicesApplication implements Serializable {

        @JsonProperty("id")
        private String id;

        @JsonProperty("application_name")
        private String name;

        @JsonProperty("homepage_url")
        private String url;

        @JsonProperty("security_groups")
        private List<TechServicesApplication.AppSecurityGroup> securityGroups;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class AppSecurityGroup implements Serializable {
            @JsonProperty("id")
            private String id;
            @JsonProperty("name")
            private String name;
        }

    }
}
