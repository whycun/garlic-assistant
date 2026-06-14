// 蒜来宝 - 利润计算器
import { getCalcSaves, saveCalculation, deleteCalcSave, getCachedPriceData } from '../storage.js';
import { formatMoney, formatPrice, genId, showToast, debounce } from '../utils.js';

/** 渲染计算器 */
export async function renderCalculator(container) {
  // 尝试获取当前市价
  let defaultMarketPrice = 2.85;
  try {
    const cached = getCachedPriceData();
    if (cached && cached.data && cached.data.regions) {
      const jinxiang = cached.data.regions.jinxiang;
      if (jinxiang && jinxiang.specs && jinxiang.specs.length > 0) {
        const midSpec = jinxiang.specs.find(s => s.name === '一般混级') || jinxiang.specs[0];
        defaultMarketPrice = (midSpec.low + midSpec.high) / 2;
      }
    }
  } catch (e) {}

  container.innerHTML = `
    <div style="font-size:11px;color:#999;margin:10px 0;">自动关联行情数据 · 修改任意数字实时计算</div>

    <div class="calc-input-row">
      <label>📊 当前市价 (元/斤)</label>
      <input type="number" class="market-price" id="calc-market-price" value="${defaultMarketPrice}" step="0.01">
    </div>
    <div class="calc-input-row">
      <label>🧄 收购均价 (元/斤)</label>
      <input type="number" id="calc-purchase-price" value="2.10" step="0.01">
    </div>
    <div class="calc-input-row">
      <label>⚖️ 购入数量 (吨)</label>
      <input type="number" id="calc-quantity" value="50" step="0.1">
    </div>
    <div class="calc-input-row">
      <label>❄️ 冷库费用 (元)</label>
      <input type="number" id="calc-storage-fee" value="18000" step="100">
    </div>
    <div class="calc-input-row">
      <label>👷 人工费用 (元)</label>
      <input type="number" id="calc-labor-fee" value="12000" step="100">
    </div>
    <div class="calc-input-row">
      <label>🗑️ 损耗费用 (元)</label>
      <input type="number" id="calc-loss-fee" value="3500" step="100">
    </div>
    <div class="calc-input-row">
      <label>🚛 运输费用 (元)</label>
      <input type="number" id="calc-transport-fee" value="5000" step="100">
    </div>
    <div class="calc-input-row">
      <label>📋 其他支出 (元)</label>
      <input type="number" id="calc-other-fee" value="1500" step="100">
    </div>

    <!-- 计算结果 -->
    <div class="result-bar" id="calc-result">
      <div style="font-size:11px;color:#666;">成本概算</div>
      <div id="calc-result-detail"></div>
    </div>

    <button class="btn btn-primary" id="save-calc-btn">💾 保存这条核算</button>
    <button class="btn btn-outline" id="show-saved-btn">📋 查看历史记录</button>

    <!-- 历史记录 -->
    <div id="calc-history" class="hidden" style="margin-top:12px;"></div>
  `;

  // 绑定输入事件（防抖计算）
  const calc = debounce(updateCalculation, 200);
  container.querySelectorAll('input[type="number"]').forEach(input => {
    input.addEventListener('input', calc);
  });

  // 绑定按钮
  document.getElementById('save-calc-btn').addEventListener('click', saveCurrentCalc);
  document.getElementById('show-saved-btn').addEventListener('click', toggleHistory);

  // 初始计算
  updateCalculation();
}

