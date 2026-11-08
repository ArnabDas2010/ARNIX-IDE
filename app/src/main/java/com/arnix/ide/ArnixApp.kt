package com.arnix.ide

import android.app.Application

class ArnixApp : Application() {

    companion object {
        lateinit var instance: ArnixApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
