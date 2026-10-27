package com.dummytrading.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.dummytrading.app.ui.AppNav
import com.dummytrading.app.ui.DummyTradingTheme
import com.dummytrading.app.ui.TradingViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vm = ViewModelProvider(
            this, ViewModelProvider.AndroidViewModelFactory(application)
        )[TradingViewModel::class.java]
        setContent {
            DummyTradingTheme {
                AppNav(vm)
            }
        }
    }
}
