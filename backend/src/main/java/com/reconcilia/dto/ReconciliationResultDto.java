package com.reconcilia.dto;

import com.reconcilia.model.ReconciliationSummary;

import java.util.List;

public class ReconciliationResultDto {

    private ReconciliationSummary summary;
    private List<ReconciliationItemDto> items;
    private int dateToleranceDays;
    private java.math.BigDecimal amountTolerance;

    public ReconciliationResultDto() {
    }

    public ReconciliationResultDto(ReconciliationSummary summary, List<ReconciliationItemDto> items, int dateToleranceDays, java.math.BigDecimal amountTolerance) {
        this.summary = summary;
        this.items = items;
        this.dateToleranceDays = dateToleranceDays;
        this.amountTolerance = amountTolerance;
    }

    public ReconciliationSummary getSummary() {
        return summary;
    }

    public void setSummary(ReconciliationSummary summary) {
        this.summary = summary;
    }

    public List<ReconciliationItemDto> getItems() {
        return items;
    }

    public void setItems(List<ReconciliationItemDto> items) {
        this.items = items;
    }

    public int getDateToleranceDays() {
        return dateToleranceDays;
    }

    public void setDateToleranceDays(int dateToleranceDays) {
        this.dateToleranceDays = dateToleranceDays;
    }

    public java.math.BigDecimal getAmountTolerance() {
        return amountTolerance;
    }

    public void setAmountTolerance(java.math.BigDecimal amountTolerance) {
        this.amountTolerance = amountTolerance;
    }
}
