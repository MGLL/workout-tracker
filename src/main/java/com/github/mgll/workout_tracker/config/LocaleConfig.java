package com.github.mgll.workout_tracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

@Configuration
public class LocaleConfig {
  @Bean
  public LocaleResolver localeResolver(I18nProperties properties) {
    AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
    resolver.setSupportedLocales(properties.supportedAsLocales());
    resolver.setDefaultLocale(properties.defaultAsLocale());
    return resolver;
  }
}
