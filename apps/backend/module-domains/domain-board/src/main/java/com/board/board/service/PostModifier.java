package com.board.board.service;

import com.board.board.entity.Post;
import com.board.board.exception.PostNotFoundException;
import com.board.board.repository.PostRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 게시글 수정을 담당한다. */
@Service
public class PostModifier {

    private final PostRepository postRepository;

    public PostModifier(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    /**
     * 활성 게시글의 제목·본문을 수정한다.
     *
     * @throws PostNotFoundException 활성 게시글이 없을 때
     */
    @Transactional
    public void edit(UUID id, String title, String content) {
        Post post = postRepository.findByIdAndDeletedAtIsNull(id).orElseThrow(PostNotFoundException::new);
        post.edit(title, content);
    }
}
