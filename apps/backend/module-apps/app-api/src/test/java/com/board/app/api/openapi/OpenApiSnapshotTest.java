package com.board.app.api.openapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.board.app.api.support.ContainerConfig;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * OpenAPI 계약을 방출·검증한다.
 *
 * <p>{@code openapi.write=true}면 정렬된 openapi.json을 파일로 쓰고, 아니면 커밋본과 일치하는지 검사한다
 * (drift 게이트).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ContainerConfig.class)
class OpenApiSnapshotTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void openApiContractMatchesSnapshot() throws Exception {
        String raw = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        String normalized = sortKeysAndPrettyPrint(raw);
        Path target = Path.of(System.getProperty("openapi.file"));

        if (Boolean.getBoolean("openapi.write")) {
            Files.createDirectories(target.getParent());
            Files.writeString(target, normalized);
            return;
        }

        assertThat(Files.exists(target))
                .withFailMessage("openapi.json이 없다 — 먼저 generateOpenApiDocs를 실행한다")
                .isTrue();
        assertThat(normalized)
                .withFailMessage("계약과 커밋된 openapi.json이 다르다 — generateOpenApiDocs로 재생성한다")
                .isEqualTo(Files.readString(target));
    }

    private static String sortKeysAndPrettyPrint(String json) throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .build();
        Object tree = mapper.readValue(json, Object.class);
        return mapper.writeValueAsString(tree) + "\n";
    }
}
