package com.jobcrm.reminders.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Unit-level check of the error envelope using a throwaway controller (no Spring Boot context). */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    public record Payload(@NotBlank String name) {}

    @RestController
    public static class TestController {

        @PostMapping(path = "/test/validate", consumes = MediaType.APPLICATION_JSON_VALUE)
        public String validate(@Valid @RequestBody Payload payload) {
            return "ok";
        }

        @GetMapping("/test/boom")
        public String boom() {
            throw new IllegalStateException("secret internal detail");
        }
    }

    @Test
    void validationFailureReturns400WithFieldDetails() throws Exception {
        MvcResult result = mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(result.getResponse().getContentAsString())
                .contains("\"code\":\"VALIDATION_ERROR\"")
                .contains("\"fields\"")
                .contains("\"name\"");
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        MvcResult result = mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":"))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(result.getResponse().getContentAsString()).contains("\"code\":\"MALFORMED_REQUEST\"");
    }

    @Test
    void wrongMethodReturns405WithAllowHeader() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/validate")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(405);
        assertThat(result.getResponse().getContentAsString()).contains("\"code\":\"METHOD_NOT_ALLOWED\"");
        assertThat(result.getResponse().getHeader("Allow")).contains("POST");
    }

    @Test
    void unsupportedContentTypeReturns415() throws Exception {
        MvcResult result = mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("hello"))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(415);
        assertThat(result.getResponse().getContentAsString()).contains("\"code\":\"UNSUPPORTED_MEDIA_TYPE\"");
    }

    @Test
    void unexpectedExceptionReturnsGeneric500WithoutLeakingDetails() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/boom")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(500);
        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("\"code\":\"INTERNAL_ERROR\"");
        assertThat(body).doesNotContain("secret internal detail");
    }
}
