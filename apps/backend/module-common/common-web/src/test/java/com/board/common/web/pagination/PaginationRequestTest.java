package com.board.common.web.pagination;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** {@link PaginationRequest}의 검증 계약과 1-based 보정을 스프링 없이 검증한다. */
class PaginationRequestTest {

    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Nested
    @DisplayName("범위 검증")
    class RangeConstraints {

        static Stream<Arguments> pagingBoundaryCases() {
            return Stream.of(
                    Arguments.of("page 1 미만(0)", 0, 20, false),
                    Arguments.of("size 1 미만(0)", 1, 0, false),
                    Arguments.of("size 100 초과(101)", 1, 101, false),
                    Arguments.of("경계값 page 1·size 1", 1, 1, true),
                    Arguments.of("경계값 size 100", 1, 100, true));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("pagingBoundaryCases")
        @DisplayName("페이징 요청은 page 1 미만·size 1 미만·size 100 초과를 거부하고 경계값(page 1·size 1·size 100)을 수용한다")
        void enforcesRangeBoundaries(String label, int page, int size, boolean accepted) {
            PaginationRequest request = new PaginationRequest(page, size);

            Set<ConstraintViolation<PaginationRequest>> violations = VALIDATOR.validate(request);

            assertThat(violations.isEmpty()).isEqualTo(accepted);
        }
    }

    @Nested
    @DisplayName("기본값·변환")
    class DefaultsAndConversion {

        @Test
        @DisplayName("페이징 요청은 파라미터 생략 시 page 1·size 20으로 보정하고, 1-based 페이지를 0-based로 변환한다")
        void defaultsOmittedParamsAndConvertsToZeroBased() {
            PaginationRequest omitted = new PaginationRequest(null, null);

            assertThat(omitted.page()).isEqualTo(1);
            assertThat(omitted.size()).isEqualTo(20);
            assertThat(omitted.zeroBasedPage()).isZero();
            assertThat(new PaginationRequest(3, 20).zeroBasedPage()).isEqualTo(2);
        }
    }
}
