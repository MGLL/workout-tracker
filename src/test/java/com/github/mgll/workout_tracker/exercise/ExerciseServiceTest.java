package com.github.mgll.workout_tracker.exercise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.mgll.workout_tracker.config.I18nProperties;
import com.github.mgll.workout_tracker.exercise.domain.BodyPart;
import com.github.mgll.workout_tracker.exercise.domain.Equipment;
import com.github.mgll.workout_tracker.exercise.domain.Exercise;
import com.github.mgll.workout_tracker.exercise.domain.ExerciseTranslation;
import com.github.mgll.workout_tracker.exercise.domain.MuscleGroup;
import com.github.mgll.workout_tracker.exercise.dto.ExerciseRequest;
import com.github.mgll.workout_tracker.exercise.dto.ExerciseResponse;
import com.github.mgll.workout_tracker.exercise.dto.TranslationRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class ExerciseServiceTest {

  private static final UUID ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

  @Mock private ExerciseRepository repository;

  private ExerciseService service;

  @BeforeEach
  void setUp() {
    I18nProperties i18n = new I18nProperties("en", List.of("en", "fr"));
    service = new ExerciseService(repository, new ExerciseMapper(i18n), i18n);
  }

  @Test
  @DisplayName("renders the requested language when the exercise has it")
  void usesRequestedLanguage() {
    when(repository.findById(ID)).thenReturn(Optional.of(bilingualExercise()));

    ExerciseResponse response = service.get(ID, Locale.FRENCH);

    assertThat(response.name()).isEqualTo("Développé couché");
    assertThat(response.resolvedLocale()).isEqualTo("fr");
  }

  @Test
  @DisplayName("falls back to English when the requested language is missing")
  void fallsBackToDefaultLanguage() {
    Exercise exercise = bilingualExercise();
    exercise.getTranslations().remove("fr");
    when(repository.findById(ID)).thenReturn(Optional.of(exercise));

    ExerciseResponse response = service.get(ID, Locale.FRENCH);

    assertThat(response.name()).isEqualTo("Bench press");
    assertThat(response.resolvedLocale()).isEqualTo("en");
  }

  @Test
  @DisplayName("falls back to any remaining language when even English is missing")
  void fallsBackToAnyLanguage() {
    Exercise exercise = bilingualExercise();
    exercise.getTranslations().remove("en");
    when(repository.findById(ID)).thenReturn(Optional.of(exercise));

    ExerciseResponse response = service.get(ID, Locale.GERMAN);

    assertThat(response.name()).isEqualTo("Développé couché");
    assertThat(response.resolvedLocale()).isEqualTo("fr");
  }

  @Test
  @DisplayName("exposes every translation alongside the resolved one")
  void exposesAllTranslations() {
    when(repository.findById(ID)).thenReturn(Optional.of(bilingualExercise()));

    ExerciseResponse response = service.get(ID, Locale.ENGLISH);

    assertThat(response.translations()).containsOnlyKeys("en", "fr");
  }

  @Test
  void unknownIdIsNotFound() {
    when(repository.findById(ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.get(ID, Locale.ENGLISH))
        .isInstanceOf(ExerciseNotFoundException.class);
  }

  @Test
  @DisplayName("rejects a payload without the default language")
  void requiresDefaultLanguage() {
    ExerciseRequest request = request(Map.of("fr", new TranslationRequest("Squat", null)));

    assertThatThrownBy(() -> service.create(request, Locale.ENGLISH))
        .isInstanceOf(InvalidExerciseException.class)
        .hasMessageContaining("'en'");

    verify(repository, never()).saveAndFlush(any());
  }

  @Test
  @DisplayName("rejects a language the application is not configured for")
  void rejectsUnsupportedLanguage() {
    ExerciseRequest request =
        request(
            Map.of(
                "en", new TranslationRequest("Back squat", null),
                "de", new TranslationRequest("Kniebeuge", null)));

    assertThatThrownBy(() -> service.create(request, Locale.ENGLISH))
        .isInstanceOf(InvalidExerciseException.class)
        .hasMessageContaining("de");

    verify(repository, never()).saveAndFlush(any());
  }

  @Test
  void rejectsDuplicateCode() {
    when(repository.existsByCode("back-squat")).thenReturn(true);

    ExerciseRequest request = request(Map.of("en", new TranslationRequest("Back squat", null)));

    assertThatThrownBy(() -> service.create(request, Locale.ENGLISH))
        .isInstanceOf(DuplicateExerciseException.class)
        .hasMessageContaining("back-squat");
  }

  @Test
  @DisplayName("turns a unique-index violation into a duplicate-name conflict")
  void translatesConstraintViolation() {
    when(repository.existsByCode("back-squat")).thenReturn(false);
    when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uq_..."));

    ExerciseRequest request = request(Map.of("en", new TranslationRequest("Back squat", null)));

    assertThatThrownBy(() -> service.create(request, Locale.ENGLISH))
        .isInstanceOf(DuplicateExerciseException.class);
  }

  @Test
  @DisplayName("update replaces the translation map instead of merging into it")
  void updateReplacesTranslations() {
    Exercise existing = bilingualExercise();
    when(repository.findById(ID)).thenReturn(Optional.of(existing));
    when(repository.existsByCodeAndIdNot("bench-press", ID)).thenReturn(false);
    when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

    service.update(
        ID,
        new ExerciseRequest(
            "bench-press",
            MuscleGroup.CHEST,
            BodyPart.UPPER_BODY,
            Equipment.BARBELL,
            Map.of("en", new TranslationRequest("Barbell bench press", null))),
        Locale.ENGLISH);

    ArgumentCaptor<Exercise> saved = ArgumentCaptor.forClass(Exercise.class);
    verify(repository).saveAndFlush(saved.capture());

    assertThat(saved.getValue().getTranslations()).containsOnlyKeys("en");
    assertThat(saved.getValue().getTranslations().get("en").getName())
        .isEqualTo("Barbell bench press");
  }

  @Test
  void deleteRemovesTheExercise() {
    Exercise exercise = bilingualExercise();
    when(repository.findById(ID)).thenReturn(Optional.of(exercise));

    service.delete(ID);

    verify(repository).delete(exercise);
  }

  private static ExerciseRequest request(Map<String, TranslationRequest> translations) {
    return new ExerciseRequest(
        "back-squat", MuscleGroup.QUADRICEPS, BodyPart.LOWER_BODY, Equipment.BARBELL, translations);
  }

  private static Exercise bilingualExercise() {
    Map<String, ExerciseTranslation> translations = new HashMap<>();
    translations.put("en", new ExerciseTranslation("Bench press", "Press the barbell."));
    translations.put("fr", new ExerciseTranslation("Développé couché", "Pousser la barre."));

    return Exercise.builder()
        .id(ID)
        .code("bench-press")
        .muscleGroup(MuscleGroup.CHEST)
        .bodyPart(BodyPart.UPPER_BODY)
        .equipment(Equipment.BARBELL)
        .translations(translations)
        .build();
  }
}
