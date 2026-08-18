package de.danoeh.antennapod.wear.phone

import android.content.Context

/**
 * Free (F-Droid) build: no Google Play Services is available, so syncing
 * credentials from the phone over the Wearable Data Layer is not supported.
 */
object PhoneCredentialSync {
    /** Always reports "not supported" on the free build. */
    fun request(context: Context): Boolean = false
}
