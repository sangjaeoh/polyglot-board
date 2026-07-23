package com.board.domain.board.application.required;

import static org.assertj.core.api.Assertions.assertThat;

import com.board.domain.board.domain.Post;
import com.board.domain.board.support.ContainerConfig;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/** 활성 필터 파생 쿼리가 소프트삭제분을 제외하는지 실 PostgreSQL(도메인 소유 마이그레이션)에 대해 검증한다. */
@DataJpaTest
@Import(ContainerConfig.class)
class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Nested
    @DisplayName("활성 조회")
    class ActiveFinders {

        @Test
        @DisplayName("활성 게시글 id 조회는 소프트삭제된 게시글을 제외한다")
        void findByIdExcludesSoftDeleted() {
            Post active = postRepository.save(Post.create("남는 글", "본문", "글쓴이"));
            Post deleted = postRepository.save(Post.create("삭제될 글", "본문", "글쓴이"));
            deleted.delete();
            entityManager.flush();
            entityManager.clear();

            Optional<Post> activeFound = postRepository.findByIdAndDeletedAtIsNull(active.getId());
            Optional<Post> deletedFound = postRepository.findByIdAndDeletedAtIsNull(deleted.getId());

            assertThat(activeFound).isPresent();
            assertThat(deletedFound).isEmpty();
        }

        @Test
        @DisplayName("활성 게시글 목록 조회는 소프트삭제분을 제외하고 최신순으로 정렬한다")
        void listExcludesSoftDeletedAndOrdersLatestFirst() {
            Post first = postRepository.save(Post.create("첫 글", "본문", "글쓴이"));
            Post second = postRepository.save(Post.create("둘째 글", "본문", "글쓴이"));
            Post third = postRepository.save(Post.create("셋째 글", "본문", "글쓴이"));
            second.delete();
            entityManager.flush();
            entityManager.clear();

            Page<Post> page = postRepository.findByDeletedAtIsNullOrderByCreatedAtDescIdDesc(PageRequest.of(0, 10));

            assertThat(page.getContent()).extracting(Post::getId).containsExactly(third.getId(), first.getId());
            assertThat(page.getTotalElements()).isEqualTo(2);
        }
    }
}
