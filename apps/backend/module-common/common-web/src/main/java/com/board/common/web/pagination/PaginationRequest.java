package com.board.common.web.pagination;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 페이징 GET의 1-based 요청 파라미터다. 컨트롤러에서 {@code @Valid @ParameterObject}로 사용한다.
 *
 * <p>파라미터 생략 시 프레임워크가 생성자 바인딩으로 null을 전달한다({@code @NullMarked} 사각) — compact
 * constructor가 즉시 기본값으로 보정하므로 관측 가능한 상태는 non-null이다. 계약의 defaultValue는
 * {@code @Schema}가 별도 소유하며, 코드 기본값과의 일치는 통합 테스트가 검증한다.
 */
@Schema(description = "1-based 오프셋 페이지네이션 요청")
public record PaginationRequest(
        @Schema(description = "1-based 페이지 번호", defaultValue = "1") @Min(1)
        Integer page,

        @Schema(description = "페이지 크기(1-100)", defaultValue = "20") @Min(1) @Max(100)
        Integer size) {

    public PaginationRequest {
        page = page != null ? page : 1;
        size = size != null ? size : 20;
    }

    /** 도메인 {@code Pageable}용 0-based 페이지. 요청 변환(1-based → 0-based)을 이 메서드가 소유한다. */
    public int zeroBasedPage() {
        return page - 1;
    }
}
