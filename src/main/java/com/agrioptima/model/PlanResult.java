package com.agrioptima.model;

import java.util.Arrays;

/**
 * Represents the final result of the DP solver — the optimal plan plus performance metrics.
 * This class is strictly immutable to ensure the calculated schedule cannot be accidentally
 * modified by the frontend or other components after optimization is complete.
 */
public class PlanResult {

    private final int totalProfit;
    private final int waterUsed;
    private final int waterUsedPercent;
    private final int soilHealthChange;
    private final String[][] plan; // 2D array: plan[season][plot] = crop name
    private final double riskScore;
    private final long computationTimeMs;

    /**
     * Full constructor for the optimization output.
     * Performs a deep copy of the 2D plan array to guarantee immutability.
     */
    public PlanResult(int totalProfit, int waterUsed, int waterUsedPercent,
                      int soilHealthChange, String[][] plan,
                      double riskScore, long computationTimeMs) {
        this.totalProfit = totalProfit;
        this.waterUsed = waterUsed;
        this.waterUsedPercent = waterUsedPercent;
        this.soilHealthChange = soilHealthChange;
        this.riskScore = riskScore;
        this.computationTimeMs = computationTimeMs;

        // Deep defensive copy of the 2D array to prevent external modification
        if (plan != null) {
            this.plan = new String[plan.length][];
            for (int i = 0; i < plan.length; i++) {
                if (plan[i] != null) {
                    this.plan[i] = Arrays.copyOf(plan[i], plan[i].length);
                } else {
                    this.plan[i] = new String[0]; // Fallback for null sub-arrays
                }
            }
        } else {
            this.plan = new String[0][0]; // Fallback for completely null plan
        }
    }

    /**
     * Minimal constructor (primarily for testing scenarios).
     */
    public PlanResult(int totalProfit, String[][] plan) {
        this(totalProfit, 0, 0, 0, plan, 0.0, 0L);
    }

    // --- Getters ---

    public int getTotalProfit() { return totalProfit; }
    public int getWaterUsed() { return waterUsed; }
    public int getWaterUsedPercent() { return waterUsedPercent; }
    public int getSoilHealthChange() { return soilHealthChange; }
    public double getRiskScore() { return riskScore; }
    public long getComputationTimeMs() { return computationTimeMs; }

    /**
     * Returns a deep copy of the full schedule.
     * @return 2D array representing the plan.
     */
    public String[][] getPlan() {
        String[][] copy = new String[this.plan.length][];
        for (int i = 0; i < this.plan.length; i++) {
            copy[i] = Arrays.copyOf(this.plan[i], this.plan[i].length);
        }
        return copy;
    }

    /**
     * Returns the crops for a specific season.
     * Safely handles out-of-bounds requests.
     */
    public String[] getSeasonPlan(int season) {
        if (season < 0 || season >= plan.length) {
            return new String[0]; // Safe fallback
        }
        return Arrays.copyOf(plan[season], plan[season].length);
    }

    /**
     * Returns the specific crop for a given season and plot.
     * Safely handles out-of-bounds requests by returning "Fallow".
     */
    public String getCrop(int season, int plot) {
        if (season < 0 || season >= plan.length) {
            return "Fallow";
        }
        if (plot < 0 || plot >= plan[season].length) {
            return "Fallow";
        }
        String crop = plan[season][plot];
        return (crop != null && !crop.trim().isEmpty()) ? crop : "Fallow";
    }

    // --- Summary Methods ---

    /**
     * @return A human-readable summary of the optimization results.
     */
    public String getSummary() {
        return String.format(
                "Total Profit: ₹%,d | Water Used: %d%% | Soil Health: %+d%% | Risk: %.1f%%",
                totalProfit, waterUsedPercent, soilHealthChange, riskScore
        );
    }

    /**
     * @return true if water utilization is optimally between 80% and 100%.
     */
    public boolean isWaterEfficient() {
        return waterUsedPercent >= 80 && waterUsedPercent <= 100;
    }

    /**
     * @return true if the plan resulted in a net positive soil health change.
     */
    public boolean isSoilImproved() {
        return soilHealthChange > 0;
    }
}