package uk.gov.justice.laa.portal.landingpage.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

import static org.junit.jupiter.api.Assertions.assertEquals;


class ConditionalJdbcSessionImportSelectorTest {

    private ConditionalJdbcSessionImportSelector importSelector;
    private Environment mockEnvironment;
    private AnnotationMetadata mockMetadata;

    @BeforeEach
    void setUp() {
        importSelector = new ConditionalJdbcSessionImportSelector();
        mockEnvironment = Mockito.mock(Environment.class);
        mockMetadata = Mockito.mock(AnnotationMetadata.class);
        importSelector.setEnvironment(mockEnvironment);
    }

    @Test
    void shouldReturnJdbcHttpSessionConfigurationClassName_whenPropertyIsTrue() {
        Mockito.when(mockEnvironment.getProperty("SPRING_SESSION_JDBC_ENABLED", "false"))
                .thenReturn("true");

        String[] imports = importSelector.selectImports(mockMetadata);

        assertEquals(1, imports.length);
        assertEquals("org.springframework.session.jdbc.config.annotation.web.http.JdbcHttpSessionConfiguration",
                imports[0]);
    }

    @Test
    void shouldReturnEmptyArray_whenPropertyIsFalse() {
        Mockito.when(mockEnvironment.getProperty("SPRING_SESSION_JDBC_ENABLED", "false"))
                .thenReturn("false");

        String[] imports = importSelector.selectImports(mockMetadata);

        assertEquals(0, imports.length);
    }

    @Test
    void shouldReturnEmptyArray_whenPropertyIsMissing() {
        Mockito.when(mockEnvironment.getProperty("SPRING_SESSION_JDBC_ENABLED", "false"))
                .thenReturn(null); // Simulate missing property

        String[] imports = importSelector.selectImports(mockMetadata);

        assertEquals(0, imports.length);
    }
}
