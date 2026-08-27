package com.github.mgll.workout_tracker.exercise.dto;

import com.github.mgll.workout_tracker.exercise.domain.BodyPart;
import com.github.mgll.workout_tracker.exercise.domain.Equipment;
import com.github.mgll.workout_tracker.exercise.domain.MuscleGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Map;

@Schema(description = "Payload to create or fully replace an exercise")
public record ExerciseRequest(
    @Schema(description = "Stable language-neutral slug", example = "back-squat")
        @NotBlank
        @Size(max = 100)
        @Pattern(
            regexp = "[a-z0-9]+(-[a-z0-9]+)*",
            message = "must be a lowercase slug, e.g. back-squat")
        String code,
    @NotNull MuscleGroup muscleGroup,
    @NotNull BodyPart bodyPart,
    Equipment equipment,
    @Schema(
            description = "Translations keyed by language tag; the default language is required",
            example =
                """
                {"en": {"name": "Back squat"}, "fr": {"name": "Squat arrière"}}\
                """)
        @NotEmpty
        Map<String, @Valid @NotNull TranslationRequest> translations) {}