/** 更新计算结果 */
function updateCalculation() {
  const getVal = (id) => parseFloat(document.getElementById(id)?.value) || 0;

  const marketPrice = getVal('calc-market-price');
  const purchasePrice = getVal('calc-purchase-price');
  const quantityTons = getVal('calc-quantity');
  const storageFee = getVal('calc-storage-fee');
  const laborFee = getVal('calc-labor-fee');
  const lossFee = getVal('calc-loss-fee');
  const transportFee = getVal('calc-transport-fee');
  const otherFee = getVal('calc-other-fee');

  const purchaseTotal = purchasePrice * quantityTons * 2000;
  const extraCosts = storageFee + laborFee + lossFee + transportFee + otherFee;
  const totalCost = purchaseTotal + extraCosts;
  const unitCost = quantityTons > 0 ? totalCost / (quantityTons * 2000) : 0;
  const revenue = marketPrice * quantityTons * 2000;
  const profit = revenue - totalCost;
  const profitRate = totalCost > 0 ? (profit / totalCost * 100) : 0;

  const resultEl = document.getElementById('calc-result-detail');
  if (!resultEl) return;

  resultEl.innerHTML = `
    <div style="display:flex;justify-content:space-around;margin:8px 0;">
      <div>
        <div style="font-size:10px;color:#888;">总成本</div>
        <div style="font-size:16px;font-weight:bold;">${formatMoney(totalCost)}</div>
        <div style="font-size:10px;color:#888;">合 ${formatPrice(unitCost)}/斤</div>
      </div>
      <div>
        <div style="font-size:10px;color:#888;">按市价出货</div>
        <div style="font-size:16px;font-weight:bold;">${formatMoney(revenue)}</div>
        <div style="font-size:10px;color:#888;">${formatPrice(marketPrice)}/斤</div>
      </div>
    </div>
    <div style="border-top:1px dashed #ccc;padding-top:8px;">
      <span style="font-size:14px;font-weight:bold;">毛利：</span>
      <span class="total-profit">${formatMoney(profit)}</span>
      <span class="profit-rate">利润率 ${profitRate.toFixed(1)}%</span>
    </div>
  `;
}

/** 保存当前计算 */
function saveCurrentCalc() {
  const getVal = (id) => parseFloat(document.getElementById(id)?.value) || 0;
  const calc = {
    id: genId('calc_'),
    market_price: getVal('calc-market-price'),
    purchase_price: getVal('calc-purchase-price'),
    quantity_tons: getVal('calc-quantity'),
    cold_storage_fee: getVal('calc-storage-fee'),
    labor_fee: getVal('calc-labor-fee'),
    loss_fee: getVal('calc-loss-fee'),
    transport_fee: getVal('calc-transport-fee'),
    other_fee: getVal('calc-other-fee'),
  };
  saveCalculation(calc);
  showToast('已保存核算记录');
}

/** 切换历史记录 */
function toggleHistory() {
  const container = document.getElementById('calc-history');
  if (!container) return;

  if (container.classList.contains('hidden')) {
    const saves = getCalcSaves();
    if (saves.length === 0) {
      container.innerHTML = '<div class="empty-state"><p>暂无保存记录</p></div>';
    } else {
      container.innerHTML = `
        <div style="font-size:12px;font-weight:bold;color:#666;margin-bottom:8px;">📋 历史核算记录 (${saves.length})</div>
        ${saves.slice(0, 10).map(s => {
          const totalCost = s.purchase_price * s.quantity_tons * 2000
            + s.cold_storage_fee + s.labor_fee + s.loss_fee + s.transport_fee + s.other_fee;
          const revenue = s.market_price * s.quantity_tons * 2000;
          const profit = revenue - totalCost;
          return `
            <div class="batch-card" style="display:flex;justify-content:space-between;align-items:center;">
              <div>
                <div style="font-size:12px;font-weight:bold;">${s.quantity_tons}吨 · 成本${formatPrice(s.purchase_price)} · 市价${formatPrice(s.market_price)}</div>
                <div style="font-size:10px;color:#999;">${new Date(s.created_at).toLocaleDateString('zh-CN')}</div>
              </div>
              <div style="text-align:right;">
                <div style="font-weight:bold;color:${profit >= 0 ? '#d32f2f' : '#388e3c'}">${formatMoney(profit)}</div>
                <button class="btn-delete-calc" data-id="${s.id}" style="font-size:10px;color:#999;background:none;border:none;cursor:pointer;">删除</button>
              </div>
            </div>
          `;
        }).join('')}
      `;
      // 绑定删除
      container.querySelectorAll('.btn-delete-calc').forEach(btn => {
        btn.addEventListener('click', () => {
          deleteCalcSave(btn.dataset.id);
          showToast('已删除');
          toggleHistory();
          toggleHistory(); // 刷新
        });
      });
    }
    container.classList.remove('hidden');
    document.getElementById('show-saved-btn').textContent = '📋 收起历史记录';
  } else {
    container.classList.add('hidden');
    document.getElementById('show-saved-btn').textContent = '📋 查看历史记录';
  }
}
