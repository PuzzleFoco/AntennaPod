package de.danoeh.antennapod.wear;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertNotNull;

/**
 * Basic unit tests for WearApplication
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, application = android.app.Application.class)
public class WearApplicationTest {

    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
    }

    @Test
    public void testApplicationContextNotNull() {
        assertNotNull("Application context should not be null", context);
    }
}
