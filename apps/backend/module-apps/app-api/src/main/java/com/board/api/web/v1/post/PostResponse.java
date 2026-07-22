package com.board.api.web.v1.post;

import com.board.board.info.PostInfo;
import java.time.Instant;
import java.util.UUID;

/** 게시글 상세 응답. */
public record PostResponse(UUID id, String title, String content, String author, Instant createdAt, Instant updatedAt) {

    public static PostResponse from(PostInfo info) {
        return new PostResponse(
                info.id(), info.title(), info.content(), info.author(), info.createdAt(), info.updatedAt());
    }
}
