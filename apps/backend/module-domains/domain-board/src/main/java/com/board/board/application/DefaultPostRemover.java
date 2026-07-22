package com.board.board.application;

import com.board.board.application.provided.PostRemover;
import com.board.board.application.required.PostRepository;
import com.board.board.domain.Post;
import com.board.board.domain.exception.PostNotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** {@link PostRemover} 구현. 소프트삭제 시각 세팅은 dirty checking으로 영속된다. */
@Service
class DefaultPostRemover implements PostRemover {

    private final PostRepository postRepository;

    DefaultPostRemover(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Override
    @Transactional
    public void remove(UUID id) {
        Post post = postRepository.findByIdAndDeletedAtIsNull(id).orElseThrow(PostNotFoundException::new);
        post.delete();
    }
}
