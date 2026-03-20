package com.agrioptima.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the user-provided configuration for an AgriOptima planning session.
 * Captures constraints, preferences, and regional settings to drive the optimization model.
 * This class acts as a strict data transfer object, ensuring only valid data reaches the DP solver.
 */
public class FarmConfig {

    private int plots;
    private int waterBudget;
    private int years;
    private String region;
    private String priorityMode;
    private String weatherScenario;

    /**
     * Full constructor to initialize all configuration parameters.
     * Applies basic sanitization to prevent null strings.
     */
    public FarmConfig(int plots, int waterBudget, int years, String region,
                      String priorityMode, String weatherScenario) {
        this.plots = plots;
        this.waterBudget = waterBudget;
        this.years = years;
        this.region = region != null ? region.trim() : "";
        this.priorityMode = priorityMode != null ? priorityMode.trim().toLowerCase() : "";
        this.weatherScenario = weatherScenario != null ? weatherScenario.trim().toLowerCase() : "";
    }

    /**
     * Minimal constructor for basic initialization.
     * Applies default values for priorityMode ("profit") and weatherScenario ("normal").
     */
    public FarmConfig(int plots, int waterBudget, int years, String region) {
        this(plots, waterBudget, years, region, "profit", "normal");
    }

    // --- Getters ---

    public int getPlots() { return plots; }
    public int getWaterBudget() { return waterBudget; }
    public int getYears() { return years; }

    /** @return The total number of seasons (2 seasons per year: Kharif and Rabi). */
    public int getTotalSeasons() { return 2 * years; }

    public String getRegion() { return region; }
    public String getPriorityMode() { return priorityMode; }
    public String getWeatherScenario() { return weatherScenario; }

    // --- Setters (With basic sanitization) ---

    public void setPlots(int plots) { this.plots = plots; }
    public void setWaterBudget(int waterBudget) { this.waterBudget = waterBudget; }
    public void setYears(int years) { this.years = years; }

    public void setRegion(String region) {
        this.region = region != null ? region.trim() : "";
    }

    public void setPriorityMode(String priorityMode) {
        this.priorityMode = priorityMode != null ? priorityMode.trim().toLowerCase() : "";
    }

    public void setWeatherScenario(String weatherScenario) {
        this.weatherScenario = weatherScenario != null ? weatherScenario.trim().toLowerCase() : "";
    }

    // --- Validation Logic ---

    /**
     * Validates the configuration before passing it to the solver.
     * Ensures numerical constraints are feasible and strings match expected enumerations.
     *
     * @return true if the configuration is valid, false otherwise.
     */
    public boolean isValid() {
        return getValidationErrors().isEmpty();
    }

    /**
     * Retrieves specific validation error messages for the frontend to display.
     * Evaluates both numerical limits and required string parameters.
     *
     * @return A list of error strings. Empty if the configuration is completely valid.
     */
    public List<String> getValidationErrors() {
        List<String> errors = new ArrayList<>();

        // Numerical Checks
        if (plots <= 0) {
            errors.add("Plots must be positive.");
        }
        if (waterBudget <= 0) {
            errors.add("Water budget must be positive.");
        }
        if (years <= 0 || years > 5) {
            errors.add("Years must be between 1 and 5 to ensure calculation performance.");
        }

        // String & Enum Checks
        if (region.isEmpty()) {
            errors.add("Region must be specified.");
        }

        if (!priorityMode.equals("profit") && !priorityMode.equals("water") && !priorityMode.equals("soil")) {
            errors.add("Priority mode must be one of: 'profit', 'water', or 'soil'.");
        }

        if (!weatherScenario.equals("normal") && !weatherScenario.equals("drought") && !weatherScenario.equals("excess")) {
            errors.add("Weather scenario must be one of: 'normal', 'drought', or 'excess'.");
        }

        return errors;
    }
}