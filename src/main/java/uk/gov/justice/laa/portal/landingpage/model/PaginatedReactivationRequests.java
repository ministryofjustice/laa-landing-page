package uk.gov.justice.laa.portal.landingpage.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class PaginatedReactivationRequests implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<ReactivationRequestListItem> requests = new ArrayList<>();
    private long totalRequests;
    private int totalPages;
    private int currentPage;
    private int pageSize;
}
