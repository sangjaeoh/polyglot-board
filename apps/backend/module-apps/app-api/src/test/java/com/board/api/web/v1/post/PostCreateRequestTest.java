package com.board.api.web.v1.post;

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

/** {@link PostCreateRequest}의 Bean Validation 계약을 standalone Validator로 검증한다. */
class PostCreateRequestTest {

    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Nested
    @DisplayName("검증 거부")
    class Rejections {

        static Stream<Arguments> invalidCreateRequests() {
            return Stream.of(
                    Arguments.of("빈 제목", new PostCreateRequest("", "본문", "글쓴이")),
                    Arguments.of("200자 초과 제목", new PostCreateRequest("가".repeat(201), "본문", "글쓴이")),
                    Arguments.of("빈 본문", new PostCreateRequest("제목", "", "글쓴이")),
                    Arguments.of("10000자 초과 본문", new PostCreateRequest("제목", "가".repeat(10001), "글쓴이")),
                    Arguments.of("빈 작성자", new PostCreateRequest("제목", "본문", "")),
                    Arguments.of("20자 초과 작성자", new PostCreateRequest("제목", "본문", "가".repeat(21))));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidCreateRequests")
        @DisplayName("게시글 작성 요청은 빈 제목·200자 초과 제목·빈 본문·10000자 초과 본문·빈 작성자·20자 초과 작성자를 각각 거부한다")
        void rejectsEachConstraintViolation(String label, PostCreateRequest request) {
            Set<ConstraintViolation<PostCreateRequest>> violations = VALIDATOR.validate(request);

            assertThat(violations).isNotEmpty();
        }
    }
}
