package com.okayji.feed.entity;

import com.okayji.exception.AppError;
import com.okayji.exception.AppException;
import com.okayji.identity.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        uniqueConstraints = {
                @UniqueConstraint(name="uk_fr_pair", columnNames={"sender_id","receiver_id"})
        },
        indexes = {
                @Index(name="ix_fr_send", columnList="sender_id"),
                @Index(name="ix_fr_receive", columnList="receiver_id")
        }
)
public class FriendRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    User sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id", nullable = false)
    User receiver;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    Instant createdAt;
}
