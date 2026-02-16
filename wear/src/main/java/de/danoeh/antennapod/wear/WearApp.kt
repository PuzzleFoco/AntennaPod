package de.danoeh.antennapod.wear

import android.app.Application
import de.danoeh.antennapod.net.common.AntennapodHttpClient
import de.danoeh.antennapod.net.common.NetworkUtils
import de.danoeh.antennapod.net.common.UserAgentInterceptor
import de.danoeh.antennapod.net.ssl.SslProviderInstaller
import de.danoeh.antennapod.net.sync.service.SynchronizationQueueImpl
import de.danoeh.antennapod.net.sync.serviceinterface.SynchronizationQueue
import de.danoeh.antennapod.storage.database.PodDBAdapter
import de.danoeh.antennapod.storage.preferences.SynchronizationCredentials
import de.danoeh.antennapod.storage.preferences.SynchronizationSettings
import de.danoeh.antennapod.storage.preferences.UserPreferences
import java.io.File

class WearApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            UserAgentInterceptor.USER_AGENT = "AntennaPod/" + packageInfo.versionName
        } catch (e: Exception) {
            UserAgentInterceptor.USER_AGENT = "AntennaPod"
        }
        PodDBAdapter.init(this)
        UserPreferences.init(this)
        SynchronizationCredentials.init(this)
        SynchronizationSettings.init(this)
        SslProviderInstaller.install(this)
        NetworkUtils.init(this)
        SynchronizationQueue.setInstance(SynchronizationQueueImpl(this))
        AntennapodHttpClient.setCacheDirectory(File(cacheDir, "okhttp"))
        AntennapodHttpClient.setProxyConfig(UserPreferences.getProxyConfig())
    }
}
