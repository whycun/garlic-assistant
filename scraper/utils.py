"""
大蒜爬虫工具模块
- HTTP 会话管理（重试、超时、UA伪装）
- HTML 解析辅助
- 日期/价格格式化
"""
import time
import logging
from datetime import datetime, timezone, timedelta
import requests
from bs4 import BeautifulSoup

logging.basicConfig(level=logging.INFO, format='%(asctime)s [%(name)s] %(levelname)s: %(message)s')
logger = logging.getLogger('garlic-scraper')

# 北京时区
TZ_BEIJING = timezone(timedelta(hours=8))

HEADERS = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8',
    'Accept-Language': 'zh-CN,zh;q=0.9,en;q=0.8',
    'Accept-Encoding': 'gzip, deflate',
    'Connection': 'keep-alive',
}


def create_session():
    """创建带重试的 HTTP 会话"""
    session = requests.Session()
    session.headers.update(HEADERS)
    session.timeout = 15
    return session


def fetch_page(session, url, retries=2, backoff=5):
    """获取网页内容，带重试"""
    for attempt in range(retries + 1):
        try:
            logger.info(f'请求: {url}')
            resp = session.get(url, timeout=15)
            resp.raise_for_status()
            # 自动检测编码
            resp.encoding = resp.apparent_encoding or 'utf-8'
            return resp.text
        except requests.RequestException as e:
            if attempt < retries:
                logger.warning(f'请求失败 (第{attempt+1}次): {e}，{backoff}秒后重试...')
                time.sleep(backoff)
            else:
                logger.error(f'请求最终失败: {e}')
                raise


def parse_html(html):
    """解析 HTML"""
    return BeautifulSoup(html, 'lxml')


def clean_price(text):
    """从文本中提取价格数字（元/斤），返回 (low, high) 元组"""
    import re
    # 匹配模式如 "1.60-1.70" "2.00-2.12" "3.90-4.30"
    match = re.search(r'(\d+\.?\d*)\s*[-－~至]\s*(\d+\.?\d*)', text)
    if match:
        return float(match.group(1)), float(match.group(2))
    # 匹配单个价格 "2.10"
    match_single = re.search(r'(\d+\.?\d*)', text)
    if match_single:
        val = float(match_single.group(1))
        return val, val
    return None, None


def now_beijing():
    """返回当前北京时间"""
    return datetime.now(TZ_BEIJING)


def beijing_iso():
    """返回 ISO 格式北京时间字符串"""
    return now_beijing().isoformat()


def today_str():
    """返回今日日期字符串 YYYY-MM-DD"""
    return now_beijing().strftime('%Y-%m-%d')
