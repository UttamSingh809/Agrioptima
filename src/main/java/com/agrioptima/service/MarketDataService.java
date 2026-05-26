package com.agrioptima.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class MarketDataService {

    private static final Logger logger = LoggerFactory.getLogger(MarketDataService.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private CsvMarketDataService csvMarketDataService;

    private final Map<String, Map<String, Object>> mockPriceCache;
    private final Map<String, Integer> mspData;

    public MarketDataService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.mockPriceCache = initializeMockData();
        this.mspData = getMSPData();
    }

    private Map<String, Map<String, Object>> initializeMockData() {
        Map<String, Map<String, Object>> mockData = new HashMap<>();
        
        // Tomato prices
        Map<String, Object> tomatoPrices = new HashMap<>();
        tomatoPrices.put("chakrata", 1800);
        tomatoPrices.put("dehradoon", 2000);
        tomatoPrices.put("trend", Arrays.asList(1500, 1600, 1700, 1800, 1900, 2000));
        mockData.put("Tomato", tomatoPrices);
        
        // Onion prices
        Map<String, Object> onionPrices = new HashMap<>();
        onionPrices.put("chakrata", 1500);
        onionPrices.put("dehradoon", 1600);
        onionPrices.put("trend", Arrays.asList(1300, 1400, 1450, 1500, 1550, 1600));
        mockData.put("Onion", onionPrices);
        
        // Potato prices
        Map<String, Object> potatoPrices = new HashMap<>();
        potatoPrices.put("chakrata", 800);
        potatoPrices.put("dehradoon", 900);
        potatoPrices.put("trend", Arrays.asList(700, 750, 800, 850, 880, 900));
        mockData.put("Potato", potatoPrices);
        
        // Ginger prices
        Map<String, Object> gingerPrices = new HashMap<>();
        gingerPrices.put("chakrata", 2500);
        gingerPrices.put("dehradoon", 2800);
        gingerPrices.put("trend", Arrays.asList(2200, 2300, 2400, 2500, 2600, 2800));
        mockData.put("Ginger", gingerPrices);
        
        // Cauliflower
        Map<String, Object> cauliflowerPrices = new HashMap<>();
        cauliflowerPrices.put("chakrata", 1200);
        cauliflowerPrices.put("dehradoon", 1500);
        cauliflowerPrices.put("trend", Arrays.asList(1000, 1100, 1200, 1300, 1400, 1500));
        mockData.put("Cauliflower", cauliflowerPrices);
        
        // Cabbage
        Map<String, Object> cabbagePrices = new HashMap<>();
        cabbagePrices.put("chakrata", 800);
        cabbagePrices.put("dehradoon", 1000);
        cabbagePrices.put("trend", Arrays.asList(600, 700, 800, 900, 950, 1000));
        mockData.put("Cabbage", cabbagePrices);
        
        // Brinjal
        Map<String, Object> brinjalPrices = new HashMap<>();
        brinjalPrices.put("chakrata", 1000);
        brinjalPrices.put("dehradoon", 1200);
        brinjalPrices.put("trend", Arrays.asList(800, 900, 1000, 1100, 1150, 1200));
        mockData.put("Brinjal", brinjalPrices);
        
        // Green Chilli
        Map<String, Object> chilliPrices = new HashMap<>();
        chilliPrices.put("chakrata", 2000);
        chilliPrices.put("dehradoon", 2500);
        chilliPrices.put("trend", Arrays.asList(1800, 1900, 2000, 2200, 2400, 2500));
        mockData.put("Green Chilli", chilliPrices);
        
        return mockData;
    }

    private Map<String, Integer> getMSPData() {
        Map<String, Integer> msp = new HashMap<>();
        msp.put("Rice", 2183);
        msp.put("Wheat", 2125);
        msp.put("Maize", 2090);
        msp.put("Soybean", 4300);
        msp.put("Sugarcane", 3550);
        msp.put("Cotton", 7020);
        msp.put("Tomato", 2200);
        msp.put("Onion", 2100);
        msp.put("Potato", 1800);
        msp.put("Ginger", 4000);
        msp.put("Garlic", 5000);
        msp.put("Cauliflower", 2000);
        msp.put("Cabbage", 1500);
        msp.put("Brinjal", 2000);
        msp.put("Green Chilli", 3000);
        return msp;
    }

    public Map<String, Object> getCurrentPrice(String crop, String mandiCode) {
        // Try CSV data first
        if (csvMarketDataService != null) {
            try {
                Map<String, Object> csvPrice = csvMarketDataService.getCurrentPrice(crop, mandiCode);
                if (csvPrice != null && csvPrice.containsKey("price")) {
                    Object priceObj = csvPrice.get("price");
                    int price = 0;
                    if (priceObj instanceof Integer) {
                        price = (Integer) priceObj;
                    } else if (priceObj instanceof String) {
                        try {
                            price = Integer.parseInt((String) priceObj);
                        } catch (NumberFormatException e) {}
                    }
                    if (price > 0) {
                        logger.info("✅ Using CSV data for {}: ₹{} at market {}", crop, price, csvPrice.get("market"));
                        return csvPrice;
                    }
                } else {
                    logger.warn("⚠️ CSV returned no price for crop='{}', mandi='{}'. Response: {}", 
                               crop, mandiCode, csvPrice);
                }
            } catch (Exception e) {
                logger.error("❌ CSV data error for {}: {}", crop, e.getMessage(), e);
            }
        } else {
            logger.warn("⚠️ CsvMarketDataService is not available (null)");
        }
        
        // Fallback to mock data
        logger.info("🔄 Falling back to mock data for {}: {}", crop, mandiCode);
        return getMockPrice(crop, mandiCode);
    }

    public List<Integer> getPriceHistory(String crop) {
        // Try CSV data first
        if (csvMarketDataService != null) {
            try {
                List<Integer> history = csvMarketDataService.getPriceHistory(crop, 6);
                if (history != null && !history.isEmpty()) {
                    return history;
                }
            } catch (Exception e) {
                logger.debug("CSV history not available for {}: {}", crop, e.getMessage());
            }
        }
        
        // Fallback to mock data
        Map<String, Object> cropData = mockPriceCache.get(crop);
        if (cropData != null && cropData.containsKey("trend")) {
            @SuppressWarnings("unchecked")
            List<Integer> trend = (List<Integer>) cropData.get("trend");
            return trend;
        }
        return Arrays.asList(2000, 2050, 2100, 2150, 2200, 2250);
    }

    public Map<String, Object> predictFuturePrice(String crop, int monthsAhead) {
        List<Integer> history = getPriceHistory(crop);
        if (history == null || history.isEmpty()) {
            return Map.of("price", 0, "confidence", 0, "trend", "STABLE", "monthsAhead", monthsAhead);
        }

        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < history.size(); i++) {
            sumX += i;
            sumY += history.get(i);
            sumXY += i * history.get(i);
            sumX2 += i * i;
        }

        int n = history.size();
        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;

        int predictedPrice = (int) (intercept + slope * (history.size() + monthsAhead - 1));
        double confidence = calculateConfidence(history, slope, intercept);

        return Map.of(
                "price", Math.max(predictedPrice, 0),
                "confidence", Math.round(confidence * 100) / 100.0,
                "trend", slope > 5 ? "UP" : (slope < -5 ? "DOWN" : "STABLE"),
                "monthsAhead", monthsAhead);
    }

    public Map<String, Object> compareWithMSP(String crop, int currentPrice) {
        Integer msp = mspData.get(crop);
        if (msp == null) {
            msp = 2000;
        }

        int difference = currentPrice - msp;
        double percentageDiff = (difference * 100.0) / msp;

        String recommendation;
        String action;
        
        if (difference > 0) {
            if (percentageDiff > 15) {
                recommendation = "Excellent! Price " + String.format("%.1f", percentageDiff) 
                    + "% above MSP. Consider selling 50% now and storing rest for festival season.";
                action = "SELL_50_PERCENT";
            } else if (percentageDiff > 8) {
                recommendation = "Good! Price " + String.format("%.1f", percentageDiff) 
                    + "% above MSP. Good time to sell 30% now.";
                action = "SELL_30_PERCENT";
            } else if (percentageDiff > 3) {
                recommendation = "Price " + String.format("%.1f", percentageDiff) 
                    + "% above MSP. Hold for better prices in coming weeks.";
                action = "HOLD";
            } else {
                recommendation = "Price slightly above MSP. Monitor market for 2-3 weeks.";
                action = "MONITOR";
            }
        } else {
            double percentBelow = Math.abs(percentageDiff);
            if (percentBelow > 10) {
                recommendation = "Price significantly below MSP. Strongly consider selling to government procurement centers.";
                action = "GOVERNMENT_SELL";
            } else {
                recommendation = "Price below MSP. Consider storing for 4-6 weeks or selling to government.";
                action = "STORE_OR_GOVERNMENT";
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("msp", msp);
        result.put("currentPrice", currentPrice);
        result.put("difference", difference);
        result.put("percentageDiff", Math.round(percentageDiff * 10) / 10.0);
        result.put("recommendation", recommendation);
        result.put("action", action);
        result.put("isAboveMSP", difference > 0);
        
        return result;
    }

    public Map<String, Double> getDynamicMultipliers(String crop) {
        List<Integer> history = getPriceHistory(crop);
        double avgPrice = history.stream().mapToInt(Integer::intValue).average().orElse(2000);
        int currentPrice = getCurrentMarketPrice(crop);
        double marketStrength = (double) currentPrice / avgPrice;

        double kharifMult = Math.max(0.8, Math.min(1.3, 1.0 * marketStrength));
        double rabiMult = Math.max(0.8, Math.min(1.4, 1.1 * marketStrength));
        double festivalMult = Math.max(0.9, Math.min(1.5, 1.3 * marketStrength));

        Map<String, Double> multipliers = new HashMap<>();
        multipliers.put("kharif", Math.round(kharifMult * 100) / 100.0);
        multipliers.put("rabi", Math.round(rabiMult * 100) / 100.0);
        multipliers.put("festival", Math.round(festivalMult * 100) / 100.0);
        
        return multipliers;
    }

    private Map<String, Object> getMockPrice(String crop, String mandiCode) {
        Map<String, Object> cropData = mockPriceCache.get(crop);
        String normalizedMandi = normalizeMandiCode(mandiCode);
        
        if (cropData != null && cropData.containsKey(normalizedMandi)) {
            Integer price = (Integer) cropData.get(normalizedMandi);
            Map<String, Object> result = new HashMap<>();
            result.put("price", price);
            result.put("market", getMarketName(mandiCode));
            result.put("source", "Mock Data");
            result.put("timestamp", LocalDate.now().toString());
            result.put("arrivalDate", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            return result;
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("price", 2000);
        result.put("market", getMarketName(mandiCode));
        result.put("source", "Default Mock Data");
        result.put("timestamp", LocalDate.now().toString());
        return result;
    }

    private int getCurrentMarketPrice(String crop) {
        try {
            Map<String, Object> current = getCurrentPrice(crop, "chakrata");
            if (current != null && current.get("price") instanceof Number) {
                return ((Number) current.get("price")).intValue();
            }
        } catch (Exception e) {
            logger.debug("Failed to get current price for {}", crop);
        }
        return 2000;
    }

    private String normalizeMandiCode(String code) {
        String lower = code.toLowerCase();
        if (lower.equals("azadpur")) return "azadpur";
        if (lower.equals("chakrata")) return "chakrata";
        if (lower.equals("dehradoon")) return "dehradoon";
        if (lower.equals("rishikesh")) return "rishikesh";
        if (lower.equals("vikasnagar")) return "vikasnagar";
        return "chakrata";
    }

    private String getMarketName(String code) {
        Map<String, String> markets = new HashMap<>();
        markets.put("azadpur", "Azadpur Mandi, Delhi");
        markets.put("chakrata", "Chakrata APMC, Uttarakhand");
        markets.put("dehradoon", "Dehradoon APMC, Uttarakhand");
        markets.put("rishikesh", "Rishikesh APMC, Uttarakhand");
        markets.put("vikasnagar", "Vikasnagar APMC, Uttarakhand");
        return markets.getOrDefault(code.toLowerCase(), "Chakrata APMC, Uttarakhand");
    }

    private double calculateConfidence(List<Integer> history, double slope, double intercept) {
        if (history.size() < 2) return 50.0;
        
        double mean = history.stream().mapToInt(Integer::intValue).average().orElse(0);
        double ssRes = 0, ssTot = 0;

        for (int i = 0; i < history.size(); i++) {
            double predicted = intercept + slope * i;
            ssRes += Math.pow(history.get(i) - predicted, 2);
            ssTot += Math.pow(history.get(i) - mean, 2);
        }

        if (ssTot == 0) return 50.0;
        double rSquared = 1 - (ssRes / ssTot);
        return Math.max(0, Math.min(100, rSquared * 100));
    }
}