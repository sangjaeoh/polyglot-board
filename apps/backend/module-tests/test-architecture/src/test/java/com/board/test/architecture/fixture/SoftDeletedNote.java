package com.board.test.architecture.fixture;

import java.time.Instant;

/** base finder 규칙 픽스처 — 소프트삭제 엔티티(deletedAt 보유). */
public class SoftDeletedNote {

    private Instant deletedAt;

    public Instant deletedAt() {
        return deletedAt;
    }
}
