package org.jumpmind.metl.ui.init;

import java.util.HashSet;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
@OpenAPIDefinition
public class SwaggerConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("swagger-ui.html").addResourceLocations("classpath:/META-INF/resources/");

        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.defaultContentType(MediaType.APPLICATION_JSON).favorParameter(true).mediaType("xml", MediaType.APPLICATION_XML);
    }

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI().info(new Info().title("Metl API").description("This is the REST API for Metl"));
    }
    
    /*@Bean
    public Docket swaggerSpringMvcPlugin() {
        return new Docket(DocumentationType.SWAGGER_2).produces(contentTypes()).consumes(contentTypes())
                .apiInfo(new ApiInfo("Metl API", "This is the REST API for Metl", null, null, (Contact) null, null, null));
    }

    protected Set<String> contentTypes() {
        Set<String> set = new HashSet<String>();
        set.add("application/xml");
        set.add("application/json");
        return set;
    }*/
}
