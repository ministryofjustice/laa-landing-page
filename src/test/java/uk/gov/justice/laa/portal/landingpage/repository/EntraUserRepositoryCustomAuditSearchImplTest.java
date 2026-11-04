package uk.gov.justice.laa.portal.landingpage.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EntraUserRepositoryCustomAuditSearchImplTest {

    private static final String MULTI_FIRM_QUERY_BODY = """
            FROM entra_user u
            LEFT JOIN user_profile up ON up.entra_user_id = u.id
            WHERE (CAST(:searchTerm AS varchar) IS NULL
                OR CAST(:searchTerm AS varchar) = ''
                OR LOWER(CONCAT(u.first_name, ' ', u.last_name))
                    LIKE LOWER(CONCAT('%', CAST(:searchTerm AS varchar), '%'))
                OR LOWER(u.email)
                    LIKE LOWER(CONCAT('%', CAST(:searchTerm AS varchar), '%')))
              AND (CAST(:firmId AS varchar) IS NULL
                OR EXISTS (
                    SELECT 1
                    FROM user_profile upf
                    WHERE upf.entra_user_id = u.id
                      AND upf.firm_id = CAST(:firmId AS UUID)))
              AND (CAST(:silasRole AS varchar) IS NULL
                OR CAST(:silasRole AS varchar) = ''
                OR EXISTS (
                    SELECT 1
                    FROM user_profile up2
                    JOIN user_profile_app_role upar
                      ON upar.user_profile_id = up2.id
                    JOIN app_role ar
                      ON ar.id = upar.app_role_id
                    WHERE up2.entra_user_id = u.id
                      AND ar.authz_role = TRUE
                      AND ar.name = CAST(:silasRole AS varchar)))
              AND (CAST(:appId AS varchar) IS NULL
                OR EXISTS (
                    SELECT 1
                    FROM user_profile up3
                    JOIN user_profile_app_role upar
                      ON upar.user_profile_id = up3.id
                    JOIN app_role ar
                      ON ar.id = upar.app_role_id
                    WHERE up3.entra_user_id = u.id
                      AND ar.app_id = CAST(:appId AS uuid)))
              AND (CAST(:userType AS varchar) IS NULL
                OR (CAST(:userType AS varchar) = 'EXTERNAL'
                    AND NOT EXISTS (
                        SELECT 1
                        FROM user_profile upt
                        WHERE upt.entra_user_id = u.id))
                OR EXISTS (
                    SELECT 1
                    FROM user_profile up4
                    WHERE up4.entra_user_id = u.id
                      AND up4.user_type = CAST(:userType AS varchar)))
              AND (CAST(:multiFirm AS boolean) IS NULL
                OR u.multi_firm_user = CAST(:multiFirm AS boolean))
              AND ((CAST(:inactiveSinceDateFlag AS boolean) IS NULL
                    AND CAST(:neverActivated AS boolean) IS NULL)
                OR (CAST(:neverActivated AS boolean) IS NOT NULL
                    AND u.invitation_status != 'VERIFICATION_SUCCESS'
                    AND NOT EXISTS (
                        SELECT 1
                        FROM user_profile up5
                        WHERE up5.entra_user_id = u.id
                          AND up5.user_type = 'INTERNAL')))
              AND (CAST(:silasStatuses AS varchar) IS NULL
                OR CAST(:silasStatuses AS varchar) = ''
                OR u.status = ANY(STRING_TO_ARRAY(CAST(:silasStatuses AS varchar), ',')))
            """;

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query dataQuery = mock(Query.class);
    private final Query countQuery = mock(Query.class);
    private final EntraUserRepositoryCustomAuditSearchImpl repository =
            new EntraUserRepositoryCustomAuditSearchImpl();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(repository, "entityManager", entityManager);
        when(entityManager.createNativeQuery(anyString())).thenReturn(dataQuery, countQuery);
        when(dataQuery.getResultList()).thenReturn(List.of());
        when(countQuery.getSingleResult()).thenReturn(0L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ASC", "DESC"})
    void multiFirmSortGroupsAccountTypesInRequestedDirection(String direction) {
        repository.findAuditUsersWithDynamicProjection(
                "MULTI_FIRM", null, null, null, null, null, null, null, null, null,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.valueOf(direction), "predictionValue")));

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(entityManager, times(2)).createNativeQuery(queryCaptor.capture());
        String expectedSelect = """
                SELECT u.id AS userId, CASE WHEN COALESCE(LOWER(up.user_type), 'external') = 'external'
                AND COALESCE(u.multi_firm_user, false) = true THEN 3
                WHEN COALESCE(LOWER(up.user_type), 'external') = 'external'
                AND COALESCE(u.multi_firm_user, false) = false THEN 2
                ELSE 1 END AS predictionValue
                """;
        String expectedDataQuery = normalizeSql(expectedSelect + MULTI_FIRM_QUERY_BODY
                + " GROUP BY u.id, u.multi_firm_user, up.user_type ORDER BY predictionValue " + direction);
        String expectedCountQuery = normalizeSql("SELECT COUNT(DISTINCT u.id) " + MULTI_FIRM_QUERY_BODY);

        assertThat(queryCaptor.getAllValues().stream().map(
                EntraUserRepositoryCustomAuditSearchImplTest::normalizeSql).toList())
                .containsExactly(expectedDataQuery, expectedCountQuery);
    }

    private static String normalizeSql(String sql) {
        StringBuilder normalized = new StringBuilder();
        boolean inString = false;
        boolean whitespacePending = false;

        for (int index = 0; index < sql.length(); index++) {
            char character = sql.charAt(index);
            if (character == '\'') {
                if (whitespacePending && shouldKeepSpace(normalized, character)) {
                    normalized.append(' ');
                }
                whitespacePending = false;
                normalized.append(character);
                if (inString && index + 1 < sql.length() && sql.charAt(index + 1) == '\'') {
                    normalized.append(sql.charAt(++index));
                } else {
                    inString = !inString;
                }
            } else if (inString) {
                normalized.append(character);
            } else if (Character.isWhitespace(character)) {
                whitespacePending = true;
            } else {
                if (whitespacePending && shouldKeepSpace(normalized, character)) {
                    normalized.append(' ');
                }
                whitespacePending = false;
                normalized.append(character);
            }
        }

        return normalized.toString().trim();
    }

    private static boolean shouldKeepSpace(StringBuilder normalized, char nextCharacter) {
        if (normalized.isEmpty()) {
            return false;
        }
        char previousCharacter = normalized.charAt(normalized.length() - 1);
        return !isSqlPunctuation(previousCharacter) && !isSqlPunctuation(nextCharacter);
    }

    private static boolean isSqlPunctuation(char character) {
        return character == '(' || character == ')' || character == ',';
    }
}
