package uk.gov.justice.laa.portal.landingpage.dto;

import java.util.List;

import uk.gov.justice.laa.portal.landingpage.entity.AuthzRoleType;
import uk.gov.justice.laa.portal.landingpage.model.PaginatedReactivationRequests;
import uk.gov.justice.laa.portal.landingpage.model.ReactivationRequestPageMode;
import uk.gov.justice.laa.portal.landingpage.model.ReactivationRequestStatus;

public record ReactivationRequestsPageData(
        ReactivationRequestPageMode pageMode,
        List<ReactivationRequestStatus> appliedStatuses,
        List<AuthzRoleType> appliedActorRoleTypes,
        PaginatedReactivationRequests paginatedRequests
) {
}
