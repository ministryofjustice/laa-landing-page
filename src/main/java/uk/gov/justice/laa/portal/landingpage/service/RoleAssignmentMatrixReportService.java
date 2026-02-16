package uk.gov.justice.laa.portal.landingpage.service;


import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import jakarta.persistence.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.repository.AppRoleRepository;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoleAssignmentMatrixReportService {

    private final FirmRepository firmRepository;
    private final AppRoleRepository appRoleRepository;
    private String reportDirectory = System.getProperty("java.io.tmpdir") + File.separator + "reports";
    private final DateTimeFormatter fileTimestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm");

    public void getRoleAssignmentMatrixReport() {

        List<String> allRoles = appRoleRepository.getExternalRoleNames();
        List<Tuple> rows = firmRepository.findRoleCountsByFirm();
        Map<FirmDto, Map<String, Integer>> matrix = buildRoleMatrix(rows);
        writeToCsv(allRoles, matrix);
        log.info("Role assignment matrix report successfully written to file");
    }

    private Map<FirmDto, Map<String, Integer>> buildRoleMatrix(List<Tuple> rows) {
        Map<FirmDto, Map<String, Integer>> matrix = new LinkedHashMap<>();

        for (Tuple t : rows) {
            FirmDto firm = FirmDto.builder()
                    .id(t.get("firmId", UUID.class))
                    .name(t.get("firmName", String.class))
                    .code(t.get("firmCode", String.class))
                    .build();

            matrix.putIfAbsent(firm, new HashMap<>());
            Map<String, Integer> roleMap = matrix.get(firm);
            String roleName = t.get("roleName", String.class);
            Number userCountNumber = t.get("userCount", Number.class);
            int userCount = (userCountNumber == null) ? 0 : userCountNumber.intValue();

            roleMap.put(roleName, userCount);
        }

        return matrix;
    }

    public void writeToCsv(List<String> allRoles, Map<FirmDto, Map<String, Integer>> matrix) {

        String timestamp = LocalDateTime.now().format(fileTimestamp);
        Path outputPath = Path.of(reportDirectory, "SiLAS-role-assignment-matrix-report-" + timestamp + ".csv");

        try {
            Files.createDirectories(outputPath.getParent());

            CsvSchema.Builder schemaBuilder = CsvSchema.builder()
                    .addColumn("Firm Name")
                    .addColumn("Firm Code");

            for (String role : allRoles) {
                schemaBuilder.addColumn(role);
            }

            CsvSchema schema = schemaBuilder
                    .setUseHeader(true)
                    .build();

            CsvMapper csvMapper = CsvMapper.builder().build();

            try (Writer writer = Files.newBufferedWriter(outputPath);
                 SequenceWriter sequenceWriter = csvMapper.writer(schema).writeValues(writer)) {

                for (Map.Entry<FirmDto, Map<String, Integer>> entry : matrix.entrySet()) {
                    FirmDto firm = entry.getKey();
                    Map<String, Integer> roleCounts = entry.getValue();

                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("Firm Name", firm.getName());
                    row.put("Firm Code", firm.getCode());

                    for (String role : allRoles) {
                        row.put(role, roleCounts.getOrDefault(role, 0));
                    }

                    sequenceWriter.write(row);
                }
            }

        } catch (IOException e) {
            log.error("Error writing report to file: {}", e.getMessage());
        }
    }

}
