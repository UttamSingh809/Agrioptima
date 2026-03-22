/* AssignmentGenerator.java is useful because it tell does a provided crop asignment is valid or not.
    It will be used by DPsolver to generate valid crop assignments for the farm, which are then evaluated for profit and soil health.
*/
package com.agrioptima.solver;

import com.agrioptima.model.Crop;
import com.agrioptima.model.FarmState;

import java.util.ArrayList;
import java.util.List;
public class AssignmentGenerator {
    private final List<Crop> cropList; // Full list of available crops. Index = crop ID.
    private final int numPlots; // Number of farm plots (determines assignment array length).
    private final ConstraintEvaluator evaluator; // Used to validate each partial and complete assignment.
    public AssignmentGenerator(List<Crop> cropList, int numPlots, ConstraintEvaluator evaluator) {
        if (cropList == null || cropList.isEmpty()) {
            throw new IllegalArgumentException("cropList cannot be null or empty");
        }
        if (numPlots <= 0) {
            throw new IllegalArgumentException("numPlots must be > 0, got: "+numPlots);
        }
        if (evaluator == null) {
            throw new IllegalArgumentException("evaluator cannot be null");
        }
        this.cropList=cropList;
        this.numPlots=numPlots;
        this.evaluator=evaluator;
    }
    //getValidAssignments() generates and returns a list of all valid crop assignments for the given FarmState.
    // It initializes a working array for the current assignment and calls the recursive generate() method to fill it out.
    public List<int[]> getValidAssignments(FarmState state) {
        List<int[]> results=new ArrayList<>();
        int[] assignment=new int[numPlots];  // reused working array
        // Start recursive generation from plot 0, with 0 water used so far
        generate(state, assignment, 0, 0, results);
        return results;
    }
    // generate() is a recursive helper method that builds assignments one plot at a time.
    // applying constraints at each step to prune invalid branches early.
    private void generate(
            FarmState state,
            int[] assignment,
            int plotIndex,
            int waterUsedSoFar,
            List<int[]>results) {
        if (plotIndex==numPlots) {
            results.add(assignment.clone());  // snapshot — DO NOT add the array itself
            return;
        }
        for (int cropId=0;cropId<cropList.size();cropId++) {
            Crop crop=cropList.get(cropId);
            // every plot rotation check
            if (!evaluator.checkRotation(state.getPlotStates()[plotIndex],crop)) {
                continue;
            }
            // every plot soil check
            if (!evaluator.checkSoilSuitability(state.getPlotStates()[plotIndex],crop)) {
                continue;
            }
            // incremental water check
            int newWaterTotal=waterUsedSoFar+crop.getWaterPerAcre();
            if (newWaterTotal>state.getRemainingWater()) {
                continue;
            }
            assignment[plotIndex]=cropId;
            generate(state,assignment,plotIndex+1,newWaterTotal,results);
        }
    }
    // rawCombinationCount() returns the total number of possible assignments without any constraints, which is cropList.size()^numPlots.
    public int rawCombinationCount() {
        int count=1;
        for (int i=0;i<numPlots;i++) {
            count*=cropList.size();
        }
        return count;
    }
    //valudCount() returns the number of valid assignments for a given state,by calling getValidAssignments and returning its size.
    public int validCount(FarmState state) {
        return getValidAssignments(state).size();
    }
    //formatAssignment() formats an assignment into a human-readable string for logging and demos.
    public String formatAssignment(int[] assignment) {
        StringBuilder sb=new StringBuilder("[");
        for (int i=0;i<assignment.length;i++) {
            sb.append("Plot ").append(i).append(": ")
              .append(cropList.get(assignment[i]).getName());
            if (i<assignment.length-1)sb.append(" | ");
        }
        sb.append("]");
        return sb.toString();
    }
    //below are the getter methods.
    public int getNumPlots(){ return numPlots;}
    public List<Crop> getCropList(){return cropList;}
}