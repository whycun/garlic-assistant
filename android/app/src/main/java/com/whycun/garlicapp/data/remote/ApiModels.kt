package com.whycun.garlicapp.data.remote

import com.google.gson.annotations.SerializedName

// prices.json
data class PriceResponse(
    @SerializedName("schema_version") val schemaVersion: Int,
    @SerializedName("updated_at") val updatedAt: String,
    val status: String,
    val regions: Map<String, RegionData>,
    val history: List<HistoryEntry>,
    val factors: Map<String, FactorInfo>?,
)

data class RegionData(
    val name: String,
    @SerializedName("market_mood") val marketMood: String,
    val specs: List<SpecData>,
)

data class SpecData(
    val name: String,
    val low: Double,
    val high: Double,
    val unit: String,
    val trend: String,
)

data class HistoryEntry(
    val date: String,
    @SerializedName("jinxiang_avg") val jinxiangAvg: Double?,
    @SerializedName("qixian_avg") val qixianAvg: Double?,
    @SerializedName("pizhou_avg") val pizhouAvg: Double?,
    @SerializedName("zhongmou_avg") val zhongmouAvg: Double?,
)

data class FactorInfo(
    val label: String,
    val value: String,
    val level: String,
)

// news.json
data class NewsResponse(
    @SerializedName("schema_version") val schemaVersion: Int,
    @SerializedName("updated_at") val updatedAt: String,
    val items: List<NewsItem>,
    @SerializedName("total_count") val totalCount: Int,
)

data class NewsItem(
    val id: String,
    val title: String,
    val source: String,
    @SerializedName("source_name") val sourceName: String,
    val url: String,
    @SerializedName("published_at") val publishedAt: String,
    val tag: String,
    @SerializedName("tag_type") val tagType: String,
)
