package de.danoeh.antennapod.wear;

import android.app.Application;
import android.content.Context;

import androidx.work.Configuration;
import androidx.work.WorkManager;

import org.greenrobot.eventbus.EventBus;

import de.danoeh.antennapod.storage.preferences.UserPreferences;

/**
 * Main application class for AntennaPod Wear OS.
 * Handles initialization of core components like EventBus, WorkManager, and preferences.
 */
public class WearApplication extends Application {
    
    private static Context appContext;
    
    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
        
        // Initialize EventBus
        EventBus.builder()
                .addIndex(new ApWearEventBusIndex())
                .logNoSubscriberMessages(false)
                .sendNoSubscriberEvent(false)
                .installDefaultEventBus();
        
        // Initialize UserPreferences
        UserPreferences.init(this);
        
        // Initialize WorkManager for background sync
        initializeWorkManager();
    }
    
    private void initializeWorkManager() {
        Configuration config = new Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.INFO)
                .build();
        
        WorkManager.initialize(this, config);
    }
    
    public static Context getAppContext() {
        return appContext;
    }
}
