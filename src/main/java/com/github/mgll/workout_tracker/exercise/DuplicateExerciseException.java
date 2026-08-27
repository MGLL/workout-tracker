package com.github.mgll.workout_tracker.exercise;

public class DuplicateExerciseException extends RuntimeException {

  public DuplicateExerciseException(String message) {
    super(message);
  }

  public static DuplicateExerciseException forCode(String code) {
    return new DuplicateExerciseException("An exercise with code '" + code + "' already exists");
  }

  public static DuplicateExerciseException forName() {
    return new DuplicateExerciseException(
        "An exercise with that name already exists in one of the given languages");
  }
}
