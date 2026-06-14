// 蒜来宝 - 库存页（批次管理 + 利润计算器）
import { getBatches, saveBatch, deleteBatch, getCalcSaves } from '../storage.js';
import { fetchPrices } from '../data-fetcher.js';
import { formatMoney, formatPrice, formatTons, genId, showToast } from '../utils.js';
import { renderCalculator } from './calculator.js';

let currentTab = 'list';

/** 渲染库存页 */
export async function renderInventory(container) {
  container.innerHTML = `
    <div class="card" style="padding:0;overflow:hidden;">
      <div class="tab-switch">
        <div class="tab${currentTab === 'list' ? ' active' : ''}" data-tab="list">📋 库存清单</div>
        <div class="tab${currentTab === 'calc' ? ' active' : ''}" data-tab="calc">🧮 利润计算</div>
      </div>
      <div id="inv-content" style="padding:0 14px 14px;"></div>
    </div>

    <!-- 新增批次弹窗 -->
    <div class="modal-overlay hidden" id="batch-modal">
      <div class="modal-sheet" id="batch-modal-content"></div>
    </div>
  `;

  // Tab切换
  container.querySelectorAll('.tab').forEach(tab => {
    tab.addEventListener('click', () => {
      currentTab = tab.dataset.tab;
      container.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
      tab.classList.add('active');
      renderCurrentTab();
    });
  });

  await renderCurrentTab();
}

/** 渲染当前Tab */
async function renderCurrentTab() {
  const content = document.getElementById('inv-content');
  if (!content) return;

  if (currentTab === 'list') {
    await renderBatchList(content);
  } else {
    await renderCalculator(content);
  }
}

/** 渲染批次列表 */
async function renderBatchList(container) {
  const batches = getBatches();
  const holding = batches.filter(b => b.status === 'holding');
  const sold = batches.filter(b => b.status === 'sold');

  // 计算汇总
  const totalTons = holding.reduce((s, b) => s + (b.quantity_tons || 0), 0);
  const totalCost = holding.reduce((s, b) => s + (b.quantity_tons || 0) * (b.purchase_price_per_jin || 0) * 2000, 0);
  const totalValue = holding.reduce((s, b) => {
    const mktPrice = b.current_market_price || b.purchase_price_per_jin || 0;
    return s + (b.quantity_tons || 0) * mktPrice * 2000;
  }, 0);
  const floatingPnL = totalValue - totalCost;

  // 获取当前市价（尝试从缓存）
  let marketPrices = {};
  try {
    const { getCachedPriceData } = await import('../storage.js');
    const cached = getCachedPriceData();
    if (cached && cached.data && cached.data.regions) {
      for (const [key, region] of Object.entries(cached.data.regions)) {
        if (region.specs && region.specs.length > 0) {
          const prices = region.specs.map(s => s.low);
          marketPrices[key] = Math.min(...prices);
        }
      }
    }
  } catch (e) {}

  container.innerHTML = `
    <!-- 汇总卡片 -->
    <div class="summary-grid" style="margin:12px 0;">
      <div class="summary-cell cell-blue">
        <div class="label">总库存</div>
        <div class="value">${totalTons}<span style="font-size:12px;"> 吨</span></div>
      </div>
      <div class="summary-cell cell-orange">
        <div class="label">库存估值</div>
        <div class="value">${formatMoney(totalValue)}</div>
      </div>
      <div class="summary-cell cell-red">
        <div class="label">持仓成本</div>
        <div class="value">${formatMoney(totalCost)}</div>
      </div>
      <div class="summary-cell cell-green">
        <div class="label">浮动盈亏</div>
        <div class="value" style="color:${floatingPnL >= 0 ? '#d32f2f' : '#388e3c'}">${formatMoney(floatingPnL)}</div>
      </div>
    </div>

    <!-- 持仓批次 -->
    <div style="font-size:12px;font-weight:bold;color:#666;padding:8px 0 4px;">📦 持仓批次 (${holding.length})</div>
    <div id="holding-batches">
      ${holding.length === 0 ? '<div class="empty-state"><p>暂无持仓批次</p></div>' :
        holding.map(b => renderBatchCard(b, marketPrices)).join('')}
    </div>

    <!-- 已结算 -->
    ${sold.length > 0 ? `
      <div style="font-size:12px;font-weight:bold;color:#666;padding:12px 0 4px;">📋 已结算批次 (${sold.length})</div>
      <div id="sold-batches">
        ${sold.map(b => renderBatchCard(b, marketPrices)).join('')}
      </div>
    ` : ''}

    <button class="btn btn-primary" id="add-batch-btn" style="margin-top:12px;">+ 新增入库批次</button>
  `;

  // 绑定事件
  document.getElementById('add-batch-btn').addEventListener('click', () => showBatchModal());
  container.querySelectorAll('.btn-sell').forEach(btn => {
    btn.addEventListener('click', () => showSellModal(btn.dataset.id));
  });
  container.querySelectorAll('.btn-delete').forEach(btn => {
    btn.addEventListener('click', () => confirmDelete(btn.dataset.id));
  });
}

