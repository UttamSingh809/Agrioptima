package com.agrioptima.controller;

import com.agrioptima.service.MarketDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/market")
@CrossOrigin(origins = "*")
public class MarketController {
    private static final Logger logger = LoggerFactory.getLogger(MarketController.class);
    @Autowired
    private MarketDataService marketDataService;
    
    /**
     * Get current price for a crop
     * GET /api/market/price?crop=Rice&mandi=azadpur
     */
    @GetMapping("/price")
    public ResponseEntity<Map<String, Object>> getCurrentPrice(
            @RequestParam(name = "crop", required = true) String crop,
            @RequestParam(name = "mandi", defaultValue = "azadpur") String mandi) {
        
        try {
            Map<String, Object> priceData = marketDataService.getCurrentPrice(crop, mandi);
            return ResponseEntity.ok(priceData);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch price data");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Get price history for trends
     * GET /api/market/history?crop=Rice
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getPriceHistory(
            @RequestParam(name = "crop", required = true) String crop) {
        
        try {
            List<Integer> history = marketDataService.getPriceHistory(crop);
            Map<String, Object> response = new HashMap<>();
            response.put("crop", crop);
            response.put("history", history);
            response.put("months", Arrays.asList("Jul", "Aug", "Sep", "Oct", "Nov", "Dec", "Jan", "Feb", "Mar", "Apr", "May", "Jun"));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch price history");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Get price prediction
     * GET /api/market/predict?crop=Rice&months=3
     */
    @GetMapping("/predict")
    public ResponseEntity<Map<String, Object>> predictPrice(
            @RequestParam(name = "crop", required = true) String crop,
            @RequestParam(name = "months", defaultValue = "3") int months) {
        
        try {
            Map<String, Object> prediction = marketDataService.predictFuturePrice(crop, months);
            return ResponseEntity.ok(prediction);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to generate prediction");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Compare with MSP
     * GET /api/market/compare?crop=Rice
     */
    @GetMapping("/compare")
    public ResponseEntity<Map<String, Object>> compareWithMSP(
            @RequestParam(name = "crop", required = true) String crop) {
        
        try {
            Map<String, Object> priceData = marketDataService.getCurrentPrice(crop, "azadpur");
            Integer currentPrice = (Integer) priceData.get("price");
            Map<String, Object> comparison = marketDataService.compareWithMSP(crop, currentPrice);
            return ResponseEntity.ok(comparison);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to compare with MSP");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Get dynamic seasonal multipliers
     * GET /api/market/multipliers?crop=Rice
     */
    @GetMapping("/multipliers")
    public ResponseEntity<Map<String, Double>> getMultipliers(
            @RequestParam(name = "crop", required = true) String crop) {
        
        try {
            Map<String, Double> multipliers = marketDataService.getDynamicMultipliers(crop);
            return ResponseEntity.ok(multipliers);
        } catch (Exception e) {
            Map<String, Double> errorResponse = new HashMap<>();
            errorResponse.put("error", -1.0);
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Get all available crops with MSP data
     * GET /api/market/crops
     */
    @GetMapping("/crops")
    public ResponseEntity<Map<String, Object>> getAvailableCrops() {
        
        try {
            Map<String, Object> response = new HashMap<>();
            List<Map<String, Object>> crops = new ArrayList<>();
            
            String[] cropNames = {"Rice", "Wheat", "Maize", "Soybean", "Sugarcane", "Cotton"};
            int[] mspValues = {2183, 2125, 2090, 4300, 3550, 7020};
            
            for (int i = 0; i < cropNames.length; i++) {
                Map<String, Object> crop = new HashMap<>();
                crop.put("name", cropNames[i]);
                crop.put("msp", mspValues[i]);
                crops.add(crop);
            }
            
            response.put("crops", crops);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch crop list");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Get available markets/regions
     * GET /api/market/markets
     */
    @GetMapping("/markets")
    public ResponseEntity<Map<String, Object>> getAvailableMarkets() {
        
        try {
            Map<String, Object> response = new HashMap<>();
            List<Map<String, String>> markets = new ArrayList<>();
            
            Map<String, String> market1 = new HashMap<>();
            market1.put("code", "azadpur");
            market1.put("name", "Azadpur Mandi, Delhi");
            markets.add(market1);
            
            Map<String, String> market2 = new HashMap<>();
            market2.put("code", "ghazipur");
            market2.put("name", "Ghazipur Mandi, Uttar Pradesh");
            markets.add(market2);
            
            Map<String, String> market3 = new HashMap<>();
            market3.put("code", "nashik");
            market3.put("name", "Nashik Mandi, Maharashtra");
            markets.add(market3);
            
            Map<String, String> market4 = new HashMap<>();
            market4.put("code", "indore");
            market4.put("name", "Indore Mandi, Madhya Pradesh");
            markets.add(market4);
            
            Map<String, String> market5 = new HashMap<>();
            market5.put("code", "ludhiana");
            market5.put("name", "Ludhiana Mandi, Punjab");
            markets.add(market5);
            
            response.put("markets", markets);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch market list");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Get complete market intelligence dashboard data
     * GET /api/market/dashboard?crop=Rice&mandi=azadpur
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardData(
            @RequestParam(name = "crop", required = true) String crop,
            @RequestParam(name = "mandi", defaultValue = "azadpur") String mandi) {
        
        try {
            Map<String, Object> dashboard = new HashMap<>();
            
            // Current price
            dashboard.put("currentPrice", marketDataService.getCurrentPrice(crop, mandi));
            
            // Price history
            dashboard.put("priceHistory", marketDataService.getPriceHistory(crop));
            
            // Prediction for next 2 months
            dashboard.put("prediction", marketDataService.predictFuturePrice(crop, 2));
            
            // MSP comparison
            Map<String, Object> priceData = marketDataService.getCurrentPrice(crop, mandi);
            Integer currentPrice = (Integer) priceData.get("price");
            dashboard.put("mspComparison", marketDataService.compareWithMSP(crop, currentPrice));
            
            // Seasonal multipliers
            dashboard.put("multipliers", marketDataService.getDynamicMultipliers(crop));
            
            // Add timestamp
            dashboard.put("timestamp", new Date().toString());
            
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch dashboard data");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Bulk price check for multiple crops
     * GET /api/market/bulk?crops=Rice,Wheat,Maize&mandi=azadpur
     */
    @GetMapping("/bulk")
    public ResponseEntity<Map<String, Object>> getBulkPrices(
            @RequestParam(name = "crops", required = true) String crops,
            @RequestParam(name = "mandi", defaultValue = "azadpur") String mandi) {
        
        try {
            String[] cropArray = crops.split(",");
            Map<String, Object> bulkPrices = new HashMap<>();
            List<Map<String, Object>> priceList = new ArrayList<>();
            
            for (String crop : cropArray) {
                Map<String, Object> priceData = marketDataService.getCurrentPrice(crop.trim(), mandi);
                priceList.add(priceData);
            }
            
            bulkPrices.put("prices", priceList);
            bulkPrices.put("mandi", mandi);
            bulkPrices.put("timestamp", new Date().toString());
            
            return ResponseEntity.ok(bulkPrices);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch bulk prices");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Health check endpoint
     * GET /api/market/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "Market Intelligence API");
        status.put("version", "1.0");
        return ResponseEntity.ok(status);
    }
    // Add this field with the other fields
private final ConcurrentHashMap<String, Object> planStore = new ConcurrentHashMap<>();

// Add these methods at the end of the class, before the last closing brace

/**
 * Save crop plan for QR code
 * POST /api/market/save-plan
 */
@PostMapping("/save-plan")
public ResponseEntity<Map<String, String>> savePlan(@RequestBody Map<String, Object> planData) {
    try {
        String id = UUID.randomUUID().toString().substring(0, 8);
        // Store as JSON string to avoid serialization issues
        String jsonData = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(planData);
        planStore.put(id, jsonData);
        logger.info("✅ Plan saved with ID: {}", id);
        
        Map<String, String> response = new HashMap<>();
        response.put("id", id);
        return ResponseEntity.ok(response);
    } catch (Exception e) {
        logger.error("Failed to save plan: {}", e.getMessage(), e);
        return ResponseEntity.status(500).build();
    }
}

/**
 * Load crop plan by ID for QR code
 * GET /api/market/load-plan/{id}
 */
@GetMapping("/load-plan/{id}")
public ResponseEntity<String> loadPlan(@PathVariable String id) {
    try {
        logger.info("📋 Loading plan with ID: {}", id);
        String planData =(String) planStore.get(id);
        
        if (planData == null) {
            logger.warn("⚠️ Plan not found for ID: {}", id);
            return ResponseEntity.notFound().build();
        }
        
        logger.info("✅ Plan found for ID: {}", id);
        return ResponseEntity.ok(planData);
    } catch (Exception e) {
        logger.error("Failed to load plan: {}", e.getMessage(), e);
        return ResponseEntity.status(500).build();
    }
}

/**
 * Test endpoint to verify controller works
 * GET /api/market/test
 */
@GetMapping("/test")
public ResponseEntity<String> test() {
    return ResponseEntity.ok("MarketController is working!");
}
}