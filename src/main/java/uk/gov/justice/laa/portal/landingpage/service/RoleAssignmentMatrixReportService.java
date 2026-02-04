package uk.gov.justice.laa.portal.landingpage.service;


import jakarta.persistence.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.repository.AppRoleRepository;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
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

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            writer.write("Firm Name,Firm Code");
            for (String role : allRoles) {
                writer.write("," + role);
            }
            writer.newLine();

            for (Map.Entry<FirmDto, Map<String, Integer>> entry : matrix.entrySet()) {
                FirmDto firm = entry.getKey();
                writer.write(csvValue(firm.getName()));
                writer.write(",");
                writer.write(csvValue(firm.getCode()));
                Map<String, Integer> roleCounts = entry.getValue();

                for (String role : allRoles) {
                    writer.write(",");
                    writer.write(csvValue(String.valueOf(roleCounts.getOrDefault(role, 0))));
                }
                writer.newLine();
            }
        } catch (IOException e) {
            log.error("Error writing report to file: {}", e.getMessage());
        }

    }

    private String csvValue(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
