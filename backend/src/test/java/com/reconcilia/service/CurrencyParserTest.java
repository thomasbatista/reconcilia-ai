package com.reconcilia.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CurrencyParserTest {

    private CurrencyParser currencyParser;

    @BeforeEach
    void setUp() {
        currencyParser = new CurrencyParser();
    }

    @ParameterizedTest(name = "parse \"{0}\" should equal {1}")
    @CsvSource({
            "'1.250,50', 1250.50",
            "'R$ 1.250,50', 1250.50",
            "'R$ 150,00', 150.00",
            "'150,00', 150.00",
            "'150.00', 150.00",
            "'1,250.50', 1250.50",
            "'100', 100.00",
            "'0,05', 0.05",
            "'-50,00', -50.00",
            "'- R$ 100,00', -100.00",
            "'(75,25)', -75.25"
    })
    @DisplayName("Should correctly parse various Brazilian and international monetary formats")
    void testValidCurrencyParsing(String input, String expected) {
        BigDecimal result = currencyParser.parse(input);
        assertEquals(new BigDecimal(expected), result);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException on null or blank input")
    void testBlankInput() {
        assertThrows(IllegalArgumentException.class, () -> currencyParser.parse(null));
        assertThrows(IllegalArgumentException.class, () -> currencyParser.parse("   "));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException on non-numeric input")
    void testInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> currencyParser.parse("abc"));
        assertThrows(IllegalArgumentException.class, () -> currencyParser.parse("R$ INVALID"));
    }
}
