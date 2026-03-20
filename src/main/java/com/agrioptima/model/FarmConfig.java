package com.agrioptima.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the user-provided configuration for an AgriOptima planning session.
 * Captures constraints, preferences, and regional settings to drive the optimization model.
 * This class acts as the data transfer object between the frontend user inputs and the backend DP solver.
 */
public class FarmConfig {

    /** * Number of distinct plots/fields.
     * Determines the state space size (exponential scaling in DP).
     */
    private int plots;

    /** * Total water available for the entire planning horizon (in liters/year).
     * Acts as a hard constraint for the optimization solver.
     */
    private int waterBudget;

    /** * Planning horizon in years.
     * Determines the total number of seasons to plan for.
     */
    private int years;

    /** Geographic region (e.g., "Punjab", "Maharashtra") used to load specific crops and weather data. */
    private String region;

    /** Optimization focus (e.g., "profit", "water", "soil"). Adjusts objective function weights. */
    private String priorityMode;

    /** Rainfall assumption (e.g., "normal", "drought", "excess"). Affects stochastic profit calculations. */
    private String weatherScenario;

    /**
     * Full constructor to initialize all configuration parameters.
     * Recommended when all user preferences are explicitly provided.
     *
     * @param plots           Number of distinct plots/fields.
     * @param waterBudget     Total water available (liters/year).
     * @param years           Planning horizon in years.
     * @param region          Geographic region.
     * @param priorityMode    Optimization focus ("profit", "water", "soil").
     * @param weatherScenario Rainfall assumption ("normal", "drought", "excess").
     */
    public FarmConfig(int plots, int waterBudget, int years, String region,
                      String priorityMode, String weatherScenario) {
        this.plots = plots;
        this.waterBudget = waterBudget;
        this.years = years;
        this.region = region;
        this.priorityMode = priorityMode;
        this.weatherScenario = weatherScenario;
    }

    /**
     * Minimal constructor for basic initialization.
     * Applies default values for priorityMode ("profit") and weatherScenario ("normal").
     *
     * @param plots       Number of distinct plots/fields.
     * @param waterBudget Total water available (liters/year).
     * @param years       Planning horizon in years.
     * @param region      Geographic region.
     */
    public FarmConfig(int plots, int waterBudget, int years, String region) {
        this(plots, waterBudget, years, region, "profit", "normal");
    }

    // --- Getters ---

    /** @return The number of distinct plots/fields. */
    public int getPlots() { return plots; }

    /** @return The total water available constraint. */
    public int getWaterBudget() { return waterBudget; }

    /** @return The planning horizon in years. */
    public int getYears() { return years; }

    /** @return The total number of seasons (2 seasons per year: Kharif and Rabi). */
    public int getTotalSeasons() { return 2 * years; }

    /** @return The configured geographic region. */
    public String getRegion() { return region; }

    /** @return The primary optimization goal. */
    public String getPriorityMode() { return priorityMode; }

    /** @return The configured weather/rainfall assumption. */
    public String getWeatherScenario() { return weatherScenario; }

    // --- Setters (Enables "What-If" scenarios) ---

    /** @param plots Updates the plot count. */
    public void setPlots(int plots) { this.plots = plots; }

    /** @param waterBudget Updates the water limit. */
    public void setWaterBudget(int waterBudget) { this.waterBudget = waterBudget; }

    /** @param years Updates the planning horizon. */
    public void setYears(int years) { this.years = years; }

    /** @param region Updates the geographic region. */
    public void setRegion(String region) { this.region = region; }

    /** @param priorityMode Changes the optimization goal. */
    public void setPriorityMode(String priorityMode) { this.priorityMode = priorityMode; }

    /** @param weatherScenario Changes the weather assumption. */
    public void setWeatherScenario(String weatherScenario) { this.weatherScenario = weatherScenario; }

    // --- Validation Logic ---

    /**
     * Validates the configuration before passing it to the solver.
     * Ensures all constraints are positive and within feasible limits for Dynamic Programming.
     *
     * @return true if the configuration is valid, false otherwise.
     */
    public boolean isValid() {
        return plots > 0 &&
                waterBudget > 0 &&
                years > 0 &&
                years <= 5;  // Reasonable limit to keep DP feasible
    }

    /**
     * Retrieves specific validation error messages for the frontend to display.
     *
     * @return A list of error strings. Empty if the configuration is valid.
     */
    public List<String> getValidationErrors() {
        List<String> errors = new ArrayList<>();
        if (plots <= 0) {
            errors.add("Plots must be positive");
        }
        if (waterBudget <= 0) {
            errors.add("Water budget must be positive");
        }
        if (years <= 0 || years > 5) {
            errors.add("Years must be between 1 and 5");
        }
        return errors;
    }
}