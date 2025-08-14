package com.example.oraclejson.controller;

import com.example.oraclejson.DatabaseStorageService;
import com.example.oraclejson.dto.JsonData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@WebMvcTest(JsonDataController.class)
class JsonDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DatabaseStorageService databaseStorageService;

    @Test
    void whenGetDataWithDateRange_thenReturnJsonArray() throws Exception {
        // Given
        LocalDateTime now = LocalDateTime.now();
        JsonData jsonData = new JsonData(1L, "{\"test\":\"data\"}", now);
        List<JsonData> allData = Collections.singletonList(jsonData);

        given(databaseStorageService.getDataByDateRange(any(), any())).willReturn(allData);

        String startDate = "2023-01-01T00:00:00";
        String endDate = "2023-01-31T23:59:59";

        // When & Then
        mockMvc.perform(get("/api/data")
                .param("startDate", startDate)
                .param("endDate", endDate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].jsonData", is("{\"test\":\"data\"}")));
    }

    @Test
    void whenGetDataWithNoParams_thenReturnJsonArray() throws Exception {
        // Given
        LocalDateTime now = LocalDateTime.now();
        JsonData jsonData = new JsonData(1L, "{\"test\":\"data\"}", now);
        List<JsonData> allData = Collections.singletonList(jsonData);

        given(databaseStorageService.getDataByDateRange(null, null)).willReturn(allData);

        // When & Then
        mockMvc.perform(get("/api/data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }
}
