package com.github.mgll.workout_tracker.exercise.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "exercise")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exercise {

  @Id private UUID id;

  @Column(nullable = false, unique = true, length = 100)
  private String code;

  @Enumerated(EnumType.STRING)
  @Column(name = "muscle_group", nullable = false, length = 32)
  private MuscleGroup muscleGroup;

  @Enumerated(EnumType.STRING)
  @Column(name = "body_part", nullable = false, length = 32)
  private BodyPart bodyPart;

  @Enumerated(EnumType.STRING)
  @Column(length = 32)
  private Equipment equipment;

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "exercise_translation", joinColumns = @JoinColumn(name = "exercise_id"))
  @MapKeyColumn(name = "locale", length = 8)
  @BatchSize(size = 50)
  @Builder.Default
  private Map<String, ExerciseTranslation> translations = new HashMap<>();

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
