package com.github.mgll.workout_tracker.exercise;

import com.github.mgll.workout_tracker.config.I18nProperties;
import com.github.mgll.workout_tracker.exercise.domain.BodyPart;
import com.github.mgll.workout_tracker.exercise.domain.Exercise;
import com.github.mgll.workout_tracker.exercise.domain.ExerciseTranslation;
import com.github.mgll.workout_tracker.exercise.domain.MuscleGroup;
import com.github.mgll.workout_tracker.exercise.dto.ExerciseRequest;
import com.github.mgll.workout_tracker.exercise.dto.ExerciseResponse;
import com.github.mgll.workout_tracker.exercise.dto.ExerciseSummaryResponse;
import com.github.mgll.workout_tracker.exercise.dto.PageResponse;
import com.github.mgll.workout_tracker.exercise.dto.TranslationRequest;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class ExerciseService {

  private final ExerciseRepository repository;
  private final ExerciseMapper mapper;
  private final I18nProperties i18n;

  public ExerciseService(
      ExerciseRepository repository, ExerciseMapper mapper, I18nProperties i18n) {
    this.repository = repository;
    this.mapper = mapper;
    this.i18n = i18n;
  }

  public PageResponse<ExerciseSummaryResponse> search(
      String search, MuscleGroup muscleGroup, BodyPart bodyPart, Pageable pageable, Locale locale) {

    Page<Exercise> page =
        repository.search(
            StringUtils.hasText(search) ? search.trim() : null, muscleGroup, bodyPart, pageable);

    List<ExerciseSummaryResponse> content =
        page.getContent().stream().map(exercise -> mapper.toSummary(exercise, locale)).toList();

    return PageResponse.of(page, content);
  }

  public ExerciseResponse get(UUID id, Locale locale) {
    return mapper.toResponse(findOrThrow(id), locale);
  }

  @Transactional
  public ExerciseResponse create(ExerciseRequest request, Locale locale) {
    validateTranslations(request.translations());

    if (repository.existsByCode(request.code())) {
      throw DuplicateExerciseException.forCode(request.code());
    }

    Exercise exercise =
        Exercise.builder()
            .id(UUID.randomUUID())
            .code(request.code())
            .muscleGroup(request.muscleGroup())
            .bodyPart(request.bodyPart())
            .equipment(request.equipment())
            .translations(toEntityTranslations(request.translations()))
            .build();

    return mapper.toResponse(save(exercise), locale);
  }

  @Transactional
  public ExerciseResponse update(UUID id, ExerciseRequest request, Locale locale) {
    validateTranslations(request.translations());

    Exercise exercise = findOrThrow(id);

    if (repository.existsByCodeAndIdNot(request.code(), id)) {
      throw DuplicateExerciseException.forCode(request.code());
    }

    exercise.setCode(request.code());
    exercise.setMuscleGroup(request.muscleGroup());
    exercise.setBodyPart(request.bodyPart());
    exercise.setEquipment(request.equipment());
    exercise.getTranslations().clear();
    exercise.getTranslations().putAll(toEntityTranslations(request.translations()));
    exercise.setUpdatedAt(Instant.now());

    return mapper.toResponse(save(exercise), locale);
  }

  @Transactional
  public void delete(UUID id) {
    repository.delete(findOrThrow(id));
  }

  private Exercise findOrThrow(UUID id) {
    return repository.findById(id).orElseThrow(() -> new ExerciseNotFoundException(id));
  }

  private Exercise save(Exercise exercise) {
    try {
      return repository.saveAndFlush(exercise);
    } catch (DataIntegrityViolationException ex) {
      throw DuplicateExerciseException.forName();
    }
  }

  private void validateTranslations(Map<String, TranslationRequest> translations) {
    List<String> unsupported =
        translations.keySet().stream().filter(tag -> !i18n.supports(tag)).sorted().toList();

    if (!unsupported.isEmpty()) {
      throw new InvalidExerciseException(
          "Unsupported language(s) %s; supported: %s"
              .formatted(unsupported, i18n.supportedLocales()));
    }

    if (!translations.containsKey(i18n.defaultLocale())) {
      throw new InvalidExerciseException(
          "A translation for the default language '%s' is required"
              .formatted(i18n.defaultLocale()));
    }
  }

  private Map<String, ExerciseTranslation> toEntityTranslations(
      Map<String, TranslationRequest> translations) {
    Map<String, ExerciseTranslation> result = new HashMap<>();
    translations.forEach(
        (tag, translation) ->
            result.put(
                tag,
                new ExerciseTranslation(translation.name().trim(), translation.description())));
    return result;
  }
}
