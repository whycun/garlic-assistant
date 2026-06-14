// 蒜来宝 - 价格走势图渲染
// 依赖 Chart.js（CDN加载）

let chartInstance = null;

/**
 * 渲染/更新价格走势图
 * @param {HTMLCanvasElement} canvas - canvas 元素
 * @param {Array} history - 历史数据 [{date, jinxiang_avg, qixian_avg, pizhou_avg, zhongmou_avg}]
 * @param {string} activeRegion - 当前选中产区 'jinxiang'|'qixian'|'pizhou'|'zhongmou'
 */
export function renderChart(canvas, history, activeRegion = 'jinxiang') {
  if (!canvas || !history || history.length === 0) return;

  const regionNames = {
    jinxiang: '金乡',
    qixian: '杞县',
    pizhou: '邳州',
    zhongmou: '中牟',
  };

  const key = activeRegion + '_avg';
  const labels = history.map(h => h.date.slice(5)); // "05-15" 格式
  const data = history.map(h => h[key] || null);
  const validData = data.filter(d => d !== null);
  const minVal = Math.floor(Math.min(...validData) * 100 - 5) / 100;
  const maxVal = Math.ceil(Math.max(...validData) * 100 + 5) / 100;

  // 确保 Chart.js 已加载
  if (typeof Chart === 'undefined') {
    console.warn('[Chart] Chart.js 未加载');
    return;
  }

  // 销毁旧图表
  if (chartInstance) {
    chartInstance.destroy();
    chartInstance = null;
  }

  const ctx = canvas.getContext('2d');

  // 渐变填充
  const gradient = ctx.createLinearGradient(0, 0, 0, canvas.height);
  gradient.addColorStop(0, 'rgba(46, 125, 50, 0.25)');
  gradient.addColorStop(1, 'rgba(46, 125, 50, 0.02)');

  chartInstance = new Chart(ctx, {
    type: 'line',
    data: {
      labels,
      datasets: [{
        label: regionNames[activeRegion] + '均价(元/斤)',
        data,
        borderColor: '#2e7d32',
        backgroundColor: gradient,
        borderWidth: 2.5,
        fill: true,
        tension: 0.3,
        pointRadius: data.map((_, i) => i === data.length - 1 ? 5 : 0),
        pointBackgroundColor: data.map((_, i) => i === data.length - 1 ? '#d32f2f' : '#2e7d32'),
        pointBorderColor: '#fff',
        pointBorderWidth: 2,
        pointHoverRadius: 6,
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      interaction: {
        intersect: false,
        mode: 'index',
      },
      plugins: {
        legend: { display: false },
        tooltip: {
          backgroundColor: 'rgba(0,0,0,0.8)',
          titleFont: { size: 12 },
          bodyFont: { size: 13 },
          padding: 10,
          displayColors: false,
          callbacks: {
            label: ctx => '均价: ¥' + ctx.parsed.y.toFixed(2) + '/斤',
          }
        }
      },
      scales: {
        x: {
          grid: { display: false },
          ticks: {
            font: { size: 9 },
            color: '#999',
            maxTicksLimit: 8,
            autoSkip: true,
          }
        },
        y: {
          min: minVal,
          max: maxVal,
          grid: { color: '#f0f0f0' },
          ticks: {
            font: { size: 10 },
            color: '#999',
            callback: val => val.toFixed(2),
          }
        }
      }
    }
  });

  return chartInstance;
}

/**
 * 更新图表数据（切换产区时）
 */
export function updateChartData(history, activeRegion) {
  if (!chartInstance) return;

  const key = activeRegion + '_avg';
  const labels = history.map(h => h.date.slice(5));
  const data = history.map(h => h[key] || null);

  chartInstance.data.labels = labels;
  chartInstance.data.datasets[0].data = data;
  chartInstance.update();
}

/**
 * 销毁图表
 */
export function destroyChart() {
  if (chartInstance) {
    chartInstance.destroy();
    chartInstance = null;
  }
}
