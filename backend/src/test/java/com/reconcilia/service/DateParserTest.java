package com.reconcilia.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DateParserTest {

    private DateParser dateParser;

    @BeforeEach
    void setUp() {
        dateParser = new DateParser();
    }

    @ParameterizedTest(name = "parse \"{0}\" should equal {1}")
    @CsvSource({
            "'15/10/2026', 2026-10-15",
            "'2026-10-15', 2026-10-15",
            "'15-10-2026', 2026-10-15",
            "'01/01/2026', 2026-01-01",
            "'15/10/2026 14:30:00', 2026-10-15",
            "'2026-10-15T10:15:30', 2026-10-15"
    })
    @DisplayName("Should correctly parse Brazilian and ISO date formats")
    void testValidDateParsing(String input, String expected) {
        LocalDate result = dateParser.parse(input);
        assertEquals(LocalDate.parse(expected), result);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException on invalid date strings")
    void testInvalidDate() {
        assertThrows(IllegalArgumentException.class, () -> dateParser.parse(null));
        assertThrows(IllegalArgumentException.class, () -> dateParser.parse(""));
        assertThrows(IllegalArgumentException.class, () -> dateParser.parse("32/13/2026"));
        assertThrows(IllegalArgumentException.class, () -> dateParser.parse("not-a-date"));
    }
}