/** 渲染单个批次卡片 */
function renderBatchCard(b, marketPrices) {
  const mktPrice = marketPrices[b.region] || b.purchase_price_per_jin || 0;
  const costTotal = (b.quantity_tons || 0) * (b.purchase_price_per_jin || 0) * 2000;
  const valueTotal = (b.quantity_tons || 0) * mktPrice * 2000;
  const pnl = b.status === 'sold'
    ? ((b.sold_price_per_jin || 0) - (b.purchase_price_per_jin || 0)) * (b.quantity_tons || 0) * 2000
    : valueTotal - costTotal;

  return `
    <div class="batch-card" style="${b.status === 'sold' ? 'opacity:0.75;' : ''}">
      <div class="batch-header">
        <div>
          <span style="font-weight:bold;">批次 ${b.batch_no || b.id.slice(-4)}</span>
          <span style="font-size:10px;color:#888;margin-left:4px;">${b.region_name || ''} · ${b.spec || ''}</span>
        </div>
        <span class="batch-status ${b.status === 'holding' ? 'status-holding' : 'status-sold'}">
          ${b.status === 'holding' ? '持仓中' : '已出库'}
        </span>
      </div>
      <div class="batch-detail">
        <div>${formatTons(b.quantity_tons)} | 成本 ¥${formatPrice(b.purchase_price_per_jin)}/斤 | ${b.status === 'holding' ? '入库' : '出库'} ${b.status === 'holding' ? b.purchase_date : b.sold_date}</div>
      </div>
      <div style="display:flex;justify-content:space-between;margin-top:4px;align-items:center;">
        <span style="font-size:11px;color:#888;">
          ${b.status === 'holding'
            ? `当前市价 ¥${formatPrice(mktPrice)}`
            : `卖出 ¥${formatPrice(b.sold_price_per_jin)}/斤`}
        </span>
        <span class="batch-profit" style="color:${pnl >= 0 ? '#d32f2f' : '#388e3c'}">
          ${pnl >= 0 ? '+' : ''}${formatMoney(pnl)}
        </span>
      </div>
      ${b.status === 'holding' ? `
        <div class="batch-actions">
          <button class="btn-sell" data-id="${b.id}">标记出库</button>
          <button class="btn-delete" data-id="${b.id}">删除</button>
        </div>
      ` : ''}
    </div>
  `;
}

