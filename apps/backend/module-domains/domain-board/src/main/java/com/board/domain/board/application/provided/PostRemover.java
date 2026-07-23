package com.board.domain.board.application.provided;

import com.board.domain.board.domain.exception.PostNotFoundException;
import java.util.UUID;

/** 게시글 삭제(소프트) 진입 계약이다. 모듈 밖 호출은 이 인터페이스로만 한다. */
public interface PostRemover {

    /**
     * 활성 게시글을 소프트삭제한다.
     *
     * @throws PostNotFoundException 활성 게시글이 없을 때
     */
    void remove(UUID id);
}
