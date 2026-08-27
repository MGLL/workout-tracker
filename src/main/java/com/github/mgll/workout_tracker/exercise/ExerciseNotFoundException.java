package com.github.mgll.workout_tracker.exercise;

import java.util.UUID;

public class ExerciseNotFoundException extends RuntimeException {

  public ExerciseNotFoundException(UUID id) {
    super("No exercise found with id " + id);
  }
}
