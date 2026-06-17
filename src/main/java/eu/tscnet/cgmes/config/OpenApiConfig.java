package eu.tscnet.cgmes.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI cgmesOpenApi() {
        return new OpenAPI().info(new Info()
                .title("CGMES Utility Service API")
                .version("0.1.0")
                .description("Parse CGMES RDF, validate SHACL shapes, and expose European map GeoJSON."));
    }
}
