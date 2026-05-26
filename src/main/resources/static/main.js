
    (function() {
        console.log('[AgriOptima] ========== COMPLETE INITIALIZATION START ==========');
        
        // ============================================
        // COMPLETE CROP DATA - FIXED COLUMN MAPPING
        // ============================================
        const DEFAULT_CROPS = [
            { name: "Rice", profit: 3000, water: 2950, nImpact: -1, rotGap: 1, soil: "1,2", kharif: 1.0, rabi: 1.1, emoji: "🌾" },
            { name: "Wheat", profit: 2500, water: 2027, nImpact: 0, rotGap: 0, soil: "0,1,2", kharif: 1.1, rabi: 1.0, emoji: "🌾" },
            { name: "Soybean", profit: 2000, water: 2100, nImpact: 1, rotGap: 0, soil: "0,1,2", kharif: 1.0, rabi: 1.0, emoji: "🥜" },
            { name: "Maize", profit: 2200, water: 2400, nImpact: 0, rotGap: 1, soil: "0,1,2", kharif: 1.2, rabi: 0.8, emoji: "🌽" },
            { name: "Sugarcane", profit: 3500, water: 3000, nImpact: -1, rotGap: 2, soil: "1,2", kharif: 1.0, rabi: 0.5, emoji: "🎋" },
            { name: "Fallow", profit: 0, water: 0, nImpact: 1, rotGap: 0, soil: "0,1,2", kharif: 1.0, rabi: 1.0, emoji: "🧹" }
        ];
        
        // Crop growing days for harvest calendar
        const cropGrowingDays = {
            'Rice': 135, 'Wheat': 105, 'Maize': 100, 'Soybean': 105,
            'Tomato': 100, 'Onion': 135, 'Potato': 105, 'Ginger': 225, 'Garlic': 165
        };
        
        // State
        let priceChart = null;
        let activeAlerts = [];
        let harvestEvents = [];
        let currentOptimizationResult = null;
        
        function getElement(id) { return document.getElementById(id); }
        function updateQRPlotSelect() {
            const numPlots = parseInt(getElement('numPlots')?.value) || 4;
            const select = getElement('qrPlotSelect');
            if (!select) return;
            
            let html = '<option value="all">All Plots (Generate for every plot)</option>';
            for (let i = 1; i <= numPlots; i++) {
                html += `<option value="${i}">Plot ${i} Only</option>`;
            }
            select.innerHTML = html;
        }       
        function updateHarvestPlotSelect() {
            const numPlots = parseInt(getElement('numPlots')?.value) || 4;
            const select = getElement('plotSelect');
            if (!select) return;
            
            let html = '';
            for (let i = 1; i <= numPlots; i++) {
                html += `<option value="${i}">Plot ${i}</option>`;
            }
            select.innerHTML = html;
        }
        function enableQRFeatures() {
            // Update status banner
            const statusBanner = getElement('qrStatusBanner');
            const statusIcon = getElement('qrStatusIcon');
            const statusText = getElement('qrStatusText');
            
            if (statusBanner) {
                statusBanner.style.background = '#e8f5e9';
                statusBanner.style.borderLeftColor = '#4caf50';
                if (statusIcon) statusIcon.innerHTML = '✅';
                if (statusText) statusText.innerHTML = 'Optimization complete! QR features are now ready.';
            }
            
            // Enable Plot Markers card
            const plotCard = getElement('plotMarkersCard');
            const plotSelect = getElement('qrPlotSelect');
            const plotBtn = getElement('generateQrBtn');
            
            if (plotCard) plotCard.style.opacity = '1';
            if (plotSelect) plotSelect.disabled = false;
            if (plotBtn) {
                plotBtn.disabled = false;
                plotBtn.style.background = '#4E8B6A';
                plotBtn.style.cursor = 'pointer';
                plotBtn.innerHTML = '🏷️ Generate Plot Markers';
            }
            
            // Enable Shareable card
            const shareCard = getElement('shareableCard');
            const shareBtn = getElement('qrCodeBtn');
            
            if (shareCard) shareCard.style.opacity = '1';
            if (shareBtn) {
                shareBtn.disabled = false;
                shareBtn.style.background = 'linear-gradient(135deg, #667eea, #764ba2)';
                shareBtn.style.cursor = 'pointer';
                shareBtn.innerHTML = '📤 Generate Shareable QR';
            }
        }
        function disableQRFeatures() {
            const statusBanner = getElement('qrStatusBanner');
            const statusIcon = getElement('qrStatusIcon');
            const statusText = getElement('qrStatusText');
            
            if (statusBanner) {
                statusBanner.style.background = '#fff3e0';
                statusBanner.style.borderLeftColor = '#ff9800';
                if (statusIcon) statusIcon.innerHTML = '⚠️';
                if (statusText) statusText.innerHTML = 'Run optimization first to enable QR generation';
            }
            
            const plotSelect = getElement('qrPlotSelect');
            const plotBtn = getElement('generateQrBtn');
            const shareBtn = getElement('qrCodeBtn');
            const plotCard = getElement('plotMarkersCard');
            const shareCard = getElement('shareableCard');
            
            if (plotCard) plotCard.style.opacity = '0.6';
            if (shareCard) shareCard.style.opacity = '0.6';
            if (plotSelect) plotSelect.disabled = true;
            if (plotBtn) {
                plotBtn.disabled = true;
                plotBtn.style.background = '#b0bec5';
                plotBtn.innerHTML = '🔒 Run Optimization First';
            }
            if (shareBtn) {
                shareBtn.disabled = true;
                shareBtn.style.background = '#b0bec5';
                shareBtn.innerHTML = '🔒 Run Optimization First';
            }
        }
        function showExportSuccess(msg) {
            console.log('[AgriOptima] ✓', msg);
            const preview = getElement('exportPreview');
            if (preview) {
                preview.style.display = 'block';
                preview.innerHTML = `<p>✅ ${msg}</p>`;
                setTimeout(() => preview.style.display = 'none', 3000);
            }
        }
        
        // ============================================
        // CROP TABLE MANAGEMENT - FIXED
        // ============================================
        function addCropRow(crop = null) {
            const tbody = getElement('cropRows');
            if (!tbody) return;
            
            const row = document.createElement('tr');
            let name = '', profit = '', water = '', nImpact = '', rotGap = '', soil = '0,1,2', kharif = '1.0', rabi = '1.0';
            
            if (crop) {
                name = crop.name || '';
                profit = crop.profit !== undefined ? crop.profit : '';
                water = crop.water !== undefined ? crop.water : '';
                nImpact = crop.nImpact !== undefined ? crop.nImpact : '';
                rotGap = crop.rotGap !== undefined ? crop.rotGap : '';
                soil = crop.soil || '0,1,2';
                kharif = crop.kharif !== undefined ? crop.kharif : '1.0';
                rabi = crop.rabi !== undefined ? crop.rabi : '1.0';
            }
            
            row.innerHTML = `
                <td><input type="text" value="${name}" placeholder="e.g., Rice" style="width:100%"></td>
                <td><input type="number" value="${profit}" placeholder="0" style="width:100%"></td>
                <td><input type="number" value="${water}" placeholder="0" style="width:100%"></td>
                <td><input type="number" value="${nImpact}" placeholder="0" style="width:100%"></td>
                <td><input type="number" value="${rotGap}" placeholder="0" style="width:100%"></td>
                <td><input type="text" value="${soil}" placeholder="0,1,2" style="width:100%"></td>
                <td><input type="number" step="0.1" value="${kharif}" placeholder="1.0" style="width:100%"></td>
                <td><input type="number" step="0.1" value="${rabi}" placeholder="1.0" style="width:100%"></td>
                <td><button class="btn btn-secondary" onclick="this.closest('tr').remove()">🗑️</button></td>
            `;
            tbody.appendChild(row);
        }
        
        function resetToDefaultCrops() {
            const tbody = getElement('cropRows');
            disableQRFeatures();
            if (tbody) {
                tbody.innerHTML = '';
                DEFAULT_CROPS.forEach(crop => addCropRow(crop));
                showExportSuccess('Reset to default crops');
            }
        }
        
        function initCropLibrary() {
            const library = getElement('cropLibrary');
            if (!library) return;
            library.innerHTML = '';
            DEFAULT_CROPS.forEach(crop => {
                const card = document.createElement('div');
                card.className = 'crop-card';
                card.draggable = true;
                card.innerHTML = `<div class="crop-icon">${crop.emoji}</div><div class="crop-name">${crop.name}</div>`;
                card.addEventListener('dragstart', (e) => e.dataTransfer.setData('text/plain', JSON.stringify(crop)));
                card.addEventListener('dblclick', () => addCropRow(crop));
                library.appendChild(card);
            });
        }
        
        function setupDragDrop() {
            const cropRows = getElement('cropRows');
            if (!cropRows) return;
            cropRows.addEventListener('dragover', (e) => e.preventDefault());
            cropRows.addEventListener('drop', (e) => {
                e.preventDefault();
                try {
                    const cropData = e.dataTransfer.getData('text/plain');
                    if (cropData) addCropRow(JSON.parse(cropData));
                } catch (err) { console.error(err); }
            });
        }
        
        // ============================================
        // OPTIMIZATION
        // ============================================
        async function runOptimization(useSlider = false) {
            const loading = getElement('loading');
            const results = getElement('results');
            if (loading) loading.style.display = 'block';
            if (results) results.style.display = 'none';
            
            try {
                const numPlots = parseInt(getElement('numPlots')?.value) || 4;
                const totalSeasonsYears = parseInt(getElement('totalSeasons')?.value) || 2;
                const totalSeasons = totalSeasonsYears * 2;
                const waterBudget = useSlider ? parseInt(getElement('waterSlider')?.value) : parseInt(getElement('waterBudget')?.value) || 10000;
                const initialSoil = parseInt(getElement('initialSoil')?.value) || 1;
                
                const crops = [];
                document.querySelectorAll('#cropRows tr').forEach((row, idx) => {
                    const inputs = row.querySelectorAll('input');
                    if (inputs.length >= 8) {
                        const soilStr = inputs[5]?.value || '0,1,2';
                        const soilVals = soilStr.split(',').map(s => parseInt(s.trim()) || 0);
                        crops.push({
                            id: idx,
                            name: inputs[0]?.value?.trim() || 'Unknown',
                            profitPerAcre: parseInt(inputs[1]?.value) || 0,
                            waterPerAcre: parseInt(inputs[2]?.value) || 0,
                            nitrogenImpact: parseInt(inputs[3]?.value) || 0,
                            minRotationGap: parseInt(inputs[4]?.value) || 0,
                            soilCompatibility: soilVals,
                            seasonalMultiplier: [parseFloat(inputs[6]?.value) || 1.0, parseFloat(inputs[7]?.value) || 1.0]
                        });
                    }
                });
                
                if (crops.length === 0) {
                    resetToDefaultCrops();
                    const newRows = document.querySelectorAll('#cropRows tr');
                    newRows.forEach((row, idx) => {
                        const inputs = row.querySelectorAll('input');
                        crops.push({
                            id: idx, name: inputs[0]?.value, profitPerAcre: parseInt(inputs[1]?.value) || 0,
                            waterPerAcre: parseInt(inputs[2]?.value) || 0, nitrogenImpact: parseInt(inputs[3]?.value) || 0,
                            minRotationGap: parseInt(inputs[4]?.value) || 0,
                            soilCompatibility: (inputs[5]?.value || '0,1,2').split(',').map(s => parseInt(s.trim()) || 0),
                            seasonalMultiplier: [parseFloat(inputs[6]?.value) || 1.0, parseFloat(inputs[7]?.value) || 1.0]
                        });
                    });
                }
                
                const response = await fetch('/api/optimize', {
                    method: 'POST', headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ numPlots, totalSeasons, waterBudget, initialSoil, crops })
                });
                
                if (!response.ok) throw new Error(`Server error: ${response.status}`);
                const result = await response.json();
                currentOptimizationResult = result;
                // Store globally for other modules to access
                window.lastOptimizationResult = result;
                window.lastFarmConfig = {
                    plots: numPlots,
                    years: totalSeasonsYears,
                    waterBudget: waterBudget,
                    initialSoil: initialSoil
                };
                // Update QR dropdown to match current plot count
                updateQRPlotSelect();
                enableQRFeatures();
                // Also store for fallback
                window.currentResult = result;

                // Update KPIs
                const waterPercent = result.metrics ? Math.round((result.metrics.totalWaterUsed / waterBudget) * 100) : 0;
                const kpiProfit = getElement('kpi-profit');
                const kpiWater = getElement('kpi-water');
                const kpiSoil = getElement('kpi-soil');
                const kpiPlots = getElement('kpi-plots');
                if (kpiProfit) kpiProfit.textContent = `₹${result.maxProfit?.toLocaleString() || 0}`;
                if (kpiWater) kpiWater.textContent = `${waterPercent}%`;
                if (kpiSoil) kpiSoil.textContent = `${result.metrics?.soilHealthChange >= 0 ? '+' : ''}${result.metrics?.soilHealthChange || 0}`;
                if (kpiPlots) kpiPlots.textContent = `${numPlots}/${numPlots}`;
                
                const detailProfit = getElement('detail-profit');
                const detailWater = getElement('detail-water');
                const detailSoil = getElement('detail-soil');
                if (detailProfit) detailProfit.textContent = `₹${result.maxProfit?.toLocaleString() || 0}`;
                if (detailWater) detailWater.textContent = `${result.metrics?.totalWaterUsed?.toLocaleString() || 0} kL`;
                if (detailSoil) detailSoil.textContent = `${result.metrics?.soilHealthChange || 0}`;
                
                // Render schedule
                if (result.schedule && result.schedule.length > 0) {
                    const container = getElement('scheduleContainer');
                    if (container) {
                        let html = `<table class="schedule-table"><thead><tr><th>Season</th>`;
                        for (let i = 0; i < result.schedule[0].length; i++) html += `<th>Plot ${i + 1}</th>`;
                        html += `</tr></thead><tbody>`;
                        result.schedule.forEach((season, idx) => {
                            const year = Math.floor(idx / 2) + 1;
                            const seasonLabel = idx % 2 === 0 ? `Year ${year} Kharif` : `Year ${year} Rabi`;
                            html += `<tr><td><strong>${seasonLabel}</strong></td>`;
                            season.forEach(crop => {
                                let emoji = '🌱';
                                if (crop?.includes('Rice')) emoji = '🌾';
                                else if (crop?.includes('Wheat')) emoji = '🌾';
                                else if (crop?.includes('Soybean')) emoji = '🥜';
                                else if (crop?.includes('Maize')) emoji = '🌽';
                                html += `<td><div class="plot-cell">${emoji} ${crop || 'Fallow'}</div></td>`;
                            });
                            html += `</tr>`;
                        });
                        html += `</tbody></table>`;
                        container.innerHTML = html;
                    }
                }
                
                if (results) results.style.display = 'block';
                results?.scrollIntoView({ behavior: 'smooth' });
                showExportSuccess('Optimization complete!');
                
                // Enable QR button
                const qrBtn = getElement('generateQrBtn');
                if (qrBtn) { qrBtn.disabled = false; qrBtn.style.opacity = '1'; qrBtn.style.cursor = 'pointer'; }
                
            } catch (err) {
                console.error('[AgriOptima] Optimization failed:', err);
                alert('Optimization failed: ' + err.message);
            } finally {
                if (loading) loading.style.display = 'none';
            }
        }
        
        // ============================================
        // MARKET FUNCTIONS
        // ============================================
        async function updateMarketData() {
            const crop = getElement('marketCropSelect')?.value || 'Tomato';
            const region = getElement('marketRegionSelect')?.value || 'chakrata';
            try {
                const priceResponse = await fetch(`/api/market/price?crop=${encodeURIComponent(crop)}&mandi=${region}`);
                if (priceResponse.ok) {
                    const priceData = await priceResponse.json();
                    const priceEl = getElement('currentPrice');
                    if (priceEl) priceEl.innerHTML = `₹${priceData.price?.toLocaleString() || 0}`;
                }
                const mspResponse = await fetch(`/api/market/compare?crop=${encodeURIComponent(crop)}`);
                if (mspResponse.ok) {
                    const mspData = await mspResponse.json();
                    const mspEl = getElement('mspValue');
                    const diffEl = getElement('priceDifference');
                    const recEl = getElement('recommendationText');
                    if (mspEl) mspEl.innerHTML = `₹${mspData.msp?.toLocaleString() || 0}`;
                    if (diffEl) {
                        const diff = mspData.difference || 0;
                        const percent = mspData.percentageDiff || 0;
                        diffEl.innerHTML = `${diff >= 0 ? '+' : ''}₹${Math.abs(diff)} (${diff >= 0 ? '+' : ''}${percent.toFixed(1)}%)`;
                    }
                    if (recEl) recEl.innerHTML = mspData.recommendation || 'Market data loaded';
                }
                const historyResponse = await fetch(`/api/market/history?crop=${encodeURIComponent(crop)}`);
                if (historyResponse.ok) {
                    const historyData = await historyResponse.json();
                    updatePriceChart(crop, historyData.history);
                }
            } catch (error) { console.error(error); }
        }
        
