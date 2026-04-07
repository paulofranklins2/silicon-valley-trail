package com.pcunha.svt.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
public class MarketState {
    @Setter
    private GameEvent currentMarketEvent;
    @Setter
    private int marketCityIndex = -1;
    private Set<Integer> marketPurchased = new HashSet<>();

    public void resetMarket() {
        this.currentMarketEvent = null;
        this.marketCityIndex = -1;
        this.marketPurchased = new HashSet<>();
    }

    public void addMarketPurchase(int index) {
        this.marketPurchased.add(index);
    }
}
