"""
大蒜行情爬虫 - 主入口
由 GitHub Actions 每30分钟（交易时段）触发
"""
import sys
import os
import logging
from concurrent.futures import ThreadPoolExecutor, as_completed

# 确保项目根目录在 sys.path 中
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from scraper.utils import create_session, beijing_iso, today_str
from scraper.scrapers.fiftyone_garlic import scrape_prices as g51_scrape_prices, scrape_news as g51_scrape_news
from scraper.scrapers.mysteel import scrape_prices as ms_scrape_prices, scrape_news as ms_scrape_news
from scraper.merger import merge_prices, merge_news, save_json

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(name)s] %(levelname)s: %(message)s'
)
logger = logging.getLogger('main')


def run():
    logger.info(f'===== 大蒜行情爬虫启动 {beijing_iso()} =====')

    session = create_session()

    # 并行运行所有爬虫
    price_tasks = []
    news_tasks = []

    with ThreadPoolExecutor(max_workers=4) as executor:
        # 提交价格爬虫
        price_futures = {
            executor.submit(g51_scrape_prices, session): '51garlic_prices',
            executor.submit(ms_scrape_prices, session): 'mysteel_prices',
        }
        # 提交新闻爬虫
        news_futures = {
            executor.submit(g51_scrape_news, session): '51garlic_news',
            executor.submit(ms_scrape_news, session): 'mysteel_news',
        }

        # 收集结果
        for future in as_completed(price_futures):
            name = price_futures[future]
            try:
                result = future.result()
                price_tasks.append(result)
                status = '✅' if result['success'] else '❌'
                logger.info(f'{status} 价格爬虫 {name} 完成')
            except Exception as e:
                logger.error(f'💥 价格爬虫 {name} 异常: {e}')
                price_tasks.append({'success': False, 'data': None, 'error': str(e)})

        for future in as_completed(news_futures):
            name = news_futures[future]
            try:
                result = future.result()
                news_tasks.append(result)
                status = '✅' if result['success'] else '❌'
                logger.info(f'{status} 新闻爬虫 {name} 完成')
            except Exception as e:
                logger.error(f'💥 新闻爬虫 {name} 异常: {e}')
                news_tasks.append({'success': False, 'data': [], 'error': str(e)})

    # 合并价格数据
    price_results_success = [r for r in price_tasks if r['success']]
    if price_results_success:
        prices_data, price_news = merge_prices(price_tasks)
        # 验证数据有效性：必须有至少一个产区
        if prices_data.get('regions') and len(prices_data['regions']) > 0:
            save_json(prices_data, 'prices.json')
            logger.info(f'价格数据有效，已保存 ({len(prices_data["regions"])}个产区)')
        else:
            logger.warning('价格数据为空（regions为空），保留现有数据文件不变')
        # 把价格爬虫里附带的新闻也加入新闻列表
        if price_news:
            extra_news = {'success': True, 'data': price_news}
            news_tasks.append(extra_news)
    else:
        logger.error('所有价格爬虫均失败，保留现有数据文件不变')

    # 合并新闻数据
    news_results_success = [r for r in news_tasks if r['success']]
    if news_results_success:
        merged_news = merge_news(news_tasks)
        # 验证数据有效性：必须有至少1条新闻
        if merged_news and merged_news.get('items') and len(merged_news['items']) > 0:
            save_json(merged_news, 'news.json')
            logger.info(f'新闻数据有效，已保存 ({len(merged_news[\"items\"])}条)')
        else:
            logger.warning('新闻数据为空，保留现有数据文件不变')
    else:
        logger.warning('所有新闻爬虫均失败，保留现有数据文件不变')

    # 汇总
    total_regions = len(prices_data.get('regions', {})) if price_results_success else 0
    total_news = len(news_data.get('items', [])) if news_results_success else 0
    logger.info(f'===== 爬取完成: {total_regions} 个产区, {total_news} 条新闻 =====')


if __name__ == '__main__':
    run()
