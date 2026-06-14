// 蒜来宝 - 我的页面（个人信息 + 经营概览 + 设置）
import { getProfile, saveProfile, getSettings, saveSettings, getBatches, downloadBackup } from '../storage.js';
import { showToast } from '../utils.js';

/** 渲染我的页面 */
export function renderProfile(container) {
  const profile = getProfile();
  const settings = getSettings();
  const batches = getBatches();
  const holding = batches.filter(b => b.status === 'holding');

  // 统计本月数据
  const now = new Date();
  const thisMonth = batches.filter(b => {
    const d = new Date(b.purchase_date);
    return d.getMonth() === now.getMonth() && d.getFullYear() === now.getFullYear();
  });
  const monthPurchase = thisMonth.filter(b => b.status === 'holding').reduce((s, b) => s + b.quantity_tons, 0);
  const monthSold = thisMonth.filter(b => b.status === 'sold').reduce((s, b) => s + b.quantity_tons, 0);
  const monthProfit = thisMonth.filter(b => b.status === 'sold').reduce((s, b) => {
    return s + ((b.sold_price_per_jin || 0) - (b.purchase_price_per_jin || 0)) * b.quantity_tons * 2000;
  }, 0);

  container.innerHTML = `
    <!-- 个人信息 -->
    <div class="card" style="text-align:center;">
      <div class="profile-avatar">🧄</div>
      <div style="font-size:16px;font-weight:bold;" id="profile-nickname">${profile.nickname}</div>
      <div style="font-size:11px;color:#999;">${profile.storage_area} · ${profile.location}</div>
      <div style="display:flex;justify-content:center;gap:24px;margin-top:10px;font-size:11px;color:#666;">
        <div><span style="font-weight:bold;font-size:16px;color:#333;">${holding.length}</span><br>持仓批次</div>
        <div><span style="font-weight:bold;font-size:16px;color:#333;">${holding.reduce((s, b) => s + b.quantity_tons, 0)}</span><br>库存(吨)</div>
        <div><span style="font-weight:bold;font-size:16px;color:#333;">${batches.length}</span><br>总批次</div>
      </div>
    </div>

    <!-- 本月概览 -->
    <div class="card">
      <div class="card-header">💰 本月经营概览</div>
      <div class="summary-grid">
        <div class="summary-cell">
          <div class="label">本月收购</div>
          <div class="value">${monthPurchase}<span style="font-size:11px;">吨</span></div>
        </div>
        <div class="summary-cell">
          <div class="label">本月出货</div>
          <div class="value">${monthSold}<span style="font-size:11px;">吨</span></div>
        </div>
        <div class="summary-cell">
          <div class="label">持仓库存</div>
          <div class="value">${holding.reduce((s,b) => s + b.quantity_tons, 0)}<span style="font-size:11px;">吨</span></div>
        </div>
        <div class="summary-cell cell-green">
          <div class="label">本月盈亏</div>
          <div class="value" style="color:${monthProfit >= 0 ? '#d32f2f' : '#388e3c'};font-size:${Math.abs(monthProfit) >= 10000 ? '16px' : '20px'};">${monthProfit >= 0 ? '+' : ''}${(monthProfit / 10000).toFixed(2)}<span style="font-size:11px;">万</span></div>
        </div>
      </div>
    </div>

    <!-- 设置 -->
    <div class="card">
      <div class="card-header">⚙️ 设置</div>
      <div class="setting-item" data-action="edit-profile">
        <span>👤 编辑个人信息</span>
        <span class="arrow">›</span>
      </div>
      <div class="setting-item" data-action="default-region">
        <span>📊 默认产区</span>
        <span style="color:#999;">${getRegionName(settings.default_region)} <span class="arrow">›</span></span>
      </div>
      <div class="setting-item" data-action="price-alert">
        <span>🔔 价格预警阈值</span>
        <span style="color:#999;">涨跌${settings.price_alert_threshold}%提醒 <span class="arrow">›</span></span>
      </div>
      <div class="setting-item" data-action="backup">
        <span>💾 数据备份</span>
        <span style="color:#999;">${settings.last_backup_date ? '上次 ' + new Date(settings.last_backup_date).toLocaleDateString('zh-CN') : '未备份'} <span class="arrow">›</span></span>
      </div>
      <div class="setting-item" data-action="export">
        <span>📋 导出交易台账</span>
        <span style="color:#999;">JSON格式 <span class="arrow">›</span></span>
      </div>
      <div class="setting-item" data-action="about" style="border:none;">
        <span>ℹ️ 关于蒜来宝</span>
        <span style="color:#999;">v1.0 <span class="arrow">›</span></span>
      </div>
    </div>
  `;

  // 绑定设置点击
  container.querySelectorAll('.setting-item').forEach(item => {
    item.addEventListener('click', () => handleSetting(item.dataset.action));
  });
}

