package com.reconcilia.service;

import com.reconcilia.model.NormalizedTransaction;
import com.reconcilia.model.ReconciliationItem;
import com.reconcilia.model.ReconciliationStatus;
import com.reconcilia.model.ReconciliationSummary;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class ReconciliationService {

    /**
     * Executes deterministic transaction reconciliation between source A (e.g. ERP)
     * and source B (e.g. Acquirer / Bank).
     *
     * Guarantee: Every transaction in Source A is paired with at most one transaction
     * in Source B, and vice-versa.
     */
    public List<ReconciliationItem> reconcile(List<NormalizedTransaction> sourceA,
                                              List<NormalizedTransaction> sourceB,
                                              int dateToleranceDays,
                                              BigDecimal amountTolerance) {

        if (sourceA == null) sourceA = Collections.emptyList();
        if (sourceB == null) sourceB = Collections.emptyList();
        if (dateToleranceDays < 0) dateToleranceDays = 0;
        if (amountTolerance == null || amountTolerance.compareTo(BigDecimal.ZERO) < 0) {
            amountTolerance = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        }

        boolean[] matchedA = new boolean[sourceA.size()];
        boolean[] matchedB = new boolean[sourceB.size()];
        List<ReconciliationItem> results = new ArrayList<>();

        // Pass 1: Match by explicit Unique Identifier / Reference when provided and non-empty
        for (int i = 0; i < sourceA.size(); i++) {
            NormalizedTransaction a = sourceA.get(i);
            if (a.getId() == null || a.getId().trim().isEmpty()) {
                continue;
            }

            for (int j = 0; j < sourceB.size(); j++) {
                if (matchedB[j]) continue;
                NormalizedTransaction b = sourceB.get(j);
                if (b.getId() == null || b.getId().trim().isEmpty()) {
                    continue;
                }

                if (a.getId().equalsIgnoreCase(b.getId())) {
                    matchedA[i] = true;
                    matchedB[j] = true;

                    long daysDiff = Math.abs(ChronoUnit.DAYS.between(a.getDate(), b.getDate()));
                    BigDecimal amtDiff = a.getAmount().subtract(b.getAmount()).abs();

                    boolean dateMatches = daysDiff <= dateToleranceDays;
                    boolean amountMatches = amtDiff.compareTo(amountTolerance) <= 0;

                    if (dateMatches && amountMatches) {
                        results.add(new ReconciliationItem(ReconciliationStatus.MATCHED, a, b,
                                "Conciliado por identificador exato (" + a.getId() + ")"));
                    } else if (dateMatches) {
                        results.add(new ReconciliationItem(ReconciliationStatus.AMOUNT_MISMATCH, a, b,
                                "Mesmo identificador (" + a.getId() + "), porém com divergência no valor"));
                    } else if (amountMatches) {
                        results.add(new ReconciliationItem(ReconciliationStatus.DATE_MISMATCH, a, b,
                                "Mesmo identificador (" + a.getId() + "), porém com diferença de " + daysDiff + " dia(s)"));
                    } else {
                        results.add(new ReconciliationItem(ReconciliationStatus.AMOUNT_MISMATCH, a, b,
                                "Mesmo identificador (" + a.getId() + "), divergência de valor e data (" + daysDiff + " dias)"));
                    }
                    break;
                }
            }
        }

        // Pass 2: Match remaining transactions by Exact Amount + Exact Date
        for (int i = 0; i < sourceA.size(); i++) {
            if (matchedA[i]) continue;
            NormalizedTransaction a = sourceA.get(i);

            int bestCandidateIndex = -1;
            for (int j = 0; j < sourceB.size(); j++) {
                if (matchedB[j]) continue;
                NormalizedTransaction b = sourceB.get(j);

                if (a.getAmount().compareTo(b.getAmount()) == 0 && a.getDate().isEqual(b.getDate())) {
                    // Check if descriptions match for better pairing
                    if (isDescriptionSimilar(a.getDescription(), b.getDescription())) {
                        bestCandidateIndex = j;
                        break;
                    } else if (bestCandidateIndex == -1) {
                        bestCandidateIndex = j;
                    }
                }
            }

            if (bestCandidateIndex != -1) {
                matchedA[i] = true;
                matchedB[bestCandidateIndex] = true;
                results.add(new ReconciliationItem(ReconciliationStatus.MATCHED, a, sourceB.get(bestCandidateIndex),
                        "Valor exato e data idêntica"));
            }
        }

        // Pass 3: Match remaining transactions by Exact Amount + Date within Tolerance
        if (dateToleranceDays > 0) {
            for (int i = 0; i < sourceA.size(); i++) {
                if (matchedA[i]) continue;
                NormalizedTransaction a = sourceA.get(i);

                int bestCandidateIndex = -1;
                long minDaysDiff = Long.MAX_VALUE;

                for (int j = 0; j < sourceB.size(); j++) {
                    if (matchedB[j]) continue;
                    NormalizedTransaction b = sourceB.get(j);

                    if (a.getAmount().compareTo(b.getAmount()) == 0) {
                        long days = Math.abs(ChronoUnit.DAYS.between(a.getDate(), b.getDate()));
                        if (days <= dateToleranceDays && days < minDaysDiff) {
                            minDaysDiff = days;
                            bestCandidateIndex = j;
                        }
                    }
                }

                if (bestCandidateIndex != -1) {
                    matchedA[i] = true;
                    matchedB[bestCandidateIndex] = true;
                    results.add(new ReconciliationItem(ReconciliationStatus.MATCHED, a, sourceB.get(bestCandidateIndex),
                            "Valor exato com diferença de " + minDaysDiff + " dia(s) (dentro da tolerância)"));
                }
            }
        }

        // Pass 4: Match remaining transactions by Amount within Tolerance + Date within Tolerance
        if (amountTolerance.compareTo(BigDecimal.ZERO) > 0) {
            for (int i = 0; i < sourceA.size(); i++) {
                if (matchedA[i]) continue;
                NormalizedTransaction a = sourceA.get(i);

                int bestCandidateIndex = -1;
                BigDecimal minDiff = null;

                for (int j = 0; j < sourceB.size(); j++) {
                    if (matchedB[j]) continue;
                    NormalizedTransaction b = sourceB.get(j);

                    long days = Math.abs(ChronoUnit.DAYS.between(a.getDate(), b.getDate()));
                    if (days <= dateToleranceDays) {
                        BigDecimal diff = a.getAmount().subtract(b.getAmount()).abs();
                        if (diff.compareTo(amountTolerance) <= 0) {
                            if (minDiff == null || diff.compareTo(minDiff) < 0) {
                                minDiff = diff;
                                bestCandidateIndex = j;
                            }
                        }
                    }
                }

                if (bestCandidateIndex != -1) {
                    matchedA[i] = true;
                    matchedB[bestCandidateIndex] = true;
                    results.add(new ReconciliationItem(ReconciliationStatus.MATCHED, a, sourceB.get(bestCandidateIndex),
                            "Valor e data dentro da tolerância configurada"));
                }
            }
        }

        // Pass 5: Detect Amount Mismatches on same date with description correlation
        for (int i = 0; i < sourceA.size(); i++) {
            if (matchedA[i]) continue;
            NormalizedTransaction a = sourceA.get(i);

            int candidateIndex = -1;
            for (int j = 0; j < sourceB.size(); j++) {
                if (matchedB[j]) continue;
                NormalizedTransaction b = sourceB.get(j);

                long days = Math.abs(ChronoUnit.DAYS.between(a.getDate(), b.getDate()));
                if (days <= dateToleranceDays && isDescriptionSimilar(a.getDescription(), b.getDescription())) {
                    candidateIndex = j;
                    break;
                }
            }

            if (candidateIndex != -1) {
                matchedA[i] = true;
                matchedB[candidateIndex] = true;
                results.add(new ReconciliationItem(ReconciliationStatus.AMOUNT_MISMATCH, a, sourceB.get(candidateIndex),
                        "Mesma data e descrição compatível, com divergência de valor"));
            }
        }

        // Pass 6: Detect Date Mismatches where amount is exact but date exceeds tolerance (up to 7 days)
        for (int i = 0; i < sourceA.size(); i++) {
            if (matchedA[i]) continue;
            NormalizedTransaction a = sourceA.get(i);

            int candidateIndex = -1;
            long minDays = Long.MAX_VALUE;

            for (int j = 0; j < sourceB.size(); j++) {
                if (matchedB[j]) continue;
                NormalizedTransaction b = sourceB.get(j);

                if (a.getAmount().compareTo(b.getAmount()) == 0) {
                    long days = Math.abs(ChronoUnit.DAYS.between(a.getDate(), b.getDate()));
                    if (days > dateToleranceDays && days <= 7 && days < minDays) {
                        minDays = days;
                        candidateIndex = j;
                    }
                }
            }

            if (candidateIndex != -1) {
                matchedA[i] = true;
                matchedB[candidateIndex] = true;
                results.add(new ReconciliationItem(ReconciliationStatus.DATE_MISMATCH, a, sourceB.get(candidateIndex),
                        "Mesmo valor (" + a.getAmount() + "), porém com defasagem de " + minDays + " dia(s)"));
            }
        }

        // Pass 7: Unmatched items from Source A (Missing in Source B)
        for (int i = 0; i < sourceA.size(); i++) {
            if (!matchedA[i]) {
                results.add(new ReconciliationItem(ReconciliationStatus.UNMATCHED, sourceA.get(i), null,
                        "Transação presente no Arquivo A, mas não localizada no Arquivo B"));
            }
        }

        // Pass 8: Unmatched items from Source B (Missing in Source A)
        for (int j = 0; j < sourceB.size(); j++) {
            if (!matchedB[j]) {
                results.add(new ReconciliationItem(ReconciliationStatus.UNMATCHED, null, sourceB.get(j),
                        "Transação presente no Arquivo B, mas não localizada no Arquivo A"));
            }
        }

        return results;
    }

    /**
     * Calculates summary metrics from the reconciliation items and the raw sources.
     */
    public ReconciliationSummary calculateSummary(List<NormalizedTransaction> sourceA,
                                                 List<NormalizedTransaction> sourceB,
                                                 List<ReconciliationItem> items) {

        ReconciliationSummary summary = new ReconciliationSummary();

        if (sourceA == null) sourceA = Collections.emptyList();
        if (sourceB == null) sourceB = Collections.emptyList();
        if (items == null) items = Collections.emptyList();

        summary.setTotalSourceACount(sourceA.size());
        BigDecimal sumA = sourceA.stream()
                .map(NormalizedTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setTotalSourceAAmount(sumA);

        summary.setTotalSourceBCount(sourceB.size());
        BigDecimal sumB = sourceB.stream()
                .map(NormalizedTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setTotalSourceBAmount(sumB);

        int matchedCount = 0;
        BigDecimal matchedA = BigDecimal.ZERO;
        BigDecimal matchedB = BigDecimal.ZERO;

        int amountMismatchCount = 0;
        int dateMismatchCount = 0;

        int unmatchedSourceACount = 0;
        BigDecimal unmatchedSourceAAmount = BigDecimal.ZERO;

        int unmatchedSourceBCount = 0;
        BigDecimal unmatchedSourceBAmount = BigDecimal.ZERO;

        for (ReconciliationItem item : items) {
            switch (item.getStatus()) {
                case MATCHED -> {
                    matchedCount++;
                    if (item.getSourceA() != null) matchedA = matchedA.add(item.getSourceA().getAmount());
                    if (item.getSourceB() != null) matchedB = matchedB.add(item.getSourceB().getAmount());
                }
                case AMOUNT_MISMATCH -> amountMismatchCount++;
                case DATE_MISMATCH -> dateMismatchCount++;
                case UNMATCHED -> {
                    if (item.getSourceA() != null) {
                        unmatchedSourceACount++;
                        unmatchedSourceAAmount = unmatchedSourceAAmount.add(item.getSourceA().getAmount());
                    } else if (item.getSourceB() != null) {
                        unmatchedSourceBCount++;
                        unmatchedSourceBAmount = unmatchedSourceBAmount.add(item.getSourceB().getAmount());
                    }
                }
            }
        }

        summary.setMatchedCount(matchedCount);
        summary.setMatchedAmountSourceA(matchedA);
        summary.setMatchedAmountSourceB(matchedB);

        summary.setAmountMismatchCount(amountMismatchCount);
        summary.setDateMismatchCount(dateMismatchCount);

        summary.setUnmatchedSourceACount(unmatchedSourceACount);
        summary.setUnmatchedSourceAAmount(unmatchedSourceAAmount);

        summary.setUnmatchedSourceBCount(unmatchedSourceBCount);
        summary.setUnmatchedSourceBAmount(unmatchedSourceBAmount);

        summary.setNetDiscrepancyAmount(sumB.subtract(sumA).setScale(2, RoundingMode.HALF_EVEN));

        double matchRate = sourceA.isEmpty() ? 0.0 : ((double) matchedCount / sourceA.size()) * 100.0;
        summary.setMatchRatePercentage(Math.round(matchRate * 100.0) / 100.0);

        return summary;
    }

    private boolean isDescriptionSimilar(String desc1, String desc2) {
        if (desc1 == null || desc2 == null) return false;
        String s1 = desc1.toLowerCase().trim();
        String s2 = desc2.toLowerCase().trim();
        if (s1.isEmpty() || s2.isEmpty()) return false;
        if (s1.equals(s2)) return true;

        // Check if either contains the other or shares significant tokens (length >= 4)
        String[] tokens1 = s1.split("[\\s\\-_/]+");
        for (String t : tokens1) {
            if (t.length() >= 4 && s2.contains(t)) {
                return true;
            }
        }
        return false;
    }
}
