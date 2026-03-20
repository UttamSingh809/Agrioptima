package com.agrioptima.model;

import java.util.Arrays;

/**
 * Represents a Crop entity in the AgriOptima system.
 * Part of the Model layer implementation.
 * This class encapsulates the core attributes and logic for individual crops,
 * ensuring data integrity through strict encapsulation and defensive copying of arrays.
 */
public class Crop {

    private String name;
    private int id;
    private int profitPerAcre;
    private int waterPerAcre;
    private int nitrogenImpact;
    private int minRotationGap;
    private int[] soilCompatibility;
    private double[] seasonalMultiplier;

    /**
     * Full constructor for data loading.
     * Applies basic string sanitization and defensive array copying.
     */
    public Crop(String name, int id, int profitPerAcre, int waterPerAcre, int nitrogenImpact,
                int minRotationGap, int[] soilCompatibility, double[] seasonalMultiplier) {
        this.name = name != null ? name.trim() : "Unknown";
        this.id = id;
        this.profitPerAcre = profitPerAcre;
        this.waterPerAcre = waterPerAcre;
        this.nitrogenImpact = nitrogenImpact;
        this.minRotationGap = minRotationGap;

        // Defensive copying to prevent external array mutation
        this.soilCompatibility = soilCompatibility != null ?
                Arrays.copyOf(soilCompatibility, soilCompatibility.length) : new int[0];

        this.seasonalMultiplier = seasonalMultiplier != null ?
                Arrays.copyOf(seasonalMultiplier, seasonalMultiplier.length) : new double[0];
    }

    // --- Primitive Getters and Setters ---

    public String getName() { return name; }
    public void setName(String name) { this.name = name != null ? name.trim() : "Unknown"; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProfitPerAcre() { return profitPerAcre; }
    public void setProfitPerAcre(int profitPerAcre) { this.profitPerAcre = profitPerAcre; }

    public int getWaterPerAcre() { return waterPerAcre; }
    public void setWaterPerAcre(int waterPerAcre) { this.waterPerAcre = waterPerAcre; }

    public int getNitrogenImpact() { return nitrogenImpact; }
    public void setNitrogenImpact(int nitrogenImpact) { this.nitrogenImpact = nitrogenImpact; }

    public int getMinRotationGap() { return minRotationGap; }
    public void setMinRotationGap(int minRotationGap) { this.minRotationGap = minRotationGap; }

    // --- Array Getters and Setters (Secured) ---

    /**
     * Returns a copy of the array to prevent external code from modifying the internal state.
     */
    public int[] getSoilCompatibility() {
        return Arrays.copyOf(soilCompatibility, soilCompatibility.length);
    }

    /**
     * Stores a copy of the incoming array to maintain encapsulation.
     */
    public void setSoilCompatibility(int[] soilCompatibility) {
        this.soilCompatibility = soilCompatibility != null ?
                Arrays.copyOf(soilCompatibility, soilCompatibility.length) : new int[0];
    }

    /**
     * Returns a copy of the array to prevent external code from modifying the internal state.
     */
    public double[] getSeasonalMultiplier() {
        return Arrays.copyOf(seasonalMultiplier, seasonalMultiplier.length);
    }

    /**
     * Stores a copy of the incoming array to maintain encapsulation.
     */
    public void setSeasonalMultiplier(double[] seasonalMultiplier) {
        this.seasonalMultiplier = seasonalMultiplier != null ?
                Arrays.copyOf(seasonalMultiplier, seasonalMultiplier.length) : new double[0];
    }

    // --- Utility Methods ---

    /**
     * Calculates the profit for a specific season.
     * Safely handles out-of-bounds season requests.
     */
    public int getProfitForSeason(int seasonIndex) {
        if (seasonIndex < 0 || seasonIndex >= seasonalMultiplier.length) {
            return 0; // Safe fallback if solver asks for an invalid season
        }
        return (int)(profitPerAcre * seasonalMultiplier[seasonIndex]);
    }

    /**
     * Checks if the crop can be planted on the given soil level.
     */
    public boolean isCompatibleWithSoil(int soilLevel) {
        for (int level : soilCompatibility) {
            if (level == soilLevel) return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return name + " (ID: " + id + ", Profit: ₹" + profitPerAcre + ")";
    }
}