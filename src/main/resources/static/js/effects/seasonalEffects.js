/**
 * seasonalEffects.js - Complete Working Version
 * Dynamic seasonal backgrounds with animated effects
 */

(function() {
    let animationFrame = null;
    
    // Inject all CSS styles dynamically
function injectStyles() {
    if (document.getElementById('seasonal-effects-styles')) return;
    
    const style = document.createElement('style');
    style.id = 'seasonal-effects-styles';
    style.textContent = `
        /* Base container - STAYS AT BACK */
        .season-container {
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            pointer-events: none;
            z-index: 0 !important;
            overflow: hidden;
        }
        
        /* ALL MAIN CONTENT - GOES ABOVE */
        body > *:not(.season-container) {
            position: relative;
            z-index: 1;
        }
        
        /* Ensure all containers have proper stacking */
        .container, .main-content, .dashboard, .section, .card, header, .kpi-grid {
            position: relative;
            z-index: 1;
        }
        
        /* Interactive elements stay on top */
        button, .btn, input, select, .schedule-table, .crop-card, .modal, .modal-content {
            position: relative;
            z-index: 2;
        }
        
        /* Fix for modal to be above everything */
        .qr-modal, .modal {
            z-index: 10000 !important;
        }
        
        /* Spring - Cherry Blossom */
        .spring-bg {
            background: linear-gradient(135deg, #ffe4e1 0%, #ffdab9 50%, #e8f5e9 100%);
        }
        
        .petal {
            position: absolute;
            background: radial-gradient(circle at 30% 30%, #ffb7c5, #ff69b4);
            width: 8px;
            height: 12px;
            border-radius: 50% 50% 50% 50% / 60% 60% 40% 40%;
            opacity: 0.6;
            animation: fall linear infinite;
            pointer-events: none;
        }
        
        @keyframes fall {
            0% { transform: translateY(-10vh) rotate(0deg); opacity: 0.6; }
            100% { transform: translateY(100vh) rotate(360deg); opacity: 0; }
        }
        
        /* Summer */
        .summer-bg {
            background: linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%);
        }
        
        .sun-ray {
            position: absolute;
            background: radial-gradient(circle, rgba(255,215,0,0.2), transparent);
            width: 200%;
            height: 200%;
            top: -50%;
            left: -50%;
            animation: rotateSun 20s linear infinite;
        }
        
        @keyframes rotateSun {
            from { transform: rotate(0deg); }
            to { transform: rotate(360deg); }
        }
        
        .heat-wave {
            position: absolute;
            width: 100%;
            height: 3px;
            background: linear-gradient(90deg, transparent, rgba(255,165,0,0.3), transparent);
            animation: heatWave 3s ease-in-out infinite;
        }
        
        @keyframes heatWave {
            0%, 100% { transform: translateY(0) scaleX(1); opacity: 0.3; }
            50% { transform: translateY(20px) scaleX(1.5); opacity: 0.6; }
        }
        
        /* Monsoon */
        .monsoon-bg {
            background: linear-gradient(135deg, #2c3e50, #3498db, #2c3e50);
        }
        
        .raindrop {
            position: absolute;
            background: linear-gradient(135deg, #a4d3ff, #4fc3f7);
            width: 2px;
            height: 15px;
            border-radius: 2px;
            opacity: 0.4;
            animation: rain linear infinite;
        }
        
        @keyframes rain {
            0% { transform: translateY(-10vh) translateX(0); opacity: 0.4; }
            100% { transform: translateY(100vh) translateX(20px); opacity: 0; }
        }
        
        .puddle {
            position: absolute;
            background: radial-gradient(circle, #5dade2, #2e86c1);
            border-radius: 50%;
            opacity: 0.2;
            animation: puddleRipple 3s ease-in-out infinite;
        }
        
        @keyframes puddleRipple {
            0%, 100% { transform: scale(1); opacity: 0.2; }
            50% { transform: scale(1.05); opacity: 0.4; }
        }
        
        /* Autumn */
        .autumn-bg {
            background: linear-gradient(135deg, #f5b042 0%, #ff6b35 50%, #8b4513 100%);
        }
        
        .leaf {
            position: absolute;
            width: 12px;
            height: 12px;
            background: linear-gradient(135deg, #ff6b35, #d4a017);
            clip-path: polygon(50% 0%, 0% 100%, 100% 100%);
            opacity: 0.6;
            animation: leafFall linear infinite;
        }
        
        @keyframes leafFall {
            0% { transform: translateY(-10vh) translateX(0) rotate(0deg); opacity: 0.6; }
            100% { transform: translateY(100vh) translateX(40px) rotate(180deg); opacity: 0; }
        }
        
        /* Winter */
        .winter-bg {
            background: linear-gradient(135deg, #e0eaf5 0%, #c5d0e6 50%, #e0eaf5 100%);
        }
        
        .mist {
            position: absolute;
            background: radial-gradient(ellipse, rgba(255,255,255,0.3), transparent);
            width: 100%;
            height: 100%;
            animation: mistMove 10s ease-in-out infinite;
        }
        
        @keyframes mistMove {
            0%, 100% { opacity: 0.3; transform: scale(1); }
            50% { opacity: 0.6; transform: scale(1.05); }
        }
        
        .snowflake {
            position: absolute;
            background: white;
            width: 4px;
            height: 4px;
            border-radius: 50%;
            opacity: 0.6;
            animation: snowfall linear infinite;
        }
        
        @keyframes snowfall {
            0% { transform: translateY(-10vh) rotate(0deg); opacity: 0.6; }
            100% { transform: translateY(100vh) rotate(360deg); opacity: 0; }
        }
        
        .frost {
            position: absolute;
            bottom: 0;
            left: 0;
            right: 0;
            height: 30px;
            background: linear-gradient(180deg, transparent, rgba(200,220,255,0.3));
            animation: frostGlow 4s ease-in-out infinite;
        }
        
        @keyframes frostGlow {
            0%, 100% { opacity: 0.3; }
            50% { opacity: 0.6; }
        }
        
        @keyframes fadeOut {
            from { opacity: 0.6; }
            to { opacity: 0; }
        }
        
        @keyframes slideRight {
            from { transform: translateX(-100%); opacity: 0; }
            to { transform: translateX(100%); opacity: 0.5; }
        }
        
        @keyframes sparkle {
            0% { transform: scale(0); opacity: 0.6; }
            100% { transform: scale(3); opacity: 0; }
        }
    `;
    document.head.appendChild(style);
    console.log('✅ Seasonal effects styles injected');
}
    function getCurrentSeason() {
        const month = new Date().getMonth();
        if (month >= 2 && month <= 4) return 'spring';
        if (month >= 5 && month <= 7) return 'summer';
        if (month >= 8 && month <= 10) return 'autumn';
        return 'winter';
    }
    
    function getSeasonFromPreference() {
        const saved = localStorage.getItem('agrioptima_season');
        if (saved && ['spring', 'summer', 'monsoon', 'autumn', 'winter'].includes(saved)) {
            return saved;
        }
        return getCurrentSeason();
    }
    
    function setSeason(season) {
        localStorage.setItem('agrioptima_season', season);
        applySeason(season);
    }
function applySeason(season) {
    const oldContainer = document.querySelector('.season-container');
    if (oldContainer) oldContainer.remove();

    const container = document.createElement('div');
    container.className = `season-container ${season}-bg`;
    document.body.insertBefore(container, document.body.firstChild);

    // Clear existing effects and create new ones based on season
    switch(season) {
        case 'spring':
            createSpringEffects(container);
            break;
        case 'summer':
            createSummerEffects(container);
            break;
        case 'monsoon':
            createMonsoonEffects(container);
            break;
        case 'autumn':
            createAutumnEffects(container);
            break;
        case 'winter':
            createWinterEffects(container);
            break;
        default:
            createSpringEffects(container);
    }
    
    // Re-trigger content animation
    if (typeof animatePageContent === 'function') {
        animatePageContent();
    }
}

function createNewSeasonContainer(season) {
    const container = document.createElement('div');
    container.className = `season-container ${season}-bg`;
    container.style.opacity = '0';
    container.style.transition = 'opacity 0.4s ease';
    document.body.insertBefore(container, document.body.firstChild);
    
    // Force reflow then fade in
    setTimeout(() => { container.style.opacity = '1'; }, 10);
    
    // Clear any existing effects and create new ones based on season
    switch(season) {
        case 'spring': createSpringEffects(container); break;
        case 'summer': createSummerEffects(container); break;
        case 'monsoon': createMonsoonEffects(container); break;
        case 'autumn': createAutumnEffects(container); break;
        case 'winter': createWinterEffects(container); break;
        default: createSpringEffects(container);
    }
}
    function createSpringEffects(container) {
        const count = window.innerWidth < 768 ? 30 : 50;
        for (let i = 0; i < count; i++) {
            const petal = document.createElement('div');
            petal.className = 'petal';
            petal.style.left = Math.random() * 100 + '%';
            petal.style.animationDuration = 4 + Math.random() * 4 + 's';
            petal.style.animationDelay = Math.random() * 10 + 's';
            petal.style.width = 6 + Math.random() * 6 + 'px';
            petal.style.height = 8 + Math.random() * 8 + 'px';
            container.appendChild(petal);
        }
        console.log('🌸 Spring effects created');
    }
    
    function createSummerEffects(container) {
        const sunRay = document.createElement('div');
        sunRay.className = 'sun-ray';
        container.appendChild(sunRay);
        
        for (let i = 0; i < 5; i++) {
            const heatWave = document.createElement('div');
            heatWave.className = 'heat-wave';
            heatWave.style.top = (20 + i * 15) + '%';
            heatWave.style.animationDelay = i * 0.5 + 's';
            container.appendChild(heatWave);
        }
        
        for (let i = 0; i < 20; i++) {
            const dust = document.createElement('div');
            dust.style.position = 'absolute';
            dust.style.width = '2px';
            dust.style.height = '2px';
            dust.style.background = 'rgba(255,215,0,0.3)';
            dust.style.borderRadius = '50%';
            dust.style.left = Math.random() * 100 + '%';
            dust.style.top = Math.random() * 100 + '%';
            dust.style.animation = `heatWave ${2 + Math.random() * 2}s ease-in-out infinite`;
            container.appendChild(dust);
        }
        console.log('☀️ Summer effects created');
    }
    
    function createMonsoonEffects(container) {
        const count = window.innerWidth < 768 ? 60 : 100;
        for (let i = 0; i < count; i++) {
            const raindrop = document.createElement('div');
            raindrop.className = 'raindrop';
            raindrop.style.left = Math.random() * 100 + '%';
            raindrop.style.animationDuration = 0.5 + Math.random() * 0.5 + 's';
            raindrop.style.animationDelay = Math.random() * 5 + 's';
            raindrop.style.height = 10 + Math.random() * 10 + 'px';
            container.appendChild(raindrop);
        }
        
        for (let i = 0; i < 8; i++) {
            const puddle = document.createElement('div');
            puddle.className = 'puddle';
            puddle.style.bottom = Math.random() * 20 + '%';
            puddle.style.left = Math.random() * 100 + '%';
            puddle.style.width = 30 + Math.random() * 50 + 'px';
            puddle.style.height = 10 + Math.random() * 20 + 'px';
            puddle.style.animationDuration = 2 + Math.random() * 3 + 's';
            container.appendChild(puddle);
        }
        console.log('🌧️ Monsoon effects created');
    }
    
    function createAutumnEffects(container) {
        const count = window.innerWidth < 768 ? 25 : 40;
        for (let i = 0; i < count; i++) {
            const leaf = document.createElement('div');
            leaf.className = 'leaf';
            leaf.style.left = Math.random() * 100 + '%';
            leaf.style.animationDuration = 5 + Math.random() * 5 + 's';
            leaf.style.animationDelay = Math.random() * 10 + 's';
            leaf.style.width = 10 + Math.random() * 8 + 'px';
            leaf.style.height = 10 + Math.random() * 8 + 'px';
            leaf.style.background = `linear-gradient(135deg, 
                hsl(${20 + Math.random() * 20}, 70%, 50%), 
                hsl(${30 + Math.random() * 10}, 80%, 40%))`;
            container.appendChild(leaf);
        }
        console.log('🍂 Autumn effects created');
    }
    
    function createWinterEffects(container) {
        const count = window.innerWidth < 768 ? 40 : 70;
        for (let i = 0; i < count; i++) {
            const snowflake = document.createElement('div');
            snowflake.className = 'snowflake';
            snowflake.style.left = Math.random() * 100 + '%';
            snowflake.style.animationDuration = 4 + Math.random() * 6 + 's';
            snowflake.style.animationDelay = Math.random() * 10 + 's';
            snowflake.style.width = 3 + Math.random() * 4 + 'px';
            snowflake.style.height = 3 + Math.random() * 4 + 'px';
            container.appendChild(snowflake);
        }
        
        const mist = document.createElement('div');
        mist.className = 'mist';
        container.appendChild(mist);
        
        const frost = document.createElement('div');
        frost.className = 'frost';
        container.appendChild(frost);
        
        console.log('❄️ Winter effects created');
    }
    
    function addSeasonSelector() {
        if (document.getElementById('seasonSelector')) return;
        
        const selector = document.createElement('div');
        selector.id = 'seasonSelector';
        selector.style.cssText = `
            position: fixed;
            bottom: 20px;
            right: 20px;
            z-index: 10000;
            background: rgba(255,255,255,0.95);
            padding: 8px 12px;
            border-radius: 30px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
            display: flex;
            gap: 8px;
            backdrop-filter: blur(5px);
            font-size: 12px;
        `;
        
        const seasons = [
            { name: 'Spring', icon: '🌸', value: 'spring' },
            { name: 'Summer', icon: '☀️', value: 'summer' },
            { name: 'Monsoon', icon: '🌧️', value: 'monsoon' },
            { name: 'Autumn', icon: '🍂', value: 'autumn' },
            { name: 'Winter', icon: '❄️', value: 'winter' }
        ];
        
        const currentSeason = getSeasonFromPreference();
        
        seasons.forEach(season => {
            const btn = document.createElement('button');
            btn.innerHTML = `${season.icon} ${season.name}`;
            btn.style.cssText = `
                background: ${currentSeason === season.value ? '#4E8B6A' : 'transparent'};
                color: ${currentSeason === season.value ? 'white' : '#333'};
                border: none;
                padding: 5px 12px;
                border-radius: 20px;
                cursor: pointer;
                transition: all 0.3s;
                font-size: 11px;
                font-family: inherit;
            `;
btn.onclick = () => {
    const newSeason = season.value;
    localStorage.setItem('agrioptima_season', newSeason);
    applySeason(newSeason);
    
    // Update active button style
    document.querySelectorAll('#seasonSelector button').forEach(b => {
        b.style.background = 'transparent';
        b.style.color = '#333';
    });
    btn.style.background = '#4E8B6A';
    btn.style.color = 'white';
};
            selector.appendChild(btn);
        });
        
        document.body.appendChild(selector);
        console.log('🎨 Season selector added');
    }
        function animatePageContent() {
        const elements = document.querySelectorAll('.section, .kpi-grid, #results, .card, .schedule-table, .alert-card, .crop-library');
        elements.forEach(el => {
            el.classList.remove('animate-in');
            // Force reflow
            void el.offsetWidth;
            el.classList.add('animate-in');
        });
    }
    // Main function to start effects
    function startSeasonalEffects() {
        console.log('🎨 Starting seasonal effects...');
        
        // Inject styles first
        injectStyles();
        
        // Remove existing container if any
        const existingContainer = document.querySelector('.season-container');
        if (existingContainer) {
            existingContainer.remove();
        }
        
        const season = getSeasonFromPreference();
        console.log(`📅 Current season: ${season}`);
        
        // Create container
        const container = document.createElement('div');
        container.className = `season-container ${season}-bg`;
        document.body.insertBefore(container, document.body.firstChild);
        
        // Create effects based on season
        switch(season) {
            case 'spring':
                createSpringEffects(container);
                break;
            case 'summer':
                createSummerEffects(container);
                break;
            case 'monsoon':
                createMonsoonEffects(container);
                break;
            case 'autumn':
                createAutumnEffects(container);
                break;
            case 'winter':
                createWinterEffects(container);
                break;
            default:
                createSpringEffects(container);
        }
        
        // Add season selector
        addSeasonSelector();
        animatePageContent();
        console.log('✅ Seasonal effects started successfully!');
    }
    // After setting the new background
const mainContent = document.querySelector('.container') || document.querySelector('.main-content');
if (mainContent) {
    mainContent.style.animation = 'none';
    // Force reflow
    void mainContent.offsetHeight;
    mainContent.style.animation = 'popUp 0.6s cubic-bezier(0.2, 0.9, 0.4, 1.1)';
}
    // Auto-start when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', startSeasonalEffects);
    } else {
        // DOM already loaded, start after a short delay to ensure body is ready
        setTimeout(startSeasonalEffects, 100);
    }
    
    // Export for manual control
    window.AgriOptima = window.AgriOptima || {};
    window.AgriOptima.startSeasonalEffects = startSeasonalEffects;
    window.AgriOptima.setSeason = setSeason;
    window.AgriOptima.getCurrentSeason = getCurrentSeason;
    
    console.log('🎨 Seasonal effects module loaded');
})();