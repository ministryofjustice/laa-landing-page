package uk.gov.justice.laa.portal.landingpage.service;

import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;

public class FirmComparatorByRelevance {

    public static int relevance(FirmDto firmDto, String query) {
        String name = firmDto.getName();
        String code = firmDto.getCode();

        // Handle null code - only compare with name
        if (code == null) {
            if (name.equals(query)) {
                return 100;
            }
            if (name.equalsIgnoreCase(query)) {
                return 90;
            }
            if (name.startsWith(query)) {
                return 80;
            }
            if (name.toLowerCase().startsWith(query.toLowerCase())) {
                return 70;
            }
            if (name.contains(query)) {
                return 60;
            }
            if (name.toLowerCase().contains(query.toLowerCase())) {
                return 50;
            }
            return 0;
        }

        if (code.equals(query) || name.equals(query)) {
            return 100;
        }
        if (code.equalsIgnoreCase(query) || name.equalsIgnoreCase(query)) {
            return 90;
        }
        if (code.startsWith(query) || name.startsWith(query)) {
            return 80;
        }
        if (code.toLowerCase().startsWith(query.toLowerCase()) || name.toLowerCase().startsWith(query)) {
            return 70;
        }
        if (code.contains(query) || name.contains(query)) {
            return 60;
        }
        if (code.toLowerCase().contains(query.toLowerCase()) || name.toLowerCase().contains(query)) {
            return 50;
        }

        return 0;
    }
}
