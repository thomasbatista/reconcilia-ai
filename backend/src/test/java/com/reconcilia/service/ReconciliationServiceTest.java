package com.reconcilia.service;

import com.reconcilia.model.NormalizedTransaction;
import com.reconcilia.model.ReconciliationItem;
import com.reconcilia.model.ReconciliationStatus;
import com.reconcilia.model.ReconciliationSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReconciliationServiceTest {

    private ReconciliationService reconciliationService;

    @BeforeEach
    void setUp() {
        reconciliationService = new ReconciliationService();
    }

    @Test
    @DisplayName("Should reconcile transactions with exact ID, date, and amount as MATCHED")
    void testExactMatchById() {
        NormalizedTransaction erp = new NormalizedTransaction("TX-001", LocalDate.of(2026, 10, 15), new BigDecimal("150.00"), "Venda Cartao", "ERP", 2);
        NormalizedTransaction acq = new NormalizedTransaction("TX-001", LocalDate.of(2026, 10, 15), new BigDecimal("150.00"), "Credito Visa", "ACQUIRER", 2);

        List<ReconciliationItem> items = reconciliationService.reconcile(List.of(erp), List.of(acq), 0, BigDecimal.ZERO);

        assertEquals(1, items.size());
        ReconciliationItem item = items.get(0);
        assertEquals(ReconciliationStatus.MATCHED, item.getStatus());
        assertEquals(new BigDecimal("0.00"), item.getAmountDifference());
        assertEquals(0L, item.getDateDifferenceDays());
    }

    @Test
    @DisplayName("Should identify AMOUNT_MISMATCH when IDs match but amounts differ")
    void testAmountMismatch() {
        NormalizedTransaction erp = new NormalizedTransaction("TX-002", LocalDate.of(2026, 10, 15), new BigDecimal("200.00"), "Venda", "ERP", 2);
        NormalizedTransaction acq = new NormalizedTransaction("TX-002", LocalDate.of(2026, 10, 15), new BigDecimal("193.00"), "Venda Liquida", "ACQUIRER", 2);

        List<ReconciliationItem> items = reconciliationService.reconcile(List.of(erp), List.of(acq), 0, BigDecimal.ZERO);

        assertEquals(1, items.size());
        ReconciliationItem item = items.get(0);
        assertEquals(ReconciliationStatus.AMOUNT_MISMATCH, item.getStatus());
        // Difference is Acquirer (193.00) - ERP (200.00) = -7.00
        assertEquals(new BigDecimal("-7.00"), item.getAmountDifference());
    }

    @Test
    @DisplayName("Should identify DATE_MISMATCH when date exceeds tolerance")
    void testDateMismatch() {
        NormalizedTransaction erp = new NormalizedTransaction("TX-003", LocalDate.of(2026, 10, 10), new BigDecimal("350.00"), "Venda", "ERP", 2);
        NormalizedTransaction acq = new NormalizedTransaction("TX-003", LocalDate.of(2026, 10, 15), new BigDecimal("350.00"), "Venda", "ACQUIRER", 2);

        // Date tolerance set to 1 day, difference is 5 days
        List<ReconciliationItem> items = reconciliationService.reconcile(List.of(erp), List.of(acq), 1, BigDecimal.ZERO);

        assertEquals(1, items.size());
        ReconciliationItem item = items.get(0);
        assertEquals(ReconciliationStatus.DATE_MISMATCH, item.getStatus());
        assertEquals(5L, item.getDateDifferenceDays());
    }

    @Test
    @DisplayName("Should match within date tolerance as MATCHED")
    void testMatchWithinDateTolerance() {
        NormalizedTransaction erp = new NormalizedTransaction("TX-004", LocalDate.of(2026, 10, 14), new BigDecimal("500.00"), "Venda", "ERP", 2);
        NormalizedTransaction acq = new NormalizedTransaction("TX-004", LocalDate.of(2026, 10, 15), new BigDecimal("500.00"), "Venda", "ACQUIRER", 2);

        // Date tolerance set to 1 day, difference is 1 day
        List<ReconciliationItem> items = reconciliationService.reconcile(List.of(erp), List.of(acq), 1, BigDecimal.ZERO);

        assertEquals(1, items.size());
        assertEquals(ReconciliationStatus.MATCHED, items.get(0).getStatus());
    }

    @Test
    @DisplayName("Strict 1-to-1 constraint: Should never match a transaction multiple times")
    void testStrictOneToOneMatching() {
        // ERP has two distinct transactions of R$ 100,00 on the same day
        NormalizedTransaction erp1 = new NormalizedTransaction("", LocalDate.of(2026, 10, 15), new BigDecimal("100.00"), "Venda PIX 1", "ERP", 2);
        NormalizedTransaction erp2 = new NormalizedTransaction("", LocalDate.of(2026, 10, 15), new BigDecimal("100.00"), "Venda PIX 2", "ERP", 3);

        // Acquirer has only ONE transaction of R$ 100,00
        NormalizedTransaction acq1 = new NormalizedTransaction("", LocalDate.of(2026, 10, 15), new BigDecimal("100.00"), "PIX Recebido", "ACQUIRER", 2);

        List<ReconciliationItem> items = reconciliationService.reconcile(List.of(erp1, erp2), List.of(acq1), 0, BigDecimal.ZERO);

        assertEquals(2, items.size());

        long matchedCount = items.stream().filter(i -> i.getStatus() == ReconciliationStatus.MATCHED).count();
        long unmatchedCount = items.stream().filter(i -> i.getStatus() == ReconciliationStatus.UNMATCHED).count();

        assertEquals(1, matchedCount, "Exactly one transaction must be matched");
        assertEquals(1, unmatchedCount, "The second transaction must remain unmatched");
    }

    @Test
    @DisplayName("Should detect UNMATCHED transactions present only in one source")
    void testUnmatchedTransactions() {
        NormalizedTransaction erpOnly = new NormalizedTransaction("ERP-ONLY", LocalDate.of(2026, 10, 15), new BigDecimal("80.00"), "Cancelado", "ERP", 2);
        NormalizedTransaction acqOnly = new NormalizedTransaction("ACQ-ONLY", LocalDate.of(2026, 10, 16), new BigDecimal("95.00"), "Taxa Avulsa", "ACQUIRER", 2);

        List<ReconciliationItem> items = reconciliationService.reconcile(List.of(erpOnly), List.of(acqOnly), 0, BigDecimal.ZERO);

        assertEquals(2, items.size());
        assertTrue(items.stream().allMatch(i -> i.getStatus() == ReconciliationStatus.UNMATCHED));

        boolean foundErpUnmatched = items.stream().anyMatch(i -> i.getSourceA() != null && i.getSourceB() == null);
        boolean foundAcqUnmatched = items.stream().anyMatch(i -> i.getSourceA() == null && i.getSourceB() != null);

        assertTrue(foundErpUnmatched);
        assertTrue(foundAcqUnmatched);
    }

    @Test
    @DisplayName("Should calculate summary metrics accurately with BigDecimal precision")
    void testSummaryCalculation() {
        NormalizedTransaction erp1 = new NormalizedTransaction("1", LocalDate.of(2026, 10, 15), new BigDecimal("100.00"), "V1", "ERP", 2);
        NormalizedTransaction erp2 = new NormalizedTransaction("2", LocalDate.of(2026, 10, 15), new BigDecimal("200.00"), "V2", "ERP", 3);

        NormalizedTransaction acq1 = new NormalizedTransaction("1", LocalDate.of(2026, 10, 15), new BigDecimal("100.00"), "V1", "ACQUIRER", 2);
        NormalizedTransaction acq2 = new NormalizedTransaction("2", LocalDate.of(2026, 10, 15), new BigDecimal("195.00"), "V2", "ACQUIRER", 3);

        List<NormalizedTransaction> sourceA = List.of(erp1, erp2);
        List<NormalizedTransaction> sourceB = List.of(acq1, acq2);

        List<ReconciliationItem> items = reconciliationService.reconcile(sourceA, sourceB, 0, BigDecimal.ZERO);
        ReconciliationSummary summary = reconciliationService.calculateSummary(sourceA, sourceB, items);

        assertEquals(2, summary.getTotalSourceACount());
        assertEquals(new BigDecimal("300.00"), summary.getTotalSourceAAmount());
        assertEquals(2, summary.getTotalSourceBCount());
        assertEquals(new BigDecimal("295.00"), summary.getTotalSourceBAmount());

        assertEquals(1, summary.getMatchedCount());
        assertEquals(1, summary.getAmountMismatchCount());
        assertEquals(0, summary.getDateMismatchCount());

        // Net discrepancy: Acquirer (295.00) - ERP (300.00) = -5.00
        assertEquals(new BigDecimal("-5.00"), summary.getNetDiscrepancyAmount());
        assertEquals(50.0, summary.getMatchRatePercentage());
    }
}
