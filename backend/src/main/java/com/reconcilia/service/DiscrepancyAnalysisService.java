package com.reconcilia.service;

import com.reconcilia.model.NormalizedTransaction;
import com.reconcilia.model.ReconciliationItem;
import com.reconcilia.model.ReconciliationStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
public class DiscrepancyAnalysisService {

    private final String apiKey;
    private final String apiUrl;
    private final String model;
    private final RestClient restClient;

    public DiscrepancyAnalysisService(
            @Value("${reconcilia.ai.api-key:}") String apiKey,
            @Value("${reconcilia.ai.api-url:https://api.openai.com/v1/chat/completions}") String apiUrl,
            @Value("${reconcilia.ai.model:gpt-4o-mini}") String model) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.apiUrl = apiUrl;
        this.model = model;
        this.restClient = RestClient.builder().build();
    }

    /**
     * Enriches a reconciliation item with an explanatory hypothesis.
     * Uses server-side LLM if configured; otherwise applies deterministic financial rule-based hypotheses.
     */
    public void generateHypothesis(ReconciliationItem item) {
        if (item == null) return;

        // MATCHED items do not need discrepancy hypotheses
        if (item.getStatus() == ReconciliationStatus.MATCHED) {
            item.setAiHypothesis("Transação conciliada integralmente. Valores e datas coincidem dentro dos limites configurados.");
            item.setHypothesisAi(false);
            return;
        }

        // Try AI if API key is provided
        if (isAiAvailable()) {
            try {
                String aiResponse = requestAiHypothesis(item);
                if (aiResponse != null && !aiResponse.trim().isEmpty()) {
                    item.setAiHypothesis(aiResponse.trim());
                    item.setHypothesisAi(true);
                    return;
                }
            } catch (Exception e) {
                // Graceful fallback to rule-based analysis without interrupting flow
            }
        }

        // Rule-based deterministic hypothesis fallback
        item.setAiHypothesis(generateRuleBasedHypothesis(item));
        item.setHypothesisAi(false);
    }

    /**
     * Checks if AI API key is configured.
     */
    public boolean isAiAvailable() {
        return !apiKey.isEmpty();
    }

    /**
     * Deterministic rule-based financial hypothesis generator based on Brazilian payment realities.
     */
    public String generateRuleBasedHypothesis(ReconciliationItem item) {
        NormalizedTransaction a = item.getSourceA();
        NormalizedTransaction b = item.getSourceB();

        if (item.getStatus() == ReconciliationStatus.AMOUNT_MISMATCH && a != null && b != null) {
            BigDecimal diff = b.getAmount().subtract(a.getAmount());
            BigDecimal gross = a.getAmount().abs();

            if (gross.compareTo(BigDecimal.ZERO) > 0 && diff.compareTo(BigDecimal.ZERO) < 0) {
                BigDecimal fee = diff.abs();
                BigDecimal feePercentage = fee.multiply(new BigDecimal("100")).divide(gross, 2, RoundingMode.HALF_UP);

                if (feePercentage.compareTo(new BigDecimal("0.50")) >= 0 && feePercentage.compareTo(new BigDecimal("7.00")) <= 0) {
                    return String.format(
                            "Hipótese: Possível desconto de taxa de administração (MDR) pela adquirente/maquininha de aproximadamente %s%% (retenção de R$ %s).",
                            feePercentage, fee.setScale(2, RoundingMode.HALF_UP)
                    );
                }
            }

            if (diff.abs().compareTo(new BigDecimal("1.00")) <= 0) {
                return "Hipótese: Diferença residual de centavos decorrente de arredondamento tributário ou rateio de parcelamento.";
            }

            return String.format(
                    "Hipótese: Divergência de valor de R$ %s entre o lançamento no ERP (R$ %s) e o extrato da adquirente (R$ %s). Verificar cancelamento parcial, gorjeta ou desconto concedido no PDV.",
                    diff.abs(), a.getAmount(), b.getAmount()
            );
        }

        if (item.getStatus() == ReconciliationStatus.DATE_MISMATCH && a != null && b != null) {
            long days = ChronoUnit.DAYS.between(a.getDate(), b.getDate());
            if (days > 0 && days <= 3) {
                return String.format(
                        "Hipótese: Janela de liquidação financeira (D+%d). A venda foi registrada no ERP em %s e creditada na adquirente em %s (possível compensação de final de semana ou feriado).",
                        days, a.getDate(), b.getDate()
                );
            } else if (days < 0) {
                return String.format(
                        "Hipótese: A transação na adquirente antecede o registro no ERP em %d dias. Verificar atraso na sincronização ou lançamento manual tardio.",
                        Math.abs(days)
                );
            }
            return String.format(
                    "Hipótese: Defasagem temporal de %d dias entre o faturamento e a confirmação financeira.",
                    Math.abs(days)
            );
        }

        if (item.getStatus() == ReconciliationStatus.UNMATCHED) {
            if (a != null && b == null) {
                return "Hipótese: Venda registrada no ERP sem captura confirmada no extrato da adquirente. Causas comuns: pedido não pago, cancelado pela loja ou recusado pelo emissor do cartão.";
            } else if (a == null && b != null) {
                return "Hipótese: Crédito identificado na adquirente sem correspondente no ERP. Causas comuns: venda direta no terminal (POS) sem vínculo com pedido, estorno avulso ou ajuste manual de operadora.";
            }
        }

        return "Hipótese: Registro isolado necessitando de auditoria manual.";
    }

    private String requestAiHypothesis(ReconciliationItem item) {
        NormalizedTransaction a = item.getSourceA();
        NormalizedTransaction b = item.getSourceB();

        String prompt = buildPrompt(item, a, b);

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content",
                                "Você é um especialista em conciliação financeira e meios de pagamento no Brasil. " +
                                "Formule uma hipótese objetiva e clara em português (máximo de 2 frases) sobre a causa provável da discrepância. " +
                                "IMPORTANTE: Rotule explicitamente como hipótese, sem afirmar como fato comprovado. Não invente dados."),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.2,
                "max_tokens", 150
        );

        Map<?, ?> response = restClient.post()
                .uri(apiUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        if (response != null && response.containsKey("choices")) {
            List<?> choices = (List<?>) response.get("choices");
            if (!choices.isEmpty()) {
                Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
                Map<?, ?> message = (Map<?, ?>) firstChoice.get("message");
                if (message != null && message.containsKey("content")) {
                    return (String) message.get("content");
                }
            }
        }

        return null;
    }

    private String buildPrompt(ReconciliationItem item, NormalizedTransaction a, NormalizedTransaction b) {
        StringBuilder sb = new StringBuilder();
        sb.append("Analise esta discrepância de conciliação financeira:\n");
        sb.append("Status: ").append(item.getStatus().getLabel()).append("\n");

        if (a != null) {
            sb.append("Fonte A (ERP): ID=").append(a.getId())
                    .append(", Data=").append(a.getDate())
                    .append(", Valor=R$ ").append(a.getAmount())
                    .append(", Descrição=").append(a.getDescription()).append("\n");
        } else {
            sb.append("Fonte A (ERP): Ausente\n");
        }

        if (b != null) {
            sb.append("Fonte B (Adquirente): ID=").append(b.getId())
                    .append(", Data=").append(b.getDate())
                    .append(", Valor=R$ ").append(b.getAmount())
                    .append(", Descrição=").append(b.getDescription()).append("\n");
        } else {
            sb.append("Fonte B (Adquirente): Ausente\n");
        }

        if (item.getAmountDifference() != null) {
            sb.append("Diferença de Valor: R$ ").append(item.getAmountDifference()).append("\n");
        }
        if (item.getDateDifferenceDays() != null) {
            sb.append("Diferença de Dias: ").append(item.getDateDifferenceDays()).append(" dia(s)\n");
        }

        return sb.toString();
    }
}
