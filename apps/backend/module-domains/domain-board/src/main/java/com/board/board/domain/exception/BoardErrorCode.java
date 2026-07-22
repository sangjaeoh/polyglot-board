package com.board.board.domain.exception;

import com.board.common.core.error.ErrorCode;

/** board 도메인의 에러 코드 집합이다. */
public enum BoardErrorCode implements ErrorCode {
    POST_NOT_FOUND("POST_NOT_FOUND", "게시글을 찾을 수 없다.", 404);

    private final String code;
    private final String message;
    private final int status;

    BoardErrorCode(String code, String message, int status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public int status() {
        return status;
    }
}
