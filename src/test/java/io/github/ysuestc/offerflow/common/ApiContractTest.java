package io.github.ysuestc.offerflow.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ysuestc.offerflow.common.api.ApiErrorCode;
import io.github.ysuestc.offerflow.common.api.ApiResponse;
import io.github.ysuestc.offerflow.common.exception.BusinessException;
import io.github.ysuestc.offerflow.health.controller.HealthController;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HealthController.class,
        properties = "logging.level.io.github.ysuestc.offerflow.common.exception.GlobalExceptionHandler=OFF")
@Import(ApiContractTest.ContractProbeController.class)
class ApiContractTest {

    private static final String PRIVATE_MARKER = "PRIVATE_INPUT_OR_FAILURE_TOKEN";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void validBodyProducesCommonSuccessResponse() throws Exception {
        mvc.perform(post("/contract-probe").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProbeRequest("test"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.data.name").value("test"))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", PRIVATE_MARKER})
    void invalidBodyReturnsFieldConstraintsWithoutRejectedValues(String name) throws Exception {
        var result = mvc.perform(post("/contract-probe").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProbeRequest(name))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").isString())
                .andReturn();
        assertThat(result.getResponse().getContentAsString()).doesNotContain(PRIVATE_MARKER, "rejectedValue");
    }

    @Test
    void malformedJsonReturnsGenericBadRequest() throws Exception {
        var result = mvc.perform(post("/contract-probe").contentType(MediaType.APPLICATION_JSON)
                        .content("{INVALID_" + PRIVATE_MARKER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andReturn();
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain(PRIVATE_MARKER, "JsonParseException", "stackTrace");
    }

    @Test
    void unsupportedInputMediaTypeReturns415() throws Exception {
        mvc.perform(post("/contract-probe").contentType(MediaType.TEXT_PLAIN)
                        .accept(MediaType.APPLICATION_JSON).content("test"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void methodConstraintIsClientValidationError() throws Exception {
        mvc.perform(get("/contract-probe/quantity").param("quantity", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void wrongParameterTypeIsBadRequest() throws Exception {
        var result = mvc.perform(get("/contract-probe/quantity").param("quantity", PRIVATE_MARKER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andReturn();
        assertThat(result.getResponse().getContentAsString()).doesNotContain(PRIVATE_MARKER);
    }

    @Test
    void missingParameterIsBadRequest() throws Exception {
        mvc.perform(get("/contract-probe/quantity"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void modelBindingErrorDoesNotExposeRejectedValues() throws Exception {
        var result = mvc.perform(get("/contract-probe/query").param("quantity", PRIVATE_MARKER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("quantity"))
                .andReturn();
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain(PRIVATE_MARKER, "NumberFormatException");
    }

    @Test
    void returnConstraintFailureIsGenericServerError() throws Exception {
        var result = mvc.perform(get("/contract-probe/invalid-return"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andReturn();
        assertThat(result.getResponse().getContentAsString()).doesNotContain("must not be null", "不能为空");
    }

    @Test
    void businessConflictPreservesStatusAndControlledMessage() throws Exception {
        mvc.perform(get("/contract-probe/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("重复操作"));
    }

    @Test
    void unexpectedExceptionDoesNotExposePrivateDetails() throws Exception {
        var result = mvc.perform(get("/contract-probe/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andReturn();
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain(PRIVATE_MARKER, "IllegalStateException", "stackTrace");
    }

    @Test
    void declaredInternalFailureAlsoHidesCustomDetails() throws Exception {
        var result = mvc.perform(get("/contract-probe/internal"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andReturn();
        assertThat(result.getResponse().getContentAsString()).doesNotContain(PRIVATE_MARKER);
    }

    record ProbeRequest(
            @NotBlank(message = "名称不能为空") @Size(max = 4, message = "名称不能超过4个字符") String name) {
    }

    record ProbeQuery(@Min(1) int quantity) {
    }

    // Test-only endpoints exercise shared HTTP behavior; they are absent from the runnable Jar.
    @RestController
    static class ContractProbeController {

        @PostMapping(value = "/contract-probe", consumes = MediaType.APPLICATION_JSON_VALUE)
        ApiResponse<ProbeRequest> create(@Valid @RequestBody ProbeRequest request) {
            return ApiResponse.success(request);
        }

        @GetMapping("/contract-probe/quantity")
        ApiResponse<Integer> quantity(@RequestParam @Min(1) int quantity) {
            return ApiResponse.success(quantity);
        }

        @GetMapping("/contract-probe/query")
        ApiResponse<ProbeQuery> query(@Valid @ModelAttribute ProbeQuery query) {
            return ApiResponse.success(query);
        }

        @NotNull
        @GetMapping("/contract-probe/invalid-return")
        ApiResponse<Void> invalidReturn() {
            return null;
        }

        @GetMapping("/contract-probe/conflict")
        ApiResponse<Void> conflict() {
            throw new BusinessException(ApiErrorCode.CONFLICT, "重复操作");
        }

        @GetMapping("/contract-probe/unexpected")
        ApiResponse<Void> unexpected() {
            throw new IllegalStateException(PRIVATE_MARKER);
        }

        @GetMapping("/contract-probe/internal")
        ApiResponse<Void> internal() {
            throw new BusinessException(ApiErrorCode.INTERNAL_ERROR, PRIVATE_MARKER);
        }
    }
}
