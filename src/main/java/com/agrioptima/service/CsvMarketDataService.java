package com.agrioptima.service;

import org.springframework.stereotype.Service;
import org.springframework.core.io.ClassPathResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CsvMarketDataService {
    
    private static final Logger logger = LoggerFactory.getLogger(CsvMarketDataService.class);
    
    private Map<String, Map<String, List<PriceRecord>>> priceDatabase = new HashMap<>();
    private Map<String, String> cropNameMapping = new HashMap<>();
    
    public static class PriceRecord {
        public LocalDate date;
        public int modalPrice;
        public int minPrice;
        public int maxPrice;
        public String market;
        public String district;
        public String variety;
        
        public PriceRecord(LocalDate date, int modalPrice, int minPrice, int maxPrice, 
                          String market, String district, String variety) {
            this.date = date;
            this.modalPrice = modalPrice;
            this.minPrice = minPrice;
            this.maxPrice = maxPrice;
            this.market = market;
            this.district = district;
            this.variety = variety;
        }
    }
    
    @PostConstruct
    public void loadCsvData() {
        logger.info("Loading market data from CSV...");
        
        initializeCropMapping();
        
        try {
            ClassPathResource resource = new ClassPathResource("data/market_prices.csv");
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)
            );
            
            String line;
            boolean isFirstLine = true;
            int recordCount = 0;
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            
            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                
                String[] fields = parseCsvLine(line);
                if (fields.length < 11) continue;
                
                try {
                    LocalDate date = LocalDate.parse(fields[0].trim(), dateFormatter);
                    String commodity = fields[1].trim();
                    String district = fields[3].trim();
                    String market = fields[5].trim();
                    int maxPrice = parseIntSafe(fields[6]);
                    int minPrice = parseIntSafe(fields[7]);
                    int modalPrice = parseIntSafe(fields[8]);
                    String variety = fields.length > 10 ? fields[10].trim() : "";
                    
                    String mappedCrop = mapCropName(commodity);
                    
                    PriceRecord record = new PriceRecord(date, modalPrice, minPrice, maxPrice, 
                                                         market, district, variety);
                    
                    priceDatabase.computeIfAbsent(mappedCrop, k -> new HashMap<>())
                                 .computeIfAbsent(market, k -> new ArrayList<>())
                                 .add(record);
                    
                    recordCount++;
                    
                } catch (Exception e) {
                    logger.debug("Error parsing line: {}", e.getMessage());
                }
            }
            
            for (Map<String, List<PriceRecord>> cropData : priceDatabase.values()) {
                for (List<PriceRecord> records : cropData.values()) {
                    records.sort(Comparator.comparing(r -> r.date));
                }
            }
            
            logger.info("Loaded {} price records for {} crops", recordCount, priceDatabase.size());
            logger.info("Available crops: {}", priceDatabase.keySet());
            
        } catch (Exception e) {
            logger.error("Failed to load CSV data: {}", e.getMessage(), e);
        }
    }
    
    private void initializeCropMapping() {
        cropNameMapping.put("Ginger(Green)", "Ginger");
        cropNameMapping.put("Colacasia", "Colacasia");
        cropNameMapping.put("Field Pea", "Field Pea");
        cropNameMapping.put("Tomato", "Tomato");
        cropNameMapping.put("Onion", "Onion");
        cropNameMapping.put("Potato", "Potato");
        cropNameMapping.put("Brinjal", "Brinjal");
        cropNameMapping.put("Cauliflower", "Cauliflower");
        cropNameMapping.put("Cabbage", "Cabbage");
        cropNameMapping.put("Carrot", "Carrot");
        cropNameMapping.put("Green Chilli", "Green Chilli");
        cropNameMapping.put("Apple", "Apple");
        cropNameMapping.put("Banana", "Banana");
        cropNameMapping.put("Pomegranate", "Pomegranate");
        cropNameMapping.put("Grapes", "Grapes");
        cropNameMapping.put("Mango", "Mango");
        cropNameMapping.put("Orange", "Orange");
        cropNameMapping.put("Papaya", "Papaya");
        cropNameMapping.put("Pumpkin", "Pumpkin");
        cropNameMapping.put("Bottle gourd", "Bottle Gourd");
        cropNameMapping.put("Bitter gourd", "Bitter Gourd");
        cropNameMapping.put("Bhindi(Ladies Finger)", "Bhindi");
        cropNameMapping.put("Kinnow", "Kinnow");
        cropNameMapping.put("Lemon", "Lemon");
        cropNameMapping.put("Garlic", "Garlic");
        cropNameMapping.put("Wheat", "Wheat");
    }
    
    private String mapCropName(String csvName) {
        return cropNameMapping.getOrDefault(csvName, csvName);
    }
    
    private int parseIntSafe(String value) {
        if (value == null || value.trim().isEmpty()) return 0;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        
        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        
        return fields.toArray(new String[0]);
    }
    
    public Map<String, Object> getCurrentPrice(String crop, String marketCode) {
        String normalizedMarket = normalizeMarket(marketCode);
        String normalizedCrop = normalizeCropName(crop);
        
        logger.info("Looking for price: crop='{}', marketCode='{}', normalizedCrop='{}', normalizedMarket='{}'", 
                    crop, marketCode, normalizedCrop, normalizedMarket);
        
        Map<String, List<PriceRecord>> cropData = priceDatabase.get(normalizedCrop);
        if (cropData == null) {
            logger.warn("No CSV data found for crop '{}'. Available crops: {}", normalizedCrop, priceDatabase.keySet());
            return null;
        }
        
        List<PriceRecord> records = null;
        if (normalizedMarket != null) {
            records = cropData.get(normalizedMarket);
        }
        
        // If specific market not found, try to get any market for this crop
        if (records == null || records.isEmpty()) {
            logger.warn("No data for market '{}' in crop '{}'. Trying fallback to any available market", 
                        normalizedMarket, normalizedCrop);
            for (Map.Entry<String, List<PriceRecord>> entry : cropData.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    records = entry.getValue();
                    normalizedMarket = entry.getKey();
                    logger.info("Using fallback market: '{}'", normalizedMarket);
                    break;
                }
            }
        }
        
        if (records == null || records.isEmpty()) {
            logger.error("No price records found after fallback for crop '{}'", normalizedCrop);
            return null;
        }
        
        PriceRecord latest = records.get(records.size() - 1);
        
        Map<String, Object> result = new HashMap<>();
        result.put("price", latest.modalPrice > 0 ? latest.modalPrice : latest.maxPrice);
        result.put("minPrice", latest.minPrice);
        result.put("maxPrice", latest.maxPrice);
        result.put("market", normalizedMarket);
        result.put("district", latest.district);
        result.put("arrivalDate", latest.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        result.put("variety", latest.variety);
        result.put("source", "CSV Market Data");
        result.put("timestamp", LocalDate.now().toString());
        
        logger.info("Found price for {} at {}: ₹{}", normalizedCrop, normalizedMarket, result.get("price"));
        return result;
    }
    
    public List<Integer> getPriceHistory(String crop, int months) {
        String normalizedCrop = normalizeCropName(crop);
        Map<String, List<PriceRecord>> cropData = priceDatabase.get(normalizedCrop);
        
        if (cropData == null) {
            return null;
        }
        
        List<PriceRecord> allRecords = new ArrayList<>();
        for (List<PriceRecord> records : cropData.values()) {
            allRecords.addAll(records);
        }
        
        if (allRecords.isEmpty()) {
            return null;
        }
        
        allRecords.sort(Comparator.comparing(r -> r.date));
        
        LocalDate cutoff = LocalDate.now().minusMonths(months);
        List<Integer> history = allRecords.stream()
            .filter(r -> r.date.isAfter(cutoff))
            .map(r -> r.modalPrice > 0 ? r.modalPrice : r.maxPrice)
            .filter(p -> p > 0)
            .collect(Collectors.toList());
        
        return history.isEmpty() ? null : history;
    }
    
    public List<String> getAvailableCrops() {
        return new ArrayList<>(priceDatabase.keySet());
    }
    
    private String normalizeMarket(String marketCode) {
        Map<String, String> marketMapping = new HashMap<>();
        marketMapping.put("azadpur", "Azadpur Mandi, Delhi");
        marketMapping.put("chakrata", "Chakrata APMC");
        marketMapping.put("dehradoon", "Dehradoon APMC");
        marketMapping.put("rishikesh", "Rishikesh APMC");
        marketMapping.put("vikasnagar", "Vikasnagar APMC");
        marketMapping.put("azadpur mandi, delhi", "Azadpur Mandi, Delhi");
        
        String lower = marketCode.toLowerCase();
        if (marketMapping.containsKey(lower)) {
            return marketMapping.get(lower);
        }
        
        // Try to find matching market
        for (String market : priceDatabase.values().stream()
                .flatMap(m -> m.keySet().stream())
                .collect(Collectors.toSet())) {
            if (market.toLowerCase().contains(lower)) {
                return market;
            }
        }
        
        // If no match found, return the first available market for the crop
        logger.warn("Market '{}' not found in CSV data, will try fallback", marketCode);
        return null;
    }
    
    private String normalizeCropName(String crop) {
        Map<String, String> cropMapping = new HashMap<>();
        cropMapping.put("rice", "Rice");
        cropMapping.put("wheat", "Wheat");
        cropMapping.put("maize", "Maize");
        cropMapping.put("tomato", "Tomato");
        cropMapping.put("onion", "Onion");
        cropMapping.put("potato", "Potato");
        cropMapping.put("ginger", "Ginger");
        cropMapping.put("garlic", "Garlic");
        cropMapping.put("brinjal", "Brinjal");
        cropMapping.put("cauliflower", "Cauliflower");
        cropMapping.put("cabbage", "Cabbage");
        cropMapping.put("carrot", "Carrot");
        
        String lower = crop.toLowerCase();
        if (cropMapping.containsKey(lower)) {
            return cropMapping.get(lower);
        }
        
        // Check if crop exists in database
        for (String existingCrop : priceDatabase.keySet()) {
            if (existingCrop.toLowerCase().contains(lower)) {
                return existingCrop;
            }
        }
        
        return crop;
    }
}