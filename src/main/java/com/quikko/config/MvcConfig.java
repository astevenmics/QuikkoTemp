package com.quikko.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Friendly extension-less URLs for the static pages.
 */
@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/capsule").setViewName("forward:/capsule.html");
        registry.addViewController("/chat").setViewName("forward:/chat.html");
    }
}
