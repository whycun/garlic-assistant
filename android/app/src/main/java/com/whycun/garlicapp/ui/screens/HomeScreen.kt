package com.whycun.garlicapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.whycun.garlicapp.data.remote.NewsItem
import com.whycun.garlicapp.ui.theme.*
import com.whycun.garlicapp.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: MainViewModel = viewModel()) {
    val newsItems by vm.newsData.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val pullState = rememberPullToRefreshState()
    var refreshMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    if (pullState.isRefreshing) {
        LaunchedEffect(true) {
            try {
                vm.refreshData()
                refreshMsg = "✅ 刷新成功"
            } catch (_: Exception) {
                refreshMsg = "❌ 刷新失败"
            }
            pullState.endRefresh()
            delay(2000)
            refreshMsg = null
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Background).nestedScroll(pullState.nestedScrollConnection)) {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 8.dp)) {
            val warnings = newsItems.filter { it.tagType == "warning" }
            if (warnings.isNotEmpty()) {
                item {
                    Surface(modifier = Modifier.fillMaxWidth().padding(10.dp, 10.dp, 10.dp, 0.dp), color = CardOrange, shape = RoundedCornerShape(8.dp)) {
                        Row(modifier = Modifier.padding(12.dp)) {
                            Text("⚠️", fontSize = 14.sp); Spacer(Modifier.width(8.dp))
                            Text(warnings.first().title, fontSize = 12.sp, color = Orange, lineHeight = 18.sp)
                        }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth().padding(12.dp, 12.dp, 12.dp, 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("📰 行情资讯", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            if (isLoading && newsItems.isEmpty()) {
                items(3) {
                    Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp), color = Surface, shape = RoundedCornerShape(10.dp)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Box(Modifier.height(14.dp).fillMaxWidth(0.7f).clip(RoundedCornerShape(4.dp)).background(Color(0xFFEEEEEE)))
                            Spacer(Modifier.height(6.dp))
                            Box(Modifier.height(10.dp).fillMaxWidth(0.4f).clip(RoundedCornerShape(4.dp)).background(Color(0xFFF5F5F5)))
                        }
                    }
                }
            }

            items(newsItems) { item -> NewsCard(item) }

            if (newsItems.isEmpty() && !isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("暂无资讯", color = TextMuted, fontSize = 13.sp)
                    }
                }
            }
        }

        PullToRefreshContainer(state = pullState, modifier = Modifier.align(Alignment.TopCenter))

        // 刷新结果提示
        refreshMsg?.let { msg ->
            Snackbar(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                containerColor = if (msg.contains("✅")) Green else Red) {
                Text(msg, color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun NewsCard(item: NewsItem) {
    val tagColors = mapOf(
        "warning" to Pair(Orange, CardOrange), "analysis" to Pair(Color(0xFF1565C0), CardBlue),
        "policy" to Pair(Green, CardGreen), "dynamics" to Pair(Color(0xFF7B1FA2), Color(0xFFF3E5F5)),
        "deep" to Pair(Color(0xFF00695C), Color(0xFFE0F2F1)), "cost" to Pair(Color(0xFFF57F17), Color(0xFFFFF8E1)),
    )
    val (tagColor, tagBg) = tagColors[item.tagType] ?: Pair(TextSecondary, Divider)

    Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp), color = Surface, shape = RoundedCornerShape(10.dp), shadowElevation = 1.dp) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.tag.isNotEmpty()) {
                    Surface(color = tagBg, shape = RoundedCornerShape(3.dp)) {
                        Text(item.tag, fontSize = 9.sp, color = tagColor, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                }
                Text(item.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 19.sp, color = TextPrimary)
            }
            Spacer(Modifier.height(4.dp))
            Row { Text("${item.sourceName} · ", fontSize = 10.sp, color = TextMuted); Text(formatTime(item.publishedAt), fontSize = 10.sp, color = TextMuted) }
        }
    }
}

private fun formatTime(iso: String): String {
    if (iso.length < 10) return iso
    return try { iso.substring(5, 10).replace("-", "/") + " " + iso.substring(11, 16) } catch (e: Exception) { iso }
}
