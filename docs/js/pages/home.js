// 蒜来宝 - 首页（行情资讯流）
import { formatDate } from '../utils.js';
import { fetchNews } from '../data-fetcher.js';
import { getCachedNewsData } from '../storage.js';

let newsData = null;
let isLoading = false;

/** 渲染首页 */
export async function renderHome(container) {
  container.innerHTML = `
    <div id="alert-container"></div>
    <div class="card">
      <div class="card-header">📰 行情资讯 <span class="sub" id="news-update-time"></span></div>
      <div id="news-list">
        <div class="skeleton skeleton-card"></div>
        <div class="skeleton skeleton-card"></div>
        <div class="skeleton skeleton-card"></div>
      </div>
    </div>
    <div style="text-align:center;padding:8px;color:#2980b9;font-size:12px;cursor:pointer;" id="load-more-btn">加载更多 →</div>
  `;

  // 绑定事件
  document.getElementById('load-more-btn').addEventListener('click', () => loadMoreNews());

  // 加载数据
  await loadNews();
}

/** 加载新闻 */
async function loadNews() {
  if (isLoading) return;
  isLoading = true;

  const container = document.getElementById('news-list');
  const timeEl = document.getElementById('news-update-time');

  // 先显示缓存
  const cached = getCachedNewsData();
  if (cached && cached.data) {
    newsData = cached.data;
    renderNewsList(container, newsData.items, true);
    if (timeEl) timeEl.textContent = '(已缓存)';
  }

  // 网络获取
  const result = await fetchNews();
  isLoading = false;

  if (result.success) {
    newsData = result.data;
    renderNewsList(container, newsData.items, false);
    if (timeEl) {
      timeEl.textContent = result.stale ? '(数据已缓存)' : '· 已更新';
      timeEl.style.color = result.stale ? '#ff9800' : '#4caf50';
    }
    // 检查预警
    checkAlerts(result.data);
  } else if (!cached) {
    container.innerHTML = '<div class="empty-state"><div class="icon">📡</div><p>无法加载数据，请检查网络</p></div>';
  }
}

/** 渲染新闻列表 */
function renderNewsList(container, items, isPlaceholder = false) {
  if (!items || items.length === 0) {
    container.innerHTML = '<div class="empty-state"><p>暂无资讯</p></div>';
    return;
  }

  container.innerHTML = items.map(item => `
    <div class="news-item" data-url="${item.url || ''}" onclick="window.open('${item.url || '#'}', '_blank')">
      <div class="news-title">
        ${getTagBadge(item.tag, item.tag_type)}${escapeHtml(item.title)}
      </div>
      <div class="news-meta">
        ${item.source_name || item.source} · ${formatDate(item.published_at)}
      </div>
    </div>
  `).join('');
}

/** 加载更多（从缓存加载全部） */
function loadMoreNews() {
  const container = document.getElementById('news-list');
  if (container && newsData && newsData.items.length > 10) {
    renderNewsList(container, newsData.items, false);
    document.getElementById('load-more-btn').style.display = 'none';
  }
}

/** 检查库存预警 */
function checkAlerts(data) {
  const alertContainer = document.getElementById('alert-container');
  if (!alertContainer) return;

  // 查找预警类新闻
  const warnings = (data.items || []).filter(
    item => item.tag_type === 'warning'
  );

  if (warnings.length > 0) {
    alertContainer.innerHTML = `
      <div class="alert-banner warn">
        <span>⚠️</span>
        <span>${warnings[0].title}</span>
      </div>
    `;
  }
}

/** 获取标签徽章HTML */
function getTagBadge(tag, tagType) {
  if (!tag) return '';
  const cls = 'news-tag tag-' + (tagType || 'analysis');
  return `<span class="${cls}">${tag}</span>`;
}

/** HTML转义 */
function escapeHtml(str) {
  if (!str) return '';
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}

/** 刷新（外部调用） */
export async function refreshHome() {
  await loadNews();
}
