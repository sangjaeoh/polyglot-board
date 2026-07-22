package com.board.board.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.board.board.application.info.PostInfo;
import com.board.board.application.provided.PostReader;
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

/** {@link DefaultPostReader}의 조회 행동을 실 DB 슬라이스로 검증한다. */
@DataJpaTest
@Import({ContainerConfig.class, DefaultPostReader.class})
class DefaultPostReaderTest {

    @Autowired
    private PostReader postReader;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Nested
    @DisplayName("상세 조회")
    class GetPost {

        @Test
        @DisplayName("활성 게시글을 조회하면 Info로 반환된다")
        void getPostReturnsInfo() {
            Post post = postRepository.save(Post.create("제목", "본문", "글쓴이"));
            entityManager.flush();
            entityManager.clear();

            PostInfo info = postReader.getPost(post.getId());

            assertThat(info.id()).isEqualTo(post.getId());
            assertThat(info.title()).isEqualTo("제목");
            assertThat(info.content()).isEqualTo("본문");
            assertThat(info.author()).isEqualTo("글쓴이");
            assertThat(info.createdAt()).isNotNull();
            assertThat(info.updatedAt()).isNotNull();
        }

        @Test
        @DisplayName("없거나 삭제된 게시글 조회는 POST_NOT_FOUND 예외를 던진다")
        void getPostRejectsAbsentOrDeleted() {
            Post deleted = postRepository.save(Post.create("삭제된 글", "본문", "글쓴이"));
            deleted.delete();
            entityManager.flush();
            entityManager.clear();

            PostNotFoundException absentThrown =
                    catchThrowableOfType(PostNotFoundException.class, () -> postReader.getPost(UUID.randomUUID()));
            PostNotFoundException deletedThrown =
                    catchThrowableOfType(PostNotFoundException.class, () -> postReader.getPost(deleted.getId()));

            assertThat(absentThrown.getErrorCode()).isEqualTo(BoardErrorCode.POST_NOT_FOUND);
            assertThat(deletedThrown.getErrorCode()).isEqualTo(BoardErrorCode.POST_NOT_FOUND);
        }
    }
}
