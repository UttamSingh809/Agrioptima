/**
 * qr/qrSystem.js
 * QR code generation + download/print + scanner.
 * This split keeps handlers guarded so it won't break index.html.
 */

(function () {
  let qrInstances = [];
  let scannerStream = null;

  function getScheduleFromDOM() {
    return window.AgriOptima?.getCurrentPlanData?.()?.schedule || [];
  }

  function getFarmConfigFromDOM() {
    return window.AgriOptima?.getCurrentPlanData?.()?.farmConfig || {};
  }

  function getPlotStateInfo(plotNum) {
    // Get soil health from KPI display
    const soilChangeEl = document.getElementById('detail-soil');
    const soilChange = soilChangeEl ? parseInt(soilChangeEl.textContent) || 0 : 0;
    
    // Calculate estimated soil level for this plot based on crop history
    const schedule = getScheduleFromDOM();
    let currentSoilLevel = 1; // Start at medium (0=low, 1=medium, 2=high)
    let lastCrop = 'None';
    
    schedule.forEach((season, idx) => {
      const crop = season[plotNum - 1];
      if (crop && crop !== 'Fallow') {
        lastCrop = crop;
        // Estimate soil impact (simplified - in real app would use crop data)
        if (['Legumes', 'Pulses', 'Groundnut'].some(c => crop.includes(c))) {
          currentSoilLevel = Math.min(2, currentSoilLevel + 1);
        } else if (['Rice', 'Sugarcane', 'Maize'].some(c => crop.includes(c))) {
          currentSoilLevel = Math.max(0, currentSoilLevel - 1);
        }
      }
    });
    
    // Get moisture estimate based on recent crops
    let moistureLevel = 'Medium';
    if (['Rice', 'Sugarcane'].some(c => schedule[schedule.length - 1]?.[plotNum - 1]?.includes(c))) {
      moistureLevel = 'High';
    } else if (['Millets', 'Pulses', 'Groundnut'].some(c => schedule[schedule.length - 1]?.[plotNum - 1]?.includes(c))) {
      moistureLevel = 'Low';
    }
    
    // Calculate area (assuming equal distribution)
    const farmConfig = getFarmConfigFromDOM();
    const totalArea = 10; // Assume 10 acres total
    const areaPerPlot = (totalArea / (farmConfig.plots || 4)).toFixed(2);
    
    return {
      plotNumber: plotNum,
      currentSoilNitrogenLevel: currentSoilLevel,
      soilHealthStatus: currentSoilLevel === 2 ? 'High Fertility' : currentSoilLevel === 1 ? 'Medium Fertility' : 'Low Fertility',
      soilChangeTrend: soilChange >= 0 ? `+${soilChange} (Improving)` : `${soilChange} (Declining)`,
      lastCropPlanted: lastCrop,
      moistureLevel: moistureLevel,
      areaAcres: areaPerPlot,
      location: `Plot ${plotNum} - Field Section ${Math.ceil(plotNum / 2)}`,
      ownership: 'Farm Owner',
      plantingHistory: schedule.filter((_, idx) => idx % 2 === 0).map((season, idx) => ({
        year: idx + 1,
        season: 'Kharif',
        crop: season[plotNum - 1] || 'Fallow'
      })).concat(
        schedule.filter((_, idx) => idx % 2 === 1).map((season, idx) => ({
          year: idx + 1,
          season: 'Rabi',
          crop: season[plotNum - 1] || 'Fallow'
        }))
      )
    };
  }

  function getHarvestEventsForPlot(plotNum) {
    const events = JSON.parse(localStorage.getItem('agrioptima_harvests') || '[]');
    return events
      .filter(event => parseInt(event.plot) === plotNum)
      .map(event => ({
        crop: event.crop,
        harvestDate: new Date(event.harvestDate).toLocaleDateString(),
        daysToHarvest: Math.max(0, Math.ceil((new Date(event.harvestDate) - new Date()) / (1000 * 60 * 60 * 24)))
      }));
  }

  function downloadSingleQRCode(plotNum) {
    const qrElement = document.querySelector(`#qrcode-${plotNum} canvas`);
    if (!qrElement) {
      alert('QR code not found. Please regenerate first.');
      return;
    }

    const link = document.createElement('a');
    link.download = `plot_${plotNum}_qr.png`;
    link.href = qrElement.toDataURL();
    link.click();
    window.AgriOptima?.showExportSuccess?.(`📥 Downloaded QR code for Plot ${plotNum}`);
  }

  function generateQRCodes() {
    const generateBtn = document.getElementById('generateQrBtn');
    const container = document.getElementById('qrGrid');
    if (!container) return;

    const plotSelect = document.getElementById('qrPlotSelect')?.value || 'all';

    // STRICT CHECK: Must have valid optimization data
    const planData = window.AgriOptima?.getCurrentPlanData?.();
    console.log('[AgriOptima][QR] Plan data check:', planData);
    
    // Check if we have real optimization data (not just empty defaults)
    if (!planData || !planData.schedule || planData.schedule.length === 0) {
      alert('❌ Please run optimization first to generate crop schedule data!\n\nSteps:\n1. Go to Optimization tab\n2. Enter farm details\n3. Click "Run Optimization"\n4. Then come back here to generate QR codes');
      return;
    }
    
    // Additional check: ensure schedule has actual crop data
    const hasValidSchedule = planData.schedule.some(season => 
      season && season.length > 0 && season.some(crop => crop && crop !== 'Fallow')
    );
    
    if (!hasValidSchedule) {
      alert('❌ Optimization did not produce valid crop data!\nPlease re-run optimization with different parameters.');
      return;
    }

    const schedule = planData.schedule;
    const farmConfig = planData.farmConfig || { plots: 4, years: 2, waterBudget: 10000 };
    console.log('[AgriOptima][QR] Generating QR codes for', schedule.length, 'seasons and', farmConfig.plots, 'plots');

    container.innerHTML = '';
    qrInstances = [];

    const plotsToGenerate = plotSelect === 'all'
      ? Array.from({ length: parseInt(farmConfig.plots) || 4 }, (_, i) => i + 1)
      : [parseInt(plotSelect)];

    plotsToGenerate.forEach(plotNum => {
      const cropHistory = [];
      schedule.forEach((season, seasonIdx) => {
        const year = Math.floor(seasonIdx / 2) + 1;
        const seasonType = seasonIdx % 2 === 0 ? 'Kharif' : 'Rabi';
        const crop = season[plotNum - 1] || 'Fallow';
        cropHistory.push({
          season: `Year ${year} - ${seasonType}`,
          crop
        });
      });

      const plotStateInfo = getPlotStateInfo(plotNum);
      const upcomingHarvests = getHarvestEventsForPlot(plotNum);

      const qrData = {
        plotId: plotNum,
        plotState: plotStateInfo,
        farmConfig: {
          totalPlots: farmConfig.plots,
          planningYears: farmConfig.years,
          waterBudget: farmConfig.waterBudget
        },
        cropHistory,
        upcomingHarvests,
        generatedAt: new Date().toISOString(),
        lastUpdated: new Date().toLocaleString()
      };

      const qrCard = document.createElement('div');
      qrCard.style.cssText = `background:white;border-radius:16px;padding:20px;text-align:center;box-shadow:var(--shadow-md);border:2px solid var(--primary-green);`;

      // Add detailed plot state preview on card
      const soilStatus = plotStateInfo.soilHealthStatus || 'Unknown';
      const moistureStatus = plotStateInfo.moistureLevel || 'Unknown';
      const lastCrop = plotStateInfo.lastCropPlanted || 'None';

      qrCard.innerHTML = `
        <h4 style="color: var(--primary-dark); margin-bottom: 10px;">🌾 Plot ${plotNum}</h4>
        <div id="qrcode-${plotNum}" style="display:flex;justify-content:center;margin:15px 0;"></div>
        <div style="margin-top:10px;font-size:13px;color:var(--medium-gray);text-align:left;background:#f9f9f9;padding:10px;border-radius:8px;">
          <div><strong>📍 Location:</strong> ${plotStateInfo.location}</div>
          <div><strong>📏 Area:</strong> ${plotStateInfo.areaAcres} acres</div>
          <div><strong>🌱 Soil:</strong> ${soilStatus}</div>
          <div><strong>💧 Moisture:</strong> ${moistureStatus}</div>
          <div><strong>🌾 Last Crop:</strong> ${lastCrop}</div>
          <div><strong>📅 Updated:</strong> ${new Date().toLocaleDateString()}</div>
          <div><strong>📊 Seasons:</strong> ${cropHistory.length} recorded</div>
        </div>
        <button class="btn btn-secondary" style="margin-top:10px;padding:5px 10px;font-size:12px;" onclick="window.AgriOptima && window.AgriOptima.downloadSingleQRCode && window.AgriOptima.downloadSingleQRCode(${plotNum})">
          📥 Download QR
        </button>
      `;

      container.appendChild(qrCard);

      const qrDiv = document.getElementById(`qrcode-${plotNum}`);
      if (qrDiv && typeof window.QRCode === 'function') {
        const qr = new window.QRCode(qrDiv, {
          text: JSON.stringify(qrData),
          width: 180,
          height: 180,
          colorDark: '#2d5a27',
          colorLight: '#ffffff',
          correctLevel: window.QRCode.CorrectLevel.H
        });
        qrInstances.push({ plot: plotNum, qr });
      }
    });

    document.getElementById('qrCodesContainer') && (document.getElementById('qrCodesContainer').style.display = 'block');
    window.AgriOptima?.showExportSuccess?.(`✅ Generated QR codes for ${plotsToGenerate.length} plot(s)!`);
  }

  function printQRCodes() {
    const qrContainer = document.getElementById('qrGrid');
    if (!qrContainer || qrContainer.children.length === 0) {
      alert('No QR codes to print. Please generate QR codes first.');
      return;
    }

    const printWindow = window.open('', '_blank');
    printWindow.document.write(`
      <!DOCTYPE html>
      <html>
      <head>
        <title>AgriOptima QR Codes - Field Markers</title>
        <style>
          body{font-family:Arial,sans-serif;margin:20px;}
          h1{color:#2d5a27;text-align:center;}
          .qr-grid{display:grid;grid-template-columns:repeat(2,1fr);gap:30px;margin-top:20px;}
          .qr-card{text-align:center;border:1px solid #ddd;padding:20px;border-radius:10px;page-break-inside:avoid;}
          .instructions{margin-top:30px;text-align:center;font-size:12px;color:#666;}
          @media print{.no-print{display:none;}.qr-card{break-inside:avoid;}}
        </style>
      </head>
      <body>
        <h1>🌾 AgriOptima - Field QR Code Markers</h1>
        <p style="text-align:center;">Generated: ${new Date().toLocaleString()}</p>
        <div class="qr-grid">${Array.from(qrContainer.children).map(card => {
          const canvas = card.querySelector('canvas');
          const plotName = card.querySelector('h4')?.innerHTML || 'Plot';
          return `
            <div class="qr-card">
              <h3>${plotName}</h3>
              ${canvas ? `<img src="${canvas.toDataURL()}" alt="QR Code">` : ''}
              <p>Scan to view crop history and harvest schedule</p>
            </div>
          `;
        }).join('')}</div>
        <div class="instructions">
          <p>📌 Instructions: Cut out each QR code and attach to a stake in the corresponding plot.</p>
          <p>© ${new Date().getFullYear()} AgriOptima</p>
        </div>
        <button class="no-print" onclick="window.print()" style="margin-top:20px;padding:10px 20px;">🖨️ Print</button>
      </body>
      </html>
    `);
    printWindow.document.close();
  }

  function stopScanner() {
    if (scannerStream) {
      scannerStream.getTracks().forEach(t => t.stop());
      scannerStream = null;
    }
    const container = document.getElementById('scannerContainer');
    if (container) container.style.display = 'none';
    window.AgriOptima?.showExportSuccess?.('Scanner stopped.');
  }

  function displayScanResult(decodedText) {
    try {
      const data = JSON.parse(decodedText);
      const resultDiv = document.getElementById('scanResult');
      const resultContent = document.getElementById('scanResultContent');

      let cropHistoryHtml = '<ul style="margin-top:5px;">';
      data.cropHistory?.forEach(season => {
        cropHistoryHtml += `<li><strong>${season.season}:</strong> ${season.crop}</li>`;
      });
      cropHistoryHtml += '</ul>';

      let harvestHtml = '';
      if (data.upcomingHarvests?.length) {
        harvestHtml = '<strong>🌾 Upcoming Harvests:</strong><ul>';
        data.upcomingHarvests.forEach(h => {
          harvestHtml += `<li>${h.crop} - ${h.harvestDate} (${h.daysToHarvest} days)</li>`;
        });
        harvestHtml += '</ul>';
      } else {
        harvestHtml = '<p>No upcoming harvests scheduled.</p>';
      }

      // Add complete plot state information
      let plotStateHtml = '';
      if (data.plotState) {
        const ps = data.plotState;
        plotStateHtml = `
          <div style="background:#f0f7f0;padding:12px;border-radius:8px;margin-top:10px;text-align:left;">
            <strong>📍 Plot Details:</strong><br>
            <small>
              <strong>Location:</strong> ${ps.location || 'N/A'}<br>
              <strong>Area:</strong> ${ps.areaAcres || 'N/A'} acres<br>
              <strong>Ownership:</strong> ${ps.ownership || 'N/A'}<br>
              <strong>Soil Health:</strong> ${ps.soilHealthStatus || 'N/A'}<br>
              <strong>Soil Nitrogen Level:</strong> ${ps.currentSoilNitrogenLevel !== undefined ? (ps.currentSoilNitrogenLevel === 2 ? 'High (2)' : ps.currentSoilNitrogenLevel === 1 ? 'Medium (1)' : 'Low (0)') : 'N/A'}<br>
              <strong>Soil Trend:</strong> ${ps.soilChangeTrend || 'N/A'}<br>
              <strong>Moisture Level:</strong> ${ps.moistureLevel || 'N/A'}<br>
              <strong>Last Crop:</strong> ${ps.lastCropPlanted || 'N/A'}
            </small>
          </div>
        `;
      }

      if (resultContent) {
        resultContent.innerHTML = `
          <div style="background:#e8f5e9;padding:15px;border-radius:8px;">
            <strong>🌾 Plot ${data.plotId}</strong><br>
            <strong>📊 Total Plots:</strong> ${data.farmConfig?.totalPlots || 'N/A'}<br>
            <strong>🌱 Crop History:</strong>${cropHistoryHtml}
            ${plotStateHtml}
            ${harvestHtml}
            <hr>
            <small>📍 Scan this QR code again to refresh data</small>
          </div>
        `;
      }

      if (resultDiv) resultDiv.style.display = 'block';
      window.AgriOptima?.showExportSuccess?.(`✅ QR Code scanned! Plot ${data.plotId} information loaded.`);
    } catch (e) {
      const resultContent = document.getElementById('scanResultContent');
      if (resultContent) resultContent.innerHTML = `<pre style="word-break:break-all;">${decodedText}</pre>`;
      document.getElementById('scanResult') && (document.getElementById('scanResult').style.display = 'block');
    }
  }

  async function startScanner() {
    const scannerContainer = document.getElementById('scannerContainer');
    const video = document.getElementById('scannerVideo');
    if (!scannerContainer || !video) return;

    scannerContainer.style.display = 'block';
    try {
      scannerStream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } });
      video.srcObject = scannerStream;
      await video.play();

      // Lazy load html5-qrcode
      const script = document.createElement('script');
      script.src = 'https://unpkg.com/html5-qrcode@2.3.8/html5-qrcode.min.js';
      script.onload = () => {
        const html5QrCode = new window.Html5Qrcode('scannerVideo');
        html5QrCode.start(
          { facingMode: 'environment' },
          { fps: 10, qrbox: { width: 250, height: 250 } },
          (decodedText) => {
            displayScanResult(decodedText);
            html5QrCode.stop();
            stopScanner();
          },
          () => { /* ignore decode errors */ }
        );
      };
      document.head.appendChild(script);

      window.AgriOptima?.showExportSuccess?.('📷 Scanner started!');
    } catch (err) {
      console.error('Camera error:', err);
      alert('Unable to access camera. Please check permissions.');
      scannerContainer.style.display = 'none';
    }
  }

  function handleQRFileUpload(event) {
    const file = event.target.files[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (e) => {
      const img = new Image();
      img.onload = () => {
        const script = document.createElement('script');
        script.src = 'https://unpkg.com/html5-qrcode@2.3.8/html5-qrcode.min.js';
        script.onload = () => {
          const html5QrCode = new window.Html5Qrcode('qrFileInput');
          html5QrCode.scanFile(file, true)
            .then(decodedText => displayScanResult(decodedText))
            .catch(err => {
              console.error('Scan failed:', err);
              alert('Could not read QR code.');
            });
        };
        document.head.appendChild(script);
      };
      img.src = e.target.result;
    };
    reader.readAsDataURL(file);
  }

  function initQRCodeSystem() {
    document.getElementById('generateQrBtn')?.addEventListener('click', generateQRCodes);
    document.getElementById('printQrCodesBtn')?.addEventListener('click', printQRCodes);
    document.getElementById('startScannerBtn')?.addEventListener('click', startScanner);
    document.getElementById('stopScannerBtn')?.addEventListener('click', stopScanner);
    document.getElementById('qrFileInput')?.addEventListener('change', handleQRFileUpload);

    window.AgriOptima = window.AgriOptima || {};
    window.AgriOptima.initQRCodeSystem = initQRCodeSystem;
    window.AgriOptima.generateQRCodes = generateQRCodes;
    window.AgriOptima.downloadSingleQRCode = downloadSingleQRCode;
    window.AgriOptima.printQRCodes = printQRCodes;
  }

  window.AgriOptima = window.AgriOptima || {};
  window.AgriOptima.initQRCodeSystem = initQRCodeSystem;
})();

