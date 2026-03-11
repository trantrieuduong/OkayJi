package com.okayji.moderation.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class ModerationJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    TargetType targetType; // post or comment

    @Column(nullable = false)
    String targetId; // postId or commentId

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    ModerationJobStatus status = ModerationJobStatus.PENDING;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Builder.Default
    int retryCount = 0;

    @Builder.Default
    int maxRetries = 5;

    @OneToMany(mappedBy = "moderationJob", cascade = CascadeType.ALL, orphanRemoval = true)
    List<ModerationResult> moderationResults;
}
