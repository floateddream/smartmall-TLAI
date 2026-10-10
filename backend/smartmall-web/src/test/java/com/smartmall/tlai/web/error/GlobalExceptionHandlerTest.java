package com.smartmall.tlai.web.error;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldNotWriteJsonAfterStreamingResponseWasCommitted() throws IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.flushBuffer();

        var result = handler.handleUnexpectedException(
                new IOException("client disconnected"), response);

        assertThat(result).isNull();
        assertThat(response.isCommitted()).isTrue();
        assertThat(response.getContentAsByteArray()).isEmpty();
    }

    @Test
    void shouldKeepNormalInternalErrorResponseBeforeResponseIsCommitted() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        var result = handler.handleUnexpectedException(
                new IllegalStateException("unexpected failure"), response);

        assertThat(result).isNotNull();
        assertThat(result.getStatusCode().value()).isEqualTo(500);
        assertThat(result.getBody()).isNotNull();
    }
}
