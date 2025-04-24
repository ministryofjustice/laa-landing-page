package uk.gov.justice.laa.portal.landingpage.service;

import uk.gov.justice.laa.portal.landingpage.model.LaaApplication;
import uk.gov.justice.laa.portal.landingpage.utils.HashUtil;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.microsoft.graph.models.AppRoleAssignment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A temporary store for LAA applications of their name, description, url.
 *
 * The class loads all the LAA applications details during server startup and map them against the
 * user assigned Entra app details and help populate on landing age.
 */
@Component
public class LaaAppDetailsStore {

    private static final String LAA_APP_META_FILE_PATH = "data/laa-apps-details.csv";
    private static List<LaaApplication> laaApplications;

    public static Set<LaaApplication> getUserAssignedApps(List<AppRoleAssignment> appRoleAssignments) {
        return laaApplications.stream().filter(app -> appRoleAssignments.stream()
                        .map(AppRoleAssignment::getResourceId).anyMatch(resId -> HashUtil.sha256(resId).equals(app.getId())))
                .collect(Collectors.toSet());
    }

    @PostConstruct
    public void populateLaaApps() throws IOException {
        File csvFile = new ClassPathResource(LAA_APP_META_FILE_PATH).getFile();
        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = mapper
                .schemaFor(LaaApplication.class)
                .withoutHeader();

        try (MappingIterator<LaaApplication> iterator =
                     mapper.readerFor(LaaApplication.class).with(schema).readValues(csvFile)) {
            laaApplications = iterator.readAll();
        }
    }

}
