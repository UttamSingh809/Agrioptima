package com.agrioptima.model;

import java.util.Arrays;

/**
 * Represents a single decision — which crops to plant in the current season.
 * This intermediate model separates validation logic from the actual state transitions.
 * It is strictly immutable to ensure safe exploration within the DP solver.
 */
public class CropAssignment {

    private final int season;
    private final int[] cropIds; // cropIds[plot] = crop ID for that plot
    private final int totalProfit;
    private final int totalWater;

    /**
     * Constructs a new crop assignment decision.
     * Applies defensive copying to the array to prevent external mutation during DP search.
     */
    public CropAssignment(int season, int[] cropIds, int totalProfit, int totalWater) {
        this.season = season;
        this.totalProfit = totalProfit;
        this.totalWater = totalWater;

        // Defensive copy to ensure immutability
        this.cropIds = cropIds != null ? Arrays.copyOf(cropIds, cropIds.length) : new int[0];
    }

    // --- Getters ---

    public int getSeason() { return season; }
    public int getTotalProfit() { return totalProfit; }
    public int getTotalWater() { return totalWater; }

    /**
     * Returns a copy of the assigned crop IDs to maintain encapsulation.
     */
    public int[] getCropIds() {
        return Arrays.copyOf(cropIds, cropIds.length);
    }

    // --- Validation Logic ---

    /**
     * Validates this assignment against resource limits and agronomic constraints.
     * Safely handles fallow land (-1) without crashing.
     *
     * @param currentState The current state of the farm before this assignment.
     * @param crops        The reference array of all available Crop objects.
     * @return true if the assignment is completely valid, false otherwise.
     */
    public boolean isValid(FarmState currentState, Crop[] crops) {
        // 1. Check strict water constraint
        if (totalWater > currentState.getRemainingWater()) {
            return false;
        }

        // 2. Check plot-specific constraints (rotation and soil)
        for (int i = 0; i < cropIds.length; i++) {
            int currentCrop = cropIds[i];

            // Fallow land (-1) is always valid from an agronomic standpoint
            if (currentCrop == -1) {
                continue;
            }

            int lastCrop = currentState.getLastCropId(i);
            Crop cropData = crops[currentCrop];

            // Check rotation constraints (cannot plant the same crop if gap > 0)
            if (lastCrop == currentCrop && cropData.getMinRotationGap() > 0) {
                return false; // Violates crop rotation rule
            }

            // Check soil compatibility
            int soilLevel = currentState.getSoilNitrogenLevel(i);
            if (!cropData.isCompatibleWithSoil(soilLevel)) {
                return false; // Soil level is not suitable for this crop
            }
        }

        return true;
    }
}