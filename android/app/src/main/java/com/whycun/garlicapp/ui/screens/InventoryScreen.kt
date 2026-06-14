package com.whycun.garlicapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.whycun.garlicapp.data.local.entity.Batch
import com.whycun.garlicapp.data.local.entity.CalcSave
import com.whycun.garlicapp.ui.theme.*
import com.whycun.garlicapp.viewmodel.MainViewModel
import java.time.Instant

@Composable
fun InventoryScreen(vm: MainViewModel = viewModel()) {
    var activeTab by remember { mutableStateOf(0) } // 0=库存, 1=计算器
    val holdingBatches by vm.holdingBatches.collectAsState()
    val allBatches by vm.allBatches.collectAsState()
    val priceData by vm.priceData.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        // Tab切换
        Surface(color = Surface, shadowElevation = 1.dp) {
            Row(Modifier.fillMaxWidth()) {
                TabBtn("📋 库存清单", 0, activeTab) { activeTab = it }
                TabBtn("🧮 利润计算", 1, activeTab) { activeTab = it }
            }
        }

        if (activeTab == 0) {
            BatchListContent(vm, holdingBatches, allBatches, priceData?.regions)
        } else {
            CalculatorContent(vm)
        }
    }
}

@Composable
private fun TabBtn(text: String, idx: Int, active: Int, onClick: (Int) -> Unit) {
    TextButton(
        onClick = { onClick(idx) },
        modifier = Modifier.weight(1f),
        colors = ButtonDefaults.textButtonColors(
            contentColor = if (idx == active) Green else TextMuted
        )
    ) {
        Text(text, fontWeight = if (idx == active) FontWeight.Bold else FontWeight.Normal,
            fontSize = 12.sp)
    }
    if (idx == active) {
        HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp),
            thickness = 2.dp, color = Green)
    }
}

@Composable
fun BatchListContent(
    vm: MainViewModel,
    holding: List<Batch>,
    all: List<Batch>,
    regions: Map<String, com.whycun.garlicapp.data.remote.RegionData>?
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showSellDialog by remember { mutableStateOf<String?>(null) }

    val totalTons = holding.sumOf { it.quantityTons }
    val totalCost = holding.sumOf { it.quantityTons * it.purchasePricePerJin * 2000 }
    val totalValue = holding.sumOf { b ->
        val mkt = regions?.get(b.region)?.specs?.firstOrNull()?.low ?: b.purchasePricePerJin
        b.quantityTons * mkt * 2000
    }
    val floatingPnL = totalValue - totalCost

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp)) {
        // 汇总
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryCell("总库存", "${totalTons} 吨", CardBlue, Color(0xFF1565C0), Modifier.weight(1f))
                SummaryCell("库存估值", fmtMoney(totalValue), CardOrange, Orange, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryCell("持仓成本", fmtMoney(totalCost), CardRed, Red, Modifier.weight(1f))
                SummaryCell("浮动盈亏", fmtMoney(floatingPnL), CardGreen,
                    if (floatingPnL >= 0) Red else Green, Modifier.weight(1f))
            }
        }

        // 持仓批次
        item {
            Spacer(Modifier.height(12.dp))
            Text("📦 持仓批次 (${holding.size})", fontWeight = FontWeight.Bold,
                fontSize = 12.sp, color = TextSecondary)
        }

        items(holding) { batch ->
            val mkt = regions?.get(batch.region)?.specs?.firstOrNull()?.low ?: batch.purchasePricePerJin
            val pnl = batch.quantityTons * (mkt - batch.purchasePricePerJin) * 2000
            BatchCard(batch, mkt, pnl,
                onSell = { showSellDialog = batch.id },
                onDelete = { vm.deleteBatch(batch.id) })
        }

        if (holding.isEmpty()) {
            item { Text("暂无持仓批次", fontSize = 12.sp, color = TextMuted,
                modifier = Modifier.padding(vertical = 20.dp).fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
        }

        // 已结算
        val sold = all.filter { it.status == "sold" }
        if (sold.isNotEmpty()) {
            item {
                Spacer(Modifier.height(12.dp))
                Text("📋 已结算批次 (${sold.size})", fontWeight = FontWeight.Bold,
                    fontSize = 12.sp, color = TextSecondary)
            }
            items(sold) { batch ->
                val pnl = (batch.soldPricePerJin!! - batch.purchasePricePerJin) * batch.quantityTons * 2000
                BatchCard(batch, batch.soldPricePerJin!!, pnl, onSell = {}, onDelete = {})
            }
        }

        // 新增按钮
        item {
            Spacer(Modifier.height(8.dp))
            Button(onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Green),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)) {
                Text("+ 新增入库批次", fontSize = 14.sp)
            }
        }
    }

    // 新增弹窗
    if (showAddDialog) {
        AddBatchDialog(onDismiss = { showAddDialog = false }) { batch ->
            vm.saveBatch(batch)
            showAddDialog = false
        }
    }

    // 出库弹窗
    showSellDialog?.let { id ->
        SellDialog(id, onDismiss = { showSellDialog = null }) { price, date ->
            vm.markAsSold(id, price, date)
            showSellDialog = null
        }
    }
}

