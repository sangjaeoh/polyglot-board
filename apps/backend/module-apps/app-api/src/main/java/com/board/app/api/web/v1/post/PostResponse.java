package com.board.app.api.web.v1.post;

import com.board.domain.board.application.info.PostInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/** 게시글 상세 응답. */
@Schema(description = "게시글 상세 응답")
public record PostResponse(
        @Schema(description = "게시글 ID") UUID id,
        @Schema(description = "제목") String title,
        @Schema(description = "본문") String content,
        @Schema(description = "작성자") String author,
        @Schema(description = "작성 시각") Instant createdAt,
        @Schema(description = "수정 시각") Instant updatedAt) {

    public static PostResponse from(PostInfo info) {
        return new PostResponse(
                info.id(), info.title(), info.content(), info.author(), info.createdAt(), info.updatedAt());
    }
}
