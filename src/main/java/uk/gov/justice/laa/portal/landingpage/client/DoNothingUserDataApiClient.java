package uk.gov.justice.laa.portal.landingpage.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.datauserapi.contracts.request.BulkDeactivateFirmUsersRequest;
import uk.gov.justice.laa.datauserapi.contracts.response.CommandResult;
import uk.gov.justice.laa.datauserapi.contracts.response.FirmOfficesResponse;
import uk.gov.justice.laa.datauserapi.contracts.response.FirmPageResponse;
import uk.gov.justice.laa.datauserapi.contracts.response.FirmResponse;
import uk.gov.justice.laa.datauserapi.contracts.response.FirmSearchResponse;
import uk.gov.justice.laa.datauserapi.contracts.response.UserAccountSummaryPage;

import java.util.UUID;

/**
 * No-op implementation of {@link UserDataApiClient}.
 *
 * <p>Active when {@code app.enable.user.data.api.call=false}.
 * Use this in local development environments where laa-data-user-api is not reachable.
 * Set {@code USER_DATA_API_CALLS_ENABLED=false} to activate.
 */
@Service
@ConditionalOnProperty(name = "app.enable.user.data.api.call", havingValue = "false")
public class DoNothingUserDataApiClient implements UserDataApiClient {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public UserProfileDetailResponse me(String userAccessToken, String userOid, String correlationId) {
        logger.debug("DoNothing: skipping data API /me call (USER_DATA_API_CALLS_ENABLED=false)");
        return new UserProfileDetailResponse(
                UUID.fromString(userOid.isBlank() ? "00000000-0000-0000-0000-000000000000" : userOid),
                userOid, "", "", "", null, null, "UNKNOWN", "UNKNOWN", false, false, false);
    }

    @Override
    public FirmPageResponse queryFirms(String userAccessToken, String userOid, String correlationId,
                                       String firm, String firmType, int pageNumber, int pageSize, String sortBy) {
        throw disabled();
    }

    @Override
    public FirmResponse queryFirmById(String userAccessToken, String userOid, String correlationId, String firmId) {
        throw disabled();
    }

    @Override
    public FirmSearchResponse queryFirmsSearch(String userAccessToken, String userOid, String correlationId,
                                               String query, int limit) {
        throw disabled();
    }

    @Override
    public FirmOfficesResponse queryFirmOffices(String userAccessToken, String userOid, String correlationId,
                                                String firmId) {
        throw disabled();
    }

    @Override
    public CommandResult bulkDeactivateFirmUsers(String userAccessToken, String userOid, String correlationId,
                                                 BulkDeactivateFirmUsersRequest request) {
        throw disabled();
    }

    private UserDataApiClientException disabled() {
        logger.error("Data API call requested but USER_DATA_API_CALLS_ENABLED=false");
        return new UserDataApiClientException("Data API calls are disabled", 503);
    }

    @Override
    public UserAccountSummaryPage queryUsersByEmail(String userAccessToken, String userOid, String correlationId,
                                                   String email, int pageNumber) {
        throw disabled();
    }

    @Override
    public CreateExternalUserResult createExternalUser(String userAccessToken, String userOid, String correlationId,
                                                       CreateExternalUserRequest request) {
        throw disabled();
    }
}
