package com.board.api.web.v1.post;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * 리스트 응답의 오프셋 페이지네이션 형상이다.
 *
 * @param <T> 항목 타입
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

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
