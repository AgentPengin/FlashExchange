package com.flashexchange.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashexchange.dto.CreateOrderRequest;
import com.flashexchange.model.OrderSide;
import com.flashexchange.model.OrderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Bài kiểm tra tích hợp cho Tầng Web (REST Controller).
 * MockMvc giúp em giả lập việc gửi HTTP Request như người dùng thật mà không cần bật server.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("API Test 1: Đặt lệnh Mua qua POST /api/v1/orders")
    void testPlaceOrderApi() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
            "user-web-1",
            "FLASH-USDT",
            OrderSide.BUY,
            OrderType.LIMIT,
            100,
            20
        );

        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.orderId").isNotEmpty())
                .andExpect(jsonPath("$.symbol").value("FLASH-USDT"))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.remainingQuantity").value(20));
    }

    @Test
    @DisplayName("API Test 2: Xem độ sâu sổ lệnh qua GET /api/v1/orders/depth")
    void testGetDepthApi() throws Exception {
        mockMvc.perform(get("/api/v1/orders/depth"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("FLASH-USDT"))
                .andExpect(jsonPath("$.activeOrderCount").isNumber());
    }
}
