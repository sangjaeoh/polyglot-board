package com.board.board.application;

import com.board.board.application.provided.PostModifier;
import com.board.board.application.required.PostRepository;
import com.board.board.domain.Post;
import com.board.board.domain.exception.PostNotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** {@link PostModifier} 구현. 수정은 dirty checking으로 영속된다. */
@Service
class DefaultPostModifier implements PostModifier {

    private final PostRepository postRepository;

    DefaultPostModifier(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Override
    @Transactional
    public void edit(UUID id, String title, String content) {
        Post post = postRepository.findByIdAndDeletedAtIsNull(id).orElseThrow(PostNotFoundException::new);
        post.edit(title, content);
    }
}
