package com.linebook.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The title and description shown at the top of Swagger UI. The endpoints are described on the controllers. */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI linebookOpenApi(LinebookProperties props) {
        return new OpenAPI().info(new Info()
                .title("LineBook API")
                .version("1.0.0")
                .description("Customers, agents, monthly bills and collections for " + props.operatorName() + ". "
                        + "Errors are returned as RFC 9457 problem responses (application/problem+json)."));
    }
}
