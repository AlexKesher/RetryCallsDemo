package ru.alexkesh.retrycallsdemo

import android.app.Application
import ru.alexkesh.retrycallsdemo.di.AppCompositionRoot

class RetryCallsApp : Application() {

    val appCompositionRoot by lazy {
        AppCompositionRoot()
    }
}