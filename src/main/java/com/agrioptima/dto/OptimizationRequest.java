package com.agrioptima.dto;

import com.agrioptima.model.Crop;
import java.util.List;

/**
 * OptimizationRequest.java
 *
 * Purpose: Represents the input data structure for the optimization API endpoint.
 * It holds the configuration of the farm and the list of available crops.
 */
public class OptimizationRequest {

    // Input fields expected from the frontend/API client
    private List<Crop> crops; // The list of available crops to choose from
    private int numPlots; // Number of plots on the farm
    private int totalSeasons; // Total number of seasons to plan for (e.g., 4 for 2 years)
    private int waterBudget; // Total water budget for the entire planning period
    private int initialSoil; // Initial soil nitrogen level for all plots (or default level)

    // Default constructor required by Jackson (JSON deserializer)
    public OptimizationRequest() {}

    // Constructor with all fields
    public OptimizationRequest(List<Crop> crops, int numPlots, int totalSeasons, int waterBudget, int initialSoil) {
        this.crops = crops;
        this.numPlots = numPlots;
        this.totalSeasons = totalSeasons;
        this.waterBudget = waterBudget;
        this.initialSoil = initialSoil;
    }

    // Getters and Setters
    public List<Crop> getCrops() {
        return crops;
    }

    public void setCrops(List<Crop> crops) {
        this.crops = crops;
    }

    public int getNumPlots() {
        return numPlots;
    }

    public void setNumPlots(int numPlots) {
        this.numPlots = numPlots;
    }

    public int getTotalSeasons() {
        return totalSeasons;
    }

    public void setTotalSeasons(int totalSeasons) {
        this.totalSeasons = totalSeasons;
    }

    public int getWaterBudget() {
        return waterBudget;
    }

    public void setWaterBudget(int waterBudget) {
        this.waterBudget = waterBudget;
    }

    public int getInitialSoil() {
        return initialSoil;
    }

    public void setInitialSoil(int initialSoil) {
        this.initialSoil = initialSoil;
    }
}