package com.whycun.garlicapp.data.repository

import com.whycun.garlicapp.data.local.AppDatabase
import com.whycun.garlicapp.data.local.entity.Batch
import com.whycun.garlicapp.data.local.entity.CalcSave
import com.whycun.garlicapp.data.remote.GarlicApi
import com.whycun.garlicapp.data.remote.NewsResponse
import com.whycun.garlicapp.data.remote.PriceResponse
import kotlinx.coroutines.flow.Flow

class GarlicRepository(private val db: AppDatabase, private val api: GarlicApi) {

    // ===== 远程数据 =====

    suspend fun fetchPrices(): Result<PriceResponse> = runCatching {
        try {
            api.getPrices()
        } catch (e: Exception) {
            // 备用CDN
            val cdnApi = retrofit2.Retrofit.Builder()
                .baseUrl(GarlicApi.CDN_BASE)
                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                .build()
                .create(GarlicApi::class.java)
            cdnApi.getPrices()
        }
    }

    suspend fun fetchNews(): Result<NewsResponse> = runCatching {
        try {
            api.getNews()
        } catch (e: Exception) {
            val cdnApi = retrofit2.Retrofit.Builder()
                .baseUrl(GarlicApi.CDN_BASE)
                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                .build()
                .create(GarlicApi::class.java)
            cdnApi.getNews()
        }
    }

    // ===== 本地数据 - 批次 =====

    fun getAllBatches(): Flow<List<Batch>> = db.batchDao().getAll()
    fun getHoldingBatches(): Flow<List<Batch>> = db.batchDao().getHolding()

    suspend fun saveBatch(batch: Batch) = db.batchDao().insert(batch)
    suspend fun deleteBatch(id: String) = db.batchDao().deleteById(id)

    suspend fun markAsSold(id: String, soldPrice: Double, soldDate: String) {
        val batch = db.batchDao().getById(id) ?: return
        db.batchDao().update(batch.copy(
            status = "sold",
            soldPricePerJin = soldPrice,
            soldDate = soldDate,
            updatedAt = java.time.Instant.now().toString()
        ))
    }

    // ===== 本地数据 - 计算记录 =====

    fun getCalcSaves(): Flow<List<CalcSave>> = db.calcSaveDao().getAll()
    suspend fun saveCalc(calc: CalcSave) = db.calcSaveDao().insert(calc)
    suspend fun deleteCalc(id: String) = db.calcSaveDao().deleteById(id)
}
