package com.board.domain.board.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.board.domain.board.application.provided.PostRemover;
import com.board.domain.board.application.required.PostRepository;
import com.board.domain.board.domain.Post;
import com.board.domain.board.domain.exception.BoardErrorCode;
import com.board.domain.board.domain.exception.PostNotFoundException;
import com.board.domain.board.support.ContainerConfig;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

/** {@link DefaultPostRemover}의 소프트삭제 행동을 실 DB 슬라이스로 검증한다(flush 후 재조회). */
@DataJpaTest
@Import({ContainerConfig.class, DefaultPostRemover.class})
class DefaultPostRemoverTest {

    @Autowired
    private PostRemover postRemover;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Nested
    @DisplayName("삭제")
    class Remove {

        @Test
        @DisplayName("활성 게시글을 삭제하면 삭제 시각이 영속된다")
        void removePersistsDeletedAt() {
            Post post = postRepository.save(Post.create("제목", "본문", "글쓴이"));
            entityManager.flush();
            entityManager.clear();

            postRemover.remove(post.getId());

            entityManager.flush();
            entityManager.clear();
            assertThat(postRepository.findByIdAndDeletedAtIsNull(post.getId())).isEmpty();
            Post reloaded = entityManager.find(Post.class, post.getId());
            assertThat(reloaded.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("없거나 삭제된 게시글 삭제는 POST_NOT_FOUND 예외를 던진다")
        void removeRejectsAbsentOrDeleted() {
            Post deleted = postRepository.save(Post.create("삭제된 글", "본문", "글쓴이"));
            deleted.delete();
            entityManager.flush();
            entityManager.clear();

            PostNotFoundException absentThrown =
                    catchThrowableOfType(PostNotFoundException.class, () -> postRemover.remove(UUID.randomUUID()));
            PostNotFoundException deletedThrown =
                    catchThrowableOfType(PostNotFoundException.class, () -> postRemover.remove(deleted.getId()));

            assertThat(absentThrown.getErrorCode()).isEqualTo(BoardErrorCode.POST_NOT_FOUND);
            assertThat(deletedThrown.getErrorCode()).isEqualTo(BoardErrorCode.POST_NOT_FOUND);
        }
    }
}
