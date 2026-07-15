package com.board.common.core.error;

/**
 * 경계에 도달하는 도메인 예외의 공통 상위 타입이다.
 *
 * <p>{@link ErrorCode}를 실어 {@code ProblemDetail} 핸들러가 상태·코드·메시지를 계약으로 만든다. unchecked라
 * {@code @Transactional} 기본 롤백을 발동한다.
 */
public abstract class BaseException extends RuntimeException {

    private final transient ErrorCode errorCode;

    protected BaseException(ErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
