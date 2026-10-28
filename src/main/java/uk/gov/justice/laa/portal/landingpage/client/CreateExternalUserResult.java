package uk.gov.justice.laa.portal.landingpage.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateExternalUserResult(boolean success, UUID userEntraObjectId,
                                       UUID userProfileId) implements Serializable {
}
