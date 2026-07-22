package com.board.board.application;

import com.board.board.application.info.PostInfo;
import com.board.board.application.provided.PostAppender;
import com.board.board.application.required.PostRepository;
import com.board.board.domain.Post;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** {@link PostAppender} 구현. 생성 결과의 Info 변환까지 트랜잭션 안에서 끝내 재조회 트랜잭션을 없앤다. */
@Service
class DefaultPostAppender implements PostAppender {

    private final PostRepository postRepository;

    DefaultPostAppender(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Override
    @Transactional
    public PostInfo register(String title, String content, String author) {
        Post post = Post.create(title, content, author);
        return PostInfo.from(postRepository.save(post));
    }
}
