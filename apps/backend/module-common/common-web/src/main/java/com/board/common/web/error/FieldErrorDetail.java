package com.board.common.web.error;

/** ProblemDetail의 {@code errors[]} 확장 멤버에 실리는 필드 단위 검증 실패 항목이다. */
public record FieldErrorDetail(String field, String message) {}
