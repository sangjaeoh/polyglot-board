package com.board.api.web.v1.post;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.board.api.facade.BoardFacade;
import com.board.board.domain.exception.PostNotFoundException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** {@link BoardController}의 HTTP 계약(에러 매핑·검증 경계)을 웹 슬라이스로 검증한다. 파사드는 목으로 대체한다. */
@WebMvcTest(BoardController.class)
class BoardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BoardFacade boardFacade;

    @Nested
    @DisplayName("에러 매핑")
    class ErrorMapping {

        @Test
        @DisplayName("존재하지 않는 게시글 조회는 404와 POST_NOT_FOUND 코드로 응답한다")
        void absentPostReturnsNotFoundWithCode() throws Exception {
            UUID id = UUID.randomUUID();
            given(boardFacade.getPost(id)).willThrow(new PostNotFoundException());

            mockMvc.perform(get("/api/v1/posts/{id}", id))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
        }

        @Test
        @DisplayName("UUID가 아닌 경로 변수는 400으로 응답한다")
        void invalidUuidPathReturnsBadRequest() throws Exception {
            mockMvc.perform(get("/api/v1/posts/{id}", "not-a-uuid"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").exists());
        }

        @Test
        @DisplayName("잘못된 JSON 본문은 400으로 응답한다")
        void malformedJsonBodyReturnsBadRequest() throws Exception {
            mockMvc.perform(post("/api/v1/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ not json"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").exists());
        }
    }

    @Nested
    @DisplayName("검증 경계")
    class ValidationBoundary {

        @Test
        @DisplayName("검증 위반 요청 본문은 400 VALIDATION_FAILED와 필드 오류 목록으로 응답한다")
        void violatingRequestBodyReturnsValidationFailed() throws Exception {
            String blankBody = """
                    {"title":"","content":"","author":""}""";

            mockMvc.perform(post("/api/v1/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(blankBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors").isArray());
        }

        @Test
        @DisplayName("범위 밖 페이징 파라미터는 400 VALIDATION_FAILED로 응답한다")
        void outOfRangePagingParamReturnsValidationFailed() throws Exception {
            mockMvc.perform(get("/api/v1/posts").param("page", "0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }
    }
}
