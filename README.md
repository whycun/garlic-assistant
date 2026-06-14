# 🧄 蒜来宝 - 大蒜经营助手

个人大蒜冷库经营工具 App（PWA）。行情监控 + 库存管理 + 利润计算，零持续费用。

## 📱 功能

| 页面 | 功能 |
|------|------|
| 📰 首页 | 大蒜行情资讯流，预警信息，价格异动提醒 |
| 📊 行情 | 30天价格走势图，金乡/杞县/邳州/中牟四大产区分规格实时报价，库存/种植/出口/天气等关键影响因素 |
| 📦 库存 | 批次管理（入库/出库/浮动盈亏），利润计算器（小票式成本核算） |
| 👤 我的 | 个人信息，本月经营概览，设置（默认产区/预警阈值），数据备份导出 |

## 🏗️ 架构

```
零成本 = GitHub Actions(免费爬虫) + GitHub Pages(免费托管) + 手机本地存储
```

- **行情数据**：Python爬虫每30分钟抓取 51garlic.com / Mysteel，产出JSON文件存GitHub仓库
- **App**：PWA网页应用，可安装到Android手机桌面。从GitHub读取行情数据，经营数据全部存手机本地
- **离线**：Service Worker缓存，无网络也能查看已加载的数据

## 📂 项目结构

```
大蒜/
├── app/                  # PWA 应用（部署到 GitHub Pages）
│   ├── index.html        # 主页面
│   ├── manifest.json     # PWA 配置
│   ├── sw.js             # Service Worker（离线缓存）
│   ├── css/style.css     # 样式
│   ├── js/               # JS 模块
│   │   ├── app.js        # 应用入口
│   │   ├── data-fetcher.js  # 数据获取
│   │   ├── storage.js    # 本地存储
│   │   ├── chart-renderer.js # 图表
│   │   ├── utils.js      # 工具函数
│   │   └── pages/        # 4个页面模块
│   └── icons/            # PWA 图标
│
├── scraper/              # Python 爬虫
│   ├── main.py           # 入口
│   ├── scrapers/         # 51garlic + Mysteel 爬虫
│   ├── merger.py         # 数据合并去重
│   ├── utils.py          # 工具
│   └── requirements.txt
│
├── data/                 # 爬虫产出的行情数据（JSON）
│   ├── prices.json       # 价格
│   └── news.json         # 新闻
│
└── .github/workflows/
    └── scrape.yml        # 定时爬虫（每30分钟）
```

## 🚀 部署步骤（用户操作）

### 1. 注册 GitHub 账号（免费）
前往 https://github.com 注册，记住你的用户名（例如 `zhangsan`）。

### 2. 创建仓库
- 点击右上角 `+` → `New repository`
- Repository name 填：`garlic-assistant`（或任意名字）
- 选 **Public**（公开，Actions无限免费）或 Private（每月2000分钟，完全够用）
- 不要勾选任何初始化选项

### 3. 上传代码
```bash
# 在项目目录下
git init
git add .
git commit -m "初始化蒜来宝项目"
git branch -M main
git remote add origin https://github.com/你的用户名/garlic-assistant.git
git push -u origin main
```

### 4. 启用 GitHub Pages
- 进入仓库 → Settings → Pages
- Source 选 `Deploy from a branch`
- Branch 选 `main`，文件夹选 `/app`
- 点 Save
- 等1-2分钟，你会看到网址：`https://你的用户名.github.io/garlic-assistant/`

### 5. 修改数据源配置
- 打开 `app/js/data-fetcher.js`
- 把第7行的 `YOUR_GITHUB_USERNAME` 改成你的GitHub用户名
- 把第8行的 `garlic-assistant` 改成你的仓库名（如果不同）
- 提交修改

### 6. 手动触发第一次爬虫
- 仓库 → Actions 标签 → 左边点 "抓取大蒜行情数据"
- 点 "Run workflow" → "Run workflow"
- 等2分钟，绿色的✅出现
- 检查 `data/prices.json` 文件是否有数据

### 7. 手机上安装
- 用安卓手机 Chrome 打开：`https://你的用户名.github.io/garlic-assistant/`
- 等页面加载完，Chrome会提示"添加到主屏幕"（或点右上角菜单 → 添加到主屏幕）
- 桌面上出现"蒜来宝"图标，点开就是独立App

### 8. 开始使用
- 录入你的库存批次（库存页 → 新增入库批次）
- 看看行情页有没有最新数据
- 用计算器算算利润

## ⚠️ 注意事项

1. **数据隐私**：你的库存、成本、盈亏数据全部存在手机浏览器本地，不上传任何服务器。清除浏览器数据会丢失这些数据！请定期在"我的"页面备份。

2. **行情数据**：爬虫抓取的公开价格数据存在GitHub仓库中。如果仓库是公开的，别人也能看到这些行情数据（本来就是公开信息）。

3. **爬虫稳定性**：如果目标网站（51garlic、Mysteel）改版，爬虫可能失效。到Actions标签查看运行日志排查。

4. **更新App**：修改 `app/` 目录下的文件，提交到GitHub，GitHub Pages会自动更新。

5. **关于GitHub Pages被墙**：个别地区可能访问慢。如果遇到，可以在 `data-fetcher.js` 里把 jsDelivr CDN 作为备用。目前已经配置了jsDelivr。

## 🛠️ 开发者信息

- 前端：原生 HTML/CSS/JS + Chart.js，ES6模块，零构建工具
- 后端：无服务器。Python爬虫 + GitHub Actions
- 存储：行情数据用JSON文件（GitHub），用户数据用localStorage（手机本地）
- PWA：manifest.json + Service Worker（离线支持）

## 📄 许可

个人使用，无许可证限制。
