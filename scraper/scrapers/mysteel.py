"""
Mysteel (我的钢铁网-果蔬频道) 爬虫
抓取大蒜每日价格 + 行情分析
"""
import re
import logging
from scraper.utils import create_session, fetch_page, parse_html, clean_price, beijing_iso

logger = logging.getLogger('mysteel')

BASE_URL = 'https://guoshu.mysteel.com'
PRICE_URL = f'{BASE_URL}/market/p-2442-----071007-0--------3.html'
NEWS_URL = 'https://ncp.mysteel.com/'


def scrape_prices(session):
    """
    从 Mysteel 果蔬频道抓取大蒜价格
    """
    try:
        html = fetch_page(session, PRICE_URL)
        soup = parse_html(html)

        result = {
            'regions': {},
            'news_items': [],
        }

        # 尝试解析价格表格
        tables = soup.select('table')
        rows = soup.select('tr')

        current_region = None
        all_specs = []

        for row in rows:
            cells = row.select('td, th')
            texts = [c.get_text(strip=True) for c in cells]
            full_text = ' '.join(texts)

            if not full_text:
                continue

            # 检测产区
            for region, name in [('jinxiang', '金乡'), ('qixian', '杞县'), ('pizhou', '邳州'), ('zhongmou', '中牟')]:
                if name in full_text:
                    current_region = region
                    break

            # 检测价格
            low, high = clean_price(full_text)
            if low and high and current_region and len(texts) >= 2:
                spec_name = texts[0] if texts else '未分类'
                all_specs.append({
                    'region': current_region,
                    'name': spec_name,
                    'low': low,
                    'high': high,
                    'unit': '元/斤',
                    'trend': 'stable',
                })

        # 按产区分组
        for spec in all_specs:
            region = spec.pop('region')
            if region not in result['regions']:
                result['regions'][region] = {'specs': [], 'name': _region_name(region)}
            result['regions'][region]['specs'].append(spec)

        # 如果没解析到表格数据，尝试从文章列表解析
        if not result['regions']:
            result = _fallback_parse(soup)

        # 抓新闻
        articles = soup.select('a[href*="/a/"], .title a, h3 a')
        for a in articles[:15]:
            title = a.get_text(strip=True)
            href = a.get('href', '')
            if len(title) > 10 and any(kw in title for kw in ['大蒜', '蒜价', '日报', '周报', '行情']):
                result['news_items'].append({
                    'title': title,
                    'url': href if href.startswith('http') else BASE_URL + href,
                    'source': 'mysteel',
                    'source_name': 'Mysteel',
                })

        logger.info(f'Mysteel: 解析到 {len(result["regions"])} 个产区, {len(result["news_items"])} 条新闻')
        return {'success': True, 'data': result}

    except Exception as e:
        logger.error(f'Mysteel 爬取失败: {e}')
        return {'success': False, 'data': None, 'error': str(e)}


def scrape_news(session):
    """抓取 Mysteel 新闻"""
    try:
        news = []
        html = fetch_page(session, f'{BASE_URL}/market/p-2442-----071007-0--------3.html')
        soup = parse_html(html)

        links = soup.select('a[href]')
        for a in links[:20]:
            title = a.get_text(strip=True)
            href = a.get('href', '')
            if len(title) < 10:
                continue
            if not any(kw in title for kw in ['大蒜', '蒜', '行情', '产区', '价格', '日报', '周报']):
                continue

            full_url = href if href.startswith('http') else BASE_URL + href
            tag = '行情分析'
            tag_type = 'analysis'
            if any(w in title for w in ['日报']):
                tag = '产区动态'
                tag_type = 'dynamics'
            elif any(w in title for w in ['周报', '年报', '展望', '预测']):
                tag = '深度分析'
                tag_type = 'deep'

            news.append({
                'title': title,
                'url': full_url,
                'source': 'mysteel',
                'source_name': 'Mysteel',
                'tag': tag,
                'tag_type': tag_type,
            })

        logger.info(f'Mysteel 新闻: {len(news)} 条')
        return {'success': True, 'data': news[:20]}

    except Exception as e:
        logger.error(f'Mysteel 新闻抓取失败: {e}')
        return {'success': False, 'data': [], 'error': str(e)}


def _region_name(key):
    mapping = {'jinxiang': '金乡', 'qixian': '杞县', 'pizhou': '邳州', 'zhongmou': '中牟'}
    return mapping.get(key, key)


def _fallback_parse(soup):
    """当表格解析失败时的备选方案 - 从页面文本提取"""
    result = {'regions': {}, 'news_items': []}
    text = soup.get_text()

    region_patterns = [
        ('jinxiang', r'金乡.*?(?:价格|报价|行情).*?(\d+\.?\d*)\s*[-－~至]\s*(\d+\.?\d*)'),
        ('qixian', r'杞县.*?(?:价格|报价|行情).*?(\d+\.?\d*)\s*[-－~至]\s*(\d+\.?\d*)'),
        ('pizhou', r'邳州.*?(?:价格|报价|行情).*?(\d+\.?\d*)\s*[-－~至]\s*(\d+\.?\d*)'),
    ]

    for region, pattern in region_patterns:
        match = re.search(pattern, text)
        if match:
            result['regions'][region] = {
                'name': _region_name(region),
                'specs': [{
                    'name': '均价',
                    'low': float(match.group(1)),
                    'high': float(match.group(2)),
                    'unit': '元/斤',
                    'trend': 'stable',
                }]
            }

    return result
