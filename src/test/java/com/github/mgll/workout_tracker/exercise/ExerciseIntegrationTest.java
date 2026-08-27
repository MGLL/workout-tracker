package com.github.mgll.workout_tracker.exercise;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.mgll.workout_tracker.support.AbstractPostgresIntegrationTest;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Exercises the whole stack against a real PostgreSQL, which is the only way the Flyway migrations
 * and the seeded catalog actually get verified.
 */
class ExerciseIntegrationTest extends AbstractPostgresIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Test
  @DisplayName("the seed migration populates the catalog")
  void seededCatalogIsServedInEnglish() throws Exception {
    mockMvc
        .perform(get("/api/v1/exercises"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(4))
        .andExpect(
            jsonPath("$.content[*].code")
                .value(
                    org.hamcrest.Matchers.containsInAnyOrder(
                        "bench-press", "biceps-curl", "romanian-deadlift", "triceps-extension")))
        .andExpect(jsonPath("$.content[0].name").value("Bench press"))
        .andExpect(jsonPath("$.content[0].resolvedLocale").value("en"));
  }

  @Test
  void frenchIsServedWhenRequested() throws Exception {
    mockMvc
        .perform(get("/api/v1/exercises").header("Accept-Language", "fr"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].name").value("Développé couché"))
        .andExpect(jsonPath("$.content[0].resolvedLocale").value("fr"));
  }

  @Test
  @DisplayName("an unsupported language falls back to English")
  void unsupportedLanguageFallsBack() throws Exception {
    mockMvc
        .perform(get("/api/v1/exercises").header("Accept-Language", "de"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].name").value("Bench press"))
        .andExpect(jsonPath("$.content[0].resolvedLocale").value("en"));
  }

  @Test
  void searchMatchesNamesInAnyLanguage() throws Exception {
    mockMvc
        .perform(get("/api/v1/exercises").param("search", "bench"))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].code").value("bench-press"));

    mockMvc
        .perform(get("/api/v1/exercises").param("search", "couché").header("Accept-Language", "fr"))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].code").value("bench-press"));
  }

  @Test
  void filtersByMuscleGroupAndBodyPart() throws Exception {
    mockMvc
        .perform(get("/api/v1/exercises").param("muscleGroup", "TRICEPS"))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].code").value("triceps-extension"));

    mockMvc
        .perform(get("/api/v1/exercises").param("bodyPart", "LOWER_BODY"))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].code").value("romanian-deadlift"));
  }

  @Test
  void getByIdReturnsEveryTranslation() throws Exception {
    String id = firstIdOfSearch("bench");

    mockMvc
        .perform(get("/api/v1/exercises/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.translations.en.name").value("Bench press"))
        .andExpect(jsonPath("$.translations.fr.name").value("Développé couché"))
        .andExpect(jsonPath("$.bodyPart").value("UPPER_BODY"));
  }

  @Test
  void unknownIdReturnsProblemDetail() throws Exception {
    mockMvc
        .perform(get("/api/v1/exercises/{id}", "00000000-0000-0000-0000-000000000000"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.title").value("Exercise not found"));
  }

  @Test
  @DisplayName("create, read, update and delete round-trip")
  void crudRoundTrip() throws Exception {
    String created =
        mockMvc
            .perform(
                post("/api/v1/exercises")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"code": "back-squat", "muscleGroup": "QUADRICEPS", "bodyPart": "LOWER_BODY",
                         "equipment": "BARBELL",
                         "translations": {"en": {"name": "Back squat"},
                                          "fr": {"name": "Squat arrière"}}}\
                        """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Back squat"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    String id = objectMapper.readTree(created).get("id").asText();

    mockMvc
        .perform(get("/api/v1/exercises/{id}", id).header("Accept-Language", "fr"))
        .andExpect(jsonPath("$.name").value("Squat arrière"));

    // A full replace: the French translation is absent, so it must disappear.
    mockMvc
        .perform(
            put("/api/v1/exercises/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"code": "back-squat", "muscleGroup": "GLUTES", "bodyPart": "LOWER_BODY",
                     "translations": {"en": {"name": "High-bar back squat"}}}\
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.muscleGroup").value("GLUTES"))
        .andExpect(jsonPath("$.translations.fr").doesNotExist());

    // Read it back rather than trusting the echoed response: asserting only on the PUT
    // response would pass even if nothing reached the database.
    mockMvc
        .perform(get("/api/v1/exercises/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("High-bar back squat"))
        .andExpect(jsonPath("$.muscleGroup").value("GLUTES"))
        .andExpect(jsonPath("$.translations.fr").doesNotExist());

    mockMvc.perform(delete("/api/v1/exercises/{id}", id)).andExpect(status().isNoContent());
    mockMvc.perform(get("/api/v1/exercises/{id}", id)).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("editing a response-shaped payload is rejected, not silently ignored")
  void unknownFieldsAreRejected() throws Exception {
    // The read model exposes a top-level name/description; the write model does not, because
    // that text belongs to a language. Editing the response and PUTting it back used to
    // return 200 having changed nothing.
    String body =
        mockMvc
            .perform(get("/api/v1/exercises").param("search", "bench"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String id = objectMapper.readTree(body).get("content").get(0).get("id").asText();

    String roundTripped =
        mockMvc
            .perform(get("/api/v1/exercises/{id}", id))
            .andReturn()
            .getResponse()
            .getContentAsString();

    mockMvc
        .perform(
            put("/api/v1/exercises/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(roundTripped.replace("Bench press", "Edited name")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Unknown field"))
        .andExpect(jsonPath("$.unknownField").exists());

    // ...and the exercise is untouched.
    mockMvc
        .perform(get("/api/v1/exercises/{id}", id))
        .andExpect(jsonPath("$.name").value("Bench press"));
  }

  @Test
  @DisplayName("a translation-only edit still advances updatedAt")
  void translationOnlyEditTouchesUpdatedAt() throws Exception {
    // A seeded exercise: its timestamps come from the migration's DEFAULT now(), and none of
    // its scalar columns change below - only the translation does.
    String list =
        mockMvc
            .perform(get("/api/v1/exercises").param("search", "romanian"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String id = objectMapper.readTree(list).get("content").get(0).get("id").asText();

    Instant before = readUpdatedAt(id);

    // Hibernate does not consider the exercise row dirty for an @ElementCollection change, so
    // @LastModifiedDate never fires and updated_at would silently stay put.
    mockMvc
        .perform(
            put("/api/v1/exercises/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"code": "romanian-deadlift", "muscleGroup": "HAMSTRINGS",
                     "bodyPart": "LOWER_BODY", "equipment": "BARBELL",
                     "translations": {"en": {"name": "Romanian deadlift",
                                             "description": "Edited description."}}}\
                    """))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/api/v1/exercises/{id}", id))
        .andExpect(jsonPath("$.description").value("Edited description."));

    // Both instants are read back from PostgreSQL, so neither carries sub-microsecond
    // precision that the column would round away.
    org.assertj.core.api.Assertions.assertThat(readUpdatedAt(id))
        .as("updatedAt must advance even when only a translation changed")
        .isAfter(before);
  }

  private Instant readUpdatedAt(String id) throws Exception {
    String body =
        mockMvc
            .perform(get("/api/v1/exercises/{id}", id))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return Instant.parse(objectMapper.readTree(body).get("updatedAt").asText());
  }

  @Test
  void duplicateCodeIsRejected() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/exercises")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"code": "bench-press", "muscleGroup": "CHEST", "bodyPart": "UPPER_BODY",
                     "translations": {"en": {"name": "Something else"}}}\
                    """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.title").value("Duplicate exercise"));
  }

  @Test
  @DisplayName("the shared catalog refuses a name already used in the same language")
  void duplicateNameInSameLanguageIsRejected() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/exercises")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"code": "flat-bench-press", "muscleGroup": "CHEST", "bodyPart": "UPPER_BODY",
                     "translations": {"en": {"name": "bench PRESS"}}}\
                    """))
        .andExpect(status().isConflict());
  }

  @Test
  void missingDefaultLanguageIsRejected() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/exercises")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"code": "front-squat", "muscleGroup": "QUADRICEPS", "bodyPart": "LOWER_BODY",
                     "translations": {"fr": {"name": "Squat avant"}}}\
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Invalid exercise"));
  }

  @Test
  @DisplayName("the OpenAPI document is generated and documents every operation")
  void openApiDocumentIsServed() throws Exception {
    String json =
        mockMvc
            .perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.info.title").value("Workout Tracker API"))
            .andExpect(jsonPath("$.paths['/api/v1/exercises'].get").exists())
            .andExpect(jsonPath("$.paths['/api/v1/exercises'].post").exists())
            .andExpect(jsonPath("$.paths['/api/v1/exercises/{id}'].get").exists())
            .andExpect(jsonPath("$.paths['/api/v1/exercises/{id}'].put").exists())
            .andExpect(jsonPath("$.paths['/api/v1/exercises/{id}'].delete").exists())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode parameters =
        objectMapper.readTree(json).at("/paths/~1api~1v1~1exercises/get/parameters");

    org.assertj.core.api.Assertions.assertThat(parameters.toString())
        .as("Accept-Language must be documented, since no controller parameter carries it")
        .contains("Accept-Language");
  }

  private String firstIdOfSearch(String search) throws Exception {
    String body =
        mockMvc
            .perform(get("/api/v1/exercises").param("search", search))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(body).get("content").get(0).get("id").asText();
  }
}
