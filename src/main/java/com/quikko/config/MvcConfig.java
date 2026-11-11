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
        registry.addViewController("/privacy").setViewName("forward:/privacy.html");
        registry.addViewController("/terms").setViewName("forward:/terms.html");
        registry.addViewController("/cookies").setViewName("forward:/cookies.html");
        registry.addViewController("/refund").setViewName("forward:/refund.html");
    }
}
