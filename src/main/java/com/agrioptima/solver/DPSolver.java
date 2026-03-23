/* DPsolver.java -- the main Dynamic Programming engine for AgriOptima.
* it is using recurrence relation, meaning: dp(state) = max over all valid assignments: profit(assignment) + dp( transition(state, assignment)
* base case for this recurrence relation is dp(state)=0 (when state.season == totalSeasons)
* The best total profit from this state equals the best immediate
 * profit from any valid crop assignment this season, plus the best
 * future profit from whatever state that assignment leads to.
 * This works because our farming problem has:
 *   1. Optimal substructure  — best plan for seasons 2..N depends only on the state at season 2, not on how we got there.
 *   2. Overlapping subproblems — the same FarmState can be reached via many different paths; memoization solves it once
 *   3. Sequential decisions   — each season's choice affects the next stateThis works because our farming problem has:
 * How our DP algo will use memorization:
 * memo: HashMap<FarmState, Double>
 * Key   = FarmState (season + plotStates[] + remainingWater)
 * Value = best total profit achievable from that state onward
 * backpointer: HashMap<FarmState, int[]>
 * Key   = FarmState
 * Value = the assignment that achieved the best profit from that state (used by PlanReconstructor to rebuild the optimal schedule)
 * CRITICAL: FarmState.equals() and FarmState.hashCode() MUST be correctly
 * implemented — they determine whether two states are treated as the same
 * key in the HashMap. If they are wrong, memoization silently breaks.
*/
package com.agrioptima.solver;