function updatePriceChart(crop, historyData) {
    const ctx = getElement('priceTrendChart')?.getContext('2d');
    if (!ctx || typeof Chart === 'undefined') return;
    
    // Get current price from the DOM
    const currentPriceEl = getElement('currentPrice');
    let currentPrice = 0;
    if (currentPriceEl) {
        currentPrice = parseInt(currentPriceEl.textContent.replace(/[^0-9]/g, '')) || 0;
    }
    
    // Use history data, but replace the last point with current price
    let prices = historyData?.slice(-6) || [2000, 2100, 2200, 2250, 2300, 2280];
    if (prices.length > 0 && currentPrice > 0) {
        prices[prices.length - 1] = currentPrice;
    }
    
    // Generate labels
    const labels = [];
    const today = new Date();
    for (let i = 5; i >= 0; i--) {
        const date = new Date(today.getFullYear(), today.getMonth() - i, 1);
        labels.push(date.toLocaleString('default', { month: 'short' }));
    }
    
    if (priceChart) priceChart.destroy();
    priceChart = new Chart(ctx, {
        type: 'line',
        data: { 
            labels: labels, 
            datasets: [{ 
                label: `${crop} Price (₹/quintal)`, 
                data: prices, 
                borderColor: '#4a7c43', 
                tension: 0.4 
            }] 
        },
        options: { responsive: true }
    });
}
        
        // ============================================
        // PRICE ALERTS
        // ============================================
        function loadAlerts() {
            const saved = localStorage.getItem('agrioptima_alerts');
            if (saved) { activeAlerts = JSON.parse(saved); renderAlerts(); }
        }
        function saveAlerts() { localStorage.setItem('agrioptima_alerts', JSON.stringify(activeAlerts)); }
        function renderAlerts() {
            const container = getElement('alertsList');
            if (!container) return;
            if (activeAlerts.length === 0) { container.innerHTML = '<p style="color: var(--medium-gray);">No active alerts. Create one above!</p>'; return; }
            container.innerHTML = activeAlerts.map(alert => `<div class="alert-card"><div><strong>🌾 ${alert.crop}</strong><br><small>📍 ${alert.market}</small><br><strong>🎯 ₹${alert.targetPrice}</strong></div><div><button class="delete-alert-btn" onclick="window.deleteAlert(${alert.id})">Delete</button></div></div>`).join('');
        }
        window.deleteAlert = function(id) { activeAlerts = activeAlerts.filter(a => a.id !== id); saveAlerts(); renderAlerts(); };
        function setPriceAlert() {
    const crop = getElement('alertCrop')?.value;
    const market = getElement('alertMarket')?.value;
    const targetPrice = parseInt(getElement('targetPrice')?.value);
    
    if (!targetPrice) { 
        alert('Please enter a valid target price'); 
        return; 
    }
    
    // Check current price immediately
    fetch(`/api/market/price?crop=${encodeURIComponent(crop)}&mandi=${market}`)
        .then(res => res.json())
        .then(data => {
            const isTriggered = data.price >= targetPrice;
            
            activeAlerts.push({ 
                id: Date.now(), 
                crop, 
                market, 
                targetPrice, 
                triggered: isTriggered,
                triggeredAt: isTriggered ? new Date().toISOString() : null,
                triggeredPrice: isTriggered ? data.price : null
            });
            
            saveAlerts();
            renderAlerts();
            
            if (isTriggered) {
                showNotification(crop, data.price, targetPrice, market);
                showExportSuccess(`⚠️ Alert triggered immediately! Current price (₹${data.price}) already meets your target.`);
            } else {
                showExportSuccess(`✅ Alert set for ${crop} at ₹${targetPrice}`);
            }
        })
        .catch(err => {
            console.error('Failed to check current price:', err);
            // Still save the alert even if price check fails
            activeAlerts.push({ 
                id: Date.now(), 
                crop, 
                market, 
                targetPrice, 
                triggered: false 
            });
            saveAlerts();
            renderAlerts();
            showExportSuccess(`✅ Alert set for ${crop} at ₹${targetPrice}`);
        });
    
    getElement('targetPrice').value = '';
}
        function testNotification() {
            if (!("Notification" in window)) { alert('Browser does not support notifications'); return; }
            Notification.requestPermission().then(permission => {
                if (permission === 'granted') {
                    new Notification('AgriOptima Test', { body: 'Notifications are working! You will receive price alerts.', icon: '/favicon.ico' });
                    showExportSuccess('Test notification sent!');
                } else { alert('Please allow notifications'); }
            });
        }
        function showNotification(crop, currentPrice, targetPrice, market) {
            if (!("Notification" in window)) {
                console.log('Notifications not supported');
                return;
            }
            
            if (Notification.permission === 'granted') {
                new Notification(`🎯 Price Alert: ${crop}`, {
                    body: `Price reached ₹${currentPrice}! Target was ₹${targetPrice}. Sell now at ${market}!`,
                    icon: '/favicon.ico'
                });
            } else if (Notification.permission !== 'denied') {
                Notification.requestPermission().then(permission => {
                    if (permission === 'granted') {
                        new Notification(`🎯 Price Alert: ${crop}`, {
                            body: `Price reached ₹${currentPrice}! Target was ₹${targetPrice}.`,
                            icon: '/favicon.ico'
                        });
                    }
                });
            }
        }
        // ============================================
        // HARVEST CALENDAR
        // ============================================
        function loadHarvests() {
            const saved = localStorage.getItem('agrioptima_harvests');
            if (saved) { harvestEvents = JSON.parse(saved); renderHarvests(); }
        }
        function saveHarvests() { localStorage.setItem('agrioptima_harvests', JSON.stringify(harvestEvents)); }
        function renderHarvests() {
            const container = getElement('upcomingHarvests');
            if (!container) return;
            if (harvestEvents.length === 0) { container.innerHTML = '<p style="color: var(--medium-gray);">No upcoming harvests scheduled</p>'; return; }
            container.innerHTML = harvestEvents.map(event => `<div style="background: white; border-left: 4px solid #4caf50; padding: 12px; border-radius: 8px; margin-bottom: 10px;"><div><strong>🌾 ${event.crop}</strong> - Plot ${event.plot}<br><small>Harvest: ${new Date(event.harvestDate).toLocaleDateString()}</small></div><div><button class="delete-alert-btn" onclick="window.deleteHarvest(${event.id})">Delete</button></div></div>`).join('');
        }
        window.deleteHarvest = function(id) { harvestEvents = harvestEvents.filter(e => e.id !== id); saveHarvests(); renderHarvests(); };
        function addHarvestEvent() {
            const crop = getElement('calendarCrop')?.value;
            const plot = getElement('plotSelect')?.value;
            const plantingDate = new Date(getElement('plantingDate')?.value);
            if (!plantingDate || isNaN(plantingDate.getTime())) { alert('Please select a planting date'); return; }
            const harvestDate = new Date(plantingDate);
            harvestDate.setDate(harvestDate.getDate() + (cropGrowingDays[crop] || 120));
            harvestEvents.push({ id: Date.now(), crop, plot, plantingDate: plantingDate.toISOString(), harvestDate: harvestDate.toISOString() });
            saveHarvests(); renderHarvests();
            showExportSuccess(`Harvest scheduled for ${crop}`);
        }
