package com.reconcilia.model;

import java.util.Objects;

public class ColumnMapping {

    private String idColumn;
    private String dateColumn;
    private String amountColumn;
    private String descriptionColumn;

    public ColumnMapping() {
    }

    public ColumnMapping(String idColumn, String dateColumn, String amountColumn, String descriptionColumn) {
        this.idColumn = idColumn;
        this.dateColumn = dateColumn;
        this.amountColumn = amountColumn;
        this.descriptionColumn = descriptionColumn;
    }

    public String getIdColumn() {
        return idColumn;
    }

    public void setIdColumn(String idColumn) {
        this.idColumn = idColumn;
    }

    public String getDateColumn() {
        return dateColumn;
    }

    public void setDateColumn(String dateColumn) {
        this.dateColumn = dateColumn;
    }

    public String getAmountColumn() {
        return amountColumn;
    }

    public void setAmountColumn(String amountColumn) {
        this.amountColumn = amountColumn;
    }

    public String getDescriptionColumn() {
        return descriptionColumn;
    }

    public void setDescriptionColumn(String descriptionColumn) {
        this.descriptionColumn = descriptionColumn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ColumnMapping that = (ColumnMapping) o;
        return Objects.equals(idColumn, that.idColumn) &&
                Objects.equals(dateColumn, that.dateColumn) &&
                Objects.equals(amountColumn, that.amountColumn) &&
                Objects.equals(descriptionColumn, that.descriptionColumn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idColumn, dateColumn, amountColumn, descriptionColumn);
    }
}
