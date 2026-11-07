package com.linebook.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.linebook.common.config.LinebookProperties;
import com.linebook.common.config.OpenApiConfig;
import io.swagger.v3.oas.models.OpenAPI;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

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
