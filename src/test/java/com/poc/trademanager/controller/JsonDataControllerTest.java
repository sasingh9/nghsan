package com.poc.trademanager.controller;

import com.poc.trademanager.DatabaseStorageService;
import com.poc.trademanager.dto.ApiResponse;
import com.poc.trademanager.dto.ErrorType;
import com.poc.trademanager.dto.JsonData;
import com.poc.trademanager.dto.TradeDetailsDto;
import com.poc.trademanager.dto.TradeExceptionData;
import com.poc.trademanager.entity.JsonDoc;
import com.poc.trademanager.service.MessageProcessingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JsonDataController.class)
@WithMockUser(username = "test-user")
class JsonDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DatabaseStorageService databaseStorageService;

    @MockBean
    private MessageProcessingService messageProcessingService;

    private final String CORRELATION_ID = "test-correlation-id";
    private final String SOURCE_APP_ID = "test-app";
    private final String AUTH_TOKEN = "Bearer test-token";


    @Test
    void whenGetDataWithDateRange_thenReturnPageOfJsonData() throws Exception {
        // Given
        LocalDateTime now = LocalDateTime.now();
        String key = UUID.randomUUID().toString();
        JsonData jsonData = new JsonData(1L, key, "{\"test\":\"data\"}", now);
        List<JsonData> allData = Collections.singletonList(jsonData);
        Page<JsonData> pageData = new PageImpl<>(allData);

        given(databaseStorageService.getDataByDateRangeForUser(any(), any(), anyString(), any(Pageable.class))).willReturn(pageData);

        String startDate = "2023-01-01T00:00:00";
        String endDate = "2023-01-31T23:59:59";

        // When & Then
        mockMvc.perform(get("/api/data")
                        .header("X-Correlation-ID", CORRELATION_ID)
                        .header("X-Source-Application-ID", SOURCE_APP_ID)
                        .header("Authorization", AUTH_TOKEN)
                        .param("startDate", startDate)
                        .param("endDate", endDate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].id", is(1)))
                .andExpect(jsonPath("$.data.content[0].messageKey", is(key)))
                .andExpect(jsonPath("$.data.content[0].jsonData", is("{\"test\":\"data\"}")));
    }

    @Test
    void whenGetDataWithInvalidDateRange_thenBadRequest() throws Exception {
        String startDate = "2023-02-01T00:00:00";
        String endDate = "2023-01-01T23:59:59"; // End date is before start date

        mockMvc.perform(get("/api/data")
                        .header("X-Correlation-ID", CORRELATION_ID)
                        .header("X-Source-Application-ID", SOURCE_APP_ID)
                        .header("Authorization", AUTH_TOKEN)
                        .param("startDate", startDate)
                        .param("endDate", endDate))
                .andExpect(status().isBadRequest());
    }

    @Test
    void whenGetDataWithOver31Days_thenBadRequest() throws Exception {
        String startDate = "2023-01-01T00:00:00";
        String endDate = "2023-02-02T00:00:00"; // 32 days

        mockMvc.perform(get("/api/data")
                        .header("X-Correlation-ID", CORRELATION_ID)
                        .header("X-Source-Application-ID", SOURCE_APP_ID)
                        .header("Authorization", AUTH_TOKEN)
                        .param("startDate", startDate)
                        .param("endDate", endDate))
                .andExpect(status().isBadRequest());
    }


    @Test
    void whenGetTradesByClientReference_thenReturnJsonArray() throws Exception {
        // Given
        TradeDetailsDto tradeDetailsDto = new TradeDetailsDto();
        tradeDetailsDto.setClientReferenceNumber("CLIENT-001");
        List<TradeDetailsDto> tradeDetailsList = Collections.singletonList(tradeDetailsDto);

        given(databaseStorageService.getTradeDetailsByClientReferenceForUser(anyString(), anyString())).willReturn(tradeDetailsList);

        // When & Then
        mockMvc.perform(get("/api/trades/CLIENT-001")
                        .header("X-Correlation-ID", CORRELATION_ID)
                        .header("X-Source-Application-ID", SOURCE_APP_ID)
                        .header("Authorization", AUTH_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].clientReferenceNumber", is("CLIENT-001")));
    }

    @Test
    void whenGetExceptionsByClientReference_thenReturnJsonArray() throws Exception {
        // Given
        LocalDateTime now = LocalDateTime.now();
        TradeExceptionData exceptionData = new TradeExceptionData(1L, "CLIENT-001", "{\"bad\":\"data\"}", "Invalid trade date", ErrorType.BUSINESS, now);
        List<TradeExceptionData> allExceptions = Collections.singletonList(exceptionData);

        given(databaseStorageService.getTradeExceptionsByClientReferenceForUser(anyString(), anyString())).willReturn(allExceptions);

        // When & Then
        mockMvc.perform(get("/api/exceptions/CLIENT-001")
                        .header("X-Correlation-ID", CORRELATION_ID)
                        .header("X-Source-Application-ID", SOURCE_APP_ID)
                        .header("Authorization", AUTH_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id", is(1)))
                .andExpect(jsonPath("$.data[0].clientReferenceNumber", is("CLIENT-001")));
    }

    @Test
    @WithMockUser(username = "support-user", roles = {"SUPPORT"})
    void whenProcessMessageManuallyWithValidRole_thenOk() throws Exception {
        // Given
        String jsonMessage = "{\"valid\":\"json\"}";
        given(databaseStorageService.saveRawMessage(anyString())).willReturn(new JsonDoc());

        // When & Then
        mockMvc.perform(post("/api/data")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMessage)
                        .header("X-Correlation-ID", CORRELATION_ID)
                        .header("X-Source-Application-ID", SOURCE_APP_ID)
                        .header("Authorization", AUTH_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "hacker", roles = {"USER"})
    void whenProcessMessageManuallyWithInvalidRole_thenForbidden() throws Exception {
        String jsonMessage = "{\"valid\":\"json\"}";
        mockMvc.perform(post("/api/data")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMessage)
                        .header("X-Correlation-ID", CORRELATION_ID)
                        .header("X-Source-Application-ID", SOURCE_APP_ID)
                        .header("Authorization", AUTH_TOKEN))
                .andExpect(status().isForbidden());
    }
}