function exportToGoogleCalendar() {
    if (harvestEvents.length === 0) { 
        alert('No harvest events to export'); 
        return; 
    }
    
    // Get the first upcoming harvest (or let user choose)
    const event = harvestEvents[0];
    const harvestDate = new Date(event.harvestDate);
    const plantingDate = new Date(event.plantingDate);
    
    // Format dates for Google Calendar
    const start = harvestDate.toISOString().replace(/[-:]/g, '').split('.')[0] + 'Z';
    const end = new Date(harvestDate.getTime() + 86400000).toISOString().replace(/[-:]/g, '').split('.')[0] + 'Z';
    
    // Create detailed event description
    const details = `
🌾 Crop: ${event.crop}
📍 Plot: ${event.plot}
🌱 Planted: ${plantingDate.toLocaleDateString()}
📅 Harvest Date: ${harvestDate.toLocaleDateString()}

📋 Harvest Checklist:
□ Check crop readiness (color, size, firmness)
□ Prepare storage area
□ Arrange labor for harvesting
□ Clean and sanitize harvesting tools
□ Plan transport to market

💡 Tips for ${event.crop} harvest:
${getHarvestTips(event.crop)}

---
Generated by AgriOptima - Smart Crop Planning
    `.trim();
    
    // Encode the details for URL
    const encodedDetails = encodeURIComponent(details);
    
    // Create Google Calendar URL with full details
    const url = `https://calendar.google.com/calendar/render?action=TEMPLATE&text=🌾%20Harvest%20${encodeURIComponent(event.crop)}%20-%20Plot%20${event.plot}&dates=${start}/${start}&details=${encodedDetails}&location=Farm%20Plot%20${event.plot}`;
    
    window.open(url, '_blank');
    showExportSuccess(`📅 Harvest event added to Google Calendar for ${event.crop}`);
}

