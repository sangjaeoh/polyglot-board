package com.board.common.web.pagination;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/** {@link PaginationResponse}의 1-based 보정과 int64 string 표현을 스프링 없이 검증한다. */
class PaginationResponseTest {

    @Nested
    @DisplayName("응답 보정")
    class ResponseCorrection {

        @Test
        @DisplayName("페이지 응답은 0-based Page를 1-based page로 보정하고 전체 항목 수를 문자열로 싣는다")
        void correctsZeroBasedPageAndCarriesTotalAsString() {
            Page<String> domainPage = new PageImpl<>(List.of("항목"), PageRequest.of(2, 10), 21);

            PaginationResponse<String> response = PaginationResponse.from(domainPage);

            assertThat(response.page()).isEqualTo(3);
            assertThat(response.pageSize()).isEqualTo(10);
            assertThat(response.totalElements()).isEqualTo("21");
            assertThat(response.totalPages()).isEqualTo(3);
            assertThat(response.content()).containsExactly("항목");
        }
    }
}
