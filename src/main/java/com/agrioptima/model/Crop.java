package com.agrioptima.model;

/**
 * Represents a Crop entity in the AgriOptima system.
 * Part of the Model layer implementation.
 * This class encapsulates the core attributes and logic for individual crops,
 * ensuring data integrity through private fields and public accessors.
 */
public class Crop {

    // Instance variables are strictly private to maintain model encapsulation
    private String name;
    private int id;
    private int profitPerAcre;
    private int waterPerAcre;
    private int nitrogenImpact;
    private int minRotationGap;
    private int[] soilCompatibility;
    private double[] seasonalMultiplier;

    public Crop(String name, int id, int profitPerAcre, int waterPerAcre, int nitrogenImpact,
                int minRotationGap, int[] soilCompatibility, double[] seasonalMultiplier) {
        this.name = name;
        this.id = id;
        this.profitPerAcre = profitPerAcre;
        this.waterPerAcre = waterPerAcre;
        this.nitrogenImpact = nitrogenImpact;
        this.minRotationGap = minRotationGap;
        this.soilCompatibility = soilCompatibility;
        this.seasonalMultiplier = seasonalMultiplier;
    }

    public String getName() {
        return name;                // Returns crop name (for display)
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;                  // Returns crop's unique ID
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getProfitPerAcre() {
        return profitPerAcre;       // Returns base profit per acre
    }

    public void setProfitPerAcre(int profitPerAcre) {
        this.profitPerAcre = profitPerAcre;
    }

    public int getWaterPerAcre() {
        return waterPerAcre;         // Returns water requirement
    }

    public void setWaterPerAcre(int waterPerAcre) {
        this.waterPerAcre = waterPerAcre;
    }

    public int getNitrogenImpact() {
        return nitrogenImpact;      // Returns N-impact (-1, 0, +1)
    }

    public void setNitrogenImpact(int nitrogenImpact) {
        this.nitrogenImpact = nitrogenImpact;
    }

    public int getMinRotationGap() {
        return minRotationGap;      // Returns rotation constraint
    }

    public void setMinRotationGap(int minRotationGap) {
        this.minRotationGap = minRotationGap;
    }

    public int[] getSoilCompatibility() {
        return soilCompatibility;       // Returns allowed soil levels
    }

    public void setSoilCompatibility(int[] soilCompatibility) {
        this.soilCompatibility = soilCompatibility;
    }

    public double[] getSeasonalMultiplier() {
        return seasonalMultiplier;       // Returns seasonal price factors
    }

    public void setSeasonalMultiplier(double[] seasonalMultiplier) {
        this.seasonalMultiplier = seasonalMultiplier;
    }

    public int getProfitForSeason(int seasonIndex) {
        return (int)(profitPerAcre * seasonalMultiplier[seasonIndex]);  // Calculate profit for a specific season
    }

    public boolean isCompatibleWithSoil(int soilLevel) {
        for (int level : soilCompatibility) {
            if (level == soilLevel) return true;  // Check if crop can be planted on given soil level
        }
        return false;
    }

    @Override
    public String toString() {          // String representation for debugging
        return name + " (ID: " + id + ", Profit: ₹" + profitPerAcre + ")";
    }
}