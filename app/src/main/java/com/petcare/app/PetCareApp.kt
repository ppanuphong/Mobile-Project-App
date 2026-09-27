package com.petcare.app

import android.app.Application
import com.petcare.app.di.AppContainer
import com.petcare.app.notification.NotificationHelper

class PetCareApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.createChannel(this)
    }
}
