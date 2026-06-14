package com.whycun.garlicapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.whycun.garlicapp.GarlicApp
import com.whycun.garlicapp.data.local.entity.Batch
import com.whycun.garlicapp.data.local.entity.CalcSave
import com.whycun.garlicapp.data.remote.NewsItem
import com.whycun.garlicapp.data.remote.PriceResponse
import com.whycun.garlicapp.data.remote.RegionData
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as GarlicApp).repository

    // 行情数据
    private val _priceData = MutableStateFlow<PriceResponse?>(null)
    val priceData: StateFlow<PriceResponse?> = _priceData

    private val _newsData = MutableStateFlow<List<NewsItem>>(emptyList())
    val newsData: StateFlow<List<NewsItem>> = _newsData

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isStale = MutableStateFlow(false)
    val isStale: StateFlow<Boolean> = _isStale

    // 库存数据
    val allBatches: StateFlow<List<Batch>> = repo.getAllBatches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val holdingBatches: StateFlow<List<Batch>> = repo.getHoldingBatches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 计算记录
    val calcSaves: StateFlow<List<CalcSave>> = repo.getCalcSaves()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 当前选中产区
    private val _selectedRegion = MutableStateFlow("jinxiang")
    val selectedRegion: StateFlow<String> = _selectedRegion

    init {
        // 先秒加载本地assets数据
        viewModelScope.launch {
            repo.fetchPrices().onSuccess { _priceData.value = it }
            repo.fetchNews().onSuccess { _newsData.value = it.items }
        }
        // 后台尝试网络刷新
        refreshFromNetwork()
    }

    private fun refreshFromNetwork() {
        viewModelScope.launch {
            try {
                repo.fetchPrices().onSuccess {
                    if (it.regions.isNotEmpty()) {
                        _priceData.value = it
                        _isStale.value = false
                    }
                }
                repo.fetchNews().onSuccess {
                    if (it.items.isNotEmpty()) {
                        _newsData.value = it.items
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repo.fetchPrices().onSuccess {
                    if (it.regions.isNotEmpty()) { _priceData.value = it; _isStale.value = false }
                }
                repo.fetchNews().onSuccess {
                    if (it.items.isNotEmpty()) { _newsData.value = it.items }
                }
            } catch (_: Exception) {}
            _isLoading.value = false
        }
    }

    fun selectRegion(region: String) {
        _selectedRegion.value = region
    }

    fun getRegionData(region: String): RegionData? {
        return _priceData.value?.regions?.get(region)
    }

    fun getHistory(): List<com.whycun.garlicapp.data.remote.HistoryEntry> {
        return _priceData.value?.history ?: emptyList()
    }

    // 批次操作
    fun saveBatch(batch: Batch) = viewModelScope.launch { repo.saveBatch(batch) }
    fun deleteBatch(id: String) = viewModelScope.launch { repo.deleteBatch(id) }
    fun markAsSold(id: String, price: Double, date: String) = viewModelScope.launch {
        repo.markAsSold(id, price, date)
    }

    // 计算器
    fun saveCalc(calc: CalcSave) = viewModelScope.launch { repo.saveCalc(calc) }
    fun deleteCalc(id: String) = viewModelScope.launch { repo.deleteCalc(id) }

    // 汇总计算
    fun getTotalTons(): Double = holdingBatches.value.sumOf { it.quantityTons }
    fun getTotalCost(): Double = holdingBatches.value.sumOf {
        it.quantityTons * it.purchasePricePerJin * 2000
    }
    fun getInventoryValue(): Double {
        val prices = _priceData.value?.regions ?: return 0.0
        return holdingBatches.value.sumOf { batch ->
            val marketPrice = prices[batch.region]?.specs?.firstOrNull()?.low
                ?: batch.purchasePricePerJin
            batch.quantityTons * marketPrice * 2000
        }
    }
}
