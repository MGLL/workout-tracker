package com.github.mgll.workout_tracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class WorkoutTrackerApplication {

  public static void main(String[] args) {
    SpringApplication.run(WorkoutTrackerApplication.class, args);
  }
}
