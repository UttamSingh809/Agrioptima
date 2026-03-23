package com.agrioptima.service;

import com.agrioptima.dto.OptimizationRequest;
import com.agrioptima.dto.OptimizationResult;
import com.agrioptima.model.Crop;
import com.agrioptima.model.FarmState;
import com.agrioptima.solver.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * OptimizationService.java
 *
 * Purpose: Orchestrates the interaction between the controller and the core DP solver.
 * It takes input from the API, initializes solver components, runs the optimization,
 * reconstructs the plan, and prepares the result for the frontend.
 */
@Service
public class OptimizationService {

    private static final Logger logger = LoggerFactory.getLogger(OptimizationService.class);

    /**
     * Executes the core optimization process.
     *
     * @param request The input parameters for the optimization (crops, farm config, etc.).
     * @return An OptimizationResult containing the schedule, profit, and metrics.
     */
    public OptimizationResult runOptimization(OptimizationRequest request) {
        logger.info("Starting optimization for request: NumPlots={}, TotalSeasons={}, WaterBudget={}",
                request.getNumPlots(), request.getTotalSeasons(), request.getWaterBudget());

        try {
            // 1. Extract input data
            List<Crop> crops = request.getCrops();
            int numPlots = request.getNumPlots();
            int totalSeasons = request.getTotalSeasons();
            int waterBudget = request.getWaterBudget();
            int initialSoil = request.getInitialSoil();

            // Validate input (basic check)
            if (crops == null || crops.isEmpty() || numPlots <= 0 || totalSeasons <= 0) {
                throw new IllegalArgumentException("Invalid input: crops list is empty or farm dimensions are invalid.");
            }

            // 2. Initialize solver components
            // Note: Penalty weights can be configurable, perhaps via application.properties
            ConstraintEvaluator evaluator = new ConstraintEvaluator(2000.0, 500.0);
            StateManager stateManager = new StateManager(numPlots, totalSeasons);
            AssignmentGenerator generator = new AssignmentGenerator(crops, numPlots, evaluator);

            // The Main Engine
            DPSolver solver = new DPSolver(stateManager, generator, evaluator, crops);

            // 3. Run the solver
            logger.debug("Invoking DPSolver.solve()...");
            double maxProfit = solver.solve(waterBudget, initialSoil);
            logger.debug("DPSolver.solve() completed. Max Profit: {}", maxProfit);

            // 4. Reconstruct the plan using the backpointer map from the solver
            logger.debug("Initializing PlanReconstructor...");
            PlanReconstructor reconstructor = new PlanReconstructor(
                    solver.getBackpointer(), // Critical: use the map from the solver instance
                    solver.getInitialState(), // Critical: use the initial state from the solver instance
                    crops,
                    stateManager
            );

            logger.debug("Reconstructing schedule...");
            String[][] schedule = reconstructor.reconstructSchedule(solver.getInitialState());
            logger.debug("Schedule reconstruction completed.");

            // Calculate metrics based on the reconstructed plan and solver state
            PlanReconstructor.PlanMetrics metrics = reconstructor.calculateMetrics(solver.getInitialState(), schedule);

            // 5. Prepare and return the final result object
            OptimizationResult result = new OptimizationResult(schedule, maxProfit, metrics);
            logger.info("Optimization completed successfully. Max Profit: {}", maxProfit);
            return result;

        } catch (Exception e) {
            // Log the error with stack trace for debugging
            logger.error("Optimization failed due to an exception: {}", e.getMessage(), e);
            // Re-throw as a runtime exception to be handled by a global exception handler if needed
            throw new RuntimeException("Optimization process failed: " + e.getMessage(), e);
        }
    }
}