// Helper function for crop-specific harvest tips
function getHarvestTips(crop) {
    const tips = {
        'Rice': '• Harvest when 80% of grains turn golden yellow\n• Moisture content should be 18-22%\n• Cut early morning to reduce shattering',
        'Wheat': '• Harvest when stems turn yellow-brown\n• Grain moisture below 14%\n• Use combine harvester for efficiency',
        'Maize': '• Harvest when husks turn brown and dry\n• Kernels should be hard and shiny\n• Test moisture (below 25% for storage)',
        'Soybean': '• Harvest when pods rattle inside\n• Leaves have dropped\n• Moisture content 13-15%',
        'Tomato': '• Harvest when fruit is fully colored\n• Pick every 2-3 days\n• Handle gently to avoid bruising',
        'Onion': '• Harvest when tops fall over\n• Leaves turn yellow-brown\n• Cure for 2-3 weeks before storage',
        'Potato': '• Harvest when vines die back\n• Skin is firm (not peeling)\n• Avoid cutting during harvest',
        'Ginger': '• Harvest when leaves turn yellow\n• 8-10 months after planting\n• Use fork to avoid damaging rhizomes',
        'Garlic': '• Harvest when lower leaves brown\n• 5-6 leaves still green\n• Cure in shade for 2-3 weeks'
    };
    return tips[crop] || '• Check for optimal ripeness\n• Harvest in cool morning hours\n• Handle carefully to avoid damage';
}
        function downloadICS() {
            if (harvestEvents.length === 0) { alert('No harvest events to export'); return; }
            let ics = `BEGIN:VCALENDAR\nVERSION:2.0\n`;
            harvestEvents.forEach(event => {
                const harvestDate = new Date(event.harvestDate);
                const start = harvestDate.toISOString().replace(/[-:]/g, '').split('.')[0];
                const end = new Date(harvestDate.getTime() + 86400000).toISOString().replace(/[-:]/g, '').split('.')[0];
                ics += `BEGIN:VEVENT\nSUMMARY:Harvest ${event.crop} (Plot ${event.plot})\nDTSTART:${start}\nDTEND:${end}\nEND:VEVENT\n`;
            });
            ics += `END:VCALENDAR`;
            const blob = new Blob([ics], { type: 'text/calendar' });
            const link = document.createElement('a');
            link.href = URL.createObjectURL(blob);
            link.download = 'harvest_calendar.ics';
            link.click();
            showExportSuccess('ICS file downloaded!');
        }
        function enableNotifications() {
            if (!("Notification" in window)) { alert('Browser does not support notifications'); return; }
            Notification.requestPermission().then(permission => {
                if (permission === 'granted') showExportSuccess('Notifications enabled!');
                else alert('Please allow notifications');
            });
        }
        
        // ============================================
        // QR CODES
        // ============================================
        function generateQRCodes() {
            if (!currentOptimizationResult || !currentOptimizationResult.schedule) {
                alert('Please run optimization first to generate crop schedule data!');
                return;
            }
            const container = getElement('qrGrid');
            if (!container) return;
            const plotSelect = getElement('qrPlotSelect')?.value || 'all';
            const numPlots = parseInt(getElement('numPlots')?.value) || 4;
            const schedule = currentOptimizationResult.schedule;
            container.innerHTML = '';
            const plots = plotSelect === 'all' ? Array.from({ length: numPlots }, (_, i) => i + 1) : [parseInt(plotSelect)];
            plots.forEach(plot => {
                const cropHistory = schedule.map((season, idx) => ({
                    season: idx % 2 === 0 ? `Year ${Math.floor(idx/2)+1} Kharif` : `Year ${Math.floor(idx/2)+1} Rabi`,
                    crop: season[plot - 1] || 'Fallow'
                }));
                const qrData = { plotId: plot, cropHistory, generatedAt: new Date().toISOString() };
                const card = document.createElement('div');
                card.style.cssText = 'background:white;border-radius:12px;padding:15px;text-align:center;border:2px solid #4a7c43;';
                card.innerHTML = `<h4>🌾 Plot ${plot}</h4><div id="qr-${plot}"></div><button class="btn btn-secondary" style="margin-top:10px;" onclick="downloadQRImage(${plot})">Download</button>`;
                container.appendChild(card);
                new QRCode(document.getElementById(`qr-${plot}`), { text: JSON.stringify(qrData), width: 150, height: 150 });
            });
            getElement('qrCodesContainer').style.display = 'block';
            showExportSuccess(`QR codes generated for ${plots.length} plot(s)!`);
        }
        window.downloadQRImage = function(plot) {
            const canvas = document.querySelector(`#qr-${plot} canvas`);
            if (canvas) { const link = document.createElement('a'); link.download = `plot_${plot}_qr.png`; link.href = canvas.toDataURL(); link.click(); }
        };
        async function downloadAllQRCodes() {
            const canvases = document.querySelectorAll('#qrGrid canvas');
            if (canvases.length === 0) { alert('No QR codes to download'); return; }
            const zip = new JSZip();
            canvases.forEach((canvas, i) => zip.file(`plot_${i+1}_qr.png`, canvas.toDataURL().split(',')[1], { base64: true }));
            const content = await zip.generateAsync({ type: 'blob' });
            const link = document.createElement('a');
            link.href = URL.createObjectURL(content);
            link.download = `qrcodes_${new Date().toISOString().split('T')[0]}.zip`;
            link.click();
            showExportSuccess('ZIP downloaded!');
        }
        function printQRCodes() {
            const container = getElement('qrGrid');
            if (!container || container.children.length === 0) { alert('No QR codes to print'); return; }
            const win = window.open('', '_blank');
            win.document.write(`<html><head><title>QR Codes</title><style>body{font-family:Arial;padding:20px}.grid{display:grid;grid-template-columns:repeat(2,1fr);gap:20px}</style></head><body><h1>AgriOptima QR Codes</h1><div class="grid">${Array.from(container.children).map(card => { const canvas = card.querySelector('canvas'); return `<div><h3>${card.querySelector('h4')?.innerHTML}</h3>${canvas ? `<img src="${canvas.toDataURL()}">` : ''}</div>`; }).join('')}</div><button onclick="window.print()">Print</button></body></html>`);
            win.document.close();
        }
        
        // ============================================
        // EXPORT FUNCTIONS
        // ============================================
        function exportToCSV() {
            if (!currentOptimizationResult || !currentOptimizationResult.schedule) { alert('No plan data. Run optimization first.'); return; }
            const schedule = currentOptimizationResult.schedule;
            let csv = ['Season,' + Array.from({ length: schedule[0].length }, (_, i) => `Plot ${i+1}`).join(',')];
            schedule.forEach((season, idx) => {
                const year = Math.floor(idx / 2) + 1;
                const seasonLabel = idx % 2 === 0 ? `Year ${year} Kharif` : `Year ${year} Rabi`;
                csv.push(`"${seasonLabel}",${season.join(',')}`);
            });
            const blob = new Blob([csv.join('\n')], { type: 'text/csv' });
            const link = document.createElement('a');
            link.href = URL.createObjectURL(blob);
            link.download = `agrioptima_plan_${new Date().toISOString().split('T')[0]}.csv`;
            link.click();
            showExportSuccess('CSV downloaded!');
        }
        function exportToJSON() {
            if (!currentOptimizationResult) { alert('No plan data. Run optimization first.'); return; }
            const exportData = { exportDate: new Date().toISOString(), schedule: currentOptimizationResult.schedule, metrics: currentOptimizationResult.metrics, maxProfit: currentOptimizationResult.maxProfit };
            const blob = new Blob([JSON.stringify(exportData, null, 2)], { type: 'application/json' });
            const link = document.createElement('a');
            link.href = URL.createObjectURL(blob);
            link.download = `agrioptima_backup_${new Date().toISOString().split('T')[0]}.json`;
            link.click();
            showExportSuccess('JSON downloaded!');
        }
