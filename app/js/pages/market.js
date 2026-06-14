// 蒜来宝 - 行情页（价格走势 + 产区报价 + 影响因素）
import { fetchPrices } from '../data-fetcher.js';
import { getCachedPriceData, getSettings } from '../storage.js';
import { renderChart, updateChartData, destroyChart } from '../chart-renderer.js';
import { trendIcon, trendClass, factorLevelClass } from '../utils.js';

let pricesData = null;
let activeRegion = 'jinxiang';

const REGION_NAMES = {
  jinxiang: '金乡', qixian: '杞县', pizhou: '邳州', zhongmou: '中牟'
};

/** 渲染行情页 */
export async function renderMarket(container) {
  const settings = getSettings();
  activeRegion = settings.default_region || 'jinxiang';

  container.innerHTML = `
    <!-- 价格走势 -->
    <div class="card">
      <div class="card-header">📈 <span id="chart-title">价格走势</span> <span class="sub" id="price-update-time"></span></div>
      <div class="region-tabs" id="chart-region-tabs">
        ${Object.entries(REGION_NAMES).map(([key, name]) =>
          `<button class="region-tab${key === activeRegion ? ' active' : ''}" data-region="${key}">${name}</button>`
        ).join('')}
      </div>
      <div class="chart-container" style="height:180px;">
        <canvas id="price-chart"></canvas>
      </div>
      <div id="chart-stats" style="display:flex;justify-content:space-between;font-size:11px;color:#888;margin-top:8px;"></div>
    </div>

    <!-- 产区报价 -->
    <div class="card">
      <div class="card-header">📌 产区实时报价 <span class="sub">单位：元/斤</span></div>
      <div class="region-tabs" id="price-region-tabs">
        ${Object.entries(REGION_NAMES).map(([key, name]) =>
          `<button class="region-tab${key === activeRegion ? ' active' : ''}" data-region="${key}">${name}</button>`
        ).join('')}
      </div>
      <div id="region-prices-container">
        <div class="skeleton skeleton-card"></div>
        <div class="skeleton skeleton-card"></div>
      </div>
    </div>

    <!-- 影响因素 -->
    <div class="card">
      <div class="card-header">🌦️ 影响价格的关键因素</div>
      <div id="factors-container">
        <div class="skeleton skeleton-text"></div>
        <div class="skeleton skeleton-text"></div>
        <div class="skeleton skeleton-text"></div>
      </div>
    </div>
  `;

  // 绑定产区切换
  container.querySelectorAll('.region-tab').forEach(btn => {
    btn.addEventListener('click', () => switchRegion(btn.dataset.region));
  });

  // 加载数据
  await loadPrices();
}

/** 加载价格数据 */
async function loadPrices() {
  // 先显示缓存
  const cached = getCachedPriceData();
  if (cached && cached.data) {
    pricesData = cached.data;
    renderAll();
  }

  // 网络获取
  const result = await fetchPrices();
  if (result.success) {
    pricesData = result.data;
    renderAll();

    const timeEl = document.getElementById('price-update-time');
    if (timeEl) {
      timeEl.textContent = result.stale ? '(数据已缓存)' : '· 已更新';
      timeEl.style.color = result.stale ? '#ff9800' : '#4caf50';
    }
  }
}

/** 渲染全部内容 */
function renderAll() {
  if (!pricesData) return;
  renderChartWithData();
  renderRegionPrices();
  renderFactors();
}

/** 渲染图表 */
function renderChartWithData() {
  const canvas = document.getElementById('price-chart');
  if (!canvas || !pricesData.history) return;

  renderChart(canvas, pricesData.history, activeRegion);

  // 统计信息
  const key = activeRegion + '_avg';
  const history = pricesData.history;
  if (history.length > 0) {
    const values = history.map(h => h[key]).filter(v => v != null);
    const min30 = Math.min(...values.slice(-30));
    const max30 = Math.max(...values.slice(-30));
    const latest = values[values.length - 1];
    const change30 = latest && values.length >= 2
      ? ((latest - values[0]) / values[0] * 100).toFixed(1)
      : '0';

    const statsEl = document.getElementById('chart-stats');
    if (statsEl) {
      statsEl.innerHTML = `
        <span>30日最低: ¥${min30.toFixed(2)}</span>
        <span>30日最高: ¥${max30.toFixed(2)}</span>
        <span style="color:${change30 >= 0 ? '#d32f2f' : '#388e3c'}">30日涨跌: ${change30 >= 0 ? '+' : ''}${change30}%</span>
      `;
    }
  }

  document.getElementById('chart-title').textContent =
    `${REGION_NAMES[activeRegion]} · 近30天价格走势`;
}

/** 渲染产区报价 */
function renderRegionPrices() {
  if (!pricesData || !pricesData.regions) return;

  const region = pricesData.regions[activeRegion];
  const container = document.getElementById('region-prices-container');
  if (!container || !region) {
    container.innerHTML = '<div class="empty-state"><p>暂无该产区数据</p></div>';
    return;
  }

  container.innerHTML = `
    <div style="margin-bottom:8px;font-size:13px;font-weight:bold;">
      📍 ${region.name}产区
      <span class="region-mood" style="color:${region.market_mood.includes('旺') || region.market_mood.includes('强') ? '#d32f2f' : region.market_mood.includes('弱') ? '#388e3c' : '#666'}">${region.market_mood}</span>
    </div>
    <div class="spec-grid">
      ${(region.specs || []).map(spec => `
        <div class="spec-cell">
          <div class="spec-name">${spec.name}</div>
          <div class="spec-price ${trendClass(spec.trend)}">
            ${spec.low === spec.high ? spec.low.toFixed(2) : spec.low.toFixed(2) + '-' + spec.high.toFixed(2)}
          </div>
          <div style="font-size:9px;color:${spec.trend === 'up' ? '#d32f2f' : spec.trend === 'down' ? '#388e3c' : '#999'}">${trendIcon(spec.trend)}</div>
        </div>
      `).join('')}
    </div>
  `;

  // 也更新其他产区（切换时）
  document.querySelectorAll('#price-region-tabs .region-tab').forEach(btn => {
    btn.classList.toggle('active', btn.dataset.region === activeRegion);
  });
  document.querySelectorAll('#chart-region-tabs .region-tab').forEach(btn => {
    btn.classList.toggle('active', btn.dataset.region === activeRegion);
  });
}

/** 渲染影响因素 */
function renderFactors() {
  const container = document.getElementById('factors-container');
  if (!container || !pricesData || !pricesData.factors) return;

  const factors = pricesData.factors;
  const entries = Object.entries(factors);

  container.innerHTML = entries.map(([key, f]) => `
    <div class="factor-row">
      <span>${getFactorIcon(key)} ${f.label}</span>
      <span class="factor-level ${factorLevelClass(f.level)}">${f.value}</span>
    </div>
  `).join('');
}

/** 切换产区 */
function switchRegion(region) {
  if (region === activeRegion) return;
  activeRegion = region;
  if (pricesData) {
    renderRegionPrices();
    if (pricesData.history) {
      updateChartData(pricesData.history, activeRegion);
      renderChartWithData();
    }
  }
}

/** 因素图标 */
function getFactorIcon(key) {
  const icons = {
    national_inventory: '📦',
    planting_area: '🌱',
    export_forecast: '🚢',
    weather_risk: '🌡️',
    seed_cost: '💰',
  };
  return icons[key] || '📌';
}

/** 刷新 */
export async function refreshMarket() {
  await loadPrices();
}
