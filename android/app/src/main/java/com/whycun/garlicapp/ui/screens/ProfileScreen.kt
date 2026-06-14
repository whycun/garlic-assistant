package com.whycun.garlicapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.whycun.garlicapp.ui.theme.*
import com.whycun.garlicapp.viewmodel.MainViewModel
import com.whycun.garlicapp.data.local.entity.Batch

@Composable
fun ProfileScreen(vm: MainViewModel = viewModel()) {
    val holding by vm.holdingBatches.collectAsState()
    val all by vm.allBatches.collectAsState()

    var showProfileDialog by remember { mutableStateOf(false) }
    var showRegionDialog by remember { mutableStateOf(false) }
    var showAlertDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    var nickname by remember { mutableStateOf("金乡老张") }
    var location by remember { mutableStateOf("山东金乡") }
    var storageArea by remember { mutableStateOf("冷库A区") }
    var defaultRegion by remember { mutableStateOf("金乡") }
    var alertThreshold by remember { mutableStateOf("5") }

    val totalTons = holding.sumOf { it.quantityTons }
    val thisMonth = all.filter { b ->
        try { b.purchaseDate >= java.time.LocalDate.now().withDayOfMonth(1).toString() }
        catch (_: Exception) { false }
    }
    val monthBuy = thisMonth.filter { it.status == "holding" }.sumOf { it.quantityTons }
    val monthSold = thisMonth.filter { it.status == "sold" }.sumOf { it.quantityTons }
    val monthPnL = thisMonth.filter { it.status == "sold" }.sumOf {
        ((it.soldPricePerJin ?: 0.0) - it.purchasePricePerJin) * it.quantityTons * 2000
    }

    LazyColumn(modifier = Modifier.fillMaxSize().background(Background), contentPadding = PaddingValues(bottom = 16.dp)) {
        // 个人信息卡片
        item {
            Surface(modifier = Modifier.fillMaxWidth().padding(10.dp), color = Surface, shape = RoundedCornerShape(10.dp), shadowElevation = 1.dp) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(modifier = Modifier.size(64.dp), color = Green, shape = CircleShape) {
                        Box(contentAlignment = Alignment.Center) { Text("🧄", fontSize = 32.sp) }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(nickname, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                        modifier = Modifier.clickable { showProfileDialog = true })
                    Text("$storageArea · $location", fontSize = 11.sp, color = TextMuted,
                        modifier = Modifier.clickable { showProfileDialog = true })
                    Text("点击修改", fontSize = 10.sp, color = Green)

                    Spacer(Modifier.height(12.dp))
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
            Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp),
                color = Surface, shape = RoundedCornerShape(10.dp), shadowElevation = 1.dp) {
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
            Surface(modifier = Modifier.fillMaxWidth().padding(10.dp), color = Surface, shape = RoundedCornerShape(10.dp), shadowElevation = 1.dp) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("⚙️ 设置", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    SettingRow("👤 编辑个人信息", "$nickname · $location") { showProfileDialog = true }
                    SettingRow("📊 默认产区", defaultRegion) { showRegionDialog = true }
                    SettingRow("🔔 价格预警阈值", "涨跌 ${alertThreshold}% 提醒") { showAlertDialog = true }
                    SettingRow("ℹ️ 关于蒜来宝", "v1.0") { showAboutDialog = true }
                }
            }
        }
    }

    // ===== 弹窗 =====

    if (showProfileDialog) {
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text("编辑个人信息", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(nickname, { nickname = it }, label = { Text("昵称") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(location, { location = it }, label = { Text("所在地") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(storageArea, { storageArea = it }, label = { Text("冷库位置") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = { showProfileDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showProfileDialog = false }) { Text("取消") } }
        )
    }

    if (showRegionDialog) {
        AlertDialog(
            onDismissRequest = { showRegionDialog = false },
            title = { Text("选择默认产区") },
            text = {
                Column {
                    listOf("金乡", "杞县", "邳州", "中牟").forEach { r ->
                        Row(Modifier.fillMaxWidth().clickable { defaultRegion = r; showRegionDialog = false }.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(r, fontSize = 14.sp, fontWeight = if(r == defaultRegion) FontWeight.Bold else FontWeight.Normal)
                            if (r == defaultRegion) Text("✓", color = Green, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(color = Divider)
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showRegionDialog = false }) { Text("关闭") } }
        )
    }

    if (showAlertDialog) {
        var input by remember { mutableStateOf(alertThreshold) }
        AlertDialog(
            onDismissRequest = { showAlertDialog = false },
            title = { Text("价格预警阈值") },
            text = {
                Column {
                    Text("当价格涨跌超过设定百分比时提醒", fontSize = 12.sp, color = TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(input, { input = it }, label = { Text("涨跌幅度 (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = { alertThreshold = input; showAlertDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showAlertDialog = false }) { Text("取消") } }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("🧄 蒜来宝 v1.0") },
            text = {
                Column {
                    Text("大蒜经营助手 - 个人版", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("行情监控 · 库存管理 · 利润计算\n\n数据来源：国际大蒜贸易网、Mysteel\n卓创资讯、中国农业信息网\n\n© 2026 个人使用", fontSize = 12.sp, color = TextSecondary, lineHeight = 20.sp)
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showAboutDialog = false }) { Text("关闭") } }
        )
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
fun SettingRow(label: String, value: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, fontSize = 12.sp, color = TextMuted)
            Text(" ›", fontSize = 16.sp, color = Color(0xFFCCCCCC))
        }
    }
    HorizontalDivider(color = Divider, thickness = 0.5.dp)
}

private fun fmtPnL(v: Double): String {
    val abs = kotlin.math.abs(v)
    return if (abs >= 10000) "${if(v>=0) "+" else "-"}${"%.2f".format(abs/10000)}万"
    else "${if(v>=0) "+" else "-"}${abs.toInt()}"
}

private fun exportCsv(batches: List<Batch>, nickname: String) {
    // CSV export logic placeholder - would use Activity context
}
