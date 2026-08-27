package com.github.mgll.workout_tracker.exercise;

import com.github.mgll.workout_tracker.exercise.domain.BodyPart;
import com.github.mgll.workout_tracker.exercise.domain.Exercise;
import com.github.mgll.workout_tracker.exercise.domain.MuscleGroup;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExerciseRepository extends JpaRepository<Exercise, UUID> {

  boolean existsByCode(String code);

  boolean existsByCodeAndIdNot(String code, UUID id);

  @Query(
      """
      select e from Exercise e
      where (:muscleGroup is null or e.muscleGroup = :muscleGroup)
        and (:bodyPart is null or e.bodyPart = :bodyPart)
        and (cast(:search as String) is null or exists (
              select 1 from Exercise e2 join e2.translations t
              where e2 = e
                and lower(t.name) like lower(concat('%', cast(:search as String), '%'))))
      """)
  Page<Exercise> search(
      @Param("search") String search,
      @Param("muscleGroup") MuscleGroup muscleGroup,
      @Param("bodyPart") BodyPart bodyPart,
      Pageable pageable);
}
