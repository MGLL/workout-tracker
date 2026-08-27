package com.github.mgll.workout_tracker.config;

import java.util.List;
import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("app.i18n")
public record I18nProperties(
    @DefaultValue("en") String defaultLocale, @DefaultValue("en") List<String> supportedLocales) {

  public boolean supports(String languageTag) {
    return supportedLocales.contains(languageTag);
  }

  public Locale defaultAsLocale() {
    return Locale.forLanguageTag(defaultLocale);
  }

  public List<Locale> supportedAsLocales() {
    return supportedLocales.stream().map(Locale::forLanguageTag).toList();
  }
}
