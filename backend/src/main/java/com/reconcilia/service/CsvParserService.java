package com.reconcilia.service;

import com.reconcilia.dto.CsvPreviewDto;
import com.reconcilia.exception.CsvProcessingException;
import com.reconcilia.model.ColumnMapping;
import com.reconcilia.model.NormalizedTransaction;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

@Service
public class CsvParserService {

    private final CurrencyParser currencyParser;
    private final DateParser dateParser;

    public CsvParserService(CurrencyParser currencyParser, DateParser dateParser) {
        this.currencyParser = currencyParser;
        this.dateParser = dateParser;
    }

    /**
     * Inspects the beginning of the CSV to preview headers, suggest mappings, and return preview rows.
     */
    public CsvPreviewDto generatePreview(byte[] fileBytes, String filename, int maxPreviewRows) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new CsvProcessingException("O arquivo '" + filename + "' está vazio.");
        }

        String content = stripBom(new String(fileBytes, StandardCharsets.UTF_8));
        char delimiter = detectDelimiter(content);

        try (BufferedReader reader = new BufferedReader(new java.io.StringReader(content));
             CSVParser parser = CSVFormat.Builder.create()
                     .setDelimiter(delimiter)
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {

            List<String> headers = parser.getHeaderNames();
            if (headers == null || headers.isEmpty()) {
                throw new CsvProcessingException("Não foi possível identificar o cabeçalho no arquivo '" + filename + "'.");
            }

            ColumnMapping suggestedMapping = suggestColumnMapping(headers);
            List<Map<String, String>> previewRows = new ArrayList<>();
            long count = 0;

            for (CSVRecord record : parser) {
                if (count < maxPreviewRows) {
                    Map<String, String> rowMap = new LinkedHashMap<>();
                    for (String header : headers) {
                        rowMap.put(header, record.isSet(header) ? record.get(header) : "");
                    }
                    previewRows.add(rowMap);
                }
                count++;
            }

            return new CsvPreviewDto(filename, count, headers, suggestedMapping, previewRows);

        } catch (CsvProcessingException e) {
            throw e;
        } catch (Exception e) {
            throw new CsvProcessingException("Falha ao processar a prévia do arquivo '" + filename + "': " + e.getMessage(), e);
        }
    }

    /**
     * Parses the CSV file into a list of normalized transactions according to the provided column mapping.
     */
    public List<NormalizedTransaction> parseTransactions(byte[] fileBytes, String filename, ColumnMapping mapping, String sourceName) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new CsvProcessingException("O arquivo '" + filename + "' está vazio.");
        }

        if (mapping == null || mapping.getIdColumn() == null || mapping.getDateColumn() == null || mapping.getAmountColumn() == null) {
            throw new CsvProcessingException("Mapeamento de colunas incompleto. ID, Data e Valor são obrigatórios.");
        }

        String content = stripBom(new String(fileBytes, StandardCharsets.UTF_8));
        char delimiter = detectDelimiter(content);

        List<NormalizedTransaction> transactions = new ArrayList<>();
        List<String> parsingErrors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new java.io.StringReader(content));
             CSVParser parser = CSVFormat.Builder.create()
                     .setDelimiter(delimiter)
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {

            List<String> headers = parser.getHeaderNames();
            validateMappedColumnsExist(headers, mapping, filename);

            int rowNumber = 1; // 1-based data row counter
            for (CSVRecord record : parser) {
                rowNumber++;
                try {
                    String rawId = getRecordValue(record, mapping.getIdColumn());
                    String rawDate = getRecordValue(record, mapping.getDateColumn());
                    String rawAmount = getRecordValue(record, mapping.getAmountColumn());
                    String rawDescription = mapping.getDescriptionColumn() != null && !mapping.getDescriptionColumn().isEmpty()
                            ? getRecordValue(record, mapping.getDescriptionColumn())
                            : "";

                    if (rawId.isEmpty() && rawAmount.isEmpty() && rawDate.isEmpty()) {
                        // Skip completely empty record
                        continue;
                    }

                    if (rawDate.isEmpty()) {
                        throw new IllegalArgumentException("Data não informada");
                    }
                    if (rawAmount.isEmpty()) {
                        throw new IllegalArgumentException("Valor monetário não informado");
                    }

                    LocalDate date = dateParser.parse(rawDate);
                    BigDecimal amount = currencyParser.parse(rawAmount);

                    transactions.add(new NormalizedTransaction(rawId, date, amount, rawDescription, sourceName, rowNumber));

                } catch (Exception e) {
                    parsingErrors.add("Linha " + rowNumber + ": " + e.getMessage());
                    if (parsingErrors.size() >= 10) {
                        break;
                    }
                }
            }

            if (!parsingErrors.isEmpty()) {
                String errorSummary = String.join("; ", parsingErrors);
                throw new CsvProcessingException("Erros na validação do arquivo '" + filename + "': " + errorSummary);
            }

            if (transactions.isEmpty()) {
                throw new CsvProcessingException("Nenhuma transação válida encontrada no arquivo '" + filename + "'.");
            }

            return transactions;

        } catch (CsvProcessingException e) {
            throw e;
        } catch (Exception e) {
            throw new CsvProcessingException("Erro ao processar transações de '" + filename + "': " + e.getMessage(), e);
        }
    }

    public ColumnMapping suggestColumnMapping(List<String> headers) {
        String idCol = findMatchingHeader(headers, List.of("id", "transacao", "cod", "codigo", "cod_transacao", "nsu", "tid", "referencia", "identificador", "pedido", "order_id"));
        String dateCol = findMatchingHeader(headers, List.of("data", "date", "data_venda", "data_transacao", "dt_venda", "dt_lancamento", "created_at"));
        String amountCol = findMatchingHeader(headers, List.of("valor", "amount", "total", "valor_bruto", "valor_liquido", "vlr", "preco", "price"));
        String descCol = findMatchingHeader(headers, List.of("descricao", "description", "tipo", "metodo", "forma_pagamento", "produto", "historico", "detalhe", "memo", "cliente"));

        // Fallbacks if not detected by keywords
        if (idCol == null && !headers.isEmpty()) idCol = headers.get(0);
        if (dateCol == null && headers.size() > 1) dateCol = headers.get(1);
        if (amountCol == null && headers.size() > 2) amountCol = headers.get(2);
        if (descCol == null && headers.size() > 3) descCol = headers.get(3);

        return new ColumnMapping(idCol, dateCol, amountCol, descCol);
    }

    private String findMatchingHeader(List<String> headers, List<String> keywords) {
        // Exact match first
        for (String header : headers) {
            String normalized = normalizeHeader(header);
            for (String keyword : keywords) {
                if (normalized.equalsIgnoreCase(keyword)) {
                    return header;
                }
            }
        }
        // Substring match second
        for (String header : headers) {
            String normalized = normalizeHeader(header);
            for (String keyword : keywords) {
                if (normalized.contains(keyword)) {
                    return header;
                }
            }
        }
        return null;
    }

    private String normalizeHeader(String header) {
        if (header == null) return "";
        return header.toLowerCase().replaceAll("[^a-z0-9_]", "");
    }

    private void validateMappedColumnsExist(List<String> headers, ColumnMapping mapping, String filename) {
        if (!headers.contains(mapping.getIdColumn())) {
            throw new CsvProcessingException("Coluna de ID '" + mapping.getIdColumn() + "' não existe no arquivo '" + filename + "'.");
        }
        if (!headers.contains(mapping.getDateColumn())) {
            throw new CsvProcessingException("Coluna de Data '" + mapping.getDateColumn() + "' não existe no arquivo '" + filename + "'.");
        }
        if (!headers.contains(mapping.getAmountColumn())) {
            throw new CsvProcessingException("Coluna de Valor '" + mapping.getAmountColumn() + "' não existe no arquivo '" + filename + "'.");
        }
        if (mapping.getDescriptionColumn() != null && !mapping.getDescriptionColumn().isEmpty() && !headers.contains(mapping.getDescriptionColumn())) {
            throw new CsvProcessingException("Coluna de Descrição '" + mapping.getDescriptionColumn() + "' não existe no arquivo '" + filename + "'.");
        }
    }

    private String getRecordValue(CSVRecord record, String column) {
        if (column == null || column.isEmpty() || !record.isSet(column)) {
            return "";
        }
        return record.get(column).trim();
    }

    public char detectDelimiter(String sample) {
        if (sample == null || sample.isEmpty()) {
            return ',';
        }
        int commaCount = 0;
        int semicolonCount = 0;
        int tabCount = 0;

        String[] lines = sample.split("\r?\n", 5);
        for (String line : lines) {
            for (char c : line.toCharArray()) {
                if (c == ',') commaCount++;
                else if (c == ';') semicolonCount++;
                else if (c == '\t') tabCount++;
            }
        }

        if (semicolonCount > commaCount && semicolonCount > tabCount) {
            return ';';
        }
        if (tabCount > commaCount && tabCount > semicolonCount) {
            return '\t';
        }
        return ',';
    }

    private String stripBom(String content) {
        if (content != null && content.startsWith("\uFEFF")) {
            return content.substring(1);
        }
        return content;
    }
}
