package com.whycun.garlicapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.whycun.garlicapp.ui.screens.*
import com.whycun.garlicapp.ui.theme.*
import com.whycun.garlicapp.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GarlicTheme {
                GarlicMainScreen()
            }
        }
    }
}

data class TabItem(val key: String, val label: String, val icon: String)

val tabs = listOf(
    TabItem("home", "首页", "📰"),
    TabItem("market", "行情", "📊"),
    TabItem("inventory", "库存", "📦"),
    TabItem("me", "我的", "👤"),
)

@Composable
fun GarlicMainScreen(vm: MainViewModel = viewModel()) {
    var currentTab by remember { mutableStateOf("home") }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Surface,
                tonalElevation = 8.dp,
                modifier = Modifier.height(56.dp)
            ) {
                tabs.forEach { tab ->
                    val active = currentTab == tab.key
                    NavigationBarItem(
                        selected = active,
                        onClick = { currentTab = tab.key },
                        icon = { Text(tab.icon, fontSize = 18.sp) },
                        label = {
                            Text(tab.label, fontSize = 10.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Green,
                            selectedTextColor = Green,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentTab) {
                "home" -> HomeScreen(vm)
                "market" -> MarketScreen(vm)
                "inventory" -> InventoryScreen(vm)
                "me" -> ProfileScreen(vm)
            }
        }
    }
}
