package com.reconcilia.model;

public enum ReconciliationStatus {
    MATCHED("Conciliado", "Transação conciliada com sucesso (data e valor coincidentes dentro das tolerâncias)"),
    AMOUNT_MISMATCH("Divergência de Valor", "Transação localizada, porém com divergência no valor"),
    DATE_MISMATCH("Divergência de Data", "Transação localizada, porém com data fora da janela de tolerância"),
    UNMATCHED("Não Conciliado", "Transação presente em apenas uma das fontes");

    private final String label;
    private final String description;

    ReconciliationStatus(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}