@Composable
fun SummaryCell(label: String, value: String, bg: androidx.compose.ui.graphics.Color, vColor: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Surface(modifier = modifier, color = bg, shape = RoundedCornerShape(8.dp)) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 10.sp, color = TextMuted)
            Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = vColor)
        }
    }
}

@Composable
fun BatchCard(batch: Batch, marketPrice: Double, pnl: Double,
              onSell: () -> Unit, onDelete: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        color = Color(0xFFF8F9FA), shape = RoundedCornerShape(8.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row {
                    Text("批次 ${batch.batchNo}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(" ${batch.regionName}·${batch.spec}", fontSize = 10.sp, color = TextMuted)
                }
                Surface(color = if (batch.status == "holding") CardGreen else Color(0xFFEEEEEE),
                    shape = RoundedCornerShape(4.dp)) {
                    Text(if (batch.status == "holding") "持仓中" else "已出库",
                        fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        color = if (batch.status == "holding") Green else TextMuted,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${batch.quantityTons}吨 | 成本¥${"%.2f".format(batch.purchasePricePerJin)}/斤 | ${if (batch.status=="holding") "入库" else "出库"}${batch.purchaseDate}",
                    fontSize = 11.sp, color = TextSecondary)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (batch.status == "holding") "当前市价 ¥${"%.2f".format(marketPrice)}"
                else "卖出 ¥${"%.2f".format(batch.soldPricePerJin!!)}/斤",
                    fontSize = 11.sp, color = TextMuted)
                Text(if (pnl >= 0) "+${fmtMoney(pnl)}" else fmtMoney(pnl),
                    fontWeight = FontWeight.Bold, fontSize = 12.sp,
                    color = if (pnl >= 0) Red else Green)
            }
            if (batch.status == "holding") {
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onSell,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Green),
                        shape = RoundedCornerShape(6.dp)) { Text("标记出库", fontSize = 11.sp) }
                    OutlinedButton(onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Red),
                        shape = RoundedCornerShape(6.dp)) { Text("删除", fontSize = 11.sp) }
                }
            }
        }
    }
}

