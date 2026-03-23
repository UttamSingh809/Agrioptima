/* ConstraintEvaluator.java is useful for this project bcoz it validates crop assignments is legal or not for a given farm state
It enforces water limit, crop rotation, and soil suitabitility.
*/
package com.agrioptima.solver;

import com.agrioptima.model.Crop;
import com.agrioptima.model.FarmState;
import com.agrioptima.model.FarmState.PlotState;

import java.util.List;
public class ConstraintEvaluator {
    private static final int NO_CROP=-1; // NO_CROP flag to indicate(-1) no previous crop planted on a plot (for rotation checks)

    private final double waterPenaltyWeight; // Penalty per unit of water used over the remaining budget.
    // Higher=solver strongly prefers to stay within water limits.

    private final double soilRewardWeight; // Reward per unit of soil nitrogen improvement.
    // Higher = more emphasis on soil health.
    public ConstraintEvaluator() {
        this(2000.0,500.0);// Default weights chosen to make water violations very costly, while still rewarding soil improvements.
    }
    public ConstraintEvaluator(double waterPenaltyWeight,double soilRewardWeight) {
        if (waterPenaltyWeight<0) {
            throw new IllegalArgumentException("waterPenaltyWeight must be ≥ 0");
        }
        if (soilRewardWeight<0) {
            throw new IllegalArgumentException("soilRewardWeight must be ≥ 0");
        }
        this.waterPenaltyWeight=waterPenaltyWeight;
        this.soilRewardWeight=soilRewardWeight;
    }
    /* isValid() checks wheather a proposed crop plan (assignment) is legally acceptable for the current farm state.
    it will only return true if all constraints are satisfied, otherwise false.
    */
    public boolean isValid(FarmState state,int[] assignment,List<Crop>cropList) {
        // Constraint 1: water limit
        if (!checkWaterLimit(state,assignment,cropList)) {
            return false;
        }
        PlotState[] plotStates=state.getPlotStates();

        for (int i=0;i<assignment.length;i++) {
            Crop crop=cropList.get(assignment[i]);
            PlotState plotState=plotStates[i];
            // Constraint 2: crop rotation
            if (!checkRotation(plotState,crop)) {
                return false;
            }
            // Constraint 3: soil suitability
            if (!checkSoilSuitability(plotState,crop)) {
                return false;
            }
        }
        return true;
    }
    public boolean checkWaterLimit(FarmState state,int[] assignment,List<Crop>cropList) {
        int totalWaterNeeded=0;
        for (int i=0;i<assignment.length; i++) {
            totalWaterNeeded+=cropList.get(assignment[i]).getWaterPerAcre(); // sum up water needs for all assigned crops
        }
        return totalWaterNeeded<=state.getRemainingWater(); // valid if total water needed is within the remaining water budget
    }
    public boolean checkRotation(PlotState plotState,Crop crop) {
        int lastCropId=plotState.lastCropId;
        // No previous crop planted: rotation rule doesn't apply
        if (lastCropId==NO_CROP) {
            return true;
        }
        // Same crop as last season && crop has a rotation restriction
        if (lastCropId==crop.getId()&&crop.getMinRotationGap()>0) {
            return false;
        }
        return true;
    }
    public boolean checkSoilSuitability(PlotState plotState,Crop crop) {
        return crop.isCompatibleWithSoil(plotState.soilNitrogenLevel);
    }
    /*  scorePenalty() gives a score adjustment that is added to raw profit:
    Subtracts points if water is overused.
    Adds points if crops improve soil nitrogen.
    So total optimization score becomes: finalScore = rawProfit + adjustment
    where adjustment is: - (waterPenaltyWeight * waterOveruse) + (soilRewardWeight * totalSoilGain)
    */
    public double scorePenalty(FarmState state, int[] assignment, List<Crop>cropList) {
        int totalWaterUsed=0;
        int totalSoilGain=0;
        for (int i=0;i<assignment.length;i++) {
            Crop crop=cropList.get(assignment[i]);
            totalWaterUsed+=crop.getWaterPerAcre();
            // Only count positive nitrogen contributions as a reward
            int nitrogenImpact=crop.getNitrogenImpact();
            if(nitrogenImpact>0) {
                totalSoilGain+=nitrogenImpact;
            }
        }
        // Water overuse penalty — 0 for valid assignments (within budget)
        int waterOveruse=Math.max(0,totalWaterUsed-state.getRemainingWater());
        return (-waterPenaltyWeight*waterOveruse)+(soilRewardWeight*totalSoilGain);
    }
    /* describeViolation() checks the assignment and returns a clear text message:
    1. if water exceeds budget, returns a WATER message.
    2. else if rotation rule fails on a plot, returns a ROTATION message.
    3. else if soil suitability fails, returns a SOIL message.
    4. if nothing fails, returns valid */
    public String describeViolation(FarmState state,int[] assignment,List<Crop>cropList) {
        // Check water
        int totalWater=0;
        for (int i=0;i<assignment.length;i++) {
            totalWater+=cropList.get(assignment[i]).getWaterPerAcre();
        }
        if (totalWater>state.getRemainingWater()) {
            return String.format(
                    "WATER: needs %d units but only %d remaining.",
                    totalWater,state.getRemainingWater()
            );
        }
        // Check per-plot constraints
        PlotState[] plotStates=state.getPlotStates();
        for (int i=0;i<assignment.length;i++) {
            Crop crop=cropList.get(assignment[i]);
            PlotState plot=plotStates[i];
            if (!checkRotation(plot, crop)) {
                String lastCropName = (plot.lastCropId == NO_CROP) ? "None" : cropList.get(plot.lastCropId).getName();
                return String.format("ROTATION: Plot %d — %s cannot follow %s (minRotationGap=%d).",
                        i, crop.getName(), lastCropName, crop.getMinRotationGap());
            }
            if (!checkSoilSuitability(plot,crop)) {
                return String.format(
                        "SOIL: Plot %d — %s is not compatible with soil level %d.",
                        i, crop.getName(), plot.soilNitrogenLevel);
            }
        }
        return "VALID";
    }
    // below are the getter methods 
    public double getWaterPenaltyWeight() { return waterPenaltyWeight; }
    public double getSoilRewardWeight()   { return soilRewardWeight; }
}