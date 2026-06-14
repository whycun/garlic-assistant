"""
51garlic.com (国际大蒜贸易网) 爬虫
抓取每日产区报价 + 行情新闻
"""
import re
import logging
from ..utils import create_session, fetch_page, parse_html, clean_price, beijing_iso, today_str

logger = logging.getLogger('51garlic')

BASE_URL = 'https://www.51garlic.com'

# 各产区关键词映射
REGION_KEYWORDS = {
    'jinxiang': ['金乡'],
    'qixian': ['杞县'],
    'pizhou': ['邳州'],
    'zhongmou': ['中牟'],
}

SPEC_NAMES = [
    '蒜米料', '印尼货', '一般混级', '中大混级', '太空大混级',
    '脱水蒜', '大混级', '原皮蒜米料', '原皮印尼货',
    '5.5cm净蒜', '6.0cm净蒜', '6.5cm净蒜',
    '小混级', '杂交蒜', '太空蒜',
]


def scrape_prices(session):
    """
    抓取产区价格数据
    目标页面：51garlic.com 的行情/报价页面
    返回结构化价格数据
    """
    try:
        # 主要行情页面
        url = f'{BASE_URL}/zx/list.php?classid=1'
        html = fetch_page(session, url)
        soup = parse_html(html)

        result = {
            'updated_at': beijing_iso(),
            'regions': {},
            'news_items': [],
        }

        # 解析新闻列表获取价格信息
        items = soup.select('.list_li, .news_list li, .list_item, article, .zx_list li')
        if not items:
            items = soup.select('li')

        current_region = None
        region_data = {}

        for item in items:
            text = item.get_text(strip=True)

            # 检测产区
            for region_key, keywords in REGION_KEYWORDS.items():
                for kw in keywords:
                    if kw in text and ('产区' in text or '行情' in text or '价格' in text or '报价' in text):
                        current_region = region_key
                        if region_key not in region_data:
                            region_data[region_key] = {'specs': [], 'mood': ''}
                        break

            # 检测价格信息
            if current_region:
                for spec in SPEC_NAMES:
                    if spec in text:
                        low, high = clean_price(text)
                        if low and high:
                            region_data[current_region]['specs'].append({
                                'name': spec,
                                'low': low,
                                'high': high,
                                'unit': '元/斤',
                                'trend': 'stable'
                            })

            # 收集新闻
            link = item.select_one('a')
            if link and link.get('href'):
                title = link.get_text(strip=True)
                if len(title) > 10 and any(kw in title for kw in ['大蒜', '蒜价', '行情', '产区']):
                    result['news_items'].append({
                        'title': title,
                        'url': link['href'] if link['href'].startswith('http') else BASE_URL + link['href'],
                        'source': '51garlic',
                        'source_name': '国际大蒜贸易网',
                    })

        # 整理产区数据
        for region_key, data in region_data.items():
            # 去重
            seen = set()
            unique_specs = []
            for spec in data['specs']:
                if spec['name'] not in seen:
                    seen.add(spec['name'])
                    unique_specs.append(spec)
            result['regions'][region_key] = {
                'name': REGION_KEYWORDS[region_key][0],
                'specs': unique_specs,
                'market_mood': _detect_mood(unique_specs),
            }

        logger.info(f'51garlic: 解析到 {len(result["regions"])} 个产区, {len(result["news_items"])} 条新闻')
        return {'success': True, 'data': result}

    except Exception as e:
        logger.error(f'51garlic 爬取失败: {e}')
        return {'success': False, 'data': None, 'error': str(e)}


def scrape_news(session):
    """
    抓取大蒜新闻/分析文章
    """
    try:
        news = []
        # 新闻列表页
        urls = [
            f'{BASE_URL}/zx/list.php?classid=1',  # 最新资讯
            f'{BASE_URL}/zx/list.php?classid=2',  # 分析预测
        ]

        for url in urls:
            try:
                html = fetch_page(session, url)
                soup = parse_html(html)

                items = soup.select('.list_li a, .news_list li a, article a, h3 a, .title a')
                for item in items[:15]:
                    title = item.get_text(strip=True)
                    href = item.get('href', '')
                    if not title or len(title) < 8:
                        continue
                    if not any(kw in title for kw in ['大蒜', '蒜', '行情', '产区', '价格', '出口', '库存', '种植']):
                        continue

                    full_url = href if href.startswith('http') else BASE_URL + href

                    # 判断标签
                    tag = '行情分析'
                    tag_type = 'analysis'
                    if any(w in title for w in ['预警', '风险', '库存压力', '受冻', '病虫']):
                        tag = '风险提示'
                        tag_type = 'warning'
                    elif any(w in title for w in ['政策', '出口退税', '补贴', '农业部']):
                        tag = '政策'
                        tag_type = 'policy'
                    elif any(w in title for w in ['预测', '展望', '年报', '深度']):
                        tag = '深度分析'
                        tag_type = 'deep'
                    elif any(w in title for w in ['种植', '成本', '人工', '蒜种']):
                        tag = '成本动态'
                        tag_type = 'cost'
                    elif any(w in title for w in ['金乡', '杞县', '邳州', '购销']):
                        tag = '产区动态'
                        tag_type = 'dynamics'

                    news.append({
                        'title': title,
                        'url': full_url,
                        'source': '51garlic',
                        'source_name': '国际大蒜贸易网',
                        'tag': tag,
                        'tag_type': tag_type,
                    })
            except Exception as e:
                logger.warning(f'新闻页 {url} 抓取失败: {e}')
                continue

        logger.info(f'51garlic 新闻: {len(news)} 条')
        return {'success': True, 'data': news[:25]}

    except Exception as e:
        logger.error(f'51garlic 新闻抓取失败: {e}')
        return {'success': False, 'data': [], 'error': str(e)}


def _detect_mood(specs):
    """根据涨跌判断市场情绪"""
    if not specs:
        return '平稳'
    up = sum(1 for s in specs if s.get('trend') == 'up')
    down = sum(1 for s in specs if s.get('trend') == 'down')
    if up > down:
        return '偏强'
    elif down > up:
        return '偏弱'
    return '平稳'
