package com.board.api.web.v1.post;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 게시글 작성 요청. {@code @Size(min = 1)}은 계약에 {@code minLength: 1}을 방출하기 위한 것이다 — {@code @NotBlank}만으로는 springdoc이 minLength를 내지 않는다. */
@Schema(description = "게시글 작성 요청")
public record PostCreateRequest(
        @Schema(description = "제목") @NotBlank @Size(min = 1, max = 200)
        String title,

        @Schema(description = "본문") @NotBlank @Size(min = 1, max = 10000)
        String content,

        @Schema(description = "작성자") @NotBlank @Size(min = 1, max = 20)
        String author) {}
