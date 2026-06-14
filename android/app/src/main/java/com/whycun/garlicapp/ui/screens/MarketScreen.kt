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
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
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

val REGIONS = mapOf("jinxiang" to "金乡","qixian" to "杞县","pizhou" to "邳州","zhongmou" to "中牟")
val PERIODS = listOf("日线" to "daily", "月线" to "monthly")

@Composable
fun MarketScreen(vm: MainViewModel = viewModel()) {
    val priceData by vm.priceData.collectAsState()
    val selectedRegion by vm.selectedRegion.collectAsState()
    val isStale by vm.isStale.collectAsState()
    var selectedPeriod by remember { mutableStateOf("daily") }
    var selectedSpec by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LazyColumn(modifier = Modifier.fillMaxSize().background(Background), contentPadding = PaddingValues(bottom = 8.dp)) {
        // 走势图卡片
        item {
            Surface(modifier = Modifier.fillMaxWidth().padding(10.dp, 10.dp, 10.dp, 0.dp), color = Surface, shape = RoundedCornerShape(10.dp), shadowElevation = 1.dp) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("📈 ${REGIONS[selectedRegion]} · ${selectedSpec.ifEmpty { "均价" }}走势",
                            fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("🔄 刷新", fontSize = 10.sp, color = Green,
                            modifier = Modifier.clickable { scope.launch { vm.refreshData() } })
                    }

                    // 产区标签
                    Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        REGIONS.forEach { (key, name) ->
                            Surface(modifier = Modifier.clickable { vm.selectRegion(key); selectedSpec = "" },
                                color = if(key == selectedRegion) Green else Color(0xFFF0F0F0), shape = RoundedCornerShape(14.dp)) {
                                Text(name, color = if(key == selectedRegion) Color.White else TextSecondary,
                                    fontSize = 11.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp))
                            }
                        }
                    }

                    // 规格选择
                    val specs = priceData?.regions?.get(selectedRegion)?.specs ?: emptyList()
                    if (specs.isNotEmpty()) {
                        Row(Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(modifier = Modifier.clickable { selectedSpec = "" },
                                color = if(selectedSpec == "") CardBlue else Color(0xFFF0F0F0), shape = RoundedCornerShape(10.dp)) {
                                Text("均价", color = if(selectedSpec == "") Color(0xFF1565C0) else TextSecondary,
                                    fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                            specs.forEach { spec ->
                                Surface(modifier = Modifier.clickable { selectedSpec = spec.name },
                                    color = if(selectedSpec == spec.name) CardBlue else Color(0xFFF0F0F0), shape = RoundedCornerShape(10.dp)) {
                                    Text(spec.name, color = if(selectedSpec == spec.name) Color(0xFF1565C0) else TextSecondary,
                                        fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                }
                            }
                        }
                    }

                    // 时间周期切换
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.Center) {
                        PERIODS.forEach { (label, key) ->
                            TextButton(onClick = { selectedPeriod = key },
                                modifier = Modifier.padding(horizontal = 4.dp),
                                colors = ButtonDefaults.textButtonColors(contentColor = if(key == selectedPeriod) Green else TextMuted)) {
                                Text(label, fontWeight = if(key == selectedPeriod) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp)
                            }
                        }
                    }

                    // 图表数据（选规格则按比例推算）
                    val rawHistory = priceData?.history ?: emptyList()
                    val specMultiplier = if (selectedSpec.isNotEmpty()) {
                        val specs = priceData?.regions?.get(selectedRegion)?.specs ?: emptyList()
                        val spec = specs.find { it.name == selectedSpec }
                        val specPrice = if (spec != null) (spec.low + spec.high) / 2 else 0.0
                        val avgKey = selectedRegion + "_avg"
                        val latestAvg = rawHistory.lastOrNull()?.let { h ->
                            when(selectedRegion) { "jinxiang"->h.jinxiangAvg; "qixian"->h.qixianAvg; "pizhou"->h.pizhouAvg; "zhongmou"->h.zhongmouAvg; else->null }
                        } ?: specPrice
                        if (latestAvg > 0 && specPrice > 0) specPrice / latestAvg else 1.0
                    } else 1.0

                    val chartData = getPeriodData(rawHistory, selectedPeriod, selectedRegion).map { (date, avg) ->
                        date to avg * specMultiplier
                    }
                    PriceChart(history = chartData,
                        modifier = Modifier.fillMaxWidth().height(180.dp), lineColor = Color(0xFF2E7D32))

                    // 统计
                    val hist = priceData?.history ?: emptyList()
                    if (hist.isNotEmpty()) {
                        val avgKey = selectedRegion + "_avg"
                        val vals = hist.mapNotNull { h ->
                            when(selectedRegion) {
                                "jinxiang" -> h.jinxiangAvg; "qixian" -> h.qixianAvg
                                "pizhou" -> h.pizhouAvg; "zhongmou" -> h.zhongmouAvg
                                else -> null
                            }
                        }
                        if (vals.isNotEmpty()) {
                            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${vals.size}日最低: ¥${"%.2f".format(vals.min())}", fontSize = 10.sp, color = TextMuted)
                                Text("最高: ¥${"%.2f".format(vals.max())}", fontSize = 10.sp, color = TextMuted)
                                val chg = if(vals.size>=2) (vals.last()-vals.first())/vals.first()*100 else 0.0
                                Text("涨跌: ${if(chg>=0)"+" else ""}${"%.1f".format(chg)}%", fontSize = 10.sp,
                                    color = if(chg>=0) Red else Green)
                            }
                        }
                    }
                }
            }
        }

        // 产区报价详情
        val region = priceData?.regions?.get(selectedRegion)
        if (region != null) {
            item { RegionPriceCard(region, selectedRegion) }
        } else {
            item {
                Surface(modifier = Modifier.fillMaxWidth().padding(10.dp), color = Surface, shape = RoundedCornerShape(10.dp)) {
                    Box(Modifier.padding(30.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("暂无该产区数据", color = TextMuted, fontSize = 13.sp)
                    }
                }
            }
        }

        // 关键因素
        val factors = priceData?.factors
        if (factors != null && factors.isNotEmpty()) {
            item {
                Surface(modifier = Modifier.fillMaxWidth().padding(10.dp), color = Surface, shape = RoundedCornerShape(10.dp), shadowElevation = 1.dp) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("🌦️ 影响价格的关键因素", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                        val keys = factors.keys.toList()
                        factors.forEach { (key, f) ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${factorIcon(key)} ${f.label}", fontSize = 12.sp, color = TextSecondary)
                                Text(f.value, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    color = when(f.level) { "high" -> Red; "positive" -> Green; "down" -> TextMuted; else -> TextSecondary })
                            }
                            if (key != keys.last()) HorizontalDivider(color = Divider, thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }

    }
}

@Composable
fun PriceChart(history: List<Pair<String, Double>>, modifier: Modifier, lineColor: Color = Color(0xFF2E7D32)) {
    if (history.isEmpty()) return
    val vals = history.map { it.second }
    val minV = vals.min() - 0.05
    val maxV = vals.max() + 0.05
    val range = (maxV - minV).coerceAtLeast(0.1)

    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val pl = 44f; val pb = 22f; val pt = 10f; val pr = 12f
        val cw = w - pl - pr; val ch = h - pt - pb

        // 网格
        for (i in 0..3) {
            val y = pt + ch * i / 3
            drawLine(Color(0xFFEEEEEE), Offset(pl, y), Offset(w - pr, y), 1f)
            drawContext.canvas.nativeCanvas.drawText("%.2f".format(maxV - range * i / 3),
                4f, y + 5f, android.graphics.Paint().apply { textSize = 22f; color = 0xFF999999.toInt() })
        }

        if (vals.size >= 2) {
            val path = Path()
            vals.forEachIndexed { i, v ->
                val x = pl + cw * i / (vals.size - 1)
                val y = pt + ch * (1 - ((v - minV) / range)).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            val fp = Path().apply { addPath(path); lineTo(pl + cw, pt + ch); lineTo(pl, pt + ch); close() }
            drawPath(fp, lineColor.copy(alpha = 0.25f))
            drawPath(path, lineColor, style = Stroke(width = 3f, cap = StrokeCap.Round))

            val li = vals.size - 1
            val lx = pl + cw * li / (vals.size - 1)
            val ly = pt + ch * (1 - ((vals[li] - minV) / range)).toFloat()
            drawCircle(Color.White, 7f, Offset(lx, ly))
            drawCircle(Color(0xFFD32F2F), 5f, Offset(lx, ly))
            drawContext.canvas.nativeCanvas.drawText("%.2f".format(vals[li]), lx - 14f, ly - 10f,
                android.graphics.Paint().apply { textSize = 22f; color = 0xFFD32F2F.toInt(); isFakeBoldText = true })
        }

        // X轴标签
        val step = (history.size / 4).coerceAtLeast(1)
        history.forEachIndexed { i, (label, _) ->
            if (i % step == 0 || i == history.size - 1) {
                val x = pl + cw * i / (history.size - 1).coerceAtLeast(1)
                drawContext.canvas.nativeCanvas.drawText(label.takeLast(if(label.length>5) 5 else label.length),
                    x - 14f, h - 3f, android.graphics.Paint().apply { textSize = 20f; color = 0xFF999999.toInt() })
            }
        }
    }
}

fun getPeriodData(history: List<HistoryEntry>, period: String, region: String): List<Pair<String, Double>> {
    if (history.isEmpty()) return emptyList()
    val raw = history.mapNotNull { h ->
        val v = when(region) { "jinxiang"->h.jinxiangAvg; "qixian"->h.qixianAvg; "pizhou"->h.pizhouAvg; "zhongmou"->h.zhongmouAvg; else->null }
        v?.let { h.date to it }
    }
    return when(period) {
        "daily" -> raw
        "monthly" -> raw.groupBy { it.first.substring(0, 7) }.map { (k, vs) -> k to vs.map{it.second}.average() }.sortedBy { it.first }
        "yearly" -> raw.groupBy { it.first.substring(0, 4) }.map { (k, vs) -> k to vs.map{it.second}.average() }.sortedBy { it.first }
        else -> raw
    }
}

@Composable
fun RegionPriceCard(region: RegionData, regionKey: String) {
    Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp), color = Surface, shape = RoundedCornerShape(10.dp), shadowElevation = 1.dp) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row { Text("📍 ${region.name}产区", fontWeight = FontWeight.Bold, fontSize = 13.sp); Spacer(Modifier.width(6.dp))
                Text(region.marketMood, fontSize = 10.sp,
                    color = if(region.marketMood.contains("旺")||region.marketMood.contains("强")) Red else if(region.marketMood.contains("弱")) Green else TextSecondary) }
            Spacer(Modifier.height(8.dp))
            val specs = region.specs; val rows = (specs.size + 2) / 3
            for (r in 0 until rows) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (c in 0..2) {
                        val idx = r * 3 + c
                        if (idx < specs.size) {
                            val spec = specs[idx]
                            Surface(modifier = Modifier.weight(1f), color = Color(0xFFF8F9FA), shape = RoundedCornerShape(6.dp)) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(spec.name, fontSize = 9.sp, color = TextMuted)
                                    Text(if(spec.low==spec.high) "%.2f".format(spec.low) else "%.2f-%.2f".format(spec.low, spec.high),
                                        fontWeight = FontWeight.Bold, fontSize = 12.sp,
                                        color = when(spec.trend){"up"->Red;"down"->Green;else->TextPrimary})
                                    Text(trendArrow(spec.trend), fontSize = 9.sp,
                                        color = when(spec.trend){"up"->Red;"down"->Green;else->TextMuted})
                                }
                            }
                        } else { Spacer(Modifier.weight(1f)) }
                    }
                }
                if (r < rows - 1) Spacer(Modifier.height(6.dp))
            }
        }
    }
}

private fun trendArrow(trend: String) = when(trend) { "up" -> "↑"; "down" -> "↓"; else -> "→" }
private fun factorIcon(key: String) = when(key) {
    "national_inventory"->"📦"; "planting_area"->"🌱"; "export_forecast"->"🚢"; "weather_risk"->"🌡️"; "seed_cost"->"💰"; else->"📌"
}
