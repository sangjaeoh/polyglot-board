package com.board.api.web.v1.post;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.board.api.support.ContainerConfig;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

/** 게시글 CRUD와 에러 경로를 실 PostgreSQL에 대해 검증한다. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ContainerConfig.class)
class BoardControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
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
                .andExpect(jsonPath("$.totalElements").value(1));

        // 1-based 명시 요청: page=1이 첫 페이지다(0-based 시절의 page=0과 동일 내용).
        mockMvc.perform(get("/api/v1/posts").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id))
                .andExpect(jsonPath("$.page").value(1));

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

    @Test
    void invalidUuidPathReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/posts/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").exists());
    }

    @Test
    void malformedJsonBodyReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").exists());
    }

    @Test
    void outOfRangePageParamsReturnBadRequest() throws Exception {
        // 1-based 전환: page=0은 범위 밖이다. @ParameterObject 바인딩 검증 실패는 VALIDATION_FAILED로 떨어진다.
        mockMvc.perform(get("/api/v1/posts").param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mockMvc.perform(get("/api/v1/posts").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mockMvc.perform(get("/api/v1/posts").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mockMvc.perform(get("/api/v1/posts").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void boundaryPageParamsReturnOk() throws Exception {
        mockMvc.perform(get("/api/v1/posts").param("page", "1").param("size", "1"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/posts").param("size", "100")).andExpect(status().isOk());
    }

    @Test
    void secondPageServesNextSlice() throws Exception {
        // 1-based 오프셋 의미론 핀: 최신순 정렬에서 size=1이면 page=1=최신 글, page=2=그 이전 글.
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

    @Test
    void pageOmittedDefaultsToFirstPage() throws Exception {
        // 파라미터 생략 → record compact constructor 기본값(page=1·size=20). 계약 defaultValue와의 일치 검증.
        mockMvc.perform(get("/api/v1/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.pageSize").value(20));
    }

    @Test
    void validationFailureReturnsProblemDetail() throws Exception {
        String badBody = """
                {"title":"","content":"","author":""}""";
        mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors").isArray());
    }
}
