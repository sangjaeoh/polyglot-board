package com.board.api.facade;

import com.board.board.application.info.PostInfo;
import com.board.board.application.provided.PostAppender;
import com.board.board.application.provided.PostModifier;
import com.board.board.application.provided.PostReader;
import com.board.board.application.provided.PostRemover;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * board 도메인 서비스를 조립한다.
 *
 * <p>트랜잭션을 열지 않는다. 각 도메인 서비스가 자기 트랜잭션을 소유한다.
 */
@Component
public class BoardFacade {

    private final PostReader postReader;
    private final PostAppender postAppender;
    private final PostModifier postModifier;
    private final PostRemover postRemover;

    public BoardFacade(
            PostReader postReader, PostAppender postAppender, PostModifier postModifier, PostRemover postRemover) {
        this.postReader = postReader;
        this.postAppender = postAppender;
        this.postModifier = postModifier;
        this.postRemover = postRemover;
    }

    /** 활성 게시글을 최신순으로 페이지 조회한다. 범위 검증은 경계(컨트롤러 Bean Validation)가 맡는다. */
    public Page<PostInfo> getPosts(int page, int size) {
        return postReader.getPosts(PageRequest.of(page, size));
    }

    /** 게시글 상세를 조회한다. */
    public PostInfo getPost(UUID id) {
        return postReader.getPost(id);
    }

    /** 게시글을 작성하고 생성 결과를 반환한다. */
    public PostInfo create(String title, String content, String author) {
        return postAppender.register(title, content, author);
    }

    /** 게시글을 수정한다. */
    public void update(UUID id, String title, String content) {
        postModifier.edit(id, title, content);
    }

    /** 게시글을 소프트삭제한다. */
    public void delete(UUID id) {
        postRemover.remove(id);
    }
}
