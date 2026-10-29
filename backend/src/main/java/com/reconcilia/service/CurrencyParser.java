package com.reconcilia.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class CurrencyParser {

    /**
     * Parses a string representing a monetary value into a BigDecimal scaled to 2 decimal places.
     * Supports Brazilian (e.g. "R$ 1.250,50", "1.250,50", "1250,50") and standard ("1250.50", "1,250.50") formats.
     */
    public BigDecimal parse(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            throw new IllegalArgumentException("Valor monetário não pode ser vazio.");
        }

        String cleaned = rawValue.trim();

        // Check for negative accounting format like (150.00) or -150.00
        boolean isNegative = false;
        if (cleaned.startsWith("(") && cleaned.endsWith(")")) {
            isNegative = true;
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        } else if (cleaned.startsWith("-")) {
            isNegative = true;
            cleaned = cleaned.substring(1).trim();
        } else if (cleaned.endsWith("-")) {
            isNegative = true;
            cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
        }

        // Remove currency symbols and whitespace (R$, $, BRL, etc.)
        cleaned = cleaned.replaceAll("[R$r$BRLbrl€£\\s]", "").trim();

        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("Valor monetário inválido: '" + rawValue + "'");
        }

        // Determine decimal separator
        int lastComma = cleaned.lastIndexOf(',');
        int lastDot = cleaned.lastIndexOf('.');

        String standardDecimalString;

        if (lastComma > -1 && lastDot > -1) {
            // Contains both dot and comma
            if (lastComma > lastDot) {
                // Brazilian format: 1.250,50 -> remove dots, replace comma with dot
                standardDecimalString = cleaned.replace(".", "").replace(',', '.');
            } else {
                // US format: 1,250.50 -> remove commas
                standardDecimalString = cleaned.replace(",", "");
            }
        } else if (lastComma > -1) {
            // Only comma present: 1250,50 or 1,250
            // In Brazilian context, comma is decimal separator
            standardDecimalString = cleaned.replace(',', '.');
        } else if (lastDot > -1) {
            // Only dot present: could be 1250.50 (decimal) or 1.250 (thousand separator)
            long dotCount = cleaned.chars().filter(ch -> ch == '.').count();
            if (dotCount > 1) {
                // Multiple dots: 1.000.000 -> thousand separators
                standardDecimalString = cleaned.replace(".", "");
            } else {
                // Exactly one dot. Check how many digits follow
                int charsAfterDot = cleaned.length() - lastDot - 1;
                if (charsAfterDot == 3 && cleaned.length() > 4) {
                    // Ambiguous case like 1.500: if it's Brazilian thousand separator without cents
                    // Treat as 1500
                    standardDecimalString = cleaned.replace(".", "");
                } else {
                    // Standard decimal: 1500.50 or 15.5
                    standardDecimalString = cleaned;
                }
            }
        } else {
            // Integer: 1250
            standardDecimalString = cleaned;
        }

        try {
            BigDecimal value = new BigDecimal(standardDecimalString);
            if (isNegative) {
                value = value.negate();
            }
            return value.setScale(2, RoundingMode.HALF_EVEN);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Não foi possível converter '" + rawValue + "' em valor monetário válido: " + e.getMessage(), e);
        }
    }
}
