package com.board.board.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

/** {@link Post}의 도메인 로직(생성·수정·소프트삭제)을 스프링 없이 검증한다. */
class PostTest {

    @Test
    void createAssignsIdAndFields() {
        Post post = Post.create("제목", "본문", "글쓴이");

        assertThat(post.getId()).isNotNull();
        assertThat(post.getTitle()).isEqualTo("제목");
        assertThat(post.getContent()).isEqualTo("본문");
        assertThat(post.getAuthor()).isEqualTo("글쓴이");
        assertThat(post.getDeletedAt()).isNull();
    }

    @Test
    void createRejectsNullField() {
        assertThatIllegalArgumentException().isThrownBy(() -> Post.create(null, "본문", "글쓴이"));
        assertThatIllegalArgumentException().isThrownBy(() -> Post.create("제목", null, "글쓴이"));
        assertThatIllegalArgumentException().isThrownBy(() -> Post.create("제목", "본문", null));
    }

    @Test
    void editReplacesTitleAndContent() {
        Post post = Post.create("제목", "본문", "글쓴이");

        post.edit("새 제목", "새 본문");

        assertThat(post.getTitle()).isEqualTo("새 제목");
        assertThat(post.getContent()).isEqualTo("새 본문");
        assertThat(post.getAuthor()).isEqualTo("글쓴이");
    }

    @Test
    void editRejectsNullField() {
        Post post = Post.create("제목", "본문", "글쓴이");

        assertThatIllegalArgumentException().isThrownBy(() -> post.edit(null, "새 본문"));
        assertThatIllegalArgumentException().isThrownBy(() -> post.edit("새 제목", null));
    }

    @Test
    void deleteMarksDeletedAt() {
        Post post = Post.create("제목", "본문", "글쓴이");

        post.delete();

        assertThat(post.getDeletedAt()).isNotNull();
    }
}
