package com.board.board.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** {@link Post}의 도메인 로직(생성·수정·소프트삭제)을 스프링 없이 검증한다. */
class PostTest {

    @Nested
    @DisplayName("생성")
    class Create {

        @Test
        @DisplayName("게시글을 생성하면 ID가 부여되고 제목·본문·작성자가 설정된다")
        void createAssignsIdAndFields() {
            Post post = Post.create("제목", "본문", "글쓴이");

            assertThat(post.getId()).isNotNull();
            assertThat(post.getTitle()).isEqualTo("제목");
            assertThat(post.getContent()).isEqualTo("본문");
            assertThat(post.getAuthor()).isEqualTo("글쓴이");
            assertThat(post.getDeletedAt()).isNull();
        }

        static Stream<Arguments> nullCreateFields() {
            return Stream.of(
                    Arguments.of("제목 null", null, "본문", "글쓴이"),
                    Arguments.of("본문 null", "제목", null, "글쓴이"),
                    Arguments.of("작성자 null", "제목", "본문", null));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("nullCreateFields")
        @DisplayName("제목·본문·작성자가 null이면 생성이 거부된다")
        void createRejectsNullField(String label, String title, String content, String author) {
            assertThatIllegalArgumentException().isThrownBy(() -> Post.create(title, content, author));
        }
    }

    @Nested
    @DisplayName("수정")
    class Edit {

        @Test
        @DisplayName("게시글을 수정하면 제목·본문이 교체되고 작성자는 유지된다")
        void editReplacesTitleAndContentKeepingAuthor() {
            Post post = Post.create("제목", "본문", "글쓴이");

            post.edit("새 제목", "새 본문");

            assertThat(post.getTitle()).isEqualTo("새 제목");
            assertThat(post.getContent()).isEqualTo("새 본문");
            assertThat(post.getAuthor()).isEqualTo("글쓴이");
        }

        @Test
        @DisplayName("수정할 제목·본문이 null이면 수정이 거부된다")
        void editRejectsNullField() {
            Post post = Post.create("제목", "본문", "글쓴이");

            assertThatIllegalArgumentException().isThrownBy(() -> post.edit(null, "새 본문"));
            assertThatIllegalArgumentException().isThrownBy(() -> post.edit("새 제목", null));
        }
    }

    @Nested
    @DisplayName("삭제")
    class Delete {

        @Test
        @DisplayName("게시글을 삭제하면 삭제 시각이 설정된다")
        void deleteMarksDeletedAt() {
            Post post = Post.create("제목", "본문", "글쓴이");

            post.delete();

            assertThat(post.getDeletedAt()).isNotNull();
        }
    }
}
