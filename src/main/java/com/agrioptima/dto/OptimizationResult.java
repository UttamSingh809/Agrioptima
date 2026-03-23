package com.agrioptima.dto;

import com.agrioptima.solver.PlanReconstructor;

/**
 * OptimizationResult.java
 *
 * Purpose: Represents the output data structure for the optimization API endpoint.
 * It holds the optimal schedule, the calculated maximum profit, and associated metrics.
 */
public class OptimizationResult {

    private String[][] schedule; // [season][plot] -> String (Crop Name)
    private double maxProfit; // The maximum profit calculated
    private PlanReconstructor.PlanMetrics metrics; // Additional metrics like water used, soil change

    // Default constructor required by Jackson (JSON serializer)
    public OptimizationResult() {}

    // Constructor with all fields
    public OptimizationResult(String[][] schedule, double maxProfit, PlanReconstructor.PlanMetrics metrics) {
        this.schedule = schedule;
        this.maxProfit = maxProfit;
        this.metrics = metrics;
    }

    // Getters and Setters
    public String[][] getSchedule() {
        return schedule;
    }

    public void setSchedule(String[][] schedule) {
        this.schedule = schedule;
    }

    public double getMaxProfit() {
        return maxProfit;
    }

    public void setMaxProfit(double maxProfit) {
        this.maxProfit = maxProfit;
    }

    public PlanReconstructor.PlanMetrics getMetrics() {
        return metrics;
    }

    public void setMetrics(PlanReconstructor.PlanMetrics metrics) {
        this.metrics = metrics;
    }
}