package com.whycun.garlicapp

import android.app.Application
import com.whycun.garlicapp.data.local.AppDatabase
import com.whycun.garlicapp.data.remote.GarlicApi
import com.whycun.garlicapp.data.repository.GarlicRepository

class GarlicApp : Application() {
    lateinit var repository: GarlicRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getInstance(this)
        val api = GarlicApi.create()
        repository = GarlicRepository(db, api)
    }
}
