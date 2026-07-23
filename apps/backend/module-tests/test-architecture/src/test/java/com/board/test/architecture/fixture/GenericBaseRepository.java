package com.board.test.architecture.fixture;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** base finder 규칙 픽스처 — 제네릭 중간 인터페이스(엔티티 해석 불가 → fail-closed). */
public interface GenericBaseRepository<E> extends JpaRepository<E, UUID> {}
