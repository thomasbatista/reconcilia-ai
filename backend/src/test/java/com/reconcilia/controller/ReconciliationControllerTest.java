package com.reconcilia.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ReconciliationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/reconciliation/status should return UP")
    void testGetStatus() throws Exception {
        mockMvc.perform(get("/api/reconciliation/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.appName").value("Reconcilia AI"));
    }

    @Test
    @DisplayName("GET /api/reconciliation/demo should return realistic Brazilian synthetic reconciliation data")
    void testGetDemoData() throws Exception {
        mockMvc.perform(get("/api/reconciliation/demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").exists())
                .andExpect(jsonPath("$.summary.totalSourceACount").value(9))
                .andExpect(jsonPath("$.summary.totalSourceBCount").value(9))
                .andExpect(jsonPath("$.items", hasSize(greaterThanOrEqualTo(9))))
                .andExpect(jsonPath("$.items[?(@.status == 'MATCHED')]").isNotEmpty())
                .andExpect(jsonPath("$.items[?(@.status == 'AMOUNT_MISMATCH')]").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/reconciliation/preview should parse headers and preview rows")
    void testPreviewCsv() throws Exception {
        String csvContent = "transacao;data;valor;tipo\nTX-1;15/10/2026;150,00;PIX\n";
        MockMultipartFile file = new MockMultipartFile("file", "vendas.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/reconciliation/preview").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value("vendas.csv"))
                .andExpect(jsonPath("$.headers", hasItem("transacao")))
                .andExpect(jsonPath("$.previewRows", hasSize(1)));
    }

    @Test
    @DisplayName("POST /api/reconciliation/preview with empty file should return 400 Bad Request")
    void testPreviewEmptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0]);

        mockMvc.perform(multipart("/api/reconciliation/preview").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
