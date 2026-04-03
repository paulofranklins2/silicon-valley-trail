package com.pcunha.svt.domain;

import com.pcunha.svt.domain.model.ResourceState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResourceStateTest {

    @Test
    public void addCash() {
        ResourceState resourceState = new ResourceState(100, 100, 100);
        resourceState.changeCash(10);
        assertEquals(110, resourceState.getCash());
    }

    @Test
    public void subCash() {
        ResourceState resourceState = new ResourceState(100, 100, 100);
        resourceState.changeCash(-30);
        assertEquals(70, resourceState.getCash());
    }

    @Test
    public void CashClampAtZero() {
        ResourceState resourceState = new ResourceState(10, 100, 100);
        resourceState.changeCash(-30);
        assertEquals(0, resourceState.getCash());
    }

    @Test
    public void addFood() {
        ResourceState resourceState = new ResourceState(100, 100, 100);
        resourceState.changeFood(10);
        assertEquals(110, resourceState.getFood());
    }

    @Test
    public void subFood() {
        ResourceState resourceState = new ResourceState(100, 100, 100);
        resourceState.changeFood(-30);
        assertEquals(70, resourceState.getFood());
    }

    @Test
    public void foodClampAtZero() {
        ResourceState resourceState = new ResourceState(100, 10, 100);
        resourceState.changeFood(-30);
        assertEquals(0, resourceState.getFood());
    }

    @Test
    public void addComputeCredits() {
        ResourceState resourceState = new ResourceState(100, 100, 100);
        resourceState.changeComputeCredits(10);
        assertEquals(110, resourceState.getComputeCredits());
    }

    @Test
    public void subComputeCredits() {
        ResourceState resourceState = new ResourceState(100, 100, 100);
        resourceState.changeComputeCredits(-30);
        assertEquals(70, resourceState.getComputeCredits());
    }

    @Test
    public void ComputeCreditsClampAtZero() {
        ResourceState resourceState = new ResourceState(100, 100, 10);
        resourceState.changeComputeCredits(-30);
        assertEquals(0, resourceState.getComputeCredits());
    }
}