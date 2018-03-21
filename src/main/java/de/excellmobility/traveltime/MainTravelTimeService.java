package de.excellmobility.traveltime;


import java.util.Arrays;

import javax.servlet.ServletContext;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.Contact;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.paths.RelativePathProvider;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@SpringBootApplication
public class MainTravelTimeService
{
  private static final String swaggerBaseUrl = "/TravelTimeService-swagger";

  public static void main(String[] args)
  {
    org.springframework.boot.SpringApplication.run(MainTravelTimeService.class, args);

  }
  
  

  @Bean
  public Docket docket(ServletContext servletContext)
  {
     return new Docket(DocumentationType.SWAGGER_2)
           .apiInfo(apiInfo())
           .select()
           .apis(RequestHandlerSelectors.any())
           .paths(PathSelectors.regex("/TravelTimeService.*"))
           .build()
           .securitySchemes(Arrays.asList(new ApiKey("api_key", "Authorization", "header"))).securityContexts(Arrays.asList(SecurityContext.builder().securityReferences(Arrays.asList(new SecurityReference("api_key", new AuthorizationScope[0]))).forPaths(PathSelectors.regex("/*.*")).build())).host("dlr-integration.minglabs.com").pathProvider(new RelativePathProvider(servletContext)
           {
             @Override
             public String getApplicationBasePath()
             {
               return "/api/v1/service-request/traveltimeservice";
             }})
           ;
  }

  
  
  
  private ApiInfo apiInfo()
  {
     return new ApiInfoBuilder()
           .title("ExCELL TravelTimeService")
           .description("Über den TravelTimeService können aktuelle sowie zukünftige Reisezeiten für einzelne Abschnitte sowie zur Zeit für ganz Dresden abgerufen werden. Zurückgegeben werden entweder aktuell gemessene Reisezeiten oder Reisezeiten aus Tagesganglinien. "
           		+ "Kann auf keine der Werte zurückgegriffen werden, so werden keine Reisezeiten angegeben, d.h. es werden keine statischen Reisezeiten, resultierend aus zulässiger Höchstgeschwindigkeit "
           		+ "und Länge des Elementes, berechnet."
           		+ "\n\n "
           		+ "The TravelTimeService can be used to retrieve current and future travel times for individual sections or actual for the whole of Dresden. "
           		+ "Returned are either currently measured travel times or travel times from daily gaits. If none of the values can be used, no travel times are indicated, "
           		+ "i. E. there are no static travel times, resulting from the maximum speed and length of the element calculated. \n")
           .version("1.0")
           .contact(new Contact("TU Dresden - Chair of Traffic Control and Process Automatisation", "http://tu-dresden.de/vlp", "sebastian.pape@tu-dresden.de"))
           .build();
  }
  
  
  @Controller
  @Configuration
  public class ConfigurerAdapter implements WebMvcConfigurer
  {
    @Override
    public void addViewControllers(ViewControllerRegistry registry)
    {
      registry.addViewController(swaggerBaseUrl + "/v2/api-docs").setViewName("forward:/v2/api-docs");
      registry.addViewController(swaggerBaseUrl + "/swagger-resources/configuration/ui").setViewName("forward:/swagger-resources/configuration/ui");
      registry.addViewController(swaggerBaseUrl + "/swagger-resources/configuration/security").setViewName("forward:/swagger-resources/configuration/security");
      registry.addViewController(swaggerBaseUrl + "/swagger-resources").setViewName("forward:/swagger-resources");
      registry.addViewController(swaggerBaseUrl + "/public").setViewName("forward:/public");
      registry.addRedirectViewController(swaggerBaseUrl, swaggerBaseUrl + "/swagger-ui.html");
      registry.addRedirectViewController(swaggerBaseUrl + "/", swaggerBaseUrl + "/swagger-ui.html");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry)
    {
      registry.addResourceHandler(swaggerBaseUrl + "/**").addResourceLocations("classpath:/META-INF/resources/");
    }
  }
}
