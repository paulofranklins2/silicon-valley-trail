package com.pcunha.svt.domain.model;

public record MarketResult(boolean ok, String error) {
    public static MarketResult success() {
        return new MarketResult(true, null);
    }

    public static MarketResult error(String error) {
        return new MarketResult(false, error);
    }
}
