package com.board.board.application;

import com.board.board.application.provided.PostAppender;
import com.board.board.application.required.PostRepository;
import com.board.board.domain.Post;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** {@link PostAppender} 구현. 게시글 생성을 담당한다. */
@Service
class DefaultPostAppender implements PostAppender {

    private final PostRepository postRepository;

    DefaultPostAppender(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Override
    @Transactional
    public UUID register(String title, String content, String author) {
        Post post = Post.create(title, content, author);
        return postRepository.save(post).getId();
    }
}
