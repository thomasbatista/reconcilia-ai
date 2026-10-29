package com.reconcilia.service;

import com.reconcilia.dto.ReconciliationItemDto;
import com.reconcilia.dto.ReconciliationResultDto;
import com.reconcilia.model.NormalizedTransaction;
import com.reconcilia.model.ReconciliationItem;
import com.reconcilia.model.ReconciliationSummary;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class DemoDataService {

    private final ReconciliationService reconciliationService;
    private final DiscrepancyAnalysisService discrepancyAnalysisService;

    public DemoDataService(ReconciliationService reconciliationService,
                           DiscrepancyAnalysisService discrepancyAnalysisService) {
        this.reconciliationService = reconciliationService;
        this.discrepancyAnalysisService = discrepancyAnalysisService;
    }

    public List<NormalizedTransaction> getDemoErpTransactions() {
        List<NormalizedTransaction> list = new ArrayList<>();
        list.add(new NormalizedTransaction("TX-1001", LocalDate.of(2026, 10, 15), new BigDecimal("150.00"), "Venda PIX - Cafeteira", "ERP", 2));
        list.add(new NormalizedTransaction("TX-1002", LocalDate.of(2026, 10, 15), new BigDecimal("1890.50"), "Venda Crédito - Smartphone Galaxy", "ERP", 3));
        list.add(new NormalizedTransaction("TX-1003", LocalDate.of(2026, 10, 15), new BigDecimal("2500.00"), "Venda Crédito Visa 3x - Notebook Dell", "ERP", 4));
        list.add(new NormalizedTransaction("TX-1004", LocalDate.of(2026, 10, 10), new BigDecimal("340.00"), "Venda Débito Elo - Monitor 29", "ERP", 5));
        list.add(new NormalizedTransaction("TX-1005", LocalDate.of(2026, 10, 16), new BigDecimal("780.00"), "Venda Cartão Master - Cadeira Gamer", "ERP", 6));
        list.add(new NormalizedTransaction("TX-1006", LocalDate.of(2026, 10, 16), new BigDecimal("450.00"), "Ecommerce - Teclado Mecânico (Pedido #8491)", "ERP", 7));
        list.add(new NormalizedTransaction("TX-1008", LocalDate.of(2026, 10, 14), new BigDecimal("1200.00"), "Boleto Bancário - Fone JBL", "ERP", 8));
        list.add(new NormalizedTransaction("TX-1009", LocalDate.of(2026, 10, 17), new BigDecimal("560.00"), "Venda Crédito Master - Carregador Anker", "ERP", 9));
        list.add(new NormalizedTransaction("TX-1010", LocalDate.of(2026, 10, 17), new BigDecimal("89.90"), "Venda PIX - Cabo HDMI 2.0", "ERP", 10));
        return list;
    }

    public List<NormalizedTransaction> getDemoAcquirerTransactions() {
        List<NormalizedTransaction> list = new ArrayList<>();
        list.add(new NormalizedTransaction("TX-1001", LocalDate.of(2026, 10, 15), new BigDecimal("150.00"), "PIX Recebido - Instantâneo", "ACQUIRER", 2));
        list.add(new NormalizedTransaction("TX-1002", LocalDate.of(2026, 10, 16), new BigDecimal("1890.50"), "Captura Crédito Cielo - D+1", "ACQUIRER", 3));
        // TX-1003 has 3% MDR fee deducted: 2500.00 - 75.00 = 2425.00
        list.add(new NormalizedTransaction("TX-1003", LocalDate.of(2026, 10, 15), new BigDecimal("2425.00"), "Crédito Parcelado Visa (Líquido)", "ACQUIRER", 4));
        // TX-1004 has date 2026-10-15 (5 days settlement lag vs ERP 2026-10-10)
        list.add(new NormalizedTransaction("TX-1004", LocalDate.of(2026, 10, 15), new BigDecimal("340.00"), "Débito Elo Liquidado", "ACQUIRER", 5));
        list.add(new NormalizedTransaction("TX-1005", LocalDate.of(2026, 10, 16), new BigDecimal("780.00"), "Crédito Master Rotativo", "ACQUIRER", 6));
        // TX-1007 is POS direct charge without ERP record
        list.add(new NormalizedTransaction("TX-1007", LocalDate.of(2026, 10, 16), new BigDecimal("125.00"), "Terminal POS Balcão - Venda Direta", "ACQUIRER", 7));
        list.add(new NormalizedTransaction("TX-1008", LocalDate.of(2026, 10, 15), new BigDecimal("1200.00"), "Compensação Boleto Bancário", "ACQUIRER", 8));
        // TX-1009 has 2.5% MDR fee deducted: 560.00 - 14.00 = 546.00
        list.add(new NormalizedTransaction("TX-1009", LocalDate.of(2026, 10, 17), new BigDecimal("546.00"), "Crédito Master Stone (Líquido)", "ACQUIRER", 9));
        list.add(new NormalizedTransaction("TX-1010", LocalDate.of(2026, 10, 17), new BigDecimal("89.90"), "PIX Recebido", "ACQUIRER", 10));
        return list;
    }

    public ReconciliationResultDto getDemoReconciliationResult() {
        List<NormalizedTransaction> sourceA = getDemoErpTransactions();
        List<NormalizedTransaction> sourceB = getDemoAcquirerTransactions();

        int dateTolerance = 1;
        BigDecimal amountTolerance = BigDecimal.ZERO;

        List<ReconciliationItem> items = reconciliationService.reconcile(sourceA, sourceB, dateTolerance, amountTolerance);

        // Generate hypotheses for all items
        for (ReconciliationItem item : items) {
            discrepancyAnalysisService.generateHypothesis(item);
        }

        ReconciliationSummary summary = reconciliationService.calculateSummary(sourceA, sourceB, items);
        List<ReconciliationItemDto> itemDtos = items.stream().map(ReconciliationItemDto::fromDomain).toList();

        return new ReconciliationResultDto(summary, itemDtos, dateTolerance, amountTolerance);
    }

    public String getDemoErpCsv() {
        return "id_venda;data_emissao;valor_bruto;descricao_produto\n" +
                "TX-1001;15/10/2026;R$ 150,00;Venda PIX - Cafeteira\n" +
                "TX-1002;15/10/2026;R$ 1.890,50;Venda Crédito - Smartphone Galaxy\n" +
                "TX-1003;15/10/2026;R$ 2.500,00;Venda Crédito Visa 3x - Notebook Dell\n" +
                "TX-1004;10/10/2026;R$ 340,00;Venda Débito Elo - Monitor 29\n" +
                "TX-1005;16/10/2026;R$ 780,00;Venda Cartão Master - Cadeira Gamer\n" +
                "TX-1006;16/10/2026;R$ 450,00;Ecommerce - Teclado Mecânico (Pedido #8491)\n" +
                "TX-1008;14/10/2026;R$ 1.200,00;Boleto Bancário - Fone JBL\n" +
                "TX-1009;17/10/2026;R$ 560,00;Venda Crédito Master - Carregador Anker\n" +
                "TX-1010;17/10/2026;R$ 89,90;Venda PIX - Cabo HDMI 2.0\n";
    }

    public String getDemoAcquirerCsv() {
        return "nsu_operacao;data_liquidacao;valor_creditado;detalhe_transacao\n" +
                "TX-1001;15/10/2026;150,00;PIX Recebido - Instantâneo\n" +
                "TX-1002;16/10/2026;1890,50;Captura Crédito Cielo - D+1\n" +
                "TX-1003;15/10/2026;2425,00;Crédito Parcelado Visa (Líquido)\n" +
                "TX-1004;15/10/2026;340,00;Débito Elo Liquidado\n" +
                "TX-1005;16/10/2026;780,00;Crédito Master Rotativo\n" +
                "TX-1007;16/10/2026;125,00;Terminal POS Balcão - Venda Direta\n" +
                "TX-1008;15/10/2026;1200,00;Compensação Boleto Bancário\n" +
                "TX-1009;17/10/2026;546,00;Crédito Master Stone (Líquido)\n" +
                "TX-1010;17/10/2026;89,90;PIX Recebido\n";
    }
}
