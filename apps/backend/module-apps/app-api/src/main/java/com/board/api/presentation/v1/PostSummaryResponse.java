package com.board.api.presentation.v1;

import com.board.board.info.PostInfo;
import java.time.Instant;
import java.util.UUID;

/** 게시글 목록 항목 응답. 본문을 제외한 가벼운 형상이다. */
public record PostSummaryResponse(UUID id, String title, String author, Instant createdAt) {

    public static PostSummaryResponse from(PostInfo info) {
        return new PostSummaryResponse(info.id(), info.title(), info.author(), info.createdAt());
    }
}
