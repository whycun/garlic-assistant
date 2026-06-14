// 蒜来宝 - 主应用入口
// 初始化、导航、Service Worker注册、数据刷新

import { renderHome, refreshHome } from './pages/home.js';
import { renderMarket, refreshMarket } from './pages/market.js';
import { renderInventory, refreshInventory } from './pages/inventory.js';
import { renderProfile } from './pages/profile.js';

// 页面模块映射
const pages = {
  home: { render: renderHome, refresh: refreshHome, containerId: 'page-home' },
  market: { render: renderMarket, refresh: refreshMarket, containerId: 'page-market' },
  inventory: { render: renderInventory, refresh: refreshInventory, containerId: 'page-inventory' },
  me: { render: renderProfile, refresh: null, containerId: 'page-me' },
};

let currentPage = 'home';
let isRefreshing = false;

/** 应用初始化 */
async function init() {
  console.log('🧄 蒜来宝 启动中...');

  // 注册 Service Worker
  registerSW();

  // 监听安装事件
  listenInstallPrompt();

  // 渲染首页
  await switchPage('home');

  // 自动更新数据
  setTimeout(async () => {
    if (pages.market.refresh) await pages.market.refresh();
  }, 2000);

  console.log('✅ 蒜来宝 启动完成');
}

/** 切换页面 */
export async function switchPage(pageName) {
  if (currentPage === pageName && pageName !== 'home') return;

  // 隐藏所有页面
  document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));

  // 更新导航
  document.querySelectorAll('.nav-item').forEach(n => {
    n.classList.toggle('active', n.dataset.page === pageName);
  });

  // 显示目标页面
  const page = pages[pageName];
  if (!page) return;

  const container = document.getElementById(page.containerId);
  if (!container) return;

  container.classList.add('active');

  // 首次渲染或重新渲染
  if (!container.dataset.rendered || pageName === 'inventory') {
    await page.render(container);
    container.dataset.rendered = 'true';
  }

  currentPage = pageName;

  // 滚动到顶部
  document.querySelector('.page-content').scrollTop = 0;
}

/** 下拉刷新 */
let touchStartY = 0;
let pullDistance = 0;

function setupPullToRefresh() {
  const content = document.querySelector('.page-content');

  content.addEventListener('touchstart', (e) => {
    if (content.scrollTop <= 0) {
      touchStartY = e.touches[0].clientY;
      pullDistance = 0;
    }
  }, { passive: true });

  content.addEventListener('touchmove', (e) => {
    if (content.scrollTop <= 0 && touchStartY > 0) {
      pullDistance = e.touches[0].clientY - touchStartY;
      if (pullDistance > 30) {
        document.querySelector('.pull-indicator').style.display = 'block';
      }
    }
  }, { passive: true });

  content.addEventListener('touchend', async () => {
    document.querySelector('.pull-indicator').style.display = 'none';
    if (pullDistance > 80 && !isRefreshing) {
      await refreshCurrentPage();
    }
    touchStartY = 0;
    pullDistance = 0;
  });
}

/** 刷新当前页面 */
async function refreshCurrentPage() {
  if (isRefreshing) return;
  isRefreshing = true;

  const refreshBtn = document.getElementById('refresh-btn');
  if (refreshBtn) refreshBtn.classList.add('refreshing');

  const page = pages[currentPage];
  if (page && page.refresh) {
    await page.refresh();
  }

  setTimeout(() => {
    if (refreshBtn) refreshBtn.classList.remove('refreshing');
    isRefreshing = false;
  }, 500);
}

/** 注册 Service Worker */
function registerSW() {
  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('./sw.js')
      .then(reg => console.log('[App] SW 注册成功:', reg.scope))
      .catch(err => console.warn('[App] SW 注册失败:', err));
  }
}

/** 监听PWA安装 */
let deferredPrompt;
function listenInstallPrompt() {
  window.addEventListener('beforeinstallprompt', (e) => {
    e.preventDefault();
    deferredPrompt = e;

    // 显示安装横幅
    const banner = document.getElementById('install-banner');
    if (banner) {
      banner.classList.remove('hidden');
      banner.addEventListener('click', async () => {
        deferredPrompt.prompt();
        const result = await deferredPrompt.userChoice;
        console.log('[App] 安装结果:', result.outcome);
        deferredPrompt = null;
        banner.classList.add('hidden');
      });
    }
  });

  // 已安装
  window.addEventListener('appinstalled', () => {
    console.log('[App] PWA已安装');
    const banner = document.getElementById('install-banner');
    if (banner) banner.classList.add('hidden');
    deferredPrompt = null;
  });
}

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', init);

// 导出供HTML使用
window.switchPage = switchPage;
window.refreshCurrentPage = refreshCurrentPage;

// 监听网络状态变化
window.addEventListener('online', () => {
  console.log('[App] 网络恢复');
  refreshCurrentPage();
});

window.addEventListener('offline', () => {
  console.log('[App] 网络断开');
});