/** 处理设置操作 */
function handleSetting(action) {
  switch (action) {
    case 'edit-profile':
      editProfile();
      break;
    case 'default-region':
      changeDefaultRegion();
      break;
    case 'price-alert':
      changeAlertThreshold();
      break;
    case 'backup':
      downloadBackup();
      showToast('备份已下载');
      break;
    case 'export':
      exportTransactions();
      break;
    case 'about':
      showAbout();
      break;
  }
}

/** 编辑个人信息 */
function editProfile() {
  const profile = getProfile();
  const nickname = prompt('昵称：', profile.nickname);
  if (nickname !== null) {
    const location = prompt('所在地：', profile.location);
    if (location !== null) {
      const storage = prompt('冷库位置：', profile.storage_area);
      if (storage !== null) {
        saveProfile({ nickname, location, storage_area: storage });
        showToast('个人信息已更新');
        // 刷新页面
        location.reload();
      }
    }
  }
}

/** 修改默认产区 */
function changeDefaultRegion() {
  const settings = getSettings();
  const regions = ['金乡', '杞县', '邳州', '中牟'];
  const keys = ['jinxiang', 'qixian', 'pizhou', 'zhongmou'];
  const currentIdx = keys.indexOf(settings.default_region);

  const choice = prompt(
    `选择默认产区（输入序号）：\n1. 金乡\n2. 杞县\n3. 邳州\n4. 中牟`,
    (currentIdx + 1).toString()
  );

  const idx = parseInt(choice) - 1;
  if (idx >= 0 && idx < keys.length) {
    saveSettings({ default_region: keys[idx] });
    showToast('默认产区已设为 ' + regions[idx]);
  }
}

/** 修改预警阈值 */
function changeAlertThreshold() {
  const settings = getSettings();
  const threshold = prompt('价格涨跌超过多少%时提醒？（1-20）', settings.price_alert_threshold.toString());
  if (threshold !== null) {
    const val = parseInt(threshold);
    if (val >= 1 && val <= 20) {
      saveSettings({ price_alert_threshold: val });
      showToast('预警阈值已设为 ' + val + '%');
    }
  }
}

/** 导出交易台账 */
function exportTransactions() {
  const batches = getBatches();
  // 生成简单CSV
  let csv = '批次号,产区,规格,数量(吨),收购价(元/斤),入库日期,状态,卖出价(元/斤),出库日期,存放位置\n';
  batches.forEach(b => {
    csv += [
      b.batch_no, b.region_name, b.spec, b.quantity_tons,
      b.purchase_price_per_jin, b.purchase_date, b.status === 'holding' ? '持仓中' : '已出库',
      b.sold_price_per_jin || '', b.sold_date || '', b.storage_location || ''
    ].join(',') + '\n';
  });

  const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = '大蒜交易台账_' + new Date().toISOString().slice(0, 10) + '.csv';
  a.click();
  URL.revokeObjectURL(url);
  showToast('台账已导出');
}

/** 关于 */
function showAbout() {
  alert('🧄 蒜来宝 v1.0\n\n大蒜经营助手 - 个人版\n\n行情监控 · 库存管理 · 利润计算\n\n数据来源：国际大蒜贸易网、Mysteel\n\n© 2026 个人使用');
}

/** 产区名称映射 */
function getRegionName(key) {
  const names = { jinxiang: '金乡', qixian: '杞县', pizhou: '邳州', zhongmou: '中牟' };
  return names[key] || key;
}