/** 新增批次弹窗 */
function showBatchModal(batch = null) {
  const modal = document.getElementById('batch-modal');
  const content = document.getElementById('batch-modal-content');
  if (!modal || !content) return;

  const isEdit = !!batch;
  content.innerHTML = `
    <h3>${isEdit ? '编辑批次' : '新增入库批次'}</h3>
    <div class="form-group">
      <label>批次编号</label>
      <input type="text" id="form-batch-no" placeholder="如 A023" value="${batch?.batch_no || ''}">
    </div>
    <div class="form-row">
      <div class="form-group">
        <label>产区</label>
        <select id="form-region">
          <option value="jinxiang" ${batch?.region === 'jinxiang' ? 'selected' : ''}>金乡</option>
          <option value="qixian" ${batch?.region === 'qixian' ? 'selected' : ''}>杞县</option>
          <option value="pizhou" ${batch?.region === 'pizhou' ? 'selected' : ''}>邳州</option>
          <option value="zhongmou" ${batch?.region === 'zhongmou' ? 'selected' : ''}>中牟</option>
        </select>
      </div>
      <div class="form-group">
        <label>规格</label>
        <input type="text" id="form-spec" placeholder="如 一般混级" value="${batch?.spec || ''}">
      </div>
    </div>
    <div class="form-row">
      <div class="form-group">
        <label>数量(吨)</label>
        <input type="number" id="form-quantity" placeholder="0" step="0.1" value="${batch?.quantity_tons || ''}">
      </div>
      <div class="form-group">
        <label>收购价(元/斤)</label>
        <input type="number" id="form-price" placeholder="0.00" step="0.01" value="${batch?.purchase_price_per_jin || ''}">
      </div>
    </div>
    <div class="form-group">
      <label>入库日期</label>
      <input type="date" id="form-date" value="${batch?.purchase_date || new Date().toISOString().slice(0,10)}">
    </div>
    <div class="form-group">
      <label>存放位置</label>
      <input type="text" id="form-location" placeholder="如 冷库A区" value="${batch?.storage_location || ''}">
    </div>
    <button class="btn btn-primary" id="save-batch-btn">保存</button>
    <button class="btn btn-outline" id="cancel-batch-btn">取消</button>
  `;

  modal.classList.remove('hidden');

  document.getElementById('save-batch-btn').addEventListener('click', () => {
    const newBatch = {
      id: batch?.id || genId('batch_'),
      batch_no: document.getElementById('form-batch-no').value,
      region: document.getElementById('form-region').value,
      region_name: { jinxiang: '金乡', qixian: '杞县', pizhou: '邳州', zhongmou: '中牟' }[document.getElementById('form-region').value],
      spec: document.getElementById('form-spec').value,
      quantity_tons: parseFloat(document.getElementById('form-quantity').value) || 0,
      purchase_price_per_jin: parseFloat(document.getElementById('form-price').value) || 0,
      purchase_date: document.getElementById('form-date').value,
      storage_location: document.getElementById('form-location').value,
      status: 'holding',
    };
    saveBatch(newBatch);
    modal.classList.add('hidden');
    showToast('批次已保存');
    renderCurrentTab();
  });

  document.getElementById('cancel-batch-btn').addEventListener('click', () => {
    modal.classList.add('hidden');
  });

  modal.addEventListener('click', (e) => {
    if (e.target === modal) modal.classList.add('hidden');
  });
}

/** 标记出库 */
function showSellModal(id) {
  const batches = getBatches();
  const batch = batches.find(b => b.id === id);
  if (!batch) return;

  const price = prompt(`请输入 "${batch.batch_no}" 的卖出价格（元/斤）：`, '');
  if (!price || isNaN(parseFloat(price))) return;

  batch.status = 'sold';
  batch.sold_price_per_jin = parseFloat(price);
  batch.sold_date = new Date().toISOString().slice(0, 10);
  saveBatch(batch);
  showToast('已标记为出库');
  renderCurrentTab();
}

/** 确认删除 */
function confirmDelete(id) {
  if (!confirm('确定要删除这个批次吗？此操作不可恢复。')) return;
  deleteBatch(id);
  showToast('批次已删除');
  renderCurrentTab();
}

/** 刷新 */
export async function refreshInventory() {
  await renderCurrentTab();
}

/** 切换Tab */
export function switchToTab(tab) {
  currentTab = tab;
  renderCurrentTab();
}
