package uk.gov.justice.laa.portal.landingpage.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiConfigTest {

    private final OpenApiConfig config = new OpenApiConfig();

    @Test
    void customOpenApi_Configuration() {
        // Act
        OpenAPI openApi = config.customOpenApi();

        // Assert
        assertNotNull(openApi);
        assertNotNull(openApi.getInfo());
        assertEquals("Claim Enrichment API", openApi.getInfo().getTitle());
        assertEquals("1.0", openApi.getInfo().getVersion());
        
        // Verify security scheme
        assertNotNull(openApi.getComponents());
        assertNotNull(openApi.getComponents().getSecuritySchemes());
        assertTrue(openApi.getComponents().getSecuritySchemes().containsKey("bearerAuth"));
        
        SecurityScheme securityScheme = openApi.getComponents().getSecuritySchemes().get("bearerAuth");
        assertEquals(SecurityScheme.Type.HTTP, securityScheme.getType());
        assertEquals("bearer", securityScheme.getScheme());
        assertEquals("JWT", securityScheme.getBearerFormat());
        
        // Verify security requirement
        assertFalse(openApi.getSecurity().isEmpty());
        assertFalse(openApi.getSecurity().get(0).isEmpty());
    }
}
