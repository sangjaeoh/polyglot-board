package com.board.domain.board.domain.exception;

import com.board.common.core.error.BaseException;

/** 활성 게시글이 존재하지 않을 때 경계로 전파되는 예외다. */
public class PostNotFoundException extends BaseException {

    public PostNotFoundException() {
        super(BoardErrorCode.POST_NOT_FOUND);
    }
}
