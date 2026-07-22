package com.board.architecture.fixture;

import java.util.UUID;

/** base finder 규칙 픽스처 — 소프트삭제 리포지토리에 base finder를 호출하는 위반 케이스. */
public class SoftDeletedNoteCaller {

    private final SoftDeletedNoteRepository repository;

    public SoftDeletedNoteCaller(SoftDeletedNoteRepository repository) {
        this.repository = repository;
    }

    public void load(UUID id) {
        repository.findById(id);
    }
}
