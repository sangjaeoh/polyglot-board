package com.board.board.application.provided;

import java.util.UUID;

/** 게시글 생성 진입 계약이다. 모듈 밖 호출은 이 인터페이스로만 한다. */
public interface PostAppender {

    /** 게시글을 등록하고 생성된 ID만 반환한다(명령은 최소 결과). 응답 조립은 조회 계약 재조회로 한다. */
    UUID register(String title, String content, String author);
}
