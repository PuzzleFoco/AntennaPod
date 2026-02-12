package de.danoeh.antennapod.wear;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

import de.danoeh.antennapod.wear.sync.SyncManager;

/**
 * Unit tests for WearApplication and SyncManager
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class WearApplicationTest {

    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
    }

    @Test
    public void testApplicationContextNotNull() {
        assertNotNull("Application context should not be null", context);
    }

    @Test
    public void testSyncManagerSchedule() {
        // Test that sync manager can schedule sync without crashing
        try {
            SyncManager.schedulePeriodicSync(context);
            // If we get here, scheduling worked
            assertTrue(true);
        } catch (Exception e) {
            fail("Sync scheduling should not throw exception: " + e.getMessage());
        }
    }

    @Test
    public void testSyncManagerCancelSync() {
        // Test that sync manager can cancel sync without crashing
        try {
            SyncManager.cancelAllSync(context);
            // If we get here, cancellation worked
            assertTrue(true);
        } catch (Exception e) {
            fail("Sync cancellation should not throw exception: " + e.getMessage());
        }
    }

    @Test
    public void testSyncManagerTriggerSync() {
        // Test that sync manager can trigger immediate sync without crashing
        try {
            SyncManager.triggerImmediateSync(context);
            // If we get here, triggering worked
            assertTrue(true);
        } catch (Exception e) {
            fail("Sync trigger should not throw exception: " + e.getMessage());
        }
    }
}
