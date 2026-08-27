package com.github.mgll.workout_tracker.exercise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.mgll.workout_tracker.exercise.domain.BodyPart;
import com.github.mgll.workout_tracker.exercise.domain.Equipment;
import com.github.mgll.workout_tracker.exercise.domain.MuscleGroup;
import com.github.mgll.workout_tracker.exercise.dto.ExerciseRequest;
import com.github.mgll.workout_tracker.exercise.dto.ExerciseResponse;
import com.github.mgll.workout_tracker.exercise.dto.TranslationRequest;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(ExerciseController.class)
class ExerciseControllerTest {

  private static final UUID ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private ExerciseService service;

  @Test
  void createReturnsCreatedWithLocation() throws Exception {
    when(service.create(any(), any())).thenReturn(response());

    mockMvc
        .perform(
            post("/api/v1/exercises")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest())))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "http://localhost/api/v1/exercises/" + ID))
        .andExpect(jsonPath("$.name").value("Bench press"))
        .andExpect(jsonPath("$.resolvedLocale").value("en"))
        .andExpect(jsonPath("$.translations.fr.name").value("Développé couché"));
  }

  @Test
  @DisplayName("Accept-Language reaches the service as the resolved locale")
  void passesAcceptLanguageThrough() throws Exception {
    when(service.get(eq(ID), any())).thenReturn(response());

    mockMvc
        .perform(get("/api/v1/exercises/{id}", ID).header("Accept-Language", "fr"))
        .andExpect(status().isOk());

    ArgumentCaptor<Locale> locale = ArgumentCaptor.forClass(Locale.class);
    verify(service).get(eq(ID), locale.capture());
    assertThat(locale.getValue().getLanguage()).isEqualTo("fr");
  }

  @Test
  void invalidPayloadIsRejectedWithFieldErrors() throws Exception {
    String body =
        """
        {"code": "Not A Slug", "muscleGroup": "CHEST", "bodyPart": "UPPER_BODY",
         "translations": {"en": {"name": ""}}}\
        """;

    mockMvc
        .perform(post("/api/v1/exercises").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Validation failed"))
        .andExpect(jsonPath("$.errors").isArray());
  }

  @Test
  void unknownExerciseIsNotFound() throws Exception {
    when(service.get(eq(ID), any())).thenThrow(new ExerciseNotFoundException(ID));

    mockMvc
        .perform(get("/api/v1/exercises/{id}", ID))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Exercise not found"));
  }

  @Test
  void duplicateIsAConflict() throws Exception {
    when(service.create(any(), any())).thenThrow(DuplicateExerciseException.forCode("bench-press"));

    mockMvc
        .perform(
            post("/api/v1/exercises")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest())))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.title").value("Duplicate exercise"));
  }

  @Test
  void deleteReturnsNoContent() throws Exception {
    mockMvc.perform(delete("/api/v1/exercises/{id}", ID)).andExpect(status().isNoContent());

    verify(service).delete(ID);
  }

  private static ExerciseRequest validRequest() {
    return new ExerciseRequest(
        "bench-press",
        MuscleGroup.CHEST,
        BodyPart.UPPER_BODY,
        Equipment.BARBELL,
        Map.of("en", new TranslationRequest("Bench press", null)));
  }

  private static ExerciseResponse response() {
    return new ExerciseResponse(
        ID,
        "bench-press",
        MuscleGroup.CHEST,
        BodyPart.UPPER_BODY,
        Equipment.BARBELL,
        "Bench press",
        null,
        "en",
        Map.of(
            "en", new ExerciseResponse.TranslationResponse("Bench press", null),
            "fr", new ExerciseResponse.TranslationResponse("Développé couché", null)),
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:00Z"));
  }
}
