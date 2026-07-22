package com.board.api.web.v1.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 게시글 작성 요청. {@code @Size(min = 1)}은 계약에 {@code minLength: 1}을 방출하기 위한 것이다 — {@code @NotBlank}만으로는 springdoc이 minLength를 내지 않는다. */
public record PostCreateRequest(
        @NotBlank @Size(min = 1, max = 200) String title,
        @NotBlank @Size(min = 1, max = 10000) String content,
        @NotBlank @Size(min = 1, max = 20) String author) {}
