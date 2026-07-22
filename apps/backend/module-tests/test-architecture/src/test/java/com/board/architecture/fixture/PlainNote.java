package com.board.architecture.fixture;

/** base finder 규칙 픽스처 — deletedAt 없는 엔티티(문서 예외: base finder 그대로 사용). */
public class PlainNote {

    private String title;

    public String title() {
        return title;
    }
}
