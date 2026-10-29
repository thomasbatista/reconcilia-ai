package com.reconcilia.service;

import com.reconcilia.dto.CsvPreviewDto;
import com.reconcilia.exception.CsvProcessingException;
import com.reconcilia.model.ColumnMapping;
import com.reconcilia.model.NormalizedTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvParserServiceTest {

    private CsvParserService csvParserService;

    @BeforeEach
    void setUp() {
        CurrencyParser currencyParser = new CurrencyParser();
        DateParser dateParser = new DateParser();
        csvParserService = new CsvParserService(currencyParser, dateParser);
    }

    @Test
    @DisplayName("Should detect delimiter correctly (comma, semicolon, tab)")
    void testDetectDelimiter() {
        assertEquals(',', csvParserService.detectDelimiter("id,data,valor\n1,15/10/2026,100.00"));
        assertEquals(';', csvParserService.detectDelimiter("id;data;valor;descricao\n1;15/10/2026;100,00;Venda"));
        assertEquals('\t', csvParserService.detectDelimiter("id\tdata\tvalor\n1\t15/10/2026\t100.00"));
    }

    @Test
    @DisplayName("Should suggest column mapping based on standard headers")
    void testSuggestColumnMapping() {
        List<String> headers = List.of("cod_transacao", "data_venda", "valor_bruto", "forma_pagamento");
        ColumnMapping mapping = csvParserService.suggestColumnMapping(headers);

        assertEquals("cod_transacao", mapping.getIdColumn());
        assertEquals("data_venda", mapping.getDateColumn());
        assertEquals("valor_bruto", mapping.getAmountColumn());
        assertEquals("forma_pagamento", mapping.getDescriptionColumn());
    }

    @Test
    @DisplayName("Should generate preview of CSV with UTF-8 and semicolon delimiter")
    void testGeneratePreview() {
        String csvContent = "id;data;valor;descricao\n" +
                "TX-101;15/10/2026;R$ 150,00;Venda Loja\n" +
                "TX-102;16/10/2026;R$ 200,00;Venda Online\n";

        byte[] bytes = csvContent.getBytes(StandardCharsets.UTF_8);
        CsvPreviewDto preview = csvParserService.generatePreview(bytes, "vendas.csv", 5);

        assertNotNull(preview);
        assertEquals("vendas.csv", preview.getFilename());
        assertEquals(2, preview.getTotalRows());
        assertEquals(4, preview.getHeaders().size());
        assertEquals(2, preview.getPreviewRows().size());
        assertEquals("TX-101", preview.getPreviewRows().get(0).get("id"));
    }

    @Test
    @DisplayName("Should parse CSV transactions successfully into NormalizedTransaction records")
    void testParseTransactions() {
        String csvContent = "transacao;data;valor;tipo\n" +
                "T1;15/10/2026;1.250,50;PIX\n" +
                "T2;16/10/2026;50,00;Debito\n";

        byte[] bytes = csvContent.getBytes(StandardCharsets.UTF_8);
        ColumnMapping mapping = new ColumnMapping("transacao", "data", "valor", "tipo");

        List<NormalizedTransaction> transactions = csvParserService.parseTransactions(bytes, "erp.csv", mapping, "ERP");

        assertEquals(2, transactions.size());
        NormalizedTransaction t1 = transactions.get(0);
        assertEquals("T1", t1.getId());
        assertEquals(LocalDate.of(2026, 10, 15), t1.getDate());
        assertEquals(new BigDecimal("1250.50"), t1.getAmount());
        assertEquals("PIX", t1.getDescription());
        assertEquals("ERP", t1.getSource());
    }

    @Test
    @DisplayName("Should throw CsvProcessingException if file is empty")
    void testEmptyFile() {
        assertThrows(CsvProcessingException.class, () ->
                csvParserService.generatePreview(new byte[0], "empty.csv", 5));
    }

    @Test
    @DisplayName("Should throw CsvProcessingException when mapped column does not exist")
    void testMissingColumn() {
        String csvContent = "id,data,valor\n1,15/10/2026,100.00";
        byte[] bytes = csvContent.getBytes(StandardCharsets.UTF_8);
        ColumnMapping mapping = new ColumnMapping("id", "data_inexistente", "valor", null);

        assertThrows(CsvProcessingException.class, () ->
                csvParserService.parseTransactions(bytes, "test.csv", mapping, "ERP"));
    }
}
