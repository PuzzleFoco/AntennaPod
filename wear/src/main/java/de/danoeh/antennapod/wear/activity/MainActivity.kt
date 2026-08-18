package de.danoeh.antennapod.wear.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import de.danoeh.antennapod.net.download.serviceinterface.FeedUpdateManager
import de.danoeh.antennapod.wear.ui.WearAppNavigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LaunchedEffect(Unit) {
                // Refresh subscribed feeds whenever the app is opened so new
                // episodes show up without requiring a manual sync first.
                runCatching {
                    FeedUpdateManager.getInstance()?.runOnce(this@MainActivity)
                }
            }
            WearAppNavigation()
        }
    }
}
