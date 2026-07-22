package com.board.board.application.provided;

import com.board.board.application.info.PostInfo;

/** 게시글 생성 진입 계약이다. 모듈 밖 호출은 이 인터페이스로만 한다. */
public interface PostAppender {

    /** 게시글을 등록하고 생성 결과를 반환한다. */
    PostInfo register(String title, String content, String author);
}
