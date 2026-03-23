package com.agrioptima.controller;

import com.agrioptima.dto.OptimizationRequest;
import com.agrioptima.dto.OptimizationResult;
import com.agrioptima.service.OptimizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;

/**
 * OptimizationController.java
 *
 * Purpose: Defines the REST API endpoints for the AgriOptima optimization service.
 * It receives requests, delegates the processing to the OptimizationService,
 * and returns the results or appropriate HTTP status codes.
 */
@RestController // Indicates this class handles REST API requests
@RequestMapping("/api/optimize") // Base URL path for all endpoints in this controller
public class OptimizationController {

    private static final Logger logger = LoggerFactory.getLogger(OptimizationController.class);

    @Autowired // Injects the OptimizationService bean managed by Spring
    private OptimizationService optimizationService;

    /**
     * Endpoint to trigger the optimization process.
     * Expects a JSON payload conforming to the OptimizationRequest structure.
     *
     * @param request The request body containing farm/crop parameters.
     * @return ResponseEntity containing the OptimizationResult and HTTP status.
     */
    @PostMapping // Maps HTTP POST requests to this method
    public ResponseEntity<OptimizationResult> runOptimization(@RequestBody OptimizationRequest request) {
        logger.info("Received optimization request: NumPlots={}, TotalSeasons={}, WaterBudget={}",
                request.getNumPlots(), request.getTotalSeasons(), request.getWaterBudget());
        logger.info("Crops received: {}", request.getCrops().size());

        try {
            // Delegate the core logic to the service layer
            OptimizationResult result = optimizationService.runOptimization(request);

            // If successful, return the result with HTTP 200 OK status
            logger.info("Optimization request processed successfully. Returning result.");
            return ResponseEntity.ok(result); // Standard response for success with content

        } catch (IllegalArgumentException e) {
            // Handle bad requests (e.g., invalid input data)
            logger.warn("Bad request for optimization: {}", e.getMessage());
            return ResponseEntity.badRequest().build(); // HTTP 400 Bad Request

        } catch (RuntimeException e) {
            // Handle general runtime errors from the service (e.g., solver failure)
            logger.error("Runtime error during optimization: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // HTTP 500 Internal Server Error

        } catch (Exception e) {
            // Catch any other unexpected errors
            logger.error("Unexpected error during optimization: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // HTTP 500
        }
    }

    // Optional: Add a GET endpoint for health check or documentation
    @GetMapping("/status") // GET /api/optimize/status
    public ResponseEntity<String> getStatus() {
        return ResponseEntity.ok("AgriOptima Optimization Service is running.");
    }
}