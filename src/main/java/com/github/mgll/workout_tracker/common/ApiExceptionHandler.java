package com.github.mgll.workout_tracker.common;

import com.github.mgll.workout_tracker.exercise.DuplicateExerciseException;
import com.github.mgll.workout_tracker.exercise.ExerciseNotFoundException;
import com.github.mgll.workout_tracker.exercise.InvalidExerciseException;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(ExerciseNotFoundException.class)
  public ProblemDetail handleNotFound(ExerciseNotFoundException ex) {
    return problem(HttpStatus.NOT_FOUND, "Exercise not found", ex.getMessage());
  }

  @ExceptionHandler(DuplicateExerciseException.class)
  public ProblemDetail handleDuplicate(DuplicateExerciseException ex) {
    return problem(HttpStatus.CONFLICT, "Duplicate exercise", ex.getMessage());
  }

  @ExceptionHandler(InvalidExerciseException.class)
  public ProblemDetail handleInvalid(InvalidExerciseException ex) {
    return problem(HttpStatus.BAD_REQUEST, "Invalid exercise", ex.getMessage());
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {

    List<String> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .sorted()
            .toList();

    ProblemDetail body =
        problem(HttpStatus.BAD_REQUEST, "Validation failed", "The request payload is invalid");
    body.setProperty("errors", errors);

    return ResponseEntity.badRequest().body(body);
  }

  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {

    UnrecognizedPropertyException unrecognized = findUnrecognizedProperty(ex);
    if (unrecognized == null) {
      return super.handleHttpMessageNotReadable(ex, headers, status, request);
    }

    String known =
        unrecognized.getKnownPropertyIds().stream()
            .map(String::valueOf)
            .sorted()
            .collect(Collectors.joining(", "));

    ProblemDetail body =
        problem(
            HttpStatus.BAD_REQUEST,
            "Unknown field",
            "Unrecognized field '%s'. Accepted fields: %s."
                .formatted(unrecognized.getPropertyName(), known));
    body.setProperty("unknownField", unrecognized.getPropertyName());

    return ResponseEntity.badRequest().body(body);
  }

  private UnrecognizedPropertyException findUnrecognizedProperty(Throwable throwable) {
    for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
      if (cause instanceof UnrecognizedPropertyException unrecognized) {
        return unrecognized;
      }
      if (cause.getCause() == cause) {
        break;
      }
    }
    return null;
  }

  private ProblemDetail problem(HttpStatus status, String title, String detail) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
    problemDetail.setTitle(title);
    return problemDetail;
  }
}
