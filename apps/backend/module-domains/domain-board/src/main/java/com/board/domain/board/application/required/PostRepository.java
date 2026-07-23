package com.board.domain.board.application.required;

import com.board.domain.board.domain.Post;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 게시글 영속 포트다.
 *
 * <p>소프트삭제 엔티티라 base {@code findById}·{@code findAll} 직접 호출을 금지한다. 활성-only 파생 쿼리는
 * 이름에 활성 필터({@code DeletedAtIsNull})를 담는다.
 */
public interface PostRepository extends JpaRepository<Post, UUID> {

    /** 활성 게시글을 id로 조회한다. */
    Optional<Post> findByIdAndDeletedAtIsNull(UUID id);

    /** 활성 게시글을 최신순(생성시각·id 내림차순)으로 페이지 조회한다. */
    Page<Post> findByDeletedAtIsNullOrderByCreatedAtDescIdDesc(Pageable pageable);
}