function printPlan() {
    console.log('[AgriOptima] printPlan called');
    
    // Try to get data from multiple possible sources
    let schedule = null;
    let maxProfit = 0;
    let metrics = null;
    
    // Check where the optimization result is stored
    if (currentOptimizationResult) {
        schedule = currentOptimizationResult.schedule;
        maxProfit = currentOptimizationResult.maxProfit;
        metrics = currentOptimizationResult.metrics;
    } else if (window.lastOptimizationResult) {
        schedule = window.lastOptimizationResult.schedule;
        maxProfit = window.lastOptimizationResult.maxProfit;
        metrics = window.lastOptimizationResult.metrics;
    } else if (window.currentResult) {
        schedule = window.currentResult.schedule;
        maxProfit = window.currentResult.maxProfit;
        metrics = window.currentResult.metrics;
    } else if (currentResult) {
        schedule = currentResult.schedule;
        maxProfit = currentResult.maxProfit;
        metrics = currentResult.metrics;
    }
    
    // Also try to get from DOM if needed
    if (!schedule || schedule.length === 0) {
        const scheduleContainer = document.getElementById('scheduleContainer');
        if (scheduleContainer && scheduleContainer.innerHTML.includes('schedule-table')) {
            alert('Please run optimization first to generate plan data.');
            return;
        }
        alert('No plan data available. Please run optimization first.');
        return;
    }
    
    const totalProfit = maxProfit?.toLocaleString() || '0';
    const waterUsed = metrics?.totalWaterUsed?.toLocaleString() || '0';
    const soilChange = metrics?.soilHealthChange || 0;
    const numPlots = document.getElementById('numPlots')?.value || 4;
    const years = document.getElementById('totalSeasons')?.value || 2;
    const waterBudget = document.getElementById('waterBudget')?.value || 10000;
    const initialSoilSelect = document.getElementById('initialSoil');
    const initialSoilText = initialSoilSelect?.options?.[initialSoilSelect.selectedIndex]?.text || 'Medium';
    
    const profitPerPlot = (maxProfit / numPlots).toLocaleString();
    const waterPercent = Math.round((waterUsed / waterBudget) * 100);
    
    // Collect all unique crops for rotation summary
    const allCrops = [];
    schedule.forEach(season => {
        season.forEach(crop => {
            if (crop && crop !== 'Fallow' && !allCrops.includes(crop)) allCrops.push(crop);
        });
    });
    
    const win = window.open('', '_blank');
    
    win.document.write(`<!DOCTYPE html>
    <html>
    <head>
        <title>AgriOptima Crop Plan - ${new Date().toLocaleDateString()}</title>
        <style>
            * { margin: 0; padding: 0; box-sizing: border-box; }
            body {
                font-family: 'Segoe UI', 'Poppins', Arial, sans-serif;
                background: linear-gradient(135deg, #f5f7f0 0%, #e8f0e5 100%);
                padding: 40px;
                position: relative;
            }
            
            body::before {
                content: "";
                position: fixed;
                top: 0;
                left: 0;
                right: 0;
                bottom: 0;
                background-image: radial-gradient(circle at 10% 20%, rgba(74, 124, 67, 0.05) 2px, transparent 2px);
                background-size: 40px 40px;
                pointer-events: none;
                z-index: 0;
            }
            
            .report-container {
                max-width: 1200px;
                margin: 0 auto;
                background: white;
                border-radius: 24px;
                box-shadow: 0 20px 60px rgba(0,0,0,0.15);
                overflow: hidden;
                position: relative;
                z-index: 1;
            }
            
            .report-header {
                background: linear-gradient(135deg, #1B3A2E 0%, #4E8B6A 50%, #7AA96F 100%);
                color: white;
                padding: 40px;
                text-align: center;
                position: relative;
                overflow: hidden;
            }
            
            .report-header::before {
                content: "🌾🌱🌿";
                position: absolute;
                font-size: 120px;
                opacity: 0.1;
                bottom: -20px;
                right: -20px;
                transform: rotate(-15deg);
            }
            
            .report-header::after {
                content: "🌾🌱🌿";
                position: absolute;
                font-size: 80px;
                opacity: 0.08;
                top: -20px;
                left: -20px;
                transform: rotate(15deg);
            }
            
            .report-header h1 {
                font-size: 2.5rem;
                margin-bottom: 10px;
                letter-spacing: -0.5px;
                position: relative;
                z-index: 1;
            }
            
            .report-header p {
                opacity: 0.9;
                font-size: 1rem;
                position: relative;
                z-index: 1;
            }
            
            .stats-grid {
                display: grid;
                grid-template-columns: repeat(4, 1fr);
                gap: 20px;
                padding: 30px;
                background: #f9fbf8;
                border-bottom: 1px solid #e0e8dc;
            }
            
            .stat-card {
                text-align: center;
                padding: 20px;
                background: white;
                border-radius: 16px;
                box-shadow: 0 2px 8px rgba(0,0,0,0.05);
                transition: transform 0.2s;
                border: 1px solid #e0e8dc;
            }
            
            .stat-emoji { font-size: 2rem; margin-bottom: 8px; }
            .stat-value { font-size: 1.8rem; font-weight: 700; color: #1B3A2E; }
            .stat-label { font-size: 0.75rem; color: #7A8F85; text-transform: uppercase; letter-spacing: 1px; margin-top: 5px; }
            
            .section {
                padding: 30px;
                border-bottom: 1px solid #e0e8dc;
            }
            
            .section-title {
                font-size: 1.4rem;
                color: #1B3A2E;
                margin-bottom: 20px;
                padding-bottom: 10px;
                border-bottom: 3px solid #4E8B6A;
                display: inline-block;
            }
            
            .rotation-timeline {
                background: linear-gradient(135deg, #f0f5ed, #e8f0e5);
                border-radius: 16px;
                padding: 20px;
                margin-bottom: 20px;
            }
            
            .timeline-title {
                font-weight: 600;
                color: #4E8B6A;
                margin-bottom: 15px;
                font-size: 0.9rem;
                text-transform: uppercase;
            }
            
            .crop-badge {
                display: inline-block;
                background: white;
                padding: 6px 14px;
                margin: 4px;
                border-radius: 20px;
                font-size: 0.85rem;
                box-shadow: 0 1px 3px rgba(0,0,0,0.1);
                border-left: 3px solid #4E8B6A;
            }
            
            .schedule-table {
                width: 100%;
                border-collapse: collapse;
                margin-top: 20px;
                border-radius: 12px;
                overflow: hidden;
                box-shadow: 0 2px 8px rgba(0,0,0,0.08);
            }
            
            .schedule-table th {
                background: linear-gradient(135deg, #4E8B6A, #6BA86D);
                color: white;
                padding: 12px;
                text-align: center;
                font-weight: 600;
                font-size: 0.85rem;
            }
            
            .schedule-table td {
                padding: 10px;
                text-align: center;
                border-bottom: 1px solid #e0e8dc;
                background: white;
            }
            
            .crop-cell {
                background: linear-gradient(135deg, #e8f5e9, #c8e6c9);
                padding: 6px 12px;
                border-radius: 20px;
                display: inline-block;
                font-size: 0.85rem;
                font-weight: 500;
            }
            
            .insights-grid {
                display: grid;
                grid-template-columns: repeat(2, 1fr);
                gap: 20px;
                margin-top: 20px;
            }
            
            .insight-card {
                background: linear-gradient(135deg, #f0f5ed, #ffffff);
                padding: 20px;
                border-radius: 16px;
                border-left: 4px solid #4E8B6A;
            }
            
            .insight-icon { font-size: 1.5rem; margin-bottom: 10px; }
            .insight-text { font-size: 0.9rem; color: #4a5568; line-height: 1.5; }
            
            .farm-illustration {
                text-align: center;
                margin: 20px 0;
                padding: 20px;
                background: linear-gradient(135deg, #fdfbf7, #f9f5e8);
                border-radius: 20px;
            }
            
            .report-footer {
                background: #1B3A2E;
                color: rgba(255,255,255,0.7);
                padding: 20px;
                text-align: center;
                font-size: 0.75rem;
            }
            
            @media print {
                body { background: white; padding: 20px; }
                .stats-grid { break-inside: avoid; }
                .section { break-inside: avoid; }
                .schedule-table { break-inside: avoid; }
                .report-header { background: #1B3A2E; -webkit-print-color-adjust: exact; print-color-adjust: exact; }
                .schedule-table th { background: #4E8B6A; -webkit-print-color-adjust: exact; print-color-adjust: exact; }
                .crop-cell { -webkit-print-color-adjust: exact; print-color-adjust: exact; }
            }
        </style>
    </head>
    <body>
        <div class="report-container">
            <div class="report-header">
                <h1>🌾 AgriOptima <span style="font-weight: 300;">| Smart Crop Plan</span></h1>
                <p>Generated on ${new Date().toLocaleString()}</p>
                <p style="font-size: 0.85rem; margin-top: 8px;">${years} Year Plan | ${numPlots} Plots | ${initialSoilText} Soil</p>
            </div>
            
<!-- Stats & Rotation with Background Image -->
<div style="position: relative; overflow: hidden; background: #f9fbf8;">
    <!-- Background Image - CHANGE THE URL BELOW TO YOUR IMAGE -->
    <div style="position: absolute; top: 0; left: 0; right: 0; bottom: 0; opacity: 3; pointer-events: none; z-index: 0;">
        <img src="photo.jpg" alt="Farm landscape" style="width: 100%; height: 100%; object-fit: cover;">
    </div>
    <div style="position: relative; z-index: 1;">
        <!-- Stats Grid -->
        <div class="stats-grid">
            <div class="stat-card"><div class="stat-emoji">💰</div><div class="stat-value">₹${totalProfit}</div><div class="stat-label">Total Projected Profit</div></div>
            <div class="stat-card"><div class="stat-emoji">💧</div><div class="stat-value">${waterUsed} L</div><div class="stat-label">Total Water Usage</div></div>
            <div class="stat-card"><div class="stat-emoji">🌱</div><div class="stat-value">${soilChange >= 0 ? '+' : ''}${soilChange}</div><div class="stat-label">Soil Health Change</div></div>
            <div class="stat-card"><div class="stat-emoji">📊</div><div class="stat-value">₹${profitPerPlot}</div><div class="stat-label">Profit per Plot</div></div>
        </div>
        
        <!-- Rotation Timeline -->
        <div class="section">
            <div class="rotation-timeline">
                <div class="timeline-title">🔄 CROP ROTATION SUMMARY</div>
                <div>${allCrops.map(c => `<span class="crop-badge">${c}</span>`).join('')}</div>
                <div style="margin-top: 12px; font-size: 0.8rem; color: #666;">Recommended rotation sequence for optimal soil health</div>
            </div>
        </div>
    </div>
</div>
            
            <div class="section">
                <h2 class="section-title">📅 Seasonal Planting Schedule</h2>
                <table class="schedule-table">
                    <thead><tr><th>Season</th>${Array.from({ length: schedule[0].length }, (_, i) => `<th>Plot ${i + 1}</th>`).join('')}</thead>
                    <tbody>
                        ${schedule.map((season, idx) => {
                            const year = Math.floor(idx / 2) + 1;
                            const seasonLabel = idx % 2 === 0 ? `Year ${year} Kharif 🌧️` : `Year ${year} Rabi ☀️`;
                            return `<tr>
                                <td style="background: #f5f9f2; font-weight: 600;">${seasonLabel}</td>
                                ${season.map(crop => {
                                    let emoji = crop?.includes('Rice') ? '🌾' : crop?.includes('Wheat') ? '🌾' : crop?.includes('Soybean') ? '🥜' : crop?.includes('Maize') ? '🌽' : crop === 'Fallow' ? '🧹' : '🌱';
                                    return `<td><span class="crop-cell">${emoji} ${crop || 'Fallow'}</span></td>`;
                                }).join('')}
                            </tr>`;
                        }).join('')}
                    </tbody>
                </table>
            </div>
            
            <div class="section">
                <h2 class="section-title">💡 Insights & Recommendations</h2>
                <div class="insights-grid">
                    <div class="insight-card"><div class="insight-icon">💧</div><div class="insight-text"><strong>Water Efficiency</strong><br>${waterPercent}% of budget utilized (${waterUsed} L / ${waterBudget} L)</div></div>
                    <div class="insight-card"><div class="insight-icon">🌱</div><div class="insight-text"><strong>Soil Health</strong><br>${soilChange >= 0 ? 'Improving 📈' : 'Declining 📉'} - ${Math.abs(soilChange)} level change over ${years} years</div></div>
                    <div class="insight-card"><div class="insight-icon">🔄</div><div class="insight-text"><strong>Rotation Diversity</strong><br>${allCrops.length} different crops in rotation plan</div></div>
                    <div class="insight-card"><div class="insight-icon">📈</div><div class="insight-text"><strong>Profitability</strong><br>Average ₹${Math.round(maxProfit / (years * 2 * numPlots))} per plot per season</div></div>
                </div>
            </div>
            
            <div class="report-footer">
                <p>🌾 Generated by AgriOptima - AI-Powered Crop Planning System</p>
                <p style="margin-top: 8px;">This report is optimized for ${initialSoilText} soil conditions | Water budget: ${waterBudget.toLocaleString()} L</p>
            </div>
        </div>
        <div style="text-align: center; margin-top: 20px;">
            <button onclick="window.print()" style="background: #4E8B6A; color: white; border: none; padding: 12px 24px; border-radius: 8px; cursor: pointer; font-size: 16px; margin: 0 5px;">🖨️ Print / Save as PDF</button>
            <button onclick="window.close()" style="background: #7A8F85; color: white; border: none; padding: 12px 24px; border-radius: 8px; cursor: pointer; margin: 0 5px;">❌ Close</button>
        </div>
    </body>
    </html>`);
    win.document.close();
}
        // ============================================
        // EVENT LISTENERS
        // ============================================
        function initEventListeners() {
            getElement('runBtn')?.addEventListener('click', () => runOptimization(false));
            getElement('whatIfBtn')?.addEventListener('click', () => runOptimization(true));
            getElement('addCropBtn')?.addEventListener('click', () => addCropRow());
            getElement('clearCropsBtn')?.addEventListener('click', () => { disableQRFeatures(); if (confirm('Clear all crops?')) getElement('cropRows').innerHTML = ''; });
            getElement('resetDefaultCropsBtn')?.addEventListener('click', resetToDefaultCrops);
            getElement('refreshMarketBtn')?.addEventListener('click', updateMarketData);
            getElement('marketCropSelect')?.addEventListener('change', updateMarketData);
            getElement('marketRegionSelect')?.addEventListener('change', updateMarketData);
            getElement('setAlertBtn')?.addEventListener('click', setPriceAlert);
            getElement('testNotificationBtn')?.addEventListener('click', testNotification);
            getElement('addToCalendarBtn')?.addEventListener('click', addHarvestEvent);
            getElement('exportCalendarBtn')?.addEventListener('click', exportToGoogleCalendar);
            getElement('downloadCalendarBtn')?.addEventListener('click', downloadICS);
            getElement('enableNotificationsBtn')?.addEventListener('click', enableNotifications);
            getElement('generateQrBtn')?.addEventListener('click', generateQRCodes);
            getElement('downloadAllQrBtn')?.addEventListener('click', downloadAllQRCodes);
            getElement('printQrCodesBtn')?.addEventListener('click', printQRCodes);
            getElement('hideQrBtn')?.addEventListener('click', () => { getElement('qrCodesContainer').style.display = 'none'; });
            getElement('exportCsvBtn')?.addEventListener('click', exportToCSV);
            getElement('exportJsonBtn')?.addEventListener('click', exportToJSON);
            getElement('printPlanBtn')?.addEventListener('click', printPlan);
            document.getElementById('printPlanBtn')?.addEventListener('click', printPlan);
            // Update QR dropdown when number of plots changes
// Update harvest plot dropdown when number of plots changes
            const numPlotsInput = getElement('numPlots');
            if (numPlotsInput) {
                numPlotsInput.addEventListener('change', () => {
                    updateQRPlotSelect();
                    updateHarvestPlotSelect();
                });
                numPlotsInput.addEventListener('input', () => {
                    updateQRPlotSelect();
                    updateHarvestPlotSelect();
                });
            }
            const waterSlider = getElement('waterSlider');
            const waterSliderVal = getElement('waterSliderVal');
            if (waterSlider && waterSliderVal) {
                waterSlider.addEventListener('input', (e) => { waterSliderVal.textContent = Number(e.target.value).toLocaleString() + ' L'; });
            }
            
            document.querySelectorAll('.goal-card').forEach(card => {
                card.addEventListener('click', () => {
                    document.querySelectorAll('.goal-card').forEach(c => c.classList.remove('active'));
                    card.classList.add('active');
                });
            });
            
            const plantingDate = getElement('plantingDate');
            if (plantingDate) plantingDate.value = new Date().toISOString().split('T')[0];
        }
        
        // ============================================
        // INITIALIZATION
        // ============================================
        function init() {
            console.log('[AgriOptima] Initializing complete application...');
            initCropLibrary();
            resetToDefaultCrops();
            setupDragDrop();
            initEventListeners();
            updateMarketData();
            loadAlerts();
            loadHarvests();
            // Initialize QR plot dropdown
            updateQRPlotSelect();
            // Initialize harvest plot dropdown
            updateHarvestPlotSelect();
            // Inside your init() function or DOMContentLoaded event
if (window.AgriOptima && window.AgriOptima.initCropAnimations) {
    window.AgriOptima.initCropAnimations();
    console.log('✅ Crop animations started');
}
            // Disable QR button initially
            const qrBtn = getElement('generateQrBtn');
            if (qrBtn) { qrBtn.disabled = true; qrBtn.style.opacity = '0.6'; qrBtn.style.cursor = 'not-allowed'; }
            
            console.log('[AgriOptima] Ready! Crop rows:', document.querySelectorAll('#cropRows tr').length);
        }
        
        if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init);
        else init();
    })();