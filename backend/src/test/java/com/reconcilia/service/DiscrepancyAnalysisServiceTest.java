package com.reconcilia.service;

import com.reconcilia.model.NormalizedTransaction;
import com.reconcilia.model.ReconciliationItem;
import com.reconcilia.model.ReconciliationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DiscrepancyAnalysisServiceTest {

    private DiscrepancyAnalysisService analysisService;

    @BeforeEach
    void setUp() {
        // Initialized without API key to test deterministic rule-based analysis
        analysisService = new DiscrepancyAnalysisService("", "https://dummy.api", "model");
    }

    @Test
    @DisplayName("Should detect possible MDR card fee deduction in rule-based hypothesis")
    void testMdrFeeHypothesis() {
        NormalizedTransaction erp = new NormalizedTransaction("TX-100", LocalDate.of(2026, 10, 15), new BigDecimal("200.00"), "Venda Cartao", "ERP", 2);
        NormalizedTransaction acq = new NormalizedTransaction("TX-100", LocalDate.of(2026, 10, 15), new BigDecimal("193.00"), "Captura Credito", "ACQUIRER", 2);

        ReconciliationItem item = new ReconciliationItem(ReconciliationStatus.AMOUNT_MISMATCH, erp, acq, "Mesmo ID");
        analysisService.generateHypothesis(item);

        assertNotNull(item.getAiHypothesis());
        assertTrue(item.getAiHypothesis().contains("taxa de administração (MDR)"));
        assertFalse(item.isHypothesisAi());
    }

    @Test
    @DisplayName("Should detect settlement lag (D+1/D+2) for date mismatches")
    void testSettlementLagHypothesis() {
        NormalizedTransaction erp = new NormalizedTransaction("TX-101", LocalDate.of(2026, 10, 14), new BigDecimal("300.00"), "Venda", "ERP", 2);
        NormalizedTransaction acq = new NormalizedTransaction("TX-101", LocalDate.of(2026, 10, 16), new BigDecimal("300.00"), "Venda", "ACQUIRER", 2);

        ReconciliationItem item = new ReconciliationItem(ReconciliationStatus.DATE_MISMATCH, erp, acq, "Mesmo ID");
        analysisService.generateHypothesis(item);

        assertNotNull(item.getAiHypothesis());
        assertTrue(item.getAiHypothesis().contains("liquidação financeira"));
        assertFalse(item.isHypothesisAi());
    }

    @Test
    @DisplayName("Should provide appropriate hypothesis for unmatched transactions")
    void testUnmatchedHypotheses() {
        NormalizedTransaction erp = new NormalizedTransaction("TX-ERP", LocalDate.of(2026, 10, 15), new BigDecimal("50.00"), "Venda Cancelada", "ERP", 2);
        ReconciliationItem itemErp = new ReconciliationItem(ReconciliationStatus.UNMATCHED, erp, null, "Ausente na adquirente");
        analysisService.generateHypothesis(itemErp);
        assertTrue(itemErp.getAiHypothesis().contains("sem captura confirmada"));

        NormalizedTransaction acq = new NormalizedTransaction("TX-ACQ", LocalDate.of(2026, 10, 15), new BigDecimal("75.00"), "POS Avulso", "ACQUIRER", 2);
        ReconciliationItem itemAcq = new ReconciliationItem(ReconciliationStatus.UNMATCHED, null, acq, "Ausente no ERP");
        analysisService.generateHypothesis(itemAcq);
        assertTrue(itemAcq.getAiHypothesis().contains("sem correspondente no ERP"));
    }
}
