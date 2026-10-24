package com.linebook.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    private HttpServletRequest forwarded(MockHttpServletRequest request, MockHttpServletResponse response) throws Exception {
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        return (HttpServletRequest) chain.getRequest();
    }

    @Test
    void generatesAnIdWhenTheCallerSendsNone() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        HttpServletRequest forwarded = forwarded(new MockHttpServletRequest("GET", "/api/bills"), response);

        String id = response.getHeader(RequestIdFilter.HEADER);
        assertThat(id).isNotBlank();
        assertThat(forwarded.getHeader(RequestIdFilter.HEADER)).isEqualTo(id);
        assertThat(Collections.list(forwarded.getHeaderNames())).contains(RequestIdFilter.HEADER);
    }

    @Test
    void keepsASafeIdFromTheCaller() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/bills");
        request.addHeader(RequestIdFilter.HEADER, "web-7f3a.9c21");
        MockHttpServletResponse response = new MockHttpServletResponse();

        HttpServletRequest forwarded = forwarded(request, response);

        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo("web-7f3a.9c21");
        assertThat(forwarded.getHeader(RequestIdFilter.HEADER)).isEqualTo("web-7f3a.9c21");
    }

    @Test
    void replacesAnUnsafeIdSoItCannotForgeLogLines() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/bills");
        request.addHeader(RequestIdFilter.HEADER, "x\nFAKE LOG LINE");
        MockHttpServletResponse response = new MockHttpServletResponse();

        forwarded(request, response);

        assertThat(response.getHeader(RequestIdFilter.HEADER)).doesNotContain("FAKE").matches("[A-Za-z0-9._-]+");
    }
}
