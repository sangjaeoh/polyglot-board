package com.board.board.service;

import com.board.board.entity.Post;
import com.board.board.exception.PostNotFoundException;
import com.board.board.repository.PostRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 게시글 삭제(소프트)를 담당한다. */
@Service
public class PostRemover {

    private final PostRepository postRepository;

    public PostRemover(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    /**
     * 활성 게시글을 소프트삭제한다.
     *
     * @throws PostNotFoundException 활성 게시글이 없을 때
     */
    @Transactional
    public void remove(UUID id) {
        Post post = postRepository.findByIdAndDeletedAtIsNull(id).orElseThrow(PostNotFoundException::new);
        post.delete();
    }
}
