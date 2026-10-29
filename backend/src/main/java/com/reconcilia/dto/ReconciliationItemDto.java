package com.reconcilia.dto;

import com.reconcilia.model.NormalizedTransaction;
import com.reconcilia.model.ReconciliationItem;
import com.reconcilia.model.ReconciliationStatus;

import java.math.BigDecimal;

public class ReconciliationItemDto {

    private String id;
    private ReconciliationStatus status;
    private String statusLabel;
    private NormalizedTransaction sourceA;
    private NormalizedTransaction sourceB;
    private BigDecimal amountDifference;
    private Long dateDifferenceDays;
    private String matchReason;
    private String aiHypothesis;
    private boolean isHypothesisAi;

    public ReconciliationItemDto() {
    }

    public static ReconciliationItemDto fromDomain(ReconciliationItem item) {
        ReconciliationItemDto dto = new ReconciliationItemDto();
        dto.id = item.getId();
        dto.status = item.getStatus();
        dto.statusLabel = item.getStatus().getLabel();
        dto.sourceA = item.getSourceA();
        dto.sourceB = item.getSourceB();
        dto.amountDifference = item.getAmountDifference();
        dto.dateDifferenceDays = item.getDateDifferenceDays();
        dto.matchReason = item.getMatchReason();
        dto.aiHypothesis = item.getAiHypothesis();
        dto.isHypothesisAi = item.isHypothesisAi();
        return dto;
    }

    public String getId() {
        return id;
    }

    public ReconciliationStatus getStatus() {
        return status;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public NormalizedTransaction getSourceA() {
        return sourceA;
    }

    public NormalizedTransaction getSourceB() {
        return sourceB;
    }

    public BigDecimal getAmountDifference() {
        return amountDifference;
    }

    public Long getDateDifferenceDays() {
        return dateDifferenceDays;
    }

    public String getMatchReason() {
        return matchReason;
    }

    public String getAiHypothesis() {
        return aiHypothesis;
    }

    public void setAiHypothesis(String aiHypothesis) {
        this.aiHypothesis = aiHypothesis;
    }

    public boolean isHypothesisAi() {
        return isHypothesisAi;
    }

    public void setHypothesisAi(boolean hypothesisAi) {
        isHypothesisAi = hypothesisAi;
    }
}
