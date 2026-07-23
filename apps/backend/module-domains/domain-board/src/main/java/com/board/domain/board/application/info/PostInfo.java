package com.board.domain.board.application.info;

import com.board.domain.board.domain.Post;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 게시글의 경계 조회 모델이다. 엔티티가 도메인 모듈 경계를 넘지 않도록 변환한다.
 *
 * @param createdAt 영속된 엔티티의 감사 시각(항상 non-null)
 * @param updatedAt 영속된 엔티티의 감사 시각(항상 non-null)
 */
public record PostInfo(UUID id, String title, String content, String author, Instant createdAt, Instant updatedAt) {

    /** 영속된 게시글 엔티티를 조회 모델로 변환한다. */
    public static PostInfo from(Post post) {
        return new PostInfo(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAuthor(),
                Objects.requireNonNull(post.getCreatedAt(), "createdAt"),
                Objects.requireNonNull(post.getUpdatedAt(), "updatedAt"));
    }
}
