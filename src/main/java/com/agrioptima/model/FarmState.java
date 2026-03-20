package com.agrioptima.model;

import java.util.Arrays;

/**
 * Represents the complete state of the farm at a given decision point (season).
 * This is the most critical class for the Dynamic Programming solver, as it defines
 * the state space. It uses strict equality and hash caching for efficient memoization.
 */
public class FarmState {

    /** Current season index (0 to 2*years-1). Represents the time dimension. */
    private int season;

    /** Array of plot-specific states tracking the history of each individual plot. */
    private PlotState[] plotStates;

    /** Water left for the remaining seasons. Acts as a strict resource constraint. */
    private int remainingWater;

    /** Cached hash code for O(1) retrieval during DP memoization. */
    private int hashCode;

    /**
     * Nested class representing the isolated state of a single plot.
     */
    public static class PlotState {
        public int lastCropId;
        public int soilNitrogenLevel;

        public PlotState(int lastCropId, int soilNitrogenLevel) {
            this.lastCropId = lastCropId;
            this.soilNitrogenLevel = soilNitrogenLevel;
        }

        // Copy constructor for PlotState
        public PlotState(PlotState other) {
            this.lastCropId = other.lastCropId;
            this.soilNitrogenLevel = other.soilNitrogenLevel;
        }

        @Override
        public String toString() {
            return "{Crop: " + lastCropId + ", Soil: " + soilNitrogenLevel + "}";
        }
    }

    /**
     * Full constructor for absolute control over state initialization.
     * Performs a deep copy of the plotStates array to guarantee immutability.
     */
    public FarmState(int season, PlotState[] plotStates, int remainingWater) {
        this.season = season;
        this.remainingWater = remainingWater;

        // Deep copy to prevent external modification
        this.plotStates = new PlotState[plotStates.length];
        for (int i = 0; i < plotStates.length; i++) {
            this.plotStates[i] = new PlotState(plotStates[i]);
        }
    }

    /**
     * Initial state constructor used at the very start of the planning horizon.
     * Initializes all plots to have no crop (-1) and medium soil nitrogen (1).
     *
     * @param totalPlots   The total number of plots on the farm.
     * @param initialWater The starting water budget.
     */
    public FarmState(int totalPlots, int initialWater) {
        this.season = 0;
        this.plotStates = new PlotState[totalPlots];
        for (int i = 0; i < totalPlots; i++) {
            this.plotStates[i] = new PlotState(-1, 1); // No crop, medium soil
        }
        this.remainingWater = initialWater;
    }

    /**
     * Copy constructor used strictly for state transitions.
     * Guarantees immutability of the parent state so the DP search tree isn't corrupted.
     *
     * @param other The previous FarmState to copy from.
     */
    public FarmState(FarmState other) {
        this.season = other.season;
        this.plotStates = new PlotState[other.plotStates.length];
        for (int i = 0; i < other.plotStates.length; i++) {
            this.plotStates[i] = new PlotState(other.plotStates[i]);
        }
        this.remainingWater = other.remainingWater;
        // Note: hashCode is intentionally left as 0 so it recalculates for the new state
    }

    // --- Getters ---

    public int getSeason() { return season; }

    public int getTotalPlots() { return plotStates.length; }

    /**
     * Returns a deep copy of the plot states to maintain strict encapsulation.
     */
    public PlotState[] getPlotStates() {
        PlotState[] copy = new PlotState[plotStates.length];
        for (int i = 0; i < plotStates.length; i++) {
            copy[i] = new PlotState(plotStates[i]);
        }
        return copy;
    }

    public PlotState getPlotState(int plotIndex) {
        // Returning a copy to prevent external mutation of the state
        return new PlotState(plotStates[plotIndex]);
    }

    public int getRemainingWater() { return remainingWater; }

    public int getLastCropId(int plotIndex) { return plotStates[plotIndex].lastCropId; }

    public int getSoilNitrogenLevel(int plotIndex) { return plotStates[plotIndex].soilNitrogenLevel; }

    // --- State Transition Logic ---

    /**
     * Applies a crop assignment across all plots and generates the next chronological state.
     * Encapsulates the transition logic and safely handles fallow land (-1).
     *
     * @param cropAssignment Array of crop IDs assigned to each plot.
     * @param crops          The reference array of all available Crop objects.
     * @return A brand new FarmState representing the next season.
     */
    public FarmState getNextState(int[] cropAssignment, Crop[] crops) {
        FarmState nextState = new FarmState(this);
        nextState.season++;

        int waterUsed = 0;
        for (int i = 0; i < cropAssignment.length; i++) {
            int cropId = cropAssignment[i];

            if (cropId == -1) {
                // Handle fallow land explicitly to avoid out-of-bounds errors
                nextState.plotStates[i].lastCropId = -1;
            } else {
                Crop crop = crops[cropId];

                // Update soil nitrogen (bounded between 0 and 2)
                int newNitrogen = Math.min(2, Math.max(0,
                        nextState.plotStates[i].soilNitrogenLevel + crop.getNitrogenImpact()));
                nextState.plotStates[i].soilNitrogenLevel = newNitrogen;

                // Update last crop
                nextState.plotStates[i].lastCropId = cropId;

                // Track water usage
                waterUsed += crop.getWaterPerAcre();
            }
        }

        nextState.remainingWater -= waterUsed;
        return nextState;
    }

    // --- Equality & Hashing (CRITICAL FOR MEMOIZATION) ---

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof FarmState)) return false;

        FarmState other = (FarmState) obj;
        if (this.season != other.season) return false;
        if (this.remainingWater != other.remainingWater) return false;

        // Compare each plot state structurally
        for (int i = 0; i < this.plotStates.length; i++) {
            if (this.plotStates[i].lastCropId != other.plotStates[i].lastCropId) return false;
            if (this.plotStates[i].soilNitrogenLevel != other.plotStates[i].soilNitrogenLevel) return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {  // Cache for performance
            int result = season;
            result = 31 * result + remainingWater;
            for (PlotState ps : plotStates) {
                result = 31 * result + ps.lastCropId;
                result = 31 * result + ps.soilNitrogenLevel;
            }
            hashCode = result;
        }
        return hashCode;
    }

    // --- Utility Methods ---

    /**
     * Checks if this state has reached the end of the planning horizon.
     *
     * @param totalSeasons The maximum number of seasons allowed.
     * @return true if planning is complete, false otherwise.
     */
    public boolean isTerminal(int totalSeasons) {
        return season >= totalSeasons;
    }

    @Override
    public String toString() {
        return "Season " + season + ", Water: " + remainingWater + ", Plots: " +
                Arrays.toString(plotStates);
    }
}