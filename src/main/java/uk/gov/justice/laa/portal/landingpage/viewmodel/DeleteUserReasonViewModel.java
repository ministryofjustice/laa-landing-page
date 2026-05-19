package uk.gov.justice.laa.portal.landingpage.viewmodel;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

import lombok.Data;

@Data
public class DeleteUserReasonViewModel implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private UUID id;
    private String code;
    private String label;
}
