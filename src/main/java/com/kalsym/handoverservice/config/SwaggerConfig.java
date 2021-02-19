package com.kalsym.handoverservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * @author Sarosh
 */
@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Bean
    public Docket productApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                // .paths(PathSelectors.any())
                .apis(RequestHandlerSelectors.basePackage("com.kalsym.handoverservice"))
                .build()
                .apiInfo(apiInfo());
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("handover service for symplified")
                .description(
                        "handover service act as a bridge between different user channels and Agents interface e.g. rocket chat ")
                .termsOfServiceUrl("TBA")
                .license("TBA")
                .licenseUrl("")
                .version("1.0")
                .build();
    }
}
