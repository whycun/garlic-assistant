"""
数据合并模块
- 合并多个爬虫的价格数据
- 去重新闻
- 维护历史价格记录
- 管理影响因素
"""
import json
import os
import logging
from datetime import datetime, timezone, timedelta

logger = logging.getLogger('merger')

TZ_BEIJING = timezone(timedelta(hours=8))
DATA_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), 'data')
MAX_HISTORY_DAYS = 90
MAX_NEWS_ITEMS = 30

# 默认影响因素（当爬虫获取不到时使用）
DEFAULT_FACTORS = {
    'national_inventory': {'label': '全国库存量', 'value': '349万吨', 'level': 'high'},
    'planting_area': {'label': '种植面积', 'value': '1,200万亩 (+1.0%)', 'level': 'neutral'},
    'export_forecast': {'label': '出口量预期', 'value': '340万吨 (+6.6%)', 'level': 'positive'},
    'weather_risk': {'label': '天气风险', 'value': '晚播冻害关注中', 'level': 'high'},
    'seed_cost': {'label': '蒜种成本', 'value': '3.5-4.3元/斤', 'level': 'down'},
}


def load_existing(filename):
    """加载已有的数据文件"""
    filepath = os.path.join(DATA_DIR, filename)
    if os.path.exists(filepath):
        try:
            with open(filepath, 'r', encoding='utf-8') as f:
                return json.load(f)
        except (json.JSONDecodeError, IOError):
            logger.warning(f'无法读取 {filename}，使用空数据')
    return None


def merge_prices(price_results):
    """
    合并各爬虫的价格数据
    price_results: [{'success': bool, 'data': {...}}, ...]
    """
    regions = {}
    all_news = []

    # 合并所有成功的数据
    for result in price_results:
        if not result['success'] or not result.get('data'):
            continue
        data = result['data']

        # 合并产区数据
        for region_key, region_data in data.get('regions', {}).items():
            if region_key not in regions:
                regions[region_key] = {
                    'name': region_data.get('name', region_key),
                    'market_mood': region_data.get('market_mood', '平稳'),
                    'specs': [],
                }
            # 合并规格数据（按名称去重，新数据覆盖旧数据）
            existing_names = {s['name'] for s in regions[region_key]['specs']}
            for spec in region_data.get('specs', []):
                if spec['name'] not in existing_names:
                    regions[region_key]['specs'].append(spec)
                    existing_names.add(spec['name'])
                else:
                    # 更新已有规格
                    for i, s in enumerate(regions[region_key]['specs']):
                        if s['name'] == spec['name']:
                            regions[region_key]['specs'][i] = spec
                            break

            # 更新市场情绪
            if region_data.get('market_mood'):
                regions[region_key]['market_mood'] = region_data['market_mood']

        # 收集新闻
        all_news.extend(data.get('news_items', []))

    # 计算各产区均价用于历史记录
    today_str = datetime.now(TZ_BEIJING).strftime('%Y-%m-%d')
    history_entry = {'date': today_str}
    for region_key in ['jinxiang', 'qixian', 'pizhou', 'zhongmou']:
        if region_key in regions and regions[region_key]['specs']:
            prices = []
            for s in regions[region_key]['specs']:
                prices.extend([s['low'], s['high']])
            if prices:
                history_entry[f'{region_key}_avg'] = round(sum(prices) / len(prices), 2)

    # 加载历史数据
    existing = load_existing('prices.json')
    history = existing.get('history', []) if existing else []

    # 检查今天是否已有记录
    if history and history[-1].get('date') == today_str:
        history[-1] = history_entry  # 更新今天
    else:
        history.append(history_entry)

    # 限制历史记录数量
    if len(history) > MAX_HISTORY_DAYS:
        history = history[-MAX_HISTORY_DAYS:]

    # 使用已有因素或默认
    factors = existing.get('factors', DEFAULT_FACTORS) if existing else DEFAULT_FACTORS

    prices_data = {
        'schema_version': 1,
        'updated_at': datetime.now(TZ_BEIJING).isoformat(),
        'status': 'ok',
        'regions': regions,
        'history': history,
        'factors': factors,
    }

    return prices_data, all_news


def merge_news(news_results):
    """
    合并去重新闻
    news_results: [{'success': bool, 'data': [...]}, ...]
    """
    all_news = []
    seen_titles = set()

    for result in news_results:
        if not result['success'] or not result.get('data'):
            continue
        for item in result['data']:
            # 模糊去重（取标题前15字）
            key = item['title'][:15].strip()
            if key and key not in seen_titles:
                seen_titles.add(key)
                # 添加ID和时间
                import hashlib
                item['id'] = hashlib.md5(item['title'].encode()).hexdigest()[:16]
                item['published_at'] = datetime.now(TZ_BEIJING).isoformat()
                all_news.append(item)

    # 按时间倒序（最新的在前）
    all_news.sort(key=lambda x: x.get('published_at', ''), reverse=True)

    # 限制条数
    if len(all_news) > MAX_NEWS_ITEMS:
        all_news = all_news[:MAX_NEWS_ITEMS]

    news_data = {
        'schema_version': 1,
        'updated_at': datetime.now(TZ_BEIJING).isoformat(),
        'items': all_news,
        'total_count': len(all_news),
        'max_items': MAX_NEWS_ITEMS,
    }

    return news_data


def save_json(data, filename):
    """保存 JSON 到 data 目录"""
    filepath = os.path.join(DATA_DIR, filename)
    os.makedirs(DATA_DIR, exist_ok=True)
    with open(filepath, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    logger.info(f'已保存: {filepath}')
