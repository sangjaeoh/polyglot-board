package com.board.app.api.web.v1.post;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.board.app.api.support.ContainerConfig;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

/**
 * 앱 경계 대표 시나리오를 실 PostgreSQL에 대해 검증한다. 경계 케이스(에러 매핑·검증·페이징 경계)는
 * 단위·슬라이스가 소유한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ContainerConfig.class)
class BoardControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("대표 경로")
    class RepresentativePath {

        @Test
        @DisplayName("게시글 작성→상세→목록→수정→삭제→404의 대표 경로가 동작한다")
        void crudFlow() throws Exception {
            String createBody = """
                    {"title":"첫 글","content":"본문 내용","author":"글쓴이"}""";
            String created = mockMvc.perform(post("/api/v1/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value("첫 글"))
                    .andExpect(jsonPath("$.author").value("글쓴이"))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            String id = (String) objectMapper.readValue(created, Map.class).get("id");

            mockMvc.perform(get("/api/v1/posts/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").value("본문 내용"));

            mockMvc.perform(get("/api/v1/posts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(id))
                    .andExpect(jsonPath("$.page").value(1))
                    // int64는 계약상 string 표현이다(JSON number 정밀도 제한 대응 — docs/sharing.md).
                    .andExpect(jsonPath("$.totalElements").value("1"));

            String updateBody = """
                    {"title":"수정한 제목","content":"수정한 본문"}""";
            mockMvc.perform(put("/api/v1/posts/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("수정한 제목"))
                    .andExpect(jsonPath("$.content").value("수정한 본문"));

            mockMvc.perform(delete("/api/v1/posts/{id}", id)).andExpect(status().isNoContent());

            mockMvc.perform(get("/api/v1/posts/{id}", id))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("페이지네이션 의미론")
    class PaginationSemantics {

        @Test
        @DisplayName("size=1로 두 페이지를 차례로 조회하면 최신순 오프셋이 유지된다")
        void secondPageServesNextSlice() throws Exception {
            String first = """
                    {"title":"페이지 글 1","content":"본문","author":"작성자"}""";
            String second = """
                    {"title":"페이지 글 2","content":"본문","author":"작성자"}""";
            String firstId = createAndGetId(first);
            String secondId = createAndGetId(second);

            mockMvc.perform(get("/api/v1/posts").param("page", "1").param("size", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page").value(1))
                    .andExpect(jsonPath("$.content[0].title").value("페이지 글 2"));
            mockMvc.perform(get("/api/v1/posts").param("page", "2").param("size", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page").value(2))
                    .andExpect(jsonPath("$.content[0].title").value("페이지 글 1"));

            // 실행 순서 독립성: 이 테스트가 만든 글을 정리해 다른 테스트의 목록 단언을 오염시키지 않는다.
            mockMvc.perform(delete("/api/v1/posts/{id}", firstId)).andExpect(status().isNoContent());
            mockMvc.perform(delete("/api/v1/posts/{id}", secondId)).andExpect(status().isNoContent());
        }
    }

    private String createAndGetId(String body) throws Exception {
        String response = mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return (String) objectMapper.readValue(response, Map.class).get("id");
    }
}
