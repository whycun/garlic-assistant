package com.whycun.garlicapp.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.whycun.garlicapp.data.remote.HistoryEntry
import com.whycun.garlicapp.data.remote.RegionData
import com.whycun.garlicapp.ui.theme.*
import com.whycun.garlicapp.viewmodel.MainViewModel
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

val REGIONS = mapOf(
    "jinxiang" to "金乡", "qixian" to "杞县",
    "pizhou" to "邳州", "zhongmou" to "中牟"
)

@Composable
fun MarketScreen(vm: MainViewModel = viewModel()) {
    val priceData by vm.priceData.collectAsState()
    val selectedRegion by vm.selectedRegion.collectAsState()
    val isStale by vm.isStale.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Background),
        contentPadding = PaddingValues(bottom = 8.dp)
    ) {
        // 价格走势图
        item {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(10.dp, 10.dp, 10.dp, 0.dp),
                color = Surface, shape = RoundedCornerShape(10.dp), shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("📈 ${REGIONS[selectedRegion]} · 近30天走势",
                            fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            if (isStale) "(缓存)" else "·已更新",
                            fontSize = 10.sp,
                            color = if (isStale) Orange else GreenLight
                        )
                    }

                    // 产区切换标签
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        REGIONS.forEach { (key, name) ->
                            Surface(
                                modifier = Modifier.clickable { vm.selectRegion(key) },
                                color = if (key == selectedRegion) Green else Color(0xFFF0F0F0),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(name,
                                    color = if (key == selectedRegion) Color.White else TextSecondary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp))
                            }
                        }
                    }

                    // 图表
                    PriceChart(
                        history = priceData?.history ?: emptyList(),
                        regionKey = selectedRegion,
                        modifier = Modifier.fillMaxWidth().height(160.dp)
                    )

                    // 统计行
                    val key = selectedRegion + "_avg"
                    val history = priceData?.history ?: emptyList()
                    if (history.isNotEmpty()) {
                        val vals = history.mapNotNull { h ->
                            when (selectedRegion) {
                                "jinxiang" -> h.jinxiangAvg
                                "qixian" -> h.qixianAvg
                                "pizhou" -> h.pizhouAvg
                                "zhongmou" -> h.zhongmouAvg
                                else -> null
                            }
                        }
                        if (vals.isNotEmpty()) {
                            val min30 = vals.takeLast(30).min()
                            val max30 = vals.takeLast(30).max()
                            val change = if (vals.size >= 2) (vals.last() - vals.first()) / vals.first() * 100 else 0.0
                            Row(Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("30日最低: ¥${"%.2f".format(min30)}", fontSize = 10.sp, color = TextMuted)
                                Text("30日最高: ¥${"%.2f".format(max30)}", fontSize = 10.sp, color = TextMuted)
                                Text("涨跌: ${if (change >= 0) "+" else ""}${"%.1f".format(change)}%",
                                    fontSize = 10.sp,
                                    color = if (change >= 0) Red else Green)
                            }
                        }
                    }
                }
            }
        }

        // 产区报价
        item {
            val region = priceData?.regions?.get(selectedRegion)
            if (region != null) {
                RegionPriceCard(region, selectedRegion)
            }
        }

        // 影响因素
        item {
            val factors = priceData?.factors
            if (factors != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    color = Surface, shape = RoundedCornerShape(10.dp), shadowElevation = 1.dp
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("🌦️ 影响价格的关键因素", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                        factors.forEach { (key, f) ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${factorIcon(key)} ${f.label}", fontSize = 12.sp, color = TextSecondary)
                                Text(f.value, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    color = when(f.level) {
                                        "high" -> Red; "positive" -> Green
                                        "down" -> TextMuted; else -> TextSecondary
                                    })
                            }
                            if (key != factors.keys.last()) {
                                HorizontalDivider(color = Divider, thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PriceChart(history: List<HistoryEntry>, regionKey: String, modifier: Modifier) {
    val values = history.mapNotNull { h ->
        when (regionKey) {
            "jinxiang" -> h.jinxiangAvg; "qixian" -> h.qixianAvg
            "pizhou" -> h.pizhouAvg; "zhongmou" -> h.zhongmouAvg
            else -> null
        }
    }
    if (values.isEmpty()) return

    val minVal = values.min() - 0.05
    val maxVal = values.max() + 0.05
    val range = maxVal - minVal

    val lineColor = Color(0xFF2E7D32)
    val dotColor = Color(0xFFD32F2F)

    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val padLeft = 40f; val padBottom = 20f; val padTop = 8f; val padRight = 8f
        val chartW = w - padLeft - padRight
        val chartH = h - padTop - padBottom

        // 网格线
        for (i in 0..3) {
            val y = padTop + chartH * i / 3
            drawLine(Color(0xFFEEEEEE), Offset(padLeft, y), Offset(w - padRight, y), 1f)
            val label = "%.2f".format(maxVal - range * i / 3)
            drawContext.canvas.nativeCanvas.drawText(
                label, 4f, y + 4f,
                android.graphics.Paint().apply { textSize = 22f; color = 0xFF999999.toInt() })
        }

        // 数据线
        if (values.size >= 2) {
            val path = Path()
            values.forEachIndexed { i, v ->
                val x = padLeft + chartW * i / (values.size - 1)
                val y = padTop + chartH * (1 - (v - minVal) / range).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            // 渐变填充
            val fillPath = Path().apply { addPath(path) }
            val lastX = padLeft + chartW
            fillPath.lineTo(lastX, padTop + chartH)
            fillPath.lineTo(padLeft, padTop + chartH)
            fillPath.close()
            drawPath(fillPath, Color(0x402E7D32))

            drawPath(path, lineColor, style = Stroke(width = 3f, cap = StrokeCap.Round))

            // 最新数据点
            val lastIdx = values.size - 1
            val lx = padLeft + chartW * lastIdx / (values.size - 1)
            val ly = padTop + chartH * (1 - (values[lastIdx] - minVal) / range).toFloat()
            drawCircle(Color.White, 7f, Offset(lx, ly))
            drawCircle(dotColor, 5f, Offset(lx, ly))
        }

        // X轴标签
        listOf(0, values.size / 3, values.size * 2 / 3, values.size - 1).forEach { i ->
            if (i < history.size && i < values.size) {
                val x = padLeft + chartW * i / (values.size - 1).coerceAtLeast(1)
                val label = history[i].date.takeLast(5)
                drawContext.canvas.nativeCanvas.drawText(
                    label, x - 14f, h - 2f,
                    android.graphics.Paint().apply { textSize = 20f; color = 0xFF999999.toInt() })
            }
        }
    }
}

@Composable
fun RegionPriceCard(region: RegionData, regionKey: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp),
        color = Surface, shape = RoundedCornerShape(10.dp), shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row {
                Text("📍 ${region.name}产区", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.width(6.dp))
                Text(region.marketMood, fontSize = 10.sp,
                    color = if (region.marketMood.contains("旺") || region.marketMood.contains("强")) Red
                    else if (region.marketMood.contains("弱")) Green else TextSecondary)
            }
            Spacer(Modifier.height(8.dp))
            // 规格网格 3列
            val specs = region.specs
            val rows = (specs.size + 2) / 3
            for (r in 0 until rows) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (c in 0..2) {
                        val idx = r * 3 + c
                        if (idx < specs.size) {
                            val spec = specs[idx]
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = Color(0xFFF8F9FA),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                                ) {
                                    Text(spec.name, fontSize = 9.sp, color = TextMuted)
                                    Text(
                                        if (spec.low == spec.high) "%.2f".format(spec.low)
                                        else "%.2f-%.2f".format(spec.low, spec.high),
                                        fontWeight = FontWeight.Bold, fontSize = 12.sp,
                                        color = when(spec.trend) {
                                            "up" -> Red; "down" -> Green; else -> TextPrimary
                                        }
                                    )
                                    Text(trendArrow(spec.trend), fontSize = 9.sp,
                                        color = when(spec.trend) {
                                            "up" -> Red; "down" -> Green; else -> TextMuted
                                        })
                                }
                            }
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
                if (r < rows - 1) Spacer(Modifier.height(6.dp))
            }
        }
    }
}

private fun trendArrow(trend: String) = when(trend) { "up" -> "↑" "down" -> "↓" else -> "→" }

private fun factorIcon(key: String) = when(key) {
    "national_inventory" -> "📦"; "planting_area" -> "🌱"
    "export_forecast" -> "🚢"; "weather_risk" -> "🌡️"; "seed_cost" -> "💰"
    else -> "📌"
}
