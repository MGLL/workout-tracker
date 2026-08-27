package com.github.mgll.workout_tracker.exercise.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Name and description of an exercise in one language")
public record TranslationRequest(
    @Schema(example = "Back squat") @NotBlank @Size(max = 150) String name,
    @Schema(
            example =
                "Rest the barbell on the upper back and squat until the hips drop below the knees.")
        @Size(max = 2000)
        String description) {}
