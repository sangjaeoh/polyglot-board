package com.board.board.service;

import com.board.board.entity.Post;
import com.board.board.exception.PostNotFoundException;
import com.board.board.info.PostInfo;
import com.board.board.repository.PostRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 게시글 조회를 담당한다. Info 변환까지 트랜잭션 안에서 끝낸다. */
@Service
public class PostReader {

    private final PostRepository postRepository;

    public PostReader(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    /**
     * 활성 게시글 하나를 조회한다.
     *
     * @throws PostNotFoundException 활성 게시글이 없을 때
     */
    @Transactional(readOnly = true)
    public PostInfo getPost(UUID id) {
        Post post = postRepository.findByIdAndDeletedAtIsNull(id).orElseThrow(PostNotFoundException::new);
        return PostInfo.from(post);
    }

    /** 활성 게시글을 최신순으로 페이지 조회한다. */
    @Transactional(readOnly = true)
    public Page<PostInfo> getPosts(Pageable pageable) {
        return postRepository
                .findByDeletedAtIsNullOrderByCreatedAtDescIdDesc(pageable)
                .map(PostInfo::from);
    }
}