@Composable
fun AddBatchDialog(onDismiss: () -> Unit, onSave: (Batch) -> Unit) {
    var batchNo by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("jinxiang") }
    var spec by remember { mutableStateOf("") }
    var tons by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(java.time.LocalDate.now().toString()) }
    var location by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增入库批次", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn {
                item { OutlinedTextField(batchNo, { batchNo = it }, label = { Text("批次编号") }, modifier = Modifier.fillMaxWidth()) }
                item { Spacer(Modifier.height(8.dp)) }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(region, { region = it }, label = { Text("产区") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(spec, { spec = it }, label = { Text("规格") }, modifier = Modifier.weight(1f))
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(tons, { tons = it }, label = { Text("数量(吨)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                        OutlinedTextField(price, { price = it }, label = { Text("收购价(元/斤)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
                item { OutlinedTextField(date, { date = it }, label = { Text("入库日期") }, modifier = Modifier.fillMaxWidth()) }
                item { Spacer(Modifier.height(8.dp)) }
                item { OutlinedTextField(location, { location = it }, label = { Text("存放位置") }, modifier = Modifier.fillMaxWidth()) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val b = Batch(
                    id = "batch_${Instant.now().toEpochMilli()}",
                    batchNo = batchNo, region = region,
                    regionName = REGIONS[region] ?: region, spec = spec,
                    quantityTons = tons.toDoubleOrNull() ?: 0.0,
                    purchasePricePerJin = price.toDoubleOrNull() ?: 0.0,
                    purchaseDate = date, storageLocation = location,
                    status = "holding", createdAt = Instant.now().toString(),
                    updatedAt = Instant.now().toString()
                )
                onSave(b)
            }, colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun SellDialog(id: String, onDismiss: () -> Unit, onConfirm: (Double, String) -> Unit) {
    var price by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("标记出库") },
        text = {
            OutlinedTextField(price, { price = it }, label = { Text("卖出价格(元/斤)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        },
        confirmButton = {
            Button(onClick = {
                val p = price.toDoubleOrNull() ?: return@Button
                onConfirm(p, java.time.LocalDate.now().toString())
            }, colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("确认") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

// ===== 计算器 =====

@Composable
fun CalculatorContent(vm: MainViewModel = viewModel()) {
    val priceData by vm.priceData.collectAsState()
    val defaultPrice = priceData?.regions?.get("jinxiang")?.specs
        ?.find { it.name == "一般混级" }?.let { (it.low + it.high) / 2 } ?: 2.85

    var marketPrice by remember { mutableStateOf(defaultPrice.toString()) }
    var purchasePrice by remember { mutableStateOf("2.10") }
    var quantity by remember { mutableStateOf("50") }
    var storageFee by remember { mutableStateOf("18000") }
    var laborFee by remember { mutableStateOf("12000") }
    var lossFee by remember { mutableStateOf("3500") }
    var transportFee by remember { mutableStateOf("5000") }
    var otherFee by remember { mutableStateOf("1500") }

    val mp = marketPrice.toDoubleOrNull() ?: 0.0
    val pp = purchasePrice.toDoubleOrNull() ?: 0.0
    val q = quantity.toDoubleOrNull() ?: 0.0
    val sf = storageFee.toDoubleOrNull() ?: 0.0
    val lf = laborFee.toDoubleOrNull() ?: 0.0
    val losf = lossFee.toDoubleOrNull() ?: 0.0
    val tf = transportFee.toDoubleOrNull() ?: 0.0
    val of = otherFee.toDoubleOrNull() ?: 0.0

    val purchaseTotal = pp * q * 2000
    val extraTotal = sf + lf + losf + tf + of
    val totalCost = purchaseTotal + extraTotal
    val unitCost = if (q > 0) totalCost / (q * 2000) else 0.0
    val revenue = mp * q * 2000
    val profit = revenue - totalCost
    val profitRate = if (totalCost > 0) profit / totalCost * 100 else 0.0

    val calcSaves by vm.calcSaves.collectAsState()
    var showHistory by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp)) {
        item { Text("自动关联行情数据 · 修改数字实时计算", fontSize = 11.sp, color = TextMuted) }
        item { Spacer(Modifier.height(8.dp)) }
        item { CalcInput("📊 当前市价 (元/斤)", marketPrice, { marketPrice = it }, KeyboardType.Decimal, true) }
        item { CalcInput("🧄 收购均价 (元/斤)", purchasePrice, { purchasePrice = it }, KeyboardType.Decimal) }
        item { CalcInput("⚖️ 购入数量 (吨)", quantity, { quantity = it }, KeyboardType.Decimal) }
        item { CalcInput("❄️ 冷库费用 (元)", storageFee, { storageFee = it }, KeyboardType.Number) }
        item { CalcInput("👷 人工费用 (元)", laborFee, { laborFee = it }, KeyboardType.Number) }
        item { CalcInput("🗑️ 损耗费用 (元)", lossFee, { lossFee = it }, KeyboardType.Number) }
        item { CalcInput("🚛 运输费用 (元)", transportFee, { transportFee = it }, KeyboardType.Number) }
        item { CalcInput("📋 其他支出 (元)", otherFee, { otherFee = it }, KeyboardType.Number) }

        // 结果
        item {
            Surface(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                color = CardGreen, shape = RoundedCornerShape(10.dp)) {
                Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("成本概算", fontSize = 11.sp, color = TextSecondary)
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("总成本", fontSize = 10.sp, color = TextMuted)
                            Text(fmtMoney(totalCost), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("合${"%.2f".format(unitCost)}/斤", fontSize = 10.sp, color = TextMuted)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("按市价出货", fontSize = 10.sp, color = TextMuted)
                            Text(fmtMoney(revenue), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("${"%.2f".format(mp)}/斤", fontSize = 10.sp, color = TextMuted)
                        }
                    }
                    HorizontalDivider(color = Color(0xFFCCCCCC))
                    Spacer(Modifier.height(6.dp))
                    Text("毛利：${fmtMoney(profit)}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Red)
                    Text("利润率 ${"%.1f".format(profitRate)}%", fontSize = 13.sp, color = Red)
                }
            }
        }

        // 按钮
        item {
            Button(onClick = {
                vm.saveCalc(CalcSave(
                    id = "calc_${Instant.now().toEpochMilli()}",
                    marketPrice = mp, purchasePrice = pp, quantityTons = q,
                    coldStorageFee = sf, laborFee = lf, lossFee = losf,
                    transportFee = tf, otherFee = of,
                    createdAt = Instant.now().toString()
                ))
            }, colors = ButtonDefaults.buttonColors(containerColor = Green),
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                Text("💾 保存这条核算")
            }
        }
        item {
            OutlinedButton(onClick = { showHistory = !showHistory },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                Text(if (showHistory) "收起历史" else "📋 查看历史记录")
            }
        }

        // 历史
        if (showHistory) {
            items(calcSaves.take(10)) { calc ->
                val tc = calc.purchasePrice * calc.quantityTons * 2000 +
                        calc.coldStorageFee + calc.laborFee + calc.lossFee + calc.transportFee + calc.otherFee
                val rv = calc.marketPrice * calc.quantityTons * 2000
                val pf = rv - tc
                Surface(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    color = Color(0xFFF8F9FA), shape = RoundedCornerShape(8.dp)) {
                    Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("${calc.quantityTons}吨 · 成本${"%.2f".format(calc.purchasePrice)} · 市价${"%.2f".format(calc.marketPrice)}",
                                fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(calc.createdAt.take(10), fontSize = 10.sp, color = TextMuted)
                        }
                        Text(fmtMoney(pf), fontWeight = FontWeight.Bold,
                            color = if (pf >= 0) Red else Green)
                    }
                }
            }
        }
    }
}

@Composable
fun CalcInput(label: String, value: String, onValue: (String) -> Unit, kbType: KeyboardType, isMarket: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 13.sp, color = TextSecondary)
        OutlinedTextField(value, onValue,
            modifier = Modifier.width(140.dp),
            keyboardOptions = KeyboardOptions(keyboardType = kbType),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = if (isMarket) Green else TextPrimary,
                fontWeight = if (isMarket) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            ),
            shape = RoundedCornerShape(6.dp)
        )
    }
}

private fun fmtMoney(v: Double): String {
    val abs = kotlin.math.abs(v)
    return if (abs >= 10000) "${if (v>=0) "¥" else "-¥"}${"%.2f".format(abs/10000)}万"
    else "${if (v>=0) "¥" else "-¥"}${abs.toInt()}"
}
