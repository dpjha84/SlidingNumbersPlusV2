package com.dpjha.slidingnumbersplus

import android.app.Application
import com.dpjha.slidingnumbersplus.R
import com.google.android.gms.games.PlayGamesSdk

class SlidingNumbersApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val appId = getString(R.string.games_app_id).trim()
        if (appId.toLongOrNull()?.let { it > 0 } == true) {
            PlayGamesSdk.initialize(this)
        }
    }
}
