package com.github.mgll.workout_tracker.exercise.dto;

import com.github.mgll.workout_tracker.exercise.domain.BodyPart;
import com.github.mgll.workout_tracker.exercise.domain.Equipment;
import com.github.mgll.workout_tracker.exercise.domain.MuscleGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Schema(description = "An exercise with its resolved language and every available translation")
public record ExerciseResponse(
    UUID id,
    String code,
    MuscleGroup muscleGroup,
    BodyPart bodyPart,
    Equipment equipment,
    String name,
    String description,
    @Schema(
            description = "Language actually used; differs from the request when it fell back",
            example = "en")
        String resolvedLocale,
    Map<String, TranslationResponse> translations,
    Instant createdAt,
    Instant updatedAt) {

  public record TranslationResponse(String name, String description) {}
}
