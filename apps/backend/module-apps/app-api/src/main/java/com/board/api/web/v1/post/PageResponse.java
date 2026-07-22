package com.board.api.web.v1.post;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * 리스트 응답의 오프셋 페이지네이션 형상이다.
 *
 * @param <T> 항목 타입
 */
@Schema(description = "오프셋 페이지네이션 응답")
public record PageResponse<T>(
        @Schema(description = "페이지 항목 목록") List<T> content,
        @Schema(description = "0-based 페이지 번호") int page,
        @Schema(description = "페이지 크기") int size,
        @Schema(description = "전체 항목 수") long totalElements,
        @Schema(description = "전체 페이지 수") int totalPages) {

    /** 도메인 {@link Page}를 매퍼로 변환해 페이지 응답을 만든다. */
    public static <S, T> PageResponse<T> of(Page<S> page, Function<S, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
