package com.whycun.garlicapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "batches")
data class Batch(
    @PrimaryKey val id: String,
    val batchNo: String,
    val region: String,       // jinxiang/qixian/pizhou/zhongmou
    val regionName: String,   // 金乡/杞县/邳州/中牟
    val spec: String,         // 一般混级/6.5cm净蒜...
    val quantityTons: Double,
    val purchasePricePerJin: Double,
    val purchaseDate: String,
    val storageLocation: String,
    val status: String,       // "holding" | "sold"
    val soldPricePerJin: Double? = null,
    val soldDate: String? = null,
    val createdAt: String,
    val updatedAt: String,
)

@Entity(tableName = "calc_saves")
data class CalcSave(
    @PrimaryKey val id: String,
    val marketPrice: Double,
    val purchasePrice: Double,
    val quantityTons: Double,
    val coldStorageFee: Double,
    val laborFee: Double,
    val lossFee: Double,
    val transportFee: Double,
    val otherFee: Double,
    val createdAt: String,
)
