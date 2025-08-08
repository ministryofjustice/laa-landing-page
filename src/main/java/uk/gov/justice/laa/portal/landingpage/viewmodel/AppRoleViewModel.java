package uk.gov.justice.laa.portal.landingpage.viewmodel;

import lombok.Data;

@Data
public class AppRoleViewModel {
    private String id;
    private String name;
    private String appName;
    private String description;
    private boolean selected;
}
