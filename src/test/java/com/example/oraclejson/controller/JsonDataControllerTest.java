package com.example.oraclejson.controller;

import com.example.oraclejson.DatabaseStorageService;
import com.example.oraclejson.dto.ErrorType;
import com.example.oraclejson.dto.JsonData;
import com.example.oraclejson.dto.TradeDetails;
import com.example.oraclejson.dto.TradeExceptionData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@WebMvcTest(JsonDataController.class)
@WithMockUser(username = "test-user")
class JsonDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DatabaseStorageService databaseStorageService;

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
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(1)))
                .andExpect(jsonPath("$.content[0].messageKey", is(key)))
                .andExpect(jsonPath("$.content[0].jsonData", is("{\"test\":\"data\"}")));
    }

    @Test
    void whenGetDataWithInvalidDateRange_thenBadRequest() throws Exception {
        String startDate = "2023-02-01T00:00:00";
        String endDate = "2023-01-01T00:00:00"; // End date is before start date

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
        TradeDetails tradeDetails = new TradeDetails();
        tradeDetails.setClientReferenceNumber("CLIENT-001");
        List<TradeDetails> tradeDetailsList = Collections.singletonList(tradeDetails);

        given(databaseStorageService.getTradeDetailsByClientReferenceForUser(anyString(), anyString())).willReturn(tradeDetailsList);

        // When & Then
        mockMvc.perform(get("/api/trades/CLIENT-001")
                        .header("X-Correlation-ID", CORRELATION_ID)
                        .header("X-Source-Application-ID", SOURCE_APP_ID)
                        .header("Authorization", AUTH_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].client_reference_number", is("CLIENT-001")));
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
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].clientReferenceNumber", is("CLIENT-001")));
    }
}
