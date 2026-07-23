package com.board.common.web.correlation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    @DisplayName("X-Request-Id 요청 헤더가 있으면 그 값을 응답 헤더에 그대로 반환한다")
    void acceptsIncomingRequestId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "given-id");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {};

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader("X-Request-Id")).isEqualTo("given-id");
    }

    @Test
    @DisplayName("X-Request-Id 요청 헤더가 없으면 생성해 응답 헤더에 싣는다")
    void generatesRequestIdWhenMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {};

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader("X-Request-Id")).isNotBlank();
    }

    @Test
    @DisplayName("체인 실행 중에는 MDC 키 requestId를 싣고, 완료 후에는 제거한다")
    void loadsAndClearsMdcAroundChain() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "mdc-check-id");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String[] mdcDuringChain = new String[1];
        FilterChain chain = (req, res) -> mdcDuringChain[0] = MDC.get("requestId");

        filter.doFilter(request, response, chain);

        assertThat(mdcDuringChain[0]).isEqualTo("mdc-check-id");
        assertThat(MDC.get("requestId")).isNull();
    }
}
