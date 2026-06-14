// 蒜来宝 - 数据获取模块
// 从 GitHub raw URL 拉取行情数据，带离线缓存兜底

import { cachePriceData, getCachedPriceData, cacheNewsData, getCachedNewsData } from './storage.js';

// GitHub 仓库配置（用户部署后需修改）
const GITHUB_USER = 'whycun';
const GITHUB_REPO = 'garlic-assistant';
const GITHUB_BRANCH = 'main';

// 备用CDN（jsDelivr，国内访问更快）
const DATA_BASE_URL = `https://raw.githubusercontent.com/${GITHUB_USER}/${GITHUB_REPO}/${GITHUB_BRANCH}/data`;
const CDN_BASE_URL = `https://cdn.jsdelivr.net/gh/${GITHUB_USER}/${GITHUB_REPO}@${GITHUB_BRANCH}/data`;

// 当前使用的数据源（优先CDN）
let useCDN = true;

const FETCH_TIMEOUT = 10000; // 10秒超时

/**
 * 获取价格数据
 * 策略：网络优先 → CDN兜底 → localStorage缓存兜底
 */
export async function fetchPrices() {
  const urls = useCDN
    ? [`${CDN_BASE_URL}/prices.json`, `${DATA_BASE_URL}/prices.json`]
    : [`${DATA_BASE_URL}/prices.json`, `${CDN_BASE_URL}/prices.json`];

  for (const url of urls) {
    try {
      const data = await fetchWithTimeout(url);
      if (data && data.regions) {
        cachePriceData(data);
        return { success: true, data, source: 'network' };
      }
    } catch (e) {
      console.warn('[Fetcher] 价格URL失败:', url, e.message);
      continue;
    }
  }

  // 所有网络来源失败，使用缓存
  const cached = getCachedPriceData();
  if (cached && cached.data) {
    console.log('[Fetcher] 使用缓存价格数据');
    return { success: true, data: cached.data, source: 'cache', stale: true };
  }

  return { success: false, error: '无法获取价格数据，请检查网络连接' };
}

/**
 * 获取新闻数据
 */
export async function fetchNews() {
  const urls = useCDN
    ? [`${CDN_BASE_URL}/news.json`, `${DATA_BASE_URL}/news.json`]
    : [`${DATA_BASE_URL}/news.json`, `${CDN_BASE_URL}/news.json`];

  for (const url of urls) {
    try {
      const data = await fetchWithTimeout(url);
      if (data && data.items) {
        cacheNewsData(data);
        return { success: true, data, source: 'network' };
      }
    } catch (e) {
      console.warn('[Fetcher] 新闻URL失败:', url, e.message);
      continue;
    }
  }

  const cached = getCachedNewsData();
  if (cached && cached.data) {
    console.log('[Fetcher] 使用缓存新闻数据');
    return { success: true, data: cached.data, source: 'cache', stale: true };
  }

  return { success: false, error: '无法获取新闻数据' };
}

/**
 * 带超时的 fetch
 */
async function fetchWithTimeout(url) {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), FETCH_TIMEOUT);

  try {
    const resp = await fetch(url, {
      signal: controller.signal,
      cache: 'no-cache',
      headers: { 'Accept': 'application/json' },
    });
    if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
    return await resp.json();
  } finally {
    clearTimeout(timeoutId);
  }
}

/**
 * 配置GitHub信息（用户可在设置中修改）
 */
export function configureDataSource(username, repo, branch = 'main') {
  // 这个函数修改后需要重新构建URL
  // 实际使用中从settings读取，这里做占位
  console.log('[Fetcher] 数据源配置:', { username, repo, branch });
}

/**
 * 测试数据源连通性
 */
export async function testConnection() {
  const urls = [
    `${CDN_BASE_URL}/prices.json`,
    `${DATA_BASE_URL}/prices.json`,
  ];
  for (const url of urls) {
    try {
      const resp = await fetch(url, { method: 'HEAD', cache: 'no-cache' });
      if (resp.ok) return { ok: true, url };
    } catch (e) {
      continue;
    }
  }
  return { ok: false };
}
