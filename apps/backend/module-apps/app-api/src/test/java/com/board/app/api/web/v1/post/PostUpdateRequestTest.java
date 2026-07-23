package com.board.app.api.web.v1.post;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** {@link PostUpdateRequest}의 Bean Validation 계약을 standalone Validator로 검증한다. */
class PostUpdateRequestTest {

    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Nested
    @DisplayName("검증 거부")
    class Rejections {

        static Stream<Arguments> invalidUpdateRequests() {
            return Stream.of(
                    Arguments.of("빈 제목", new PostUpdateRequest("", "본문")),
                    Arguments.of("200자 초과 제목", new PostUpdateRequest("가".repeat(201), "본문")),
                    Arguments.of("빈 본문", new PostUpdateRequest("제목", "")),
                    Arguments.of("10000자 초과 본문", new PostUpdateRequest("제목", "가".repeat(10001))));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidUpdateRequests")
        @DisplayName("게시글 수정 요청은 빈 제목·200자 초과 제목·빈 본문·10000자 초과 본문을 각각 거부한다")
        void rejectsEachConstraintViolation(String label, PostUpdateRequest request) {
            Set<ConstraintViolation<PostUpdateRequest>> violations = VALIDATOR.validate(request);

            assertThat(violations).isNotEmpty();
        }
    }
}
