package com.board.test.architecture.fixture;

import java.util.UUID;

/** base finder 규칙 픽스처 — 중간 인터페이스 경유 호출(엔티티 해석 불가라 fail-closed 위반 케이스). */
public class GenericBaseCaller {

    private final GenericBaseRepository<PlainNote> repository;

    public GenericBaseCaller(GenericBaseRepository<PlainNote> repository) {
        this.repository = repository;
    }

    public void load(UUID id) {
        repository.findById(id);
    }
}
