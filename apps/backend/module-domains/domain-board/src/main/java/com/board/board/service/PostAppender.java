package com.board.board.service;

import com.board.board.entity.Post;
import com.board.board.info.PostInfo;
import com.board.board.repository.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 게시글 생성을 담당한다. Info 변환까지 트랜잭션 안에서 끝낸다. */
@Service
public class PostAppender {

    private final PostRepository postRepository;

    public PostAppender(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    /** 게시글을 등록하고 생성 결과를 반환한다. 응답 조립을 위한 재조회 트랜잭션을 없앤다. */
    @Transactional
    public PostInfo register(String title, String content, String author) {
        Post post = Post.create(title, content, author);
        return PostInfo.from(postRepository.save(post));
    }
}
