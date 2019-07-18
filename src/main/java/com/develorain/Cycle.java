package com.develorain;

import java.util.List;

public class Cycle implements Comparable {
    public final List<String> cycleString; // List of string currencies
    public double multiplier;              // Worst-case multiplier for the cycle
    public double[] tradePrices;           // Worst-case price (can be either ask or bid)

    // this is basically tradePrices, but does 1.0 / tradePrices for buying base currencies, so that we can do the math easier, but still have the base tradePrices in memory
    // basically, when using the top half, you gotta 1.0 / top-half as you're going from other currency back to base currency
    public double[] tradeRates;

    public double[] tradeQuantitiesInStartCurrency; // this is temporary, should NOT be an attribute, remove this!!

    public double[] tradeQuantities;                // this is temporary, the final value ends up in actualTradeQuantitiesForEachCurrency
    public double[] actualTradeQuantitiesForEachCurrency;
    public int size;

    public Cycle(List<String> cycleString) {
        this.cycleString = cycleString;
        this.multiplier = 1.0;
        this.size = cycleString.size();

        this.tradePrices = new double[cycleString.size()];
        this.tradeRates = new double[cycleString.size()];
        this.tradeQuantities = new double[cycleString.size()];
        this.tradeQuantitiesInStartCurrency = new double[cycleString.size()];
        this.actualTradeQuantitiesForEachCurrency = new double[cycleString.size()];
    }

    @Override
    public String toString() {
        return "<~~ " + cycleString + " with multiplier " + Tools.formatPrice(multiplier) + " ~~>";
    }

    @Override
    public int compareTo(Object o) {
        Cycle otherCycle = (Cycle) o;

        if (multiplier > otherCycle.multiplier) {
            return 1;
        }

        if (multiplier < otherCycle.multiplier) {
            return -1;
        }

        return 0;
    }
}