package uk.gov.justice.laa.portal.landingpage.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CccdApplicationEntraIds {

    private final List<String> entraIds;

    public CccdApplicationEntraIds(@Value("${cccd.application.entra.ids:}") List<String> entraIds) {
        this.entraIds = new ArrayList<>();
        this.entraIds.addAll(entraIds);
    }

    public void add(String entraId) {
        this.entraIds.add(entraId);
    }

    public void remove(String entraId) {
        this.entraIds.remove(entraId);
    }

    public boolean contains(String entraId) {
        return this.entraIds.contains(entraId);
    }

}
