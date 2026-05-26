package com.agrioptima.controller;

import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

@RestController
@RequestMapping("/api/plan")
@CrossOrigin(origins = "*")
public class TemporaryPlanController {

    private static final Logger logger = LoggerFactory.getLogger(TemporaryPlanController.class);
    
    // In-memory store for plans (for demo purposes)
    private final ConcurrentHashMap<String, Object> planStore = new ConcurrentHashMap<>();

    @PostMapping("/save")
    public String savePlan(@RequestBody Object planData) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        planStore.put(id, planData);
        logger.info("Plan saved with ID: {}", id);
        
        // Optional: Auto-cleanup after 1 hour (simplified - just log)
        return id;
    }

    @GetMapping("/{id}")
    public Object getPlan(@PathVariable String id) {
        Object plan = planStore.get(id);
        if (plan == null) {
            logger.warn("Plan not found for ID: {}", id);
            throw new RuntimeException("Plan not found");
        }
        logger.info("Plan retrieved for ID: {}", id);
        return plan;
    }
}