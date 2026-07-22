package com.board.board.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.board.board.application.provided.PostModifier;
import com.board.board.application.required.PostRepository;
import com.board.board.domain.Post;
import com.board.board.domain.exception.BoardErrorCode;
import com.board.board.domain.exception.PostNotFoundException;
import com.board.board.support.ContainerConfig;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

/** {@link DefaultPostModifier}의 수정 행동을 실 DB 슬라이스로 검증한다(flush 후 재조회). */
@DataJpaTest
@Import({ContainerConfig.class, DefaultPostModifier.class})
class DefaultPostModifierTest {

    @Autowired
    private PostModifier postModifier;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Nested
    @DisplayName("수정")
    class Edit {

        @Test
        @DisplayName("활성 게시글을 수정하면 영속 상태에 반영된다")
        void editPersistsReplacedTitleAndContent() {
            Post post = postRepository.save(Post.create("제목", "본문", "글쓴이"));
            entityManager.flush();
            entityManager.clear();

            postModifier.edit(post.getId(), "새 제목", "새 본문");

            entityManager.flush();
            entityManager.clear();
            Post reloaded =
                    postRepository.findByIdAndDeletedAtIsNull(post.getId()).orElseThrow();
            assertThat(reloaded.getTitle()).isEqualTo("새 제목");
            assertThat(reloaded.getContent()).isEqualTo("새 본문");
            assertThat(reloaded.getAuthor()).isEqualTo("글쓴이");
        }

        @Test
        @DisplayName("없거나 삭제된 게시글 수정은 POST_NOT_FOUND 예외를 던진다")
        void editRejectsAbsentOrDeleted() {
            Post deleted = postRepository.save(Post.create("삭제된 글", "본문", "글쓴이"));
            deleted.delete();
            entityManager.flush();
            entityManager.clear();

            PostNotFoundException absentThrown = catchThrowableOfType(
                    PostNotFoundException.class, () -> postModifier.edit(UUID.randomUUID(), "새 제목", "새 본문"));
            PostNotFoundException deletedThrown = catchThrowableOfType(
                    PostNotFoundException.class, () -> postModifier.edit(deleted.getId(), "새 제목", "새 본문"));

            assertThat(absentThrown.getErrorCode()).isEqualTo(BoardErrorCode.POST_NOT_FOUND);
            assertThat(deletedThrown.getErrorCode()).isEqualTo(BoardErrorCode.POST_NOT_FOUND);
        }
    }
}
