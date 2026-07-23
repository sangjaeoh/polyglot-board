package com.board.domain.board.application.provided;

import com.board.domain.board.application.info.PostInfo;
import com.board.domain.board.domain.exception.PostNotFoundException;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** 게시글 조회 진입 계약이다. 모듈 밖 호출은 이 인터페이스로만 한다. */
public interface PostReader {

    /**
     * 활성 게시글 하나를 조회한다.
     *
     * @throws PostNotFoundException 활성 게시글이 없을 때
     */
    PostInfo getPost(UUID id);

    /** 활성 게시글을 최신순으로 페이지 조회한다. */
    Page<PostInfo> getPosts(Pageable pageable);
}
