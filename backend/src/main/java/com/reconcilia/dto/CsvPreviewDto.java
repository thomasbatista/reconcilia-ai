package com.reconcilia.dto;

import com.reconcilia.model.ColumnMapping;

import java.util.List;
import java.util.Map;

public class CsvPreviewDto {

    private String filename;
    private long totalRows;
    private List<String> headers;
    private ColumnMapping suggestedMapping;
    private List<Map<String, String>> previewRows;

    public CsvPreviewDto() {
    }

    public CsvPreviewDto(String filename, long totalRows, List<String> headers, ColumnMapping suggestedMapping, List<Map<String, String>> previewRows) {
        this.filename = filename;
        this.totalRows = totalRows;
        this.headers = headers;
        this.suggestedMapping = suggestedMapping;
        this.previewRows = previewRows;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public long getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(long totalRows) {
        this.totalRows = totalRows;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }

    public ColumnMapping getSuggestedMapping() {
        return suggestedMapping;
    }

    public void setSuggestedMapping(ColumnMapping suggestedMapping) {
        this.suggestedMapping = suggestedMapping;
    }

    public List<Map<String, String>> getPreviewRows() {
        return previewRows;
    }

    public void setPreviewRows(List<Map<String, String>> previewRows) {
        this.previewRows = previewRows;
    }
}
