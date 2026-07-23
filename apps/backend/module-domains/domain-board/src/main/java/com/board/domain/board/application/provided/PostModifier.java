package com.board.domain.board.application.provided;

import com.board.domain.board.domain.exception.PostNotFoundException;
import java.util.UUID;

/** 게시글 수정 진입 계약이다. 모듈 밖 호출은 이 인터페이스로만 한다. */
public interface PostModifier {

    /**
     * 활성 게시글의 제목·본문을 수정한다.
     *
     * @throws PostNotFoundException 활성 게시글이 없을 때
     */
    void edit(UUID id, String title, String content);
}
