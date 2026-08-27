package com.github.mgll.workout_tracker.exercise;

import com.github.mgll.workout_tracker.exercise.domain.BodyPart;
import com.github.mgll.workout_tracker.exercise.domain.MuscleGroup;
import com.github.mgll.workout_tracker.exercise.dto.ExerciseRequest;
import com.github.mgll.workout_tracker.exercise.dto.ExerciseResponse;
import com.github.mgll.workout_tracker.exercise.dto.ExerciseSummaryResponse;
import com.github.mgll.workout_tracker.exercise.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Locale;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/exercises")
@Tag(name = "Exercises", description = "Public exercise library, shared by every user")
public class ExerciseController {

  private final ExerciseService service;

  public ExerciseController(ExerciseService service) {
    this.service = service;
  }

  @GetMapping
  @Operation(
      summary = "List exercises",
      description = "All filters are optional. The name search matches any language.")
  public PageResponse<ExerciseSummaryResponse> list(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) MuscleGroup muscleGroup,
      @RequestParam(required = false) BodyPart bodyPart,
      @ParameterObject @PageableDefault(size = 20, sort = "code", direction = Sort.Direction.ASC)
          Pageable pageable,
      Locale locale) {

    return service.search(search, muscleGroup, bodyPart, pageable, locale);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get one exercise")
  @ApiResponses(
      @ApiResponse(
          responseCode = "404",
          description = "No such exercise",
          content = @Content(schema = @Schema(implementation = ProblemDetail.class))))
  public ExerciseResponse get(@PathVariable UUID id, Locale locale) {
    return service.get(id, locale);
  }

  @PostMapping
  @Operation(summary = "Add an exercise to the library")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Created"),
    @ApiResponse(
        responseCode = "400",
        description = "Invalid payload or missing default language",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "409",
        description = "Code or name already in the library",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<ExerciseResponse> create(
      @Valid @RequestBody ExerciseRequest request, Locale locale, UriComponentsBuilder uriBuilder) {

    ExerciseResponse created = service.create(request, locale);
    URI location = uriBuilder.path("/api/v1/exercises/{id}").build(created.id());
    return ResponseEntity.created(location).body(created);
  }

  @PutMapping("/{id}")
  @Operation(
      summary = "Replace an exercise",
      description = "A full replace: translations absent from the payload are removed.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "400",
        description = "Invalid payload or missing default language",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "404",
        description = "No such exercise",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "409",
        description = "Code or name already in the library",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ExerciseResponse update(
      @PathVariable UUID id, @Valid @RequestBody ExerciseRequest request, Locale locale) {

    return service.update(id, request, locale);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Remove an exercise from the library")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Deleted"),
    @ApiResponse(
        responseCode = "404",
        description = "No such exercise",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  })
  public void delete(@PathVariable UUID id) {
    service.delete(id);
  }
}
