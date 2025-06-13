package uk.gov.justice.laa.portal.landingpage.service;

import uk.gov.justice.laa.portal.landingpage.dto.ClaimEnrichmentRequest;
import uk.gov.justice.laa.portal.landingpage.dto.ClaimEnrichmentResponse;

public interface ClaimEnrichmentInterface {
    ClaimEnrichmentResponse enrichClaims(ClaimEnrichmentRequest request);
}