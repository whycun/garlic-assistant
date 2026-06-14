// 蒜来宝 - 本地存储模块
// 所有用户私人数据存 localStorage，不上传服务器

const KEYS = {
  BATCHES: 'garlic_batches',
  CALC_SAVES: 'garlic_calc_saves',
  SETTINGS: 'garlic_settings',
  PROFILE: 'garlic_profile',
  PRICE_CACHE: 'garlic_price_cache',
  NEWS_CACHE: 'garlic_news_cache',
};

// ===== 通用读写 =====

function getItem(key, fallback = null) {
  try {
    const raw = localStorage.getItem(key);
    return raw ? JSON.parse(raw) : fallback;
  } catch (e) {
    console.error('[Storage] 读取失败:', key, e);
    return fallback;
  }
}

function setItem(key, value) {
  try {
    localStorage.setItem(key, JSON.stringify(value));
    return true;
  } catch (e) {
    console.error('[Storage] 写入失败:', key, e);
    return false;
  }
}

// ===== 库存批次 =====

export function getBatches() {
  return getItem(KEYS.BATCHES, []);
}

export function saveBatch(batch) {
  const batches = getBatches();
  const idx = batches.findIndex(b => b.id === batch.id);
  if (idx >= 0) {
    batches[idx] = { ...batch, updated_at: new Date().toISOString() };
  } else {
    batch.id = batch.id || 'batch_' + Date.now();
    batch.created_at = new Date().toISOString();
    batch.updated_at = new Date().toISOString();
    batches.push(batch);
  }
  setItem(KEYS.BATCHES, batches);
  return batch;
}

export function deleteBatch(id) {
  const batches = getBatches().filter(b => b.id !== id);
  setItem(KEYS.BATCHES, batches);
}

export function getBatchById(id) {
  return getBatches().find(b => b.id === id);
}

// ===== 计算器保存 =====

export function getCalcSaves() {
  return getItem(KEYS.CALC_SAVES, []);
}

export function saveCalculation(calc) {
  const saves = getCalcSaves();
  calc.id = calc.id || 'calc_' + Date.now();
  calc.created_at = new Date().toISOString();
  saves.unshift(calc); // 最新在前
  if (saves.length > 50) saves.length = 50; // 最多50条
  setItem(KEYS.CALC_SAVES, saves);
  return calc;
}

export function deleteCalcSave(id) {
  const saves = getCalcSaves().filter(c => c.id !== id);
  setItem(KEYS.CALC_SAVES, saves);
}

// ===== 设置 =====

const DEFAULT_SETTINGS = {
  default_region: 'jinxiang',
  price_alert_threshold: 5,
  price_alert_enabled: true,
  data_auto_refresh: true,
  last_backup_date: null,
};

export function getSettings() {
  return { ...DEFAULT_SETTINGS, ...getItem(KEYS.SETTINGS, {}) };
}

export function saveSettings(settings) {
  const current = getSettings();
  setItem(KEYS.SETTINGS, { ...current, ...settings });
}

// ===== 用户信息 =====

const DEFAULT_PROFILE = {
  nickname: '金乡老张',
  location: '山东金乡',
  storage_area: '冷库A区',
};

export function getProfile() {
  return { ...DEFAULT_PROFILE, ...getItem(KEYS.PROFILE, {}) };
}

export function saveProfile(profile) {
  const current = getProfile();
  setItem(KEYS.PROFILE, { ...current, ...profile });
}

// ===== 行情缓存（离线兜底） =====

export function cachePriceData(data) {
  setItem(KEYS.PRICE_CACHE, { data, cached_at: new Date().toISOString() });
}

export function getCachedPriceData() {
  return getItem(KEYS.PRICE_CACHE);
}

export function cacheNewsData(data) {
  setItem(KEYS.NEWS_CACHE, { data, cached_at: new Date().toISOString() });
}

export function getCachedNewsData() {
  return getItem(KEYS.NEWS_CACHE);
}

// ===== 数据导出 =====

export function exportAllData() {
  return {
    库存批次: getBatches(),
    计算记录: getCalcSaves(),
    设置: getSettings(),
    个人信息: getProfile(),
    导出时间: new Date().toISOString(),
  };
}

export function downloadBackup() {
  const data = exportAllData();
  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = '蒜来宝_备份_' + new Date().toISOString().slice(0, 10) + '.json';
  a.click();
  URL.revokeObjectURL(url);
  saveSettings({ last_backup_date: new Date().toISOString() });
}
