package com.board.board.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.board.board.application.provided.PostAppender;
import com.board.board.application.required.PostRepository;
import com.board.board.domain.Post;
import com.board.board.support.ContainerConfig;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

/** {@link DefaultPostAppender}의 등록 행동을 실 DB 슬라이스로 검증한다. */
@DataJpaTest
@Import({ContainerConfig.class, DefaultPostAppender.class})
class DefaultPostAppenderTest {

    @Autowired
    private PostAppender postAppender;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Nested
    @DisplayName("등록")
    class Register {

        @Test
        @DisplayName("게시글을 등록하면 영속되고 ID가 반환된다")
        void registerPersistsPostAndReturnsId() {
            UUID id = postAppender.register("제목", "본문", "글쓴이");

            entityManager.flush();
            entityManager.clear();
            Post persisted = postRepository.findByIdAndDeletedAtIsNull(id).orElseThrow();
            assertThat(persisted.getTitle()).isEqualTo("제목");
            assertThat(persisted.getContent()).isEqualTo("본문");
            assertThat(persisted.getAuthor()).isEqualTo("글쓴이");
        }
    }
}
