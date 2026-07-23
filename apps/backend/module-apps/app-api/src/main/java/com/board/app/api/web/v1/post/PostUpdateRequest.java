package com.board.app.api.web.v1.post;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 게시글 수정 요청. 작성자는 불변이라 제목·본문만 받는다. {@code @Size(min = 1)}은 계약에 {@code minLength: 1}을 방출하기 위한 것이다. */
@Schema(description = "게시글 수정 요청")
public record PostUpdateRequest(
        @Schema(description = "제목") @NotBlank @Size(min = 1, max = 200)
        String title,

        @Schema(description = "본문") @NotBlank @Size(min = 1, max = 10000)
        String content) {}
