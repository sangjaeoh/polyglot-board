package com.board.api.web.v1.post;

import com.board.board.info.PostInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/** 게시글 목록 항목 응답. 본문을 제외한 가벼운 형상이다. */
@Schema(description = "게시글 목록 항목 응답")
public record PostSummaryResponse(
        @Schema(description = "게시글 ID") UUID id,
        @Schema(description = "제목") String title,
        @Schema(description = "작성자") String author,
        @Schema(description = "작성 시각") Instant createdAt) {

    public static PostSummaryResponse from(PostInfo info) {
        return new PostSummaryResponse(info.id(), info.title(), info.author(), info.createdAt());
    }
}
