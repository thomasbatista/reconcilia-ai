package com.reconcilia.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class ReconciliationItem {

    private final String id;
    private final ReconciliationStatus status;
    private final NormalizedTransaction sourceA;
    private final NormalizedTransaction sourceB;
    private final BigDecimal amountDifference;
    private final Long dateDifferenceDays;
    private final String matchReason;
    private String aiHypothesis;
    private boolean isHypothesisAi;

    public ReconciliationItem(ReconciliationStatus status,
                              NormalizedTransaction sourceA,
                              NormalizedTransaction sourceB,
                              String matchReason) {
        this.id = UUID.randomUUID().toString();
        this.status = status;
        this.sourceA = sourceA;
        this.sourceB = sourceB;
        this.matchReason = matchReason;

        if (sourceA != null && sourceB != null) {
            // difference: sourceB - sourceA (e.g. acquirer minus erp)
            this.amountDifference = sourceB.getAmount().subtract(sourceA.getAmount()).setScale(2, RoundingMode.HALF_EVEN);
            this.dateDifferenceDays = ChronoUnit.DAYS.between(sourceA.getDate(), sourceB.getDate());
        } else if (sourceA != null) {
            this.amountDifference = sourceA.getAmount().negate().setScale(2, RoundingMode.HALF_EVEN);
            this.dateDifferenceDays = null;
        } else if (sourceB != null) {
            this.amountDifference = sourceB.getAmount().setScale(2, RoundingMode.HALF_EVEN);
            this.dateDifferenceDays = null;
        } else {
            this.amountDifference = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
            this.dateDifferenceDays = null;
        }
    }

    public String getId() {
        return id;
    }

    public ReconciliationStatus getStatus() {
        return status;
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
