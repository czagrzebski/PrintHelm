package com.czagrzebski.printhelm.web.domain;

import java.math.BigDecimal;

public class QuoteLineItem {
    private String label;
    private BigDecimal amount;

    public QuoteLineItem() {}

    public QuoteLineItem(String label, BigDecimal amount) {
        this.label = label;
        this.amount = amount;
    }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
