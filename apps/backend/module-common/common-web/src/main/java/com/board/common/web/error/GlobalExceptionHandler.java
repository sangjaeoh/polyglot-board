package com.board.common.web.error;

import com.board.common.core.error.BaseException;
import com.board.common.core.error.ErrorCode;
import com.board.common.web.correlation.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 경계 예외를 RFC 9457 ProblemDetail로 매핑한다.
 *
 * <p>{@link ResponseEntityExceptionHandler}를 상속해 표준 Spring MVC 클라이언트 오류(잘못된 JSON·타입
 * 불일치·미지원 메서드 등)를 올바른 4xx로 매핑하고, 모든 응답에 기계 분기용 {@code code} 확장 멤버와 관측용
 * {@code traceId}·{@code instance}를 붙인다. 기계 분기는 {@code code}로만 한다. {@code detail}·{@code title}은
 * 사람용 문자열이다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 도메인 예외를 그 {@link ErrorCode}의 상태·코드로 변환한다. */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ProblemDetail> handleDomain(BaseException ex, HttpServletRequest request) {
        ErrorCode errorCode = ex.getErrorCode();
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(errorCode.status()), ex.getMessage());
        problem.setTitle(errorCode.message());
        problem.setProperty("code", errorCode.code());
        applyObservability(problem, request.getRequestURI());
        return ResponseEntity.status(errorCode.status()).body(problem);
    }

    /** Bean Validation 실패를 400 + 필드별 {@code errors[]}로 변환한다. */
    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "요청 값이 유효하지 않다.");
        problem.setTitle("요청 값이 유효하지 않다.");
        problem.setProperty("code", "VALIDATION_FAILED");
        List<FieldErrorDetail> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new FieldErrorDetail(
                        fieldError.getField(), Objects.requireNonNullElse(fieldError.getDefaultMessage(), "유효하지 않은 값")))
                .toList();
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    /** MVC가 매핑한 모든 ProblemDetail에 {@code code}(상태명 기반)와 관측 멤버를 보장한다. */
    @Override
    protected @Nullable ResponseEntity<Object> handleExceptionInternal(
            Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        Object resolvedBody = body != null ? body : ProblemDetail.forStatus(statusCode);
        if (resolvedBody instanceof ProblemDetail problem) {
            Map<String, Object> properties = problem.getProperties();
            if (properties == null || !properties.containsKey("code")) {
                problem.setProperty(
                        "code", HttpStatus.valueOf(statusCode.value()).name());
            }
            String path = request instanceof ServletWebRequest servletRequest
                    ? servletRequest.getRequest().getRequestURI()
                    : null;
            applyObservability(problem, path);
        }
        return super.handleExceptionInternal(ex, resolvedBody, headers, statusCode, request);
    }

    /** 예상치 못한 예외를 500으로 감싸 계약(ProblemDetail)을 유지한다. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "내부 오류가 발생했다.");
        problem.setTitle("내부 오류가 발생했다.");
        problem.setProperty("code", "INTERNAL_ERROR");
        String traceId = applyObservability(problem, request.getRequestURI());
        log.error("처리되지 않은 예외 traceId={}", traceId, ex);
        return ResponseEntity.internalServerError().body(problem);
    }

    /**
     * 관측용 확장 멤버를 싣는다 — {@code instance}는 요청 경로, {@code traceId}는 상관 ID(MDC 키
     * {@code requestId}, {@link RequestIdFilter} 소유. 없으면 생성 UUID). 반환한 traceId를 서버 로그에
     * 남기면 사용자 에러 리포트와 상관시킬 수 있다.
     */
    private static String applyObservability(ProblemDetail problem, @Nullable String path) {
        if (path != null) {
            problem.setInstance(URI.create(path));
        }
        String traceId = MDC.get("requestId");
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
        }
        problem.setProperty("traceId", traceId);
        return traceId;
    }
}
