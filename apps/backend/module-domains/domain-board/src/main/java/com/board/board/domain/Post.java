package com.board.board.domain;

import com.board.common.core.id.UuidV7Generator;
import com.board.common.jpa.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * 게시판 글. board 애그리거트 루트다.
 *
 * <p>생성은 {@link #create}, 수정은 {@link #edit}, 삭제는 {@link #delete}(소프트삭제)로만 한다. setter를
 * 두지 않아 불변식과 상태 전이를 엔티티가 소유한다.
 */
@Entity
@Table(schema = "board", name = "post")
public class Post extends BaseTimeEntity<UUID> {

    @Id
    private UUID id;

    @Column(length = 200)
    private String title;

    @Column(length = 10000)
    private String content;

    @Column(length = 20)
    private String author;

    private @Nullable Instant deletedAt;

    protected Post() {}

    private Post(UUID id, String title, String content, String author) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.author = author;
    }

    /**
     * 새 게시글을 만든다.
     *
     * @throws IllegalArgumentException 값이 {@code null}인 경우(경계 DTO 검증 이후의 선행조건 백스톱)
     */
    public static Post create(String title, String content, String author) {
        return new Post(
                UuidV7Generator.generate(),
                required(title, "title"),
                required(content, "content"),
                required(author, "author"));
    }

    /**
     * 제목·본문을 수정한다.
     *
     * @throws IllegalArgumentException 값이 {@code null}인 경우
     */
    public void edit(String title, String content) {
        this.title = required(title, "title");
        this.content = required(content, "content");
    }

    private static String required(@Nullable String value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + "는 null일 수 없다");
        }
        return value;
    }

    /** 소프트삭제한다. {@code deletedAt}을 세팅할 뿐 물리 DELETE 하지 않는다. */
    public void delete() {
        this.deletedAt = Instant.now();
    }

    @Override
    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getAuthor() {
        return author;
    }

    public @Nullable Instant getDeletedAt() {
        return deletedAt;
    }
}
