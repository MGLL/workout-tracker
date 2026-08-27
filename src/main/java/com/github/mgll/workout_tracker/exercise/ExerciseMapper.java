package com.github.mgll.workout_tracker.exercise;

import com.github.mgll.workout_tracker.config.I18nProperties;
import com.github.mgll.workout_tracker.exercise.domain.Exercise;
import com.github.mgll.workout_tracker.exercise.domain.ExerciseTranslation;
import com.github.mgll.workout_tracker.exercise.dto.ExerciseResponse;
import com.github.mgll.workout_tracker.exercise.dto.ExerciseSummaryResponse;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ExerciseMapper {

  private final I18nProperties i18n;

  public ExerciseMapper(I18nProperties i18n) {
    this.i18n = i18n;
  }

  public ExerciseSummaryResponse toSummary(Exercise exercise, Locale locale) {
    Resolved resolved = resolve(exercise, locale);
    return new ExerciseSummaryResponse(
        exercise.getId(),
        exercise.getCode(),
        exercise.getMuscleGroup(),
        exercise.getBodyPart(),
        exercise.getEquipment(),
        resolved.name(),
        resolved.description(),
        resolved.locale());
  }

  public ExerciseResponse toResponse(Exercise exercise, Locale locale) {
    Resolved resolved = resolve(exercise, locale);

    Map<String, ExerciseResponse.TranslationResponse> translations = new LinkedHashMap<>();
    exercise
        .getTranslations()
        .forEach(
            (tag, translation) ->
                translations.put(
                    tag,
                    new ExerciseResponse.TranslationResponse(
                        translation.getName(), translation.getDescription())));

    return new ExerciseResponse(
        exercise.getId(),
        exercise.getCode(),
        exercise.getMuscleGroup(),
        exercise.getBodyPart(),
        exercise.getEquipment(),
        resolved.name(),
        resolved.description(),
        resolved.locale(),
        translations,
        exercise.getCreatedAt(),
        exercise.getUpdatedAt());
  }

  private Resolved resolve(Exercise exercise, Locale locale) {
    Map<String, ExerciseTranslation> translations = exercise.getTranslations();
    String requested = locale == null ? i18n.defaultLocale() : locale.getLanguage();

    ExerciseTranslation match = translations.get(requested);
    if (match != null) {
      return Resolved.of(requested, match);
    }

    match = translations.get(i18n.defaultLocale());
    if (match != null) {
      return Resolved.of(i18n.defaultLocale(), match);
    }

    return translations.entrySet().stream()
        .findFirst()
        .map(entry -> Resolved.of(entry.getKey(), entry.getValue()))
        .orElseGet(() -> new Resolved(null, null, null));
  }

  private record Resolved(String locale, String name, String description) {

    static Resolved of(String locale, ExerciseTranslation translation) {
      return new Resolved(locale, translation.getName(), translation.getDescription());
    }
  }
}
