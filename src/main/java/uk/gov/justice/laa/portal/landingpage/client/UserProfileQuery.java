package uk.gov.justice.laa.portal.landingpage.client;

public record UserProfileQuery(int pageNumber, int pageSize, String sortBy, String user,
                               String userType, String firm, String firmId,
                               boolean firmAdmins, boolean multiFirmUsers) {
}
