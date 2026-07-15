package com.board.common.core.error;

/**
 * 도메인별 에러 코드 집합의 계약이다.
 *
 * <p>{@code ProblemDetail} 핸들러가 이 계약(코드 문자열·사람용 메시지·HTTP 상태)으로 응답을 만든다. 각 도메인의
 * {@code {Name}ErrorCode} enum이 이를 구현한다.
 */
public interface ErrorCode {

    /** 기계 분기용 안정 코드 문자열을 반환한다. */
    String code();

    /** 사람용 기본 메시지를 반환한다. */
    String message();

    /** 매핑되는 HTTP 상태 코드를 반환한다. */
    int status();
}
