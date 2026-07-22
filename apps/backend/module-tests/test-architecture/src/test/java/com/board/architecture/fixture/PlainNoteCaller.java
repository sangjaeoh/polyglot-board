package com.board.architecture.fixture;

import java.util.UUID;

/** base finder 규칙 픽스처 — deletedAt 없는 엔티티 리포지토리의 base finder 호출(허용 케이스). */
public class PlainNoteCaller {

    private final PlainNoteRepository repository;

    public PlainNoteCaller(PlainNoteRepository repository) {
        this.repository = repository;
    }

    public void load(UUID id) {
        repository.findById(id);
    }
}
