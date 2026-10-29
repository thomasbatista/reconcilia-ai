package com.reconcilia.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ReconciliationSummary {

    private int totalSourceACount;
    private BigDecimal totalSourceAAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
    private int totalSourceBCount;
    private BigDecimal totalSourceBAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);

    private int matchedCount;
    private BigDecimal matchedAmountSourceA = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
    private BigDecimal matchedAmountSourceB = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);

    private int amountMismatchCount;
    private int dateMismatchCount;

    private int unmatchedSourceACount;
    private BigDecimal unmatchedSourceAAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
    private int unmatchedSourceBCount;
    private BigDecimal unmatchedSourceBAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);

    private BigDecimal netDiscrepancyAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
    private double matchRatePercentage;

    public ReconciliationSummary() {
    }

    public int getTotalSourceACount() {
        return totalSourceACount;
    }

    public void setTotalSourceACount(int totalSourceACount) {
        this.totalSourceACount = totalSourceACount;
    }

    public BigDecimal getTotalSourceAAmount() {
        return totalSourceAAmount;
    }

    public void setTotalSourceAAmount(BigDecimal totalSourceAAmount) {
        this.totalSourceAAmount = totalSourceAAmount != null ? totalSourceAAmount.setScale(2, RoundingMode.HALF_EVEN) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
    }

    public int getTotalSourceBCount() {
        return totalSourceBCount;
    }

    public void setTotalSourceBCount(int totalSourceBCount) {
        this.totalSourceBCount = totalSourceBCount;
    }

    public BigDecimal getTotalSourceBAmount() {
        return totalSourceBAmount;
    }

    public void setTotalSourceBAmount(BigDecimal totalSourceBAmount) {
        this.totalSourceBAmount = totalSourceBAmount != null ? totalSourceBAmount.setScale(2, RoundingMode.HALF_EVEN) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
    }

    public int getMatchedCount() {
        return matchedCount;
    }

    public void setMatchedCount(int matchedCount) {
        this.matchedCount = matchedCount;
    }

    public BigDecimal getMatchedAmountSourceA() {
        return matchedAmountSourceA;
    }

    public void setMatchedAmountSourceA(BigDecimal matchedAmountSourceA) {
        this.matchedAmountSourceA = matchedAmountSourceA != null ? matchedAmountSourceA.setScale(2, RoundingMode.HALF_EVEN) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
    }

    public BigDecimal getMatchedAmountSourceB() {
        return matchedAmountSourceB;
    }

    public void setMatchedAmountSourceB(BigDecimal matchedAmountSourceB) {
        this.matchedAmountSourceB = matchedAmountSourceB != null ? matchedAmountSourceB.setScale(2, RoundingMode.HALF_EVEN) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
    }

    public int getAmountMismatchCount() {
        return amountMismatchCount;
    }

    public void setAmountMismatchCount(int amountMismatchCount) {
        this.amountMismatchCount = amountMismatchCount;
    }

    public int getDateMismatchCount() {
        return dateMismatchCount;
    }

    public void setDateMismatchCount(int dateMismatchCount) {
        this.dateMismatchCount = dateMismatchCount;
    }

    public int getUnmatchedSourceACount() {
        return unmatchedSourceACount;
    }

    public void setUnmatchedSourceACount(int unmatchedSourceACount) {
        this.unmatchedSourceACount = unmatchedSourceACount;
    }

    public BigDecimal getUnmatchedSourceAAmount() {
        return unmatchedSourceAAmount;
    }

    public void setUnmatchedSourceAAmount(BigDecimal unmatchedSourceAAmount) {
        this.unmatchedSourceAAmount = unmatchedSourceAAmount != null ? unmatchedSourceAAmount.setScale(2, RoundingMode.HALF_EVEN) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
    }

    public int getUnmatchedSourceBCount() {
        return unmatchedSourceBCount;
    }

    public void setUnmatchedSourceBCount(int unmatchedSourceBCount) {
        this.unmatchedSourceBCount = unmatchedSourceBCount;
    }

    public BigDecimal getUnmatchedSourceBAmount() {
        return unmatchedSourceBAmount;
    }

    public void setUnmatchedSourceBAmount(BigDecimal unmatchedSourceBAmount) {
        this.unmatchedSourceBAmount = unmatchedSourceBAmount != null ? unmatchedSourceBAmount.setScale(2, RoundingMode.HALF_EVEN) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
    }

    public BigDecimal getNetDiscrepancyAmount() {
        return netDiscrepancyAmount;
    }

    public void setNetDiscrepancyAmount(BigDecimal netDiscrepancyAmount) {
        this.netDiscrepancyAmount = netDiscrepancyAmount != null ? netDiscrepancyAmount.setScale(2, RoundingMode.HALF_EVEN) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
    }

    public double getMatchRatePercentage() {
        return matchRatePercentage;
    }

    public void setMatchRatePercentage(double matchRatePercentage) {
        this.matchRatePercentage = matchRatePercentage;
    }
}
