package com.board.board.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.board.board.entity.Post;
import com.board.board.support.ContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

/** 활성 필터 파생 쿼리가 소프트삭제분을 제외하는지 실 PostgreSQL(도메인 소유 마이그레이션)에 대해 검증한다. */
@SpringBootTest
@Import(ContainerConfig.class)
@Transactional
class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Test
    void findByIdExcludesSoftDeleted() {
        Post post = postRepository.save(Post.create("삭제될 글", "본문", "글쓴이"));
        assertThat(postRepository.findByIdAndDeletedAtIsNull(post.getId())).isPresent();

        post.delete();

        assertThat(postRepository.findByIdAndDeletedAtIsNull(post.getId())).isEmpty();
    }

    @Test
    void listExcludesSoftDeletedAndOrdersLatestFirst() {
        Post first = postRepository.save(Post.create("첫 글", "본문", "글쓴이"));
        Post second = postRepository.save(Post.create("둘째 글", "본문", "글쓴이"));
        Post third = postRepository.save(Post.create("셋째 글", "본문", "글쓴이"));

        second.delete();

        Page<Post> page = postRepository.findByDeletedAtIsNullOrderByCreatedAtDescIdDesc(PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Post::getId).containsExactly(third.getId(), first.getId());
        assertThat(page.getTotalElements()).isEqualTo(2);
    }
}
