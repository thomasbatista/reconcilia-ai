package com.reconcilia.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

public class NormalizedTransaction {

    private final String id;
    private final LocalDate date;
    private final BigDecimal amount;
    private final String description;
    private final String source;
    private final int rowNumber;

    public NormalizedTransaction(String id, LocalDate date, BigDecimal amount, String description, String source, int rowNumber) {
        this.id = id != null ? id.trim() : "";
        this.date = date;
        this.amount = amount != null ? amount.setScale(2, RoundingMode.HALF_EVEN) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        this.description = description != null ? description.trim() : "";
        this.source = source != null ? source.trim() : "";
        this.rowNumber = rowNumber;
    }

    public String getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public String getSource() {
        return source;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NormalizedTransaction that = (NormalizedTransaction) o;
        return rowNumber == that.rowNumber &&
                Objects.equals(id, that.id) &&
                Objects.equals(date, that.date) &&
                Objects.equals(amount, that.amount) &&
                Objects.equals(source, that.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, date, amount, source, rowNumber);
    }

    @Override
    public String toString() {
        return "NormalizedTransaction{" +
                "id='" + id + '\'' +
                ", date=" + date +
                ", amount=" + amount +
                ", description='" + description + '\'' +
                ", source='" + source + '\'' +
                ", rowNumber=" + rowNumber +
                '}';
    }
}
