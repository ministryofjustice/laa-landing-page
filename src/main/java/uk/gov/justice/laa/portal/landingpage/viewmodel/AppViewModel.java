package uk.gov.justice.laa.portal.landingpage.viewmodel;

import lombok.Data;

import java.io.Serializable;

@Data
public class AppViewModel implements Serializable {
    private String id;
    private String name;
    private boolean selected;
}
