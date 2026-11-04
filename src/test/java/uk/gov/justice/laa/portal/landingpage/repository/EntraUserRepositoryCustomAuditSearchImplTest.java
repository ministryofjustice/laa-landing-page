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
        String dataQuerySql = queryCaptor.getAllValues().getFirst();

        assertThat(dataQuerySql)
                .contains("CASE WHEN COALESCE(LOWER(up.user_type), 'external') = 'external'")
                .contains("AND COALESCE(u.multi_firm_user, false) = true THEN 3")
                .contains("AND COALESCE(u.multi_firm_user, false) = false THEN 2")
                .contains("ELSE 1 END AS predictionValue")
                .contains("GROUP BY u.id, u.multi_firm_user, up.user_type")
                .endsWith("ORDER BY predictionValue " + direction)
                .contains("LEFT JOIN user_profile up ON up.entra_user_id = u.id");
    }
}
