package com.board.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.servers.Server;
import java.util.ArrayList;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI 계약 메타데이터·에러 모델. 계약의 유일 언어중립 원천(openapi.json)의 제목·버전·ProblemDetail을 소유한다. */
@Configuration
public class OpenApiConfig {

    private static final String PROBLEM_REF = "#/components/schemas/ProblemDetail";

    @Bean
    public OpenAPI boardOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Board API").version("v1").description("간단한 게시판 CRUD API"))
                // 계약 표면에 내부 URL을 싣지 않는다 — 상대 경로 서버로 벤더 중립을 유지한다.
                .servers(List.of(new Server().url("/")));
    }

    /**
     * 에러 모델 등록과 nullability 정합을 계약에 반영한다.
     *
     * <p>RFC 9457 ProblemDetail(+{@code code}·{@code errors[]}·{@code traceId} 확장)을 실어 프론트가 타입드
     * 에러로 소비한다.
     * ProblemDetail은 {@link org.springframework.http.ProblemDetail}가 런타임에 동적 프로퍼티로 채우는 형상이라
     * springdoc이 introspect할 수 없어, 계약 형상을 이 한 곳에서 선언한다. DTO 스키마는 JSpecify
     * {@code @NullMarked}로 non-null이나 springdoc이 {@code required}를 방출하지 않으므로, ProblemDetail을 제외한
     * DTO 프로퍼티를 required로 표시해 생성 Zod가 optional로 거짓말하지 않게 한다.
     */
    @Bean
    public OpenApiCustomizer contractCustomizer() {
        return openApi -> {
            Schema<?> fieldError = new ObjectSchema()
                    .addProperty("field", new StringSchema())
                    .addProperty("message", new StringSchema());
            fieldError.setRequired(List.of("field", "message"));
            Schema<?> problemDetail = new ObjectSchema()
                    .description("RFC 9457 ProblemDetail 에러 응답")
                    .addProperty("type", new StringSchema().format("uri"))
                    .addProperty("title", new StringSchema())
                    .addProperty("status", new IntegerSchema())
                    .addProperty("detail", new StringSchema())
                    .addProperty("instance", new StringSchema().format("uri"))
                    .addProperty("code", new StringSchema().description("기계 분기용 안정 코드"))
                    .addProperty("errors", new ArraySchema().items(fieldError))
                    .addProperty("traceId", new StringSchema().description("관측용 트레이스 ID"));
            problemDetail.setRequired(List.of("status", "title", "code"));
            openApi.getComponents().addSchemas("ProblemDetail", problemDetail);

            openApi.getComponents().getSchemas().forEach((name, schema) -> {
                if (!"ProblemDetail".equals(name) && schema.getProperties() != null) {
                    schema.setRequired(new ArrayList<>(schema.getProperties().keySet()));
                }
            });
        };
    }

    /** 각 오퍼레이션에 ProblemDetail 에러 응답을 붙여 계약이 엔드포인트별 에러를 광고하게 한다. */
    @Bean
    public OperationCustomizer errorResponseCustomizer() {
        return (operation, handlerMethod) -> {
            Content content = new Content()
                    .addMediaType("application/problem+json", new MediaType().schema(new Schema<>().$ref(PROBLEM_REF)));
            ApiResponses responses = operation.getResponses();
            responses.addApiResponse(
                    "400", new ApiResponse().description("잘못된 요청").content(content));
            responses.addApiResponse(
                    "500", new ApiResponse().description("서버 오류").content(content));
            boolean hasPathParam = operation.getParameters() != null
                    && operation.getParameters().stream().anyMatch(p -> "path".equals(p.getIn()));
            if (hasPathParam) {
                responses.addApiResponse(
                        "404", new ApiResponse().description("리소스를 찾을 수 없음").content(content));
            }
            return operation;
        };
    }
}
