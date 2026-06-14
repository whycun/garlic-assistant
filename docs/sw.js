// 蒜来宝 Service Worker
// 缓存策略：App壳缓存优先，数据网络优先+缓存兜底
const CACHE_VERSION = 'garlic-v1';
const SHELL_CACHE = 'garlic-shell-v1';
const DATA_CACHE = 'garlic-data-v1';

// App壳文件列表（首次安装时预缓存）
const SHELL_FILES = [
  './',
  './index.html',
  './css/style.css',
  './manifest.json',
  './js/app.js',
  './js/utils.js',
  './js/storage.js',
  './js/data-fetcher.js',
  './js/chart-renderer.js',
  './js/pages/home.js',
  './js/pages/market.js',
  './js/pages/inventory.js',
  './js/pages/calculator.js',
  './js/pages/profile.js',
  'https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js'
];

// 安装：预缓存App壳
self.addEventListener('install', event => {
  console.log('[SW] 安装中...');
  event.waitUntil(
    caches.open(SHELL_CACHE).then(cache => {
      console.log('[SW] 预缓存App壳文件');
      return cache.addAll(SHELL_FILES).catch(err => {
        // 某些CDN文件可能加载失败，不阻塞安装
        console.warn('[SW] 部分文件缓存失败:', err);
      });
    })
  );
  self.skipWaiting();
});

// 激活：清理旧缓存
self.addEventListener('activate', event => {
  console.log('[SW] 激活');
  event.waitUntil(
    caches.keys().then(keys => {
      return Promise.all(
        keys.filter(key => key !== SHELL_CACHE && key !== DATA_CACHE)
          .map(key => caches.delete(key))
      );
    })
  );
  self.clients.claim();
});

// 请求拦截
self.addEventListener('fetch', event => {
  const url = new URL(event.request.url);

  // API数据请求（行情JSON）：网络优先 + 缓存兜底
  if (url.pathname.includes('prices.json') || url.pathname.includes('news.json') ||
      url.hostname === 'raw.githubusercontent.com') {
    event.respondWith(networkFirst(event.request));
    return;
  }

  // App壳文件：缓存优先
  event.respondWith(cacheFirst(event.request));
});

// 缓存优先策略
async function cacheFirst(request) {
  const cached = await caches.match(request);
  if (cached) return cached;
  try {
    const response = await fetch(request);
    if (response.ok) {
      const cache = await caches.open(SHELL_CACHE);
      cache.put(request, response.clone());
    }
    return response;
  } catch (err) {
    // 离线且无缓存，返回空
    return new Response('', { status: 503 });
  }
}

// 网络优先策略
async function networkFirst(request) {
  try {
    const response = await fetch(request);
    if (response.ok) {
      const cache = await caches.open(DATA_CACHE);
      cache.put(request, response.clone());
    }
    return response;
  } catch (err) {
    // 网络失败，尝试返回缓存
    const cached = await caches.match(request);
    if (cached) {
      console.log('[SW] 使用缓存数据');
      return cached;
    }
    return new Response(JSON.stringify({ status: 'offline', error: '无法连接网络' }), {
      status: 503,
      headers: { 'Content-Type': 'application/json' }
    });
  }
}
