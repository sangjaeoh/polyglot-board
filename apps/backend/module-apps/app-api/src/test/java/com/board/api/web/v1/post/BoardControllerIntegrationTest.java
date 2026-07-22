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
                .andExpect(jsonPath("$.totalElements").value(1));

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
        mockMvc.perform(get("/api/v1/posts").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").exists());

        mockMvc.perform(get("/api/v1/posts").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").exists());

        mockMvc.perform(get("/api/v1/posts").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").exists());
    }

    @Test
    void boundaryPageParamsReturnOk() throws Exception {
        mockMvc.perform(get("/api/v1/posts").param("page", "0").param("size", "1"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/posts").param("size", "100")).andExpect(status().isOk());
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
