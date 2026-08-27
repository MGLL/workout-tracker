package com.github.mgll.workout_tracker.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI workoutTrackerOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Workout Tracker API")
                .description("Workout planner and tracker with automatic progressive overload.")
                .version("v1")
                .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
        .servers(List.of(new Server().url("http://localhost:8080").description("Local")));
  }

  @Bean
  public OperationCustomizer acceptLanguageHeader(I18nProperties properties) {
    return (operation, handlerMethod) ->
        operation.addParametersItem(
            new Parameter()
                .in("header")
                .name(HttpHeaders.ACCEPT_LANGUAGE)
                .description(
                    "Response language. Falls back to '%s' when absent or unsupported."
                        .formatted(properties.defaultLocale()))
                .required(false)
                .schema(
                    new StringSchema()
                        ._enum(properties.supportedLocales())
                        ._default(properties.defaultLocale())));
  }
}
