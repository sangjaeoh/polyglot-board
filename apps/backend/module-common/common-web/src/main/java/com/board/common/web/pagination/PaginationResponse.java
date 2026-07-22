package com.board.common.web.pagination;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

/**
 * 리스트 응답의 오프셋 페이지네이션 형상이다. {@code page} 컴포넌트는 1-based다.
 *
 * @param <T> 항목 타입
 */
@Schema(description = "오프셋 페이지네이션 응답(1-based)")
public record PaginationResponse<T>(
        @Schema(description = "페이지 항목 목록") List<T> content,
        @Schema(description = "1-based 페이지 번호") int page,
        @Schema(description = "페이지 크기") int pageSize,

        @Schema(description = "전체 항목 수(int64 — JSON number의 2^53 정밀도 제한을 피해 string으로 표현)")
        String totalElements,

        @Schema(description = "전체 페이지 수") int totalPages) {

    public PaginationResponse {
        // 컬렉션 필드는 방어적 복사 후 불변화한다(coding-conventions).
        content = List.copyOf(content);
    }

    /** 도메인 {@link Page}(0-based)에서 만든다. 응답 보정(0-based → 1-based)을 이 메서드가 소유한다. */
    public static <T> PaginationResponse<T> from(Page<T> page) {
        return new PaginationResponse<>(
                page.getContent(),
                page.getNumber() + 1,
                page.getSize(),
                String.valueOf(page.getTotalElements()),
                page.getTotalPages());
    }
}
