package com.shopping.system;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Bridges the Spring Boot application into a traditional servlet container
 * (Apache Tomcat 9.0.x). Without this class the WAR deploys but the Spring
 * context never starts.
 */
public class ServletInitializer extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(ShoppingApplication.class);
    }
}