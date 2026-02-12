package de.danoeh.antennapod.wear.ui;

import android.content.ComponentName;
import android.os.Bundle;

import androidx.activity.ComponentActivity;
import androidx.activity.compose.setContent;
import androidx.compose.foundation.background;
import androidx.compose.foundation.layout.Arrangement;
import androidx.compose.foundation.layout.Box;
import androidx.compose.foundation.layout.Column;
import androidx.compose.foundation.layout.fillMaxSize;
import androidx.compose.foundation.layout.fillMaxWidth;
import androidx.compose.foundation.layout.padding;
import androidx.compose.foundation.layout.size;
import androidx.compose.runtime.Composable;
import androidx.compose.runtime.mutableStateOf;
import androidx.compose.runtime.remember;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.graphics.Color;
import androidx.compose.ui.text.style.TextAlign;
import androidx.compose.ui.unit.dp;
import androidx.wear.compose.material.Button;
import androidx.wear.compose.material.ButtonDefaults;
import androidx.wear.compose.material.Chip;
import androidx.wear.compose.material.ChipDefaults;
import androidx.wear.compose.material.MaterialTheme;
import androidx.wear.compose.material.Text;
import androidx.wear.compose.material.TimeText;
import androidx.wear.compose.navigation.SwipeDismissableNavHost;
import androidx.wear.compose.navigation.composable;
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController;

import de.danoeh.antennapod.wear.R;

/**
 * Main activity for AntennaPod Wear OS app.
 * Provides navigation between different screens using Wear Compose navigation.
 */
public class MainActivity extends ComponentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContent(() -> {
            MaterialTheme theme = new MaterialTheme();
            return theme.content(this::WearApp);
        });
    }

    @Composable
    private void WearApp() {
        var navController = rememberSwipeDismissableNavController();

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            TimeText();

            SwipeDismissableNavHost(
                navController = navController,
                startDestination = "home"
            ) {
                composable("home") {
                    HomeScreen(
                        onNowPlayingClick = () -> navController.navigate("nowplaying"),
                        onLibraryClick = () -> navController.navigate("library"),
                        onSyncClick = () -> navController.navigate("sync"),
                        onSettingsClick = () -> navController.navigate("settings")
                    );
                }

                composable("nowplaying") {
                    NowPlayingScreen();
                }

                composable("library") {
                    LibraryScreen();
                }

                composable("sync") {
                    SyncScreen();
                }

                composable("settings") {
                    SettingsScreen();
                }
            }
        }
    }

    @Composable
    private void HomeScreen(
        Runnable onNowPlayingClick,
        Runnable onLibraryClick,
        Runnable onSyncClick,
        Runnable onSettingsClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = getString(R.string.app_name),
                style = MaterialTheme.typography.title2,
                textAlign = TextAlign.Center
            );

            Chip(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                label = { Text(getString(R.string.now_playing)); },
                onClick = onNowPlayingClick,
                colors = ChipDefaults.primaryChipColors()
            );

            Chip(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                label = { Text(getString(R.string.library)); },
                onClick = onLibraryClick,
                colors = ChipDefaults.secondaryChipColors()
            );

            Chip(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                label = { Text(getString(R.string.sync)); },
                onClick = onSyncClick,
                colors = ChipDefaults.secondaryChipColors()
            );

            Chip(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                label = { Text(getString(R.string.settings)); },
                onClick = onSettingsClick,
                colors = ChipDefaults.secondaryChipColors()
            );
        }
    }

    @Composable
    private void NowPlayingScreen() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = getString(R.string.now_playing),
                style = MaterialTheme.typography.title2,
                textAlign = TextAlign.Center
            );

            Text(
                text = "No episode playing",
                style = MaterialTheme.typography.body1,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            );

            Button(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .size(52.dp),
                onClick = () -> { /* TODO: Play/Pause */ },
                colors = ButtonDefaults.primaryButtonColors()
            ) {
                Text("▶");
            }
        }
    }

    @Composable
    private void LibraryScreen() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = getString(R.string.library),
                style = MaterialTheme.typography.title2,
                textAlign = TextAlign.Center
            );

            Text(
                text = getString(R.string.no_episodes),
                style = MaterialTheme.typography.body1,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            );
        }
    }

    @Composable
    private void SyncScreen() {
        var syncState = remember { mutableStateOf("Not synced") };

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = getString(R.string.sync),
                style = MaterialTheme.typography.title2,
                textAlign = TextAlign.Center
            );

            Text(
                text = syncState.getValue(),
                style = MaterialTheme.typography.body1,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            );

            Button(
                modifier = Modifier.padding(top = 16.dp),
                onClick = () -> {
                    syncState.setValue("Syncing...");
                    // TODO: Trigger sync
                },
                colors = ButtonDefaults.primaryButtonColors()
            ) {
                Text("Sync Now");
            }
        }
    }

    @Composable
    private void SettingsScreen() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = getString(R.string.settings),
                style = MaterialTheme.typography.title2,
                textAlign = TextAlign.Center
            );

            Chip(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                label = { Text(getString(R.string.sync_settings)); },
                onClick = () -> { /* TODO: Open sync settings */ },
                colors = ChipDefaults.secondaryChipColors()
            );

            Chip(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                label = { Text(getString(R.string.playback_settings)); },
                onClick = () -> { /* TODO: Open playback settings */ },
                colors = ChipDefaults.secondaryChipColors()
            );

            Chip(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                label = { Text(getString(R.string.about)); },
                onClick = () -> { /* TODO: Open about */ },
                colors = ChipDefaults.secondaryChipColors()
            );
        }
    }
}
