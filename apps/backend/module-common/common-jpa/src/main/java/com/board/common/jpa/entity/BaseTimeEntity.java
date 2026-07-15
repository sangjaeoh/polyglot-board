package com.board.common.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.io.Serializable;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 감사 시각과 식별자 기반 동등성을 제공하는 엔티티 상위 타입이다.
 *
 * <p>{@code created_at}·{@code updated_at}은 JPA Auditing이 채운다. {@link Persistable}을 구현해 수동
 * UUIDv7 {@code @Id}가 유발하는 merge penalty를 막는다({@code createdAt == null}이면 신규로 판정).
 *
 * @param <ID> 식별자 타입
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity<ID extends Serializable> implements Persistable<ID> {

    @CreatedDate
    @Column(updatable = false)
    private @Nullable Instant createdAt;

    @LastModifiedDate
    @Column
    private @Nullable Instant updatedAt;

    /** 생성 시각을 반환한다. 영속 전에는 {@code null}이다. */
    public @Nullable Instant getCreatedAt() {
        return createdAt;
    }

    /** 최종 수정 시각을 반환한다. 영속 전에는 {@code null}이다. */
    public @Nullable Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public abstract ID getId();

    @Override
    public boolean isNew() {
        return this.createdAt == null;
    }

    @Override
    public final boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BaseTimeEntity<?> that)) {
            return false;
        }
        return getId() != null && getId().equals(that.getId());
    }

    @Override
    public final int hashCode() {
        return getId() == null ? 0 : getId().hashCode();
    }
}
