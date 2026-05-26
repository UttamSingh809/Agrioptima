# 🌾 AgriOptima - Smart Crop Planning System

[![Java](https://img.shields.io/badge/Java-17-blue.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.18-green.svg)](https://spring.io/projects/spring-boot)
> **Dynamic Programming-Powered Crop Rotation Planning for Maximum Profit & Sustainable Farming**

AgriOptima helps farmers optimize crop rotation using **Dynamic Programming** to maximize profit while managing water budgets, maintaining soil health, and tracking real-time market prices from Indian APMCs.

📹 **Demo Video:** 

https://drive.google.com/file/d/1plhlOQlyEvmoiaK6GJWEeDCryLH-EsUs/

## ✨ Key Features

### 🎯 Smart Optimization Engine

- Dynamic Programming Solver with memoization for optimal crop scheduling
- Multi-objective optimization balancing profit, water usage, and soil health
- Constraint evaluation for crop rotation rules, soil compatibility, and water budgets
- State management tracking soil nitrogen levels (0-2) per plot across seasons

### 📈 Market Intelligence

- Real-time price tracking from 5+ APMC markets (Azadpur, Chakrata, Dehradoon, Rishikesh, Vikasnagar)
- 6,800+ price records integrated from Agmarknet data
- Price prediction using linear regression with confidence scoring
- MSP comparison with actionable recommendations (SELL/HOLD/GOVERNMENT_SELL)
- Interactive price trends with Chart.js visualizations

### 🔔 Smart Alerts & Calendar

- Browser notifications when crops hit target prices
- Harvest calendar with planting date selection and harvest date calculation
- Google Calendar integration with pre-filled harvest event details
- ICS file download for external calendar imports
- Crop-specific harvest tips for 10+ crops

### 📱 QR Code Field Markers

- Generate printable QR codes for individual plots or all plots
- Encode complete crop history, soil health status, and harvest schedule
- Mobile-optimized QR viewer with responsive design
- Batch ZIP export for all QR codes
- Server-side plan storage for shareable QR codes

### 📊 Export & Reporting

- CSV export with season-wise planting schedule
- JSON backup with complete optimization results
- PDF generation via html2pdf with professional report layout
- Print-friendly reports with profit breakdown and rotation summary

### 🎨 User Interface

- Drag-drop crop library with double-click to add functionality
- Editable crop table with profit, water, nitrogen impact, rotation gap, and seasonal multipliers
- What-if analysis slider for water budget adjustments
- Dynamic seasonal backgrounds (Spring/Summer/Monsoon/Autumn/Winter) with animated effects
- Responsive design optimized for desktop, tablet, and mobile

## 🛠️ Tech Stack

### Backend

- Spring Boot 2.7 - REST API framework
- Java 17 - Core language
- Maven - Build automation
- JUnit 5 - Unit testing

### Frontend

- HTML5/CSS3 - Structure & styling
- Vanilla JavaScript - Interactive features
- Chart.js - Price trend visualizations
- QRCode.js - QR code generation
- SheetJS (XLSX) - Excel export
- html2pdf - PDF generation

### Algorithms

- Crop Optimization - Dynamic Programming with memoization
- Price Prediction - Linear regression
- State Management - HashMap with custom equals/hashCode
- Constraint Evaluation - Recursive backtracking with pruning

## 🚀 Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.8+ or use included Maven wrapper
- Git (optional, for cloning)

### Installation

```bash
# Clone the repository
git clone https://github.com/UttamSingh809/Agrioptima.git
cd Agrioptima

# Build the project
./mvnw clean package

# Run the application
java -jar target/AgriOptima-1.0.0.jar
```

### Access the Application

Open your browser and navigate to:

- Main Application: http://localhost:8080/index.html
- Landing Page: http://localhost:8080/landing.html
- QR Viewer: http://localhost:8080/qr-viewer.html

## 📁 Project Structure

```
AgriOptima/
├── src/main/java/com/agrioptima/
│   ├── controller/
│   │   ├── HomeController.java
│   │   ├── MarketController.java
│   │   ├── OptimizationController.java
│   │   └── TemporaryPlanController.java
│   ├── service/
│   │   ├── MarketDataService.java
│   │   ├── CsvMarketDataService.java
│   │   └── OptimizationService.java
│   ├── solver/
│   │   ├── DPSolver.java
│   │   ├── StateManager.java
│   │   ├── AssignmentGenerator.java
│   │   ├── ConstraintEvaluator.java
│   │   └── PlanReconstructor.java
│   ├── model/
│   │   ├── Crop.java
│   │   ├── FarmState.java
│   │   ├── FarmConfig.java
│   │   ├── CropAssignment.java
│   │   └── PlanResult.java
│   └── dto/
│       ├── OptimizationRequest.java
│       └── OptimizationResult.java
├── src/main/resources/
│   ├── static/
│   │   ├── index.html
│   │   ├── landing.html
│   │   ├── qr-viewer.html
│   │   ├── css/style.css
│   │   └── js/
│   │       ├── main.js
│   │       ├── effects/
│   │       └── exports/
│   └── application.properties
├── pom.xml
└── .gitignore
```

## 🔧 API Endpoints

### Optimization API

| Endpoint             | Method | Description           |
| -------------------- | ------ | --------------------- |
| /api/optimize        | POST   | Run crop optimization |
| /api/optimize/status | GET    | Health check          |

### Market Intelligence API

| Endpoint              | Method | Description        |
| --------------------- | ------ | ------------------ |
| /api/market/price     | GET    | Get current price  |
| /api/market/history   | GET    | Get price history  |
| /api/market/predict   | GET    | Price prediction   |
| /api/market/compare   | GET    | MSP comparison     |
| /api/market/crops     | GET    | Available crops    |
| /api/market/markets   | GET    | Available markets  |
| /api/market/dashboard | GET    | Complete dashboard |
| /api/market/bulk      | GET    | Bulk price check   |

### QR Code API

| Endpoint                   | Method | Description      |
| -------------------------- | ------ | ---------------- |
| /api/market/save-plan      | POST   | Save plan for QR |
| /api/market/load-plan/{id} | GET    | Load saved plan  |

## 💡 Usage Guide

### 1. Configure Your Farm

- Enter number of plots (1-10)
- Set planning years (1-5)
- Define water budget (kL/acre)
- Select initial soil level (Low/Medium/High)

### 2. Select Crops

- Drag crops from library or double-click to add
- Edit crop parameters:
  - Profit per acre (₹)
  - Water requirement (kL/acre)
  - Nitrogen impact (-1 to +1)
  - Minimum rotation gap (seasons)
  - Soil compatibility (0-2)
  - Seasonal multipliers (Kharif/Rabi)

### 3. Run Optimization

- Click "Generate Optimal Plan"
- DP solver computes best schedule
- View season-by-season planting plan
- Review profit, water usage, and soil metrics

### 4. Analyze Market Data

- Select crop and market
- View current price vs MSP
- See price trends and predictions
- Get selling recommendations

### 5. Set Up Alerts

- Set target price for any crop
- Receive browser notifications when price is reached
- Add harvest dates to calendar

### 6. Generate QR Codes

- Run optimization first
- Select plot(s) to generate QR codes
- Print and attach to physical field markers
- Scan with any smartphone to view crop history

### 7. Export Reports

- Download CSV for spreadsheet analysis
- Export JSON for backup
- Generate PDF for professional reports
- Print plan for field reference

## 📊 Sample Output

### Optimization Result

- Total Profit: ₹24,500
- Water Used: 18,500 L (92.5% of budget)
- Soil Health Change: +2 (Improving)
- Schedule: 2 Years (4 Seasons) x 4 Plots

### Market Comparison

- Crop: Tomato
- Current Price: ₹2,500/quintal
- MSP: ₹2,200/quintal
- Difference: +₹300 (+13.6%)
- Recommendation: Excellent! Consider selling 50% now

## 🎯 Performance Metrics

- State space size: Up to 10,000 states
- Solver complexity: O(states × assignments)
- Memoization hit rate: 85-95%
- Average optimization time: < 1 second
- CSV load time: ~500ms for 6,800 records

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create your feature branch (git checkout -b feature/AmazingFeature)
3. Commit your changes (git commit -m 'Add some AmazingFeature')
4. Push to the branch (git push origin feature/AmazingFeature)
5. Open a Pull Request

## 🙏 Acknowledgments

- Market Price Data: Agmarknet (https://agmarknet.gov.in/) - Government of India
- MSP Data: Ministry of Agriculture & Farmers Welfare, Government of India
- Icons & Emojis: Open source emoji set
- Libraries: Spring Boot, Chart.js, QRCode.js, SheetJS, html2pdf

## ⭐ Show Your Support

If this project helped you, please give it a ⭐ on GitHub!

---

<p align="center">
  Made with ❤️ for Indian Farmers
</p>
