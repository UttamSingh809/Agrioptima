/* PlanReconstructor — rebuilds the optimal crop schedule from the DP backpointers.
* DPSolver.dp() computes the MAXIMUM PROFIT VALUE but does not directly
 * produce the schedule. The profit number alone is useless to a farmer —
 * they need to know: "What do I actually plant, and when?"
 * every time a better assignment was found for a state, it
 * was saved in the backpointer map:
 * backpointer.get(state)=the assignment that achieves best profit from state

* it produces List<SeasonPlan> -one entry per season, each containing
 *   the crop assignment and computed metrics for that season.
 * it follows iterative, not recursive approach.
 * */
package com.agrioptima.solver;

import com.agrioptima.model.Crop;
import com.agrioptima.model.FarmState;
import com.agrioptima.model.PlotState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
public class PlanReconstructor {
    private final StateManager stateManager;
    private final List<Crop> cropList;
    /*SeasonPlan — a single season's planting decision and its computed metrics.
    * One SeasonPlan is created per season during reconstruct().
    * The full schedule is a List<SeasonPlan>.*/
    public static class SeasonPlan {
        /** Season index (0-based). */
        public final int season;
        /**
         * Crop assignment for this season.
         * cropAssignment[i] = crop ID planted on plot i.
         */
        public final int[] cropAssignment;
        /** Crop names per plot — derived from cropAssignment for easy display. */
        public final String[] cropNames;
        /** Total water consumed by all plots this season. */
        public final int waterUsed;
        /** Total profit earned this season (before seasonal multiplier). */
        public final double profit;
        /** Soil nitrogen level per plot AFTER this season's planting. */
        public final int[] soilAfter;
        /** Season type label: "Kharif" (even) or "Rabi" (odd). */
        public final String seasonType;
        /** Seasonal price multiplier applied to profit this season. */
        public final double seasonalMultiplier;
        SeasonPlan(int season, int[] cropAssignment, String[] cropNames,
                   int waterUsed, double profit, int[] soilAfter,
                   double seasonalMultiplier) {
            this.season=season;
            this.cropAssignment=cropAssignment;
            this.cropNames=cropNames;
            this.waterUsed=waterUsed;
            this.profit=profit;
            this.soilAfter=soilAfter;
            this.seasonalMultiplier=seasonalMultiplier;
            this.seasonType=(season%2==0)?"Kharif":"Rabi";
        }
    }
    public PlanReconstructor(StateManager stateManager, List<Crop> cropList) {
        if (stateManager == null) throw new IllegalArgumentException("stateManager cannot be null");
        if (cropList == null || cropList.isEmpty()) {
            throw new IllegalArgumentException("cropList cannot be null or empty");
        }
        this.stateManager = stateManager;
        this.cropList     = cropList;
    }
     /* Reconstructs the optimal planting schedule by following backpointers
     * from the initial state through every season.
     * This is a simple iterative walk — no recursion needed here.
     * At each step:
     *   1. Look up the best assignment for the current state from backpointer
     *   2. Build a SeasonPlan with metrics for that season
     *   3. Transition to the next state
     *   4. Repeat until all seasons are processed*/
    public List<SeasonPlan> reconstruct(FarmState initialState,Map<FarmState,int[]> backpointer) {
        if (initialState==null){
            throw new IllegalArgumentException("initialState cannot be null");
        }
        if (backpointer==null){
            throw new IllegalArgumentException("backpointer cannot be null");
        }
        List<SeasonPlan> schedule=new ArrayList<>();
        FarmState current=initialState;
        int totalSeasons=stateManager.getTotalSeasons();
        int numPlots=stateManager.getNumPlots();
        for(int season=0;season<totalSeasons;season++) {
            //Look up the best assignment for this state
            int[] assignment=backpointer.get(current);
            if (assignment == null) {
                // No valid assignment exists for this state
                // Insert a fallow-everything season and continue
                assignment = buildFallowAssignment(numPlots);
            }
            //Compute metrics for this season
            double seasonalMultiplier=(season%2==1)?1.1:1.0;
            int totalWater=0;
            double totalProfit=0.0;
            String[] cropNames=new String[numPlots];
            int[] soilAfter=new int[numPlots];
            PlotState[] plotStates=current.getPlotStates();
            for(int i=0;i<numPlots;i++){
                Crop crop=cropList.get(assignment[i]);
                cropNames[i]=crop.getName();
                totalWater+=crop.getWaterPerAcre();
                totalProfit+=crop.getProfitPerAcre()*seasonalMultiplier;
                // Soil after this season = clamped transition
                int currentSoil=plotStates[i].getSoilNitrogen();
                soilAfter[i]=stateManager.computeNewSoil(currentSoil, crop);
            }
            // Record this season's plan
            schedule.add(new SeasonPlan(season,assignment.clone(),cropNames,totalWater,totalProfit,soilAfter,seasonalMultiplier));
            //Transition to next state
            current=stateManager.transition(current,assignment,cropList);
        }
        return schedule;
    }
    public void printSchedule(List<SeasonPlan> schedule){ // Prints a formatted seasonal planting calendar to stdout
        String[] soilLabels = {"Low", "Medium", "High"};
        System.out.println(" AgriOptima — Optimal Planting Schedule");
        for(SeasonPlan plan:schedule) {
            System.out.printf("Season %d (%s)  ×%.1f multiplier%n",plan.season+1,plan.seasonType,plan.seasonalMultiplier);
            for(int i=0;i<plan.cropNames.length;i++){
                Crop crop=cropList.get(plan.cropAssignment[i]);
                System.out.printf("  Plot %d : %-14s | Water: %3d | Soil after: %s%n",i,plan.cropNames[i],crop.getWaterPerAcre(),soilLabels[plan.soilAfter[i]]
                );
            }
            System.out.printf("  Season profit: ₹%,.0f   Water used: %d%n",plan.profit,plan.waterUsed);
        }
    }
    public void printSummary(List<SeasonPlan> schedule,FarmState initialState){ // To generate a concise summary of the full plan's key metrics.
        if (schedule.isEmpty()) {
            System.out.println("No schedule to summarise.");
            return;
        }
        double totalProfit=0;
        int totalWater=0;
        int numPlots=stateManager.getNumPlots();
        for(SeasonPlan plan:schedule){
            totalProfit+=plan.profit;
            totalWater+=plan.waterUsed;
        }
        // Soil trend: compare initial soil vs soil after last season
        SeasonPlan lastSeason=schedule.get(schedule.size() - 1);
        PlotState[] initialPlots=initialState.getPlotStates();
        System.out.println(" AgriOptima — Plan Summary\n");
        System.out.printf("  Total profit      : ₹%,.0f%n",totalProfit);
        System.out.printf("  Total water used  : %d units%n",totalWater);
        System.out.printf("  Seasons planned   : %d  (%d years)%n",schedule.size(),schedule.size()/2);
        System.out.printf("  Avg profit/season : ₹%,.0f%n",totalProfit/schedule.size());
        // Soil trend arrows
        StringBuilder soilTrend = new StringBuilder("  Soil trend        :");
        for(int i=0;i<numPlots;i++) {
            int before=initialPlots[i].getSoilNitrogen();
            int after=lastSeason.soilAfter[i];
            String arrow=(after>before)?"↑":(after<before)?"↓":"→";
            soilTrend.append(String.format("  Plot %d: %s", i, arrow));
        }
        System.out.println(soilTrend);
    }
    private int[] buildFallowAssignment(int numPlots) { // builds a fallback assignment where every plot is assigned Fallow (crop ID 0)
        // Used when the backpointer has no entry for a state (e.g. water exhausted).
        int[] fallow=new int[numPlots];
        return fallow;
    }
}