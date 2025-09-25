package uk.gov.justice.laa.portal.landingpage.service;

import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;

public class FirmComparatorByRelevance {

    public static int relevance(FirmDto firmDto, String query) {
        String name = firmDto.getName();
        String code = firmDto.getCode();

        if (code.equals(query) || name.equals(query)) return 100;
        if (code.equalsIgnoreCase(query) || name.equalsIgnoreCase(query)) return 90;
        if (code.startsWith(query) || name.startsWith(query)) return 80;
        if (code.toLowerCase().startsWith(query.toLowerCase()) || name.toLowerCase().startsWith(query)) return 70;
        if (code.contains(query) || name.contains(query)) return 60;
        if (code.toLowerCase().contains(query.toLowerCase()) || name.toLowerCase().contains(query)) return 50;

        return 0;
    }
}