import com.agrioptima.model.Crop;
import com.agrioptima.model.FarmState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class DPSolver {
    private final StateManager stateManager;
    private final AssignmentGenerator generator;
    private final ConstraintEvaluator evaluator;
    private final List<Crop> cropList;
    private final Map<FarmState, Double>  memo; // Memoization table.
     // memo.get(state) = best total profit achievable from that state to the end. Populated lazily during the dp() recursion.
    private final Map<FarmState, int[]> backpointer; //Backpointer store.
    // backpointer.get(state) = the crop assignment that leads to the best profit from that state, Used by PlanReconstructor to rebuild the full seasonal schedule.
    private FarmState initialState; // The initial state used in the most recent call to solve()
    private int statesExplored; // Number of unique states solved (memo hits + new computations)
    private int memoHits; // Number of times a memo cache hit occurred

    public DPSolver(StateManager stateManager,AssignmentGenerator generator,ConstraintEvaluator evaluator,List<Crop>cropList) {
        if (stateManager==null)throw new IllegalArgumentException("stateManager cannot be null");
        if (generator==null)throw new IllegalArgumentException("generator cannot be null");
        if (evaluator==null)throw new IllegalArgumentException("evaluator cannot be null");
        if (cropList==null||cropList.isEmpty())throw new IllegalArgumentException("cropList cannot be null or empty");
        this.stateManager=stateManager;
        this.generator=generator;
        this.evaluator=evaluator;
        this.cropList=cropList;
        this.memo=new HashMap<>();
        this.backpointer=new HashMap<>();
    }
    // Runs the DP solver and returns the maximum total profit achievable
    //     * over all seasons, starting from the given farm configuration.
    //     * Steps:
    //     *   1. Reset memo, backpointer, and statistics from any previous run
    //     *   2. Create the initial FarmState
    //     *   3. Call dp(initialState) — the recursive DP engine
    //     *   4. Return the result
    //     * After this returns, call:
    //     *   getInitialState() to get the starting state for PlanReconstructor
    //     *   getBackpointer() to pass to PlanReconstructor
    //     *   printStats() to log performance data
    public double solve(int waterBudget,int initialSoilLevel){
        memo.clear();
        backpointer.clear();
        statesExplored=0;
        memoHits=0;
        initialState=stateManager.createInitialState(waterBudget,initialSoilLevel);
        return dp(initialState);
    }
    // the core recurrance function,
    //Computes the maximum total profit achievable from the given state to the end of the planning horizon.
    private double dp(FarmState state) {
        if (state.getSeason() == stateManager.getTotalSeasons()) { // dp(state) = 0 if season == totalSeasons
            return 0.0;
        }
        if (memo.containsKey(state)) { // it checks have we solved this state before?
            // if yes, return the stored answer instantly — no recomputation needed.
            // FarmState.equals() and hashCode() make this work correctly.
            memoHits++;
            return memo.get(state);
        }
        statesExplored++;
        List<int[]>validAssignments=generator.getValidAssignments(state); // in this list i have passed rotation, soil, and water checks.
        if (validAssignments.isEmpty()) { // it handles if no valid assignment exists
            memo.put(state,0.0);
            // No backpointer entry — PlanReconstructor handles missing entries
            return 0.0;
        }
        double bestTotal=Double.NEGATIVE_INFINITY; // Evaluate every valid assignment and pick the best
        int[]  bestAssignment=null;
        for (int[] assignment:validAssignments) {
            // Step 1: Compute immediate profit for this assignment
            // This is the profit earned THIS season from this crop plan.
            double immediateProfit=calculateImmediateProfit(state,assignment);
            // Step 2: Add the multi-objective score adjustment
            // Incorporates water penalties and soil health rewards.
            // For valid assignments, water penalty is always 0.
            double scoreAdjustment = evaluator.scorePenalty(state,assignment,cropList);
            // Step 3: Compute the next state
            // What does the farm look like after planting this assignment?
            FarmState nextState=stateManager.transition(state,assignment,cropList);
            // Step 4: Recurse — best future profit from next state
            // THIS IS THE RECURRENCE: total = immediate + future
            double futureProfit=dp(nextState);
            double totalProfit=immediateProfit+scoreAdjustment+futureProfit;
            // Step 5: Update best if this assignment is better
            if (totalProfit>bestTotal) {
                bestTotal=totalProfit;
                bestAssignment=assignment;
            }
        }
        //Store result in memo and backpointer, memo: so future visits to this state return instantly and backpointer:so PlanReconstructor can trace the optimal path
        memo.put(state,bestTotal);
        backpointer.put(state,bestAssignment);
        return bestTotal;
    }
    // Profit Calculator:
    // profit=crop.profitPerAcre * seasonalMultiplier(state.season)
    // Seasonal multiplier reflects market price fluctuations:
    // Kharif seasons(even index: 0,2,4,...)=1.0(base price)
    // Rabi seasons(odd index: 1,3,5,...)=1.1(10% premium) bcoz: winter crops
    // (Rabi) often command slightly higher prices due to lower supply.
    private double calculateImmediateProfit(FarmState state, int[] assignment) {
        double seasonalMultiplier=getSeasonalMultiplier(state.getSeason());
        double totalProfit=0.0;
        for (int i=0;i<assignment.length;i++) {
            Crop crop=cropList.get(assignment[i]);
            totalProfit+=crop.getProfitPerAcre()*seasonalMultiplier;
        }
        return totalProfit;
    }
    private double getSeasonalMultiplier(int season){
        // Rabi seasons: carry a 10% market premium
        return (season%2==1)?1.1:1.0;
    }

    // OPTIONAL: printStats() a fun build just to showcase DP solver work in AgriOptima if UI is not prepared.
    public void printStats() {
        System.out.println("DPSolver Statistics==");
        System.out.printf("Total seasons:%d%n",stateManager.getTotalSeasons());
        System.out.printf("  Unique states solved:%,d%n",statesExplored);
        System.out.printf("  Memo cache hits:%,d%n",memoHits);
        System.out.printf("  Memo table size:%,d%n",memo.size());
        System.out.printf("  Raw combinations/state:%,d(%d crops ^ %d plots)%n",
                generator.rawCombinationCount(),
                cropList.size(),
                stateManager.getNumPlots());
        double hitRate = (statesExplored+memoHits)== 0?0
                : (100.0*memoHits/(statesExplored+memoHits));
        System.out.printf("  Cache hit rate: %.1f%%%n",hitRate);
    }
    // below are the getter methods.
    public FarmState getInitialState() {
        if (initialState == null) {
            throw new IllegalStateException("solve() must be called before getInitialState()");
        }
        return initialState;
    }
    public Map<FarmState, int[]> getBackpointer() {
        return backpointer;
    }
    public Map<FarmState, Double> getMemo() {
        return memo;
    }
    public int getStatesExplored() { return statesExplored; }
    public int getMemoHits()       { return memoHits; }
}
