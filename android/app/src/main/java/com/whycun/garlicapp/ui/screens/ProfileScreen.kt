package com.whycun.garlicapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.whycun.garlicapp.ui.theme.Green
import com.whycun.garlicapp.ui.theme.Red
import com.whycun.garlicapp.ui.theme.*
import com.whycun.garlicapp.viewmodel.MainViewModel

@Composable
fun ProfileScreen(vm: MainViewModel = viewModel()) {
    val holding by vm.holdingBatches.collectAsState()
    val all by vm.allBatches.collectAsState()

    val totalTons = holding.sumOf { it.quantityTons }
    val thisMonth = all.filter {
        try { it.purchaseDate >= java.time.LocalDate.now().withDayOfMonth(1).toString() } catch (e: Exception) { false }
    }
    val monthBuy = thisMonth.filter { it.status == "holding" }.sumOf { it.quantityTons }
    val monthSold = thisMonth.filter { it.status == "sold" }.sumOf { it.quantityTons }
    val monthPnL = thisMonth.filter { it.status == "sold" }.sumOf {
        ((it.soldPricePerJin ?: 0.0) - it.purchasePricePerJin) * it.quantityTons * 2000
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Background),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // 个人信息
        item {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                color = Surface, shape = RoundedCornerShape(10.dp), shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(modifier = Modifier.size(60.dp), color = Green, shape = CircleShape) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🧄", fontSize = 28.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("金乡老张", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("冷库A区 · 山东金乡", fontSize = 11.sp, color = TextMuted)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatItem("${holding.size}", "持仓批次")
                        StatItem("$totalTons", "库存(吨)")
                        StatItem("${all.size}", "总批次")
                    }
                }
            }
        }

        // 本月概览
        item {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp),
                color = Surface, shape = RoundedCornerShape(10.dp), shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("💰 本月经营概览", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SummaryCell("本月收购", "${monthBuy}吨", CardBlue, Color(0xFF1565C0), Modifier.weight(1f))
                        SummaryCell("本月出货", "${monthSold}吨", CardOrange, Orange, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SummaryCell("持仓库存", "${totalTons}吨", CardRed, Red, Modifier.weight(1f))
                        SummaryCell("本月盈亏", fmtPnL(monthPnL), CardGreen,
                            if (monthPnL >= 0) Red else Green, Modifier.weight(1f))
                    }
                }
            }
        }

        // 设置
        item {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                color = Surface, shape = RoundedCornerShape(10.dp), shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("⚙️ 设置", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    SettingRow("📊 默认产区", "金乡")
                    SettingRow("🔔 价格预警阈值", "涨跌5%提醒")
                    SettingRow("💾 数据备份", "点击导出")
                    SettingRow("📋 关于蒜来宝", "v1.0")
                }
            }
        }
    }
}

@Composable
fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
        Text(label, fontSize = 10.sp, color = TextMuted)
    }
}

@Composable
fun SettingRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().clickable { }.padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp)
        Text(value, fontSize = 13.sp, color = TextMuted)
    }
    HorizontalDivider(color = Divider, thickness = 0.5.dp)
}

private fun fmtPnL(v: Double): String {
    val abs = kotlin.math.abs(v)
    return if (abs >= 10000) "${if(v>=0) "+" else "-"}${"%.2f".format(abs/10000)}万"
    else "${if(v>=0) "+" else "-"}${abs.toInt()}"
}
