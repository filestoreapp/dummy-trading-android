package com.dummytrading.app

import android.app.Application
import androidx.room.Room
import com.dummytrading.app.data.AppDatabase

class DummyTradingApp : Application() {
    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext, AppDatabase::class.java, "dummy-trading.db"
        ).build()
    }
}
