package com.github.mgll.workout_tracker.exercise.domain;

public enum BodyPart {
  UPPER_BODY(2.5),
  LOWER_BODY(5.0);

  private final double weightIncrementKg;

  BodyPart(double weightIncrementKg) {
    this.weightIncrementKg = weightIncrementKg;
  }

  public double weightIncrementKg() {
    return weightIncrementKg;
  }
}
