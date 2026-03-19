package com.agrioptima.model;

public class Crop {
    String name;
    int id;
    int ProfitPerAcre;
    int WaterPerAcre;
    int nitrogenImpact;
    int minRotationGap;
    int[] SoilCompatibility;
    double[] SeasonalMultiplier;

    public Crop(String name, int id, int profitPerAcre, int waterPerAcre, int nitrogenImpact,
                int minRotationGap, int[] soilCompatibility, double[] seasonalMultiplier) {
        this.name = name;
        this.id = id;
        ProfitPerAcre = profitPerAcre;
        WaterPerAcre = waterPerAcre;
        this.nitrogenImpact = nitrogenImpact;
        this.minRotationGap = minRotationGap;
        SoilCompatibility = soilCompatibility;
        SeasonalMultiplier = seasonalMultiplier;
    }

    public String getName() {
        return name;                // Returns crop name (for display)
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;                // Returns crop's unique ID
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getProfitPerAcre() {
        return ProfitPerAcre;       // Returns base profit per acre
    }

    public void setProfitPerAcre(int profitPerAcre) {
        ProfitPerAcre = profitPerAcre;
    }

    public int getWaterPerAcre() {
        return WaterPerAcre;         // Returns water requirement
    }

    public void setWaterPerAcre(int waterPerAcre) {
        WaterPerAcre = waterPerAcre;
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
        return SoilCompatibility;       // Returns allowed soil levels
    }

    public void setSoilCompatibility(int[] soilCompatibility) {
        SoilCompatibility = soilCompatibility;
    }

    public double[] getSeasonalMultiplier() {
        return SeasonalMultiplier;       // Returns seasonal price factors
    }

    public void setSeasonalMultiplier(double[] seasonalMultiplier) {
        SeasonalMultiplier = seasonalMultiplier;
    }

    public int getProfitForSeason(int seasonIndex) {
        return (int)(ProfitPerAcre * SeasonalMultiplier[seasonIndex]);  // Calculate profit for a specific season
    }

    public boolean isCompatibleWithSoil(int soilLevel) {
        for (int level : SoilCompatibility) {
            if (level == soilLevel) return true;  // Check if crop can be planted on given soil level
        }
        return false;
    }

    @Override
    public String toString() {          // String representation for debugging
        return name + " (ID: " + id + ", Profit: ₹" + ProfitPerAcre + ")";
    }
}
