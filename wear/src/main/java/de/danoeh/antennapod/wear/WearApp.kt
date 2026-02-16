package de.danoeh.antennapod.wear

import android.app.Application
import de.danoeh.antennapod.storage.preferences.UserPreferences

class WearApp : Application() {
    override fun onCreate() {
        super.onCreate()
        UserPreferences.init(this)
    }
}
