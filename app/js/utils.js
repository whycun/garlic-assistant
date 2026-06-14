// 蒜来宝 - 工具函数

/** 格式化金额 */
export function formatMoney(val) {
  if (val == null || isNaN(val)) return '¥0';
  const abs = Math.abs(val);
  if (abs >= 10000) {
    return (val >= 0 ? '¥' : '-¥') + (abs / 10000).toFixed(2) + '万';
  }
  return (val >= 0 ? '¥' : '-¥') + abs.toLocaleString('zh-CN');
}

/** 格式化价格（元/斤） */
export function formatPrice(val) {
  if (val == null || isNaN(val)) return '--';
  return val.toFixed(2);
}

/** 格式化日期 */
export function formatDate(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  const now = new Date();
  const diff = now - d;
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前';
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前';
  if (diff < 172800000) return '昨天';
  const m = d.getMonth() + 1;
  const day = d.getDate();
  return m + '/' + day;
}

/** 格式化吨数 */
export function formatTons(val) {
  if (val == null) return '--';
  return val + '吨';
}

/** 计算利润率 */
export function calcProfitRate(cost, revenue) {
  if (!cost || cost === 0) return 0;
  return ((revenue - cost) / cost * 100);
}

/** 生成唯一ID */
export function genId(prefix = '') {
  return prefix + Date.now() + '_' + Math.random().toString(36).slice(2, 8);
}

/** 防抖 */
export function debounce(fn, delay = 300) {
  let timer;
  return function(...args) {
    clearTimeout(timer);
    timer = setTimeout(() => fn.apply(this, args), delay);
  };
}

/** 显示Toast */
export function showToast(msg, duration = 2000) {
  let toast = document.querySelector('.toast');
  if (!toast) {
    toast = document.createElement('div');
    toast.className = 'toast';
    document.body.appendChild(toast);
  }
  toast.textContent = msg;
  toast.classList.add('show');
  clearTimeout(toast._timer);
  toast._timer = setTimeout(() => toast.classList.remove('show'), duration);
}

/** 获取趋势图标 */
export function trendIcon(trend) {
  if (trend === 'up') return '↑';
  if (trend === 'down') return '↓';
  return '→';
}

/** 获取趋势CSS类 */
export function trendClass(trend) {
  if (trend === 'up') return 'price-up';
  if (trend === 'down') return 'price-down';
  return 'price-stable';
}

/** 获取因素等级CSS */
export function factorLevelClass(level) {
  return 'level-' + (level || 'neutral');
}
