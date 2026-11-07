package com.linebook.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Lives in com.linebook.common.config because OpenApiConfig#linebookOpenApi is package-private,
 * which is the normal visibility for a @Bean method. The file stays in the common/ test folder,
 * the same way RequestIdFilterTest declares com.linebook.common.web from that folder.
 */
class OpenApiConfigTest {

    @Test
    void theDocsAreTitledAndNameTheOperator() {
        LinebookProperties props = new LinebookProperties("Asia/Kolkata", "Surya Cable Network", "ag-01", true,
                new LinebookProperties.Cors(List.of("http://localhost:8080"), Duration.ofHours(1)),
                new LinebookProperties.Auth("test-secret-for-unit-tests-only", Duration.ofHours(12), "", "", "LineBook Admin"));

        OpenAPI api = new OpenApiConfig().linebookOpenApi(props);

        assertThat(api.getInfo().getTitle()).isEqualTo("LineBook API");
        assertThat(api.getInfo().getDescription()).contains("Surya Cable Network");
    }
}