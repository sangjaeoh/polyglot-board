package com.board.test.architecture.fixture;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** base finder 규칙 픽스처 — deletedAt 없는 엔티티의 리포지토리. */
public interface PlainNoteRepository extends JpaRepository<PlainNote, UUID> {}
