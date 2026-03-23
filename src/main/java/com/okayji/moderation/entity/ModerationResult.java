package com.okayji.moderation.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class ModerationResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "moderation_job_id", nullable = false)
    ModerationJob moderationJob;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    InputType inputType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    ModerationDecision decision;

    boolean flagged;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    Map<String, Boolean> categories;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    Map<String, Double> categoryScores;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
