package com.reconcilia.service;

import com.reconcilia.dto.ReconciliationItemDto;
import com.reconcilia.model.NormalizedTransaction;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class CsvExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Exports reconciliation items to a byte array representing a UTF-8 encoded CSV.
     */
    public byte[] exportToCsv(List<ReconciliationItemDto> items) {
        if (items == null) items = List.of();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                     .setHeader(
                             "Status",
                             "ID ERP",
                             "Data ERP",
                             "Valor ERP",
                             "Descricao ERP",
                             "ID Adquirente",
                             "Data Adquirente",
                             "Valor Adquirente",
                             "Descricao Adquirente",
                             "Diferenca Valor (B - A)",
                             "Diferenca Dias",
                             "Motivo Conciliacao",
                             "Hipotese Discrepancia"
                     )
                     .build())) {

            for (ReconciliationItemDto item : items) {
                NormalizedTransaction a = item.getSourceA();
                NormalizedTransaction b = item.getSourceB();

                String idA = a != null ? a.getId() : "";
                String dateA = a != null && a.getDate() != null ? a.getDate().format(DATE_FORMATTER) : "";
                String amountA = a != null && a.getAmount() != null ? a.getAmount().toString() : "";
                String descA = a != null ? a.getDescription() : "";

                String idB = b != null ? b.getId() : "";
                String dateB = b != null && b.getDate() != null ? b.getDate().format(DATE_FORMATTER) : "";
                String amountB = b != null && b.getAmount() != null ? b.getAmount().toString() : "";
                String descB = b != null ? b.getDescription() : "";

                String diffAmount = item.getAmountDifference() != null ? item.getAmountDifference().toString() : "";
                String diffDays = item.getDateDifferenceDays() != null ? item.getDateDifferenceDays().toString() : "";

                printer.printRecord(
                        item.getStatusLabel(),
                        idA,
                        dateA,
                        amountA,
                        descA,
                        idB,
                        dateB,
                        amountB,
                        descB,
                        diffAmount,
                        diffDays,
                        item.getMatchReason() != null ? item.getMatchReason() : "",
                        item.getAiHypothesis() != null ? item.getAiHypothesis() : ""
                );
            }

            printer.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Falha ao exportar resultados para CSV: " + e.getMessage(), e);
        }
    }
}
