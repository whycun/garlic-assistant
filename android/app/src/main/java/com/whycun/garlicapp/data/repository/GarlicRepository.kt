package com.whycun.garlicapp.data.repository

import android.content.Context
import com.google.gson.Gson
import com.whycun.garlicapp.data.local.AppDatabase
import com.whycun.garlicapp.data.local.entity.Batch
import com.whycun.garlicapp.data.local.entity.CalcSave
import com.whycun.garlicapp.data.remote.GarlicApi
import com.whycun.garlicapp.data.remote.NewsResponse
import com.whycun.garlicapp.data.remote.PriceResponse
import kotlinx.coroutines.flow.Flow
import java.io.BufferedReader
import java.io.InputStreamReader

class GarlicRepository(private val db: AppDatabase, private val api: GarlicApi, private val context: Context) {

    // ===== 快速加载本地assets（不经过网络） =====

    fun loadPricesFromAssetsSync(): PriceResponse? = runCatching {
        Gson().fromJson(readAsset("prices.json"), PriceResponse::class.java)
    }.getOrNull()

    fun loadNewsFromAssetsSync(): NewsResponse? = runCatching {
        Gson().fromJson(readAsset("news.json"), NewsResponse::class.java)
    }.getOrNull()

    // ===== 远程数据（网络优先 + 本地assets兜底） =====

    suspend fun fetchPrices(): Result<PriceResponse> = runCatching {
        try {
            api.getPrices()
        } catch (e1: Exception) {
            try {
                val cdnApi = retrofit2.Retrofit.Builder()
                    .baseUrl(GarlicApi.CDN_BASE)
                    .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                    .build().create(GarlicApi::class.java)
                cdnApi.getPrices()
            } catch (e2: Exception) {
                // 网络全失败，读本地assets
                loadPricesFromAssets()
            }
        }
    }

    suspend fun fetchNews(): Result<NewsResponse> = runCatching {
        try {
            api.getNews()
        } catch (e1: Exception) {
            try {
                val cdnApi = retrofit2.Retrofit.Builder()
                    .baseUrl(GarlicApi.CDN_BASE)
                    .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                    .build().create(GarlicApi::class.java)
                cdnApi.getNews()
            } catch (e2: Exception) {
                loadNewsFromAssets()
            }
        }
    }

    private fun loadPricesFromAssets(): PriceResponse {
        val json = readAsset("prices.json")
        return Gson().fromJson(json, PriceResponse::class.java)
    }

    private fun loadNewsFromAssets(): NewsResponse {
        val json = readAsset("news.json")
        return Gson().fromJson(json, NewsResponse::class.java)
    }

    private fun readAsset(filename: String): String {
        val stream = context.assets.open(filename)
        return BufferedReader(InputStreamReader(stream, "UTF-8")).use { it.readText() }
    }

    // ===== 本地数据 - 批次 =====

    fun getAllBatches(): Flow<List<Batch>> = db.batchDao().getAll()
    fun getHoldingBatches(): Flow<List<Batch>> = db.batchDao().getHolding()

    suspend fun saveBatch(batch: Batch) = db.batchDao().insert(batch)
    suspend fun deleteBatch(id: String) = db.batchDao().deleteById(id)

    suspend fun markAsSold(id: String, soldPrice: Double, soldDate: String) {
        val batch = db.batchDao().getById(id) ?: return
        db.batchDao().update(batch.copy(
            status = "sold", soldPricePerJin = soldPrice, soldDate = soldDate,
            updatedAt = java.time.Instant.now().toString()
        ))
    }

    // ===== 本地数据 - 计算记录 =====

    fun getCalcSaves(): Flow<List<CalcSave>> = db.calcSaveDao().getAll()
    suspend fun saveCalc(calc: CalcSave) = db.calcSaveDao().insert(calc)
    suspend fun deleteCalc(id: String) = db.calcSaveDao().deleteById(id)
}
