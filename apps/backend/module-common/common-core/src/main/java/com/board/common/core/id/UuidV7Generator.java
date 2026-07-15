package com.board.common.core.id;

import com.github.f4b6a3.uuid.UuidCreator;
import java.util.UUID;

/**
 * 애플리케이션 생성 UUIDv7을 발급한다.
 *
 * <p>DB 왕복 없이 시간순 정렬 가능한 식별자를 확정해 엔티티 {@code create()} 팩토리가 {@code @Id}를 채운다.
 */
public final class UuidV7Generator {

    private UuidV7Generator() {}

    /** 시간순 정렬 가능한 새 UUIDv7을 반환한다. */
    public static UUID generate() {
        return UuidCreator.getTimeOrderedEpoch();
    }
}
