/* the following StateManager class creates, manages, and transitions between DP states.
It is important because it tells:
   1. What does the initial farm state look like before Season 0?
   2. Given a state + a crop assignment, what does the next state look like? */
package com.agrioptima.solver;

import com.agrioptima.model.Crop; // needed to look up crop properties during state transitions
import com.agrioptima.model.FarmState; // represents the state of the farm at a given season (soil levels, last crops, water)
import com.agrioptima.model.FarmState.PlotState; // represents the state of an individual plot (last crop planted, soil nitrogen level)

import java.util.List;

public class StateManager {
    //Soil nitrogen is stored as an integer 0, 1, or 2.
    public static final int SOIL_LOW    = 0;
    public static final int SOIL_MEDIUM = 1;
    public static final int SOIL_HIGH   = 2;

    //Minimum and maximum soil nitrogen values (for clamping).
    private static final int SOIL_MIN = SOIL_LOW;
    private static final int SOIL_MAX = SOIL_HIGH;
    /**
     * Total number of seasons in the planning horizon.
     * Example: 3 years × 2 seasons/year = 6 total seasons.
     */
    private final int totalSeasons;
    /**
     * Total number of farm plots.
     * Example: 4 plots → each state tracks 4 PlotStates.
     */
    private final int numPlots;
    
    // Constructor takes the fixed parameters of the problem that define the state space.
    public StateManager(int numPlots, int totalSeasons) {
        if (numPlots <= 0) {
            throw new IllegalArgumentException("numPlots must be > 0, got: " + numPlots);
        }
        if (totalSeasons <= 0) {
            throw new IllegalArgumentException("totalSeasons must be > 0, got: " + totalSeasons);
        }
        this.numPlots     = numPlots;
        this.totalSeasons = totalSeasons;
    }

    // --------------------------------------------------------
    // Public API — the two methods the rest of the solver uses
    // --------------------------------------------------------
    public FarmState createInitialState(int waterBudget, int initialSoilLevel) {
        if (waterBudget < 0) {
            throw new IllegalArgumentException("waterBudget cannot be negative, got: " + waterBudget);
        }
        validateSoilLevel(initialSoilLevel, "initialSoilLevel");

        // Build a PlotState for each plot — all identical at the start
        PlotState[] plotStates = new PlotState[numPlots];
        for (int i = 0; i < numPlots;++i) {
            plotStates[i] = new PlotState(
                /* lastCropId      = */ PlotState.NO_PREVIOUS_CROP,  // nothing planted yet
                /* soilNitrogen    = */ initialSoilLevel
            );
        }

        return new FarmState(
            /* season          = */ 0,
            /* plotStates      = */ plotStates,
            /* remainingWater  = */ waterBudget
        );
    }

    /**
     * Computes the next FarmState after applying a crop assignment to the current state.
     * @param current    The current FarmState
     * @param assignment cropAssignment[i] = crop ID planted on plot i this season
     * @param cropList   Full list of available crops (used to look up water/nitrogen values)
     * @return           The resulting FarmState after applying the assignment
     * @throws IllegalArgumentException if assignment length doesn't match numPlots,
     *                                  or if a crop ID is out of range
     */
    public FarmState transition(FarmState current,int[] assignment,List<Crop> cropList) {
        validateAssignment(assignment, cropList.size());
        PlotState[] newPlotStates = new PlotState[numPlots];
        int totalWaterUsed = 0;

        for (int i = 0; i < numPlots; ++i) {
            Crop crop = cropList.get(assignment[i]);

            // Soil update
            // Each crop adds or removes nitrogen from the soil.
            // nitrogenImpact can be negative (e.g. heavy feeders like corn)
            // or positive (e.g. legumes like soybeans fix nitrogen).
            int currentSoil = current.getPlotStates()[i].soilNitrogenLevel;
            int newSoil     = clamp(currentSoil + crop.getNitrogenImpact(), SOIL_MIN, SOIL_MAX);

            newPlotStates[i] = new PlotState(
                /* lastCropId   = */ crop.getId(),
                /* soilNitrogen = */ newSoil
            );

            //Water tracking
            totalWaterUsed+=crop.getWaterPerAcre();
        }

        //Build and return the new state
        return new FarmState(
            /* season         = */ current.getSeason() + 1,
            /* plotStates     = */ newPlotStates,
            /* remainingWater = */ current.getRemainingWater() - totalWaterUsed
        );
    }

    /*  Helper — convenience overload without explicit cropList lookup
        (useful in tests where you pass crop objects directly)
        
     * Convenience method for a single-plot scenario or unit tests.
     * Computes how soil changes when a specific crop is planted.
     *
     * @param currentSoil  Current soil nitrogen level (0, 1, or 2)
     * @param crop         The crop being planted
     * @return             New soil nitrogen level after planting
     */
    public int computeNewSoil(int currentSoil, Crop crop) {
        validateSoilLevel(currentSoil, "currentSoil");
        return clamp(currentSoil + crop.getNitrogenImpact(), SOIL_MIN, SOIL_MAX);
    }
    //Getters — used by other solver classes
    public int getTotalSeasons() { return totalSeasons; }
    public int getNumPlots()     { return numPlots; }

    
    /* Private helpers
     * Clamps a value to [min, max].
     *
     * Used for soil nitrogen so it never goes below SOIL_LOW (0)
     * or above SOIL_HIGH (2), no matter what the crop's impact is.
     *
     * Example:
     *   clamp(3, 0, 2) → 2    (high-nitrogen crop on already-rich soil)
     *   clamp(-1, 0, 2) → 0   (heavy feeder on depleted soil)
     */
    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /** Throws if soilLevel is not 0, 1, or 2. */
    private void validateSoilLevel(int soilLevel, String paramName) {
        if (soilLevel < SOIL_MIN || soilLevel > SOIL_MAX) {
            throw new IllegalArgumentException(
                paramName + " must be 0 (Low), 1 (Medium), or 2 (High), got: " + soilLevel
            );
        }
    }

    /**
     * Validates that an assignment array has the correct length and
     * that every crop ID is within the valid range [0, numCrops).
     */
    private void validateAssignment(int[] assignment, int numCrops) {
        if (assignment == null) {
            throw new IllegalArgumentException("assignment array cannot be null");
        }
        if (assignment.length != numPlots) {
            throw new IllegalArgumentException(
                "assignment length must equal numPlots (" + numPlots + "), got: " + assignment.length
            );
        }
        for (int i = 0; i < assignment.length; i++) {
            if (assignment[i] < 0 || assignment[i] >= numCrops) {
                throw new IllegalArgumentException(
                    "assignment[" + i + "] = " + assignment[i] +
                    " is out of range [0, " + numCrops + ")"
                );
            }
        }
    }
}