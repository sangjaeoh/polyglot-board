package com.board.architecture.fixture;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** base finder 규칙 픽스처 — 소프트삭제 엔티티의 리포지토리. */
public interface SoftDeletedNoteRepository extends JpaRepository<SoftDeletedNote, UUID> {}
