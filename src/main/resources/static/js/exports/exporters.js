/**
 * exports/exporters.js
 * Export & share utilities: CSV/Excel/JSON/PDF/Print + QR Code.
 */

(function () {
  console.log('Exporters module loaded');

  // Helper: safe DOM access
  function getElement(id) {
    return document.getElementById(id);
  }

  function getScheduleFromDOM() {
    const table = document.querySelector('.schedule-table');
    if (!table) return [];
    const schedule = [];
    const rows = table.querySelectorAll('tbody tr');
    rows.forEach(row => {
      const cells = row.querySelectorAll('td');
      const seasonCrops = [];
      cells.forEach((cell, idx) => {
        if (idx > 0) {
          const plotNameEl = cell.querySelector('.plot-cell span:last-child');
          seasonCrops.push(plotNameEl?.textContent || 'Fallow');
        }
      });
      if (seasonCrops.length > 0) schedule.push(seasonCrops);
    });
    return schedule;
  }

 function getCurrentPlanData() {
    // FIRST: Try to get data from global optimization result (set by your app)
    if (window.lastOptimizationResult && window.lastOptimizationResult.schedule) {
        console.log('Using window.lastOptimizationResult');
        const schedule = window.lastOptimizationResult.schedule;
        const metrics = {
            profit: `₹${window.lastOptimizationResult.maxProfit?.toLocaleString() || '0'}`,
            water: `${window.lastOptimizationResult.metrics?.totalWaterUsed?.toLocaleString() || 0} L`,
            soilChange: window.lastOptimizationResult.metrics?.soilHealthChange || 0,
            rotationCompliance: '100%'
        };
        const farmConfig = {
            plots: window.lastFarmConfig?.plots || document.getElementById('numPlots')?.value || 4,
            years: window.lastFarmConfig?.years || document.getElementById('totalSeasons')?.value || 2,
            waterBudget: window.lastFarmConfig?.waterBudget || document.getElementById('waterBudget')?.value || 10000,
            initialSoil: window.lastFarmConfig?.initialSoil || document.getElementById('initialSoil')?.value || 'Medium'
        };
        console.log('Schedule data:', schedule);
        return { schedule, metrics, farmConfig };
    }
    
    // SECOND: Try to get from DOM table (if schedule is rendered)
    const table = document.querySelector('.schedule-table');
    if (table && table.querySelectorAll('tbody tr').length > 0) {
        console.log('Reading schedule from DOM table');
        const schedule = [];
        const rows = table.querySelectorAll('tbody tr');
        rows.forEach(row => {
            const cells = row.querySelectorAll('td');
            const seasonCrops = [];
            cells.forEach((cell, idx) => {
                if (idx > 0) {
                    const plotCell = cell.querySelector('.plot-cell');
                    const cropName = plotCell?.querySelector('span:last-child')?.textContent || 
                                    plotCell?.textContent?.trim() || 
                                    'Fallow';
                    seasonCrops.push(cropName);
                }
            });
            if (seasonCrops.length > 0) schedule.push(seasonCrops);
        });
        
        if (schedule.length > 0) {
            const metrics = {
                profit: document.getElementById('detail-profit')?.textContent || '₹0',
                water: document.getElementById('detail-water')?.textContent || '0 L',
                soilChange: document.getElementById('detail-soil')?.textContent || '0',
                rotationCompliance: '100%'
            };
            const farmConfig = {
                plots: document.getElementById('numPlots')?.value || 4,
                years: document.getElementById('totalSeasons')?.value || 2,
                waterBudget: document.getElementById('waterBudget')?.value || 10000,
                initialSoil: document.getElementById('initialSoil')?.options?.[document.getElementById('initialSoil')?.selectedIndex]?.text || 'Medium'
            };
            return { schedule, metrics, farmConfig };
        }
    }
    
    // THIRD: Try to get from global currentResult (set by inline script)
    if (window.currentResult && window.currentResult.schedule) {
        console.log('Using window.currentResult');
        const schedule = window.currentResult.schedule;
        const metrics = {
            profit: `₹${window.currentResult.maxProfit?.toLocaleString() || '0'}`,
            water: `${window.currentResult.metrics?.totalWaterUsed?.toLocaleString() || 0} L`,
            soilChange: window.currentResult.metrics?.soilHealthChange || 0,
            rotationCompliance: '100%'
        };
        const farmConfig = {
            plots: document.getElementById('numPlots')?.value || 4,
            years: document.getElementById('totalSeasons')?.value || 2,
            waterBudget: document.getElementById('waterBudget')?.value || 10000,
            initialSoil: document.getElementById('initialSoil')?.options?.[document.getElementById('initialSoil')?.selectedIndex]?.text || 'Medium'
        };
        return { schedule, metrics, farmConfig };
    }
    
    // LAST RESORT: Return empty (will show alert)
    console.warn('No optimization data found');
    return { schedule: [], metrics: {}, farmConfig: {} };
}

  function showExportSuccess(message) {
    const preview = document.getElementById('exportPreview');
    if (!preview) return;
    preview.style.display = 'block';
    const msgElement = preview.querySelector('p');
    if (msgElement) msgElement.textContent = `✅ ${message}`;
    setTimeout(() => {
      preview.style.display = 'none';
    }, 3000);
  }

  function exportToCSV() { /* your existing code */ }
  function exportToJSON() { /* your existing code */ }
  function printPlan() { /* your existing code */ }
  async function exportToPDF() { /* your existing code */ }

  // ---------- QR CODE GENERATION (fixed) ----------
// ---------- QR CODE GENERATION (server-stored) ----------
async function generateQRCode() {
    console.log('generateQRCode called');
    const { schedule, metrics, farmConfig } = getCurrentPlanData();
    if (!schedule || schedule.length === 0) {
        alert('No plan data available. Please run optimization first.');
        return;
    }

    // Create simplified data (matches what qr-viewer.html expects)
    const exportData = {
        v: '1.0',
        date: new Date().toISOString(),
        plots: parseInt(farmConfig.plots),
        years: parseInt(farmConfig.years),
        waterBudget: parseInt(farmConfig.waterBudget),
        schedule: schedule,
        profit: metrics.profit,
        water: metrics.water,
        soil: metrics.soilChange
    };

    // Convert to JSON and encode
    const jsonStr = JSON.stringify(exportData);
    const encoded = encodeURIComponent(jsonStr);
    const qrUrl = window.location.origin + '/qr-viewer.html?data=' + encoded;
    
    console.log('QR URL length:', qrUrl.length);

    // Load QR library and show modal
    if (typeof qrcode === 'undefined') {
        const script = document.createElement('script');
        script.src = 'https://cdn.jsdelivr.net/npm/qrcode-generator@1.4.4/qrcode.min.js';
        script.onload = () => {
            console.log('QR library loaded');
            showQRModal(qrUrl, exportData);
        };
        script.onerror = () => {
            alert('Failed to load QR library. Please check your internet connection.');
        };
        document.head.appendChild(script);
    } else {
        showQRModal(qrUrl, exportData);
    }
}

function showQRModal(url, planData) {
    console.log('showQRModal called');
    // Remove existing modal
    const oldModal = document.getElementById('qrModal');
    if (oldModal) oldModal.remove();

    // Create modal container (styles now come from CSS file)
    const modal = document.createElement('div');
    modal.id = 'qrModal';
    modal.className = 'qr-modal';
    modal.innerHTML = `
        <div class="qr-modal-content">
            <div class="qr-modal-header">
                <h2>📱 Share Your Crop Plan</h2>
                <span class="qr-close">&times;</span>
            </div>
            <div class="qr-modal-body">
                <div style="text-align: center;">
                    <canvas id="qrCanvas" width="250" height="250" style="margin: 20px auto; display: block; border: 1px solid #ddd; border-radius: 12px;"></canvas>
                    <p style="margin: 15px 0;">Scan to view your personalized crop plan</p>
                    <div class="qr-options">
                        <button id="downloadQrBtn" class="qr-btn qr-btn-primary">💾 Download QR</button>
                        <button id="shareQrBtn" class="qr-btn qr-btn-secondary">📤 Share QR</button>
                    </div>
                    <div class="qr-preview-info">
                        <h4>✨ When scanned, users will see:</h4>
                        <ul>
                            <li>📊 Interactive profit & water metrics</li>
                            <li>📅 Visual crop schedule with emojis</li>
                            <li>📥 Download CSV/JSON options</li>
                            <li>📱 Mobile-optimized design</li>
                        </ul>
                    </div>
                    <div class="qr-url-box">
                        <input type="text" id="qrUrlInput" value="${url}" readonly>
                        <button id="copyUrlBtn">Copy Link</button>
                    </div>
                </div>
            </div>
        </div>
    `;

    document.body.appendChild(modal);

    // Generate QR code on canvas (rest of the code remains the same)
    if (typeof qrcode !== 'undefined') {
        const qr = qrcode(40, 'L');
        qr.addData(url);
        qr.make();
        const cellSize = 4;
        const margin = 4;
        const size = qr.getModuleCount();
        const canvasSize = (size + margin * 2) * cellSize;
        const canvas = document.getElementById('qrCanvas');
        canvas.width = canvasSize;
        canvas.height = canvasSize;
        const ctx = canvas.getContext('2d');
        ctx.fillStyle = '#ffffff';
        ctx.fillRect(0, 0, canvasSize, canvasSize);
        ctx.fillStyle = '#000000';
        for (let row = 0; row < size; row++) {
            for (let col = 0; col < size; col++) {
                if (qr.isDark(row, col)) {
                    ctx.fillRect((col + margin) * cellSize, (row + margin) * cellSize, cellSize, cellSize);
                }
            }
        }
    } else {
        document.getElementById('qrCanvas').outerHTML = '<div style="color:red;">QR library failed to load. Please refresh.</div>';
    }

    // Event handlers
    document.querySelector('.qr-close').onclick = () => modal.remove();
    window.onclick = (e) => { if (e.target === modal) modal.remove(); };
    
    document.getElementById('downloadQrBtn').onclick = () => {
        const canvas = document.getElementById('qrCanvas');
        if (canvas) {
            const link = document.createElement('a');
            link.download = `agrioptima_qr.png`;
            link.href = canvas.toDataURL();
            link.click();
            showExportSuccess('QR Code downloaded');
        }
    };
    
    document.getElementById('shareQrBtn').onclick = async () => {
        const canvas = document.getElementById('qrCanvas');
        if (canvas && navigator.share && window.isSecureContext !== false) {
            try {
                const blob = await new Promise(resolve => canvas.toBlob(resolve, 'image/png'));
                await navigator.share({
                    title: 'AgriOptima Crop Plan',
                    files: [new File([blob], 'agrioptima_qr.png', { type: 'image/png' })]
                });
            } catch (err) {
                console.warn('Share failed:', err);
                // Fallback to copy link
                const input = document.getElementById('qrUrlInput');
                if (input) {
                    input.select();
                    document.execCommand('copy');
                    showExportSuccess('Link copied to clipboard (share not available)');
                }
            }
        } else {
            const input = document.getElementById('qrUrlInput');
            if (input) {
                input.select();
                document.execCommand('copy');
                showExportSuccess('Link copied to clipboard');
            }
        }
    };
    
    document.getElementById('copyUrlBtn').onclick = () => {
        const input = document.getElementById('qrUrlInput');
        input.select();
        document.execCommand('copy');
        showExportSuccess('Link copied');
    };
}

  // Expose functions globally
  window.AgriOptima = window.AgriOptima || {};
  window.AgriOptima.generateQRCode = generateQRCode;
  window.AgriOptima.exportToCSV = exportToCSV;
  window.AgriOptima.exportToJSON = exportToJSON;
  window.AgriOptima.printPlan = printPlan;
  window.AgriOptima.getCurrentPlanData = getCurrentPlanData;
  window.AgriOptima.showExportSuccess = showExportSuccess;
  window.AgriOptima.initExportsUI = initExportsUI;

  function initExportsUI() {
    console.log('initExportsUI called');
    const csvBtn = document.getElementById('exportCsvBtn');
    if (csvBtn) csvBtn.addEventListener('click', exportToCSV);
    const jsonBtn = document.getElementById('exportJsonBtn');
    if (jsonBtn) jsonBtn.addEventListener('click', exportToJSON);
    const printBtn = document.getElementById('printPlanBtn');
    if (printBtn) printBtn.addEventListener('click', printPlan);
    const qrBtn = document.getElementById('qrCodeBtn');
    if (qrBtn) {
      console.log('QR button found, attaching listener');
      qrBtn.addEventListener('click', generateQRCode);
    } else {
      console.warn('QR button not found in DOM');
    }
  }

  // Auto-initialize when DOM ready
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initExportsUI);
  } else {
    initExportsUI();
  }
})();