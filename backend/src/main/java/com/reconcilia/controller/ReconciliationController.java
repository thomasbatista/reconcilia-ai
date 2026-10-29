package com.reconcilia.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reconcilia.dto.CsvPreviewDto;
import com.reconcilia.dto.ReconciliationItemDto;
import com.reconcilia.dto.ReconciliationResultDto;
import com.reconcilia.model.ColumnMapping;
import com.reconcilia.model.NormalizedTransaction;
import com.reconcilia.model.ReconciliationItem;
import com.reconcilia.model.ReconciliationSummary;
import com.reconcilia.service.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reconciliation")
public class ReconciliationController {

    private final CsvParserService csvParserService;
    private final ReconciliationService reconciliationService;
    private final DiscrepancyAnalysisService discrepancyAnalysisService;
    private final CsvExportService csvExportService;
    private final DemoDataService demoDataService;
    private final ObjectMapper objectMapper;

    public ReconciliationController(CsvParserService csvParserService,
                                    ReconciliationService reconciliationService,
                                    DiscrepancyAnalysisService discrepancyAnalysisService,
                                    CsvExportService csvExportService,
                                    DemoDataService demoDataService,
                                    ObjectMapper objectMapper) {
        this.csvParserService = csvParserService;
        this.reconciliationService = reconciliationService;
        this.discrepancyAnalysisService = discrepancyAnalysisService;
        this.csvExportService = csvExportService;
        this.demoDataService = demoDataService;
        this.objectMapper = objectMapper;
    }

    /**
     * Inspects uploaded CSV and returns detected headers, suggested mappings, and first 5 rows.
     */
    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CsvPreviewDto> previewCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "maxRows", defaultValue = "5") int maxRows) throws Exception {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("O arquivo selecionado está vazio.");
        }

        CsvPreviewDto preview = csvParserService.generatePreview(
                file.getBytes(),
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "arquivo.csv",
                maxRows
        );

        return ResponseEntity.ok(preview);
    }

    /**
     * Processes two CSV files in memory and executes deterministic reconciliation.
     */
    @PostMapping(value = "/process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReconciliationResultDto> processReconciliation(
            @RequestParam("fileA") MultipartFile fileA,
            @RequestParam("fileB") MultipartFile fileB,
            @RequestParam(value = "mappingAJson", required = false) String mappingAJson,
            @RequestParam(value = "mappingBJson", required = false) String mappingBJson,
            @RequestParam(value = "dateToleranceDays", defaultValue = "1") int dateToleranceDays,
            @RequestParam(value = "amountTolerance", defaultValue = "0.00") BigDecimal amountTolerance) throws Exception {

        if (fileA.isEmpty()) {
            throw new IllegalArgumentException("Arquivo Fonte A (ERP) não pode ser vazio.");
        }
        if (fileB.isEmpty()) {
            throw new IllegalArgumentException("Arquivo Fonte B (Adquirente) não pode ser vazio.");
        }

        byte[] bytesA = fileA.getBytes();
        byte[] bytesB = fileB.getBytes();

        ColumnMapping mappingA;
        if (mappingAJson != null && !mappingAJson.trim().isEmpty()) {
            mappingA = objectMapper.readValue(mappingAJson, ColumnMapping.class);
        } else {
            CsvPreviewDto previewA = csvParserService.generatePreview(bytesA, fileA.getOriginalFilename(), 1);
            mappingA = previewA.getSuggestedMapping();
        }

        ColumnMapping mappingB;
        if (mappingBJson != null && !mappingBJson.trim().isEmpty()) {
            mappingB = objectMapper.readValue(mappingBJson, ColumnMapping.class);
        } else {
            CsvPreviewDto previewB = csvParserService.generatePreview(bytesB, fileB.getOriginalFilename(), 1);
            mappingB = previewB.getSuggestedMapping();
        }

        String filenameA = fileA.getOriginalFilename() != null ? fileA.getOriginalFilename() : "ERP.csv";
        String filenameB = fileB.getOriginalFilename() != null ? fileB.getOriginalFilename() : "Adquirente.csv";

        List<NormalizedTransaction> transactionsA = csvParserService.parseTransactions(bytesA, filenameA, mappingA, "ERP");
        List<NormalizedTransaction> transactionsB = csvParserService.parseTransactions(bytesB, filenameB, mappingB, "Adquirente");

        List<ReconciliationItem> items = reconciliationService.reconcile(transactionsA, transactionsB, dateToleranceDays, amountTolerance);

        // Generate discrepancy hypotheses (AI or rule-based)
        for (ReconciliationItem item : items) {
            discrepancyAnalysisService.generateHypothesis(item);
        }

        ReconciliationSummary summary = reconciliationService.calculateSummary(transactionsA, transactionsB, items);
        List<ReconciliationItemDto> dtos = items.stream().map(ReconciliationItemDto::fromDomain).toList();

        return ResponseEntity.ok(new ReconciliationResultDto(summary, dtos, dateToleranceDays, amountTolerance));
    }

    /**
     * Returns pre-reconciled realistic Brazilian demo data without requiring file uploads or external API keys.
     */
    @GetMapping("/demo")
    public ResponseEntity<ReconciliationResultDto> getDemoData() {
        return ResponseEntity.ok(demoDataService.getDemoReconciliationResult());
    }

    /**
     * Downloadable synthetic ERP CSV file.
     */
    @GetMapping(value = "/demo/erp-sample.csv", produces = "text/csv; charset=UTF-8")
    public ResponseEntity<byte[]> getDemoErpCsv() {
        byte[] csvData = demoDataService.getDemoErpCsv().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"exemplo_vendas_erp.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }

    /**
     * Downloadable synthetic Acquirer CSV file.
     */
    @GetMapping(value = "/demo/acquirer-sample.csv", produces = "text/csv; charset=UTF-8")
    public ResponseEntity<byte[]> getDemoAcquirerCsv() {
        byte[] csvData = demoDataService.getDemoAcquirerCsv().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"exemplo_extrato_adquirente.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }

    /**
     * Exports reconciliation items to CSV.
     */
    @PostMapping(value = "/export", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> exportReconciliationCsv(@RequestBody List<ReconciliationItemDto> items) {
        byte[] csvBytes = csvExportService.exportToCsv(items);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reconciliacao_resultado.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvBytes);
    }

    /**
     * Returns server status, configuration info, and AI availability.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "appName", "Reconcilia AI",
                "version", "1.0.0",
                "aiAvailable", discrepancyAnalysisService.isAiAvailable(),
                "defaultDateToleranceDays", 1,
                "defaultAmountTolerance", "0.00"
        ));
    }
}
