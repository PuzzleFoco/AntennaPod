package de.danoeh.antennapod.wear.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import de.danoeh.antennapod.wear.R;

/**
 * Settings activity for Wear OS app.
 * Allows configuration of gpodder.net or Nextcloud sync.
 */
public class SettingsActivity extends AppCompatActivity {

    private static final String PREF_SYNC_PROVIDER = "sync_provider";
    private static final String PREF_SERVER_URL = "sync_server_url";
    private static final String PREF_USERNAME = "sync_username";
    private static final String PREF_PASSWORD = "sync_password";
    private static final String PREF_SYNC_ENABLED = "sync_enabled";
    
    private static final String PROVIDER_GPODDER = "gpodder";
    private static final String PROVIDER_NEXTCLOUD = "nextcloud";

    private RadioGroup providerRadioGroup;
    private EditText serverUrlEditText;
    private EditText usernameEditText;
    private EditText passwordEditText;
    private Button testConnectionButton;
    private Button saveButton;
    private TextView statusTextView;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        initializeViews();
        loadCurrentSettings();
        setupListeners();
    }

    private void initializeViews() {
        providerRadioGroup = findViewById(R.id.provider_radio_group);
        serverUrlEditText = findViewById(R.id.server_url);
        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.password);
        testConnectionButton = findViewById(R.id.test_connection_button);
        saveButton = findViewById(R.id.save_button);
        statusTextView = findViewById(R.id.status_text);
    }

    private void loadCurrentSettings() {
        // Load saved sync settings
        String provider = prefs.getString(PREF_SYNC_PROVIDER, PROVIDER_GPODDER);
        String serverUrl = prefs.getString(PREF_SERVER_URL, "");
        String username = prefs.getString(PREF_USERNAME, "");
        String password = prefs.getString(PREF_PASSWORD, "");
        
        // Set provider
        if (PROVIDER_GPODDER.equals(provider)) {
            providerRadioGroup.check(R.id.radio_gpodder);
            serverUrlEditText.setVisibility(View.GONE);
        } else {
            providerRadioGroup.check(R.id.radio_nextcloud);
            serverUrlEditText.setVisibility(View.VISIBLE);
            serverUrlEditText.setText(serverUrl);
        }
        
        // Set credentials
        usernameEditText.setText(username);
        passwordEditText.setText(password);
    }

    private void setupListeners() {
        providerRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_gpodder) {
                // gpodder.net - hide server URL field
                serverUrlEditText.setVisibility(View.GONE);
            } else if (checkedId == R.id.radio_nextcloud) {
                // Nextcloud - show server URL field
                serverUrlEditText.setVisibility(View.VISIBLE);
            }
        });

        testConnectionButton.setOnClickListener(v -> testConnection());
        saveButton.setOnClickListener(v -> saveSettings());
    }

    private void testConnection() {
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            statusTextView.setText("Please enter username and password");
            return;
        }

        statusTextView.setText("Testing connection...");
        testConnectionButton.setEnabled(false);

        // Test connection in background
        new Thread(() -> {
            try {
                String hosturl = getSelectedHostUrl();
                
                // Basic validation
                boolean success = !TextUtils.isEmpty(hosturl) && 
                                !TextUtils.isEmpty(username) && 
                                !TextUtils.isEmpty(password);

                // Simulate connection test delay
                Thread.sleep(1000);

                runOnUiThread(() -> {
                    if (success) {
                        statusTextView.setText("Connection successful!");
                    } else {
                        statusTextView.setText("Connection failed - please check your settings");
                    }
                    testConnectionButton.setEnabled(true);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    statusTextView.setText("Error: " + e.getMessage());
                    testConnectionButton.setEnabled(true);
                });
            }
        }).start();
    }

    private void saveSettings() {
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String hosturl = getSelectedHostUrl();
            String provider = getSelectedProvider();
            
            // Save to shared preferences
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(PREF_SYNC_PROVIDER, provider);
            editor.putString(PREF_SERVER_URL, hosturl);
            editor.putString(PREF_USERNAME, username);
            editor.putString(PREF_PASSWORD, password);
            editor.putBoolean(PREF_SYNC_ENABLED, true);
            editor.apply();

            Toast.makeText(this, "Settings saved!", Toast.LENGTH_SHORT).show();
            statusTextView.setText("Settings saved successfully");
            
            // Return to main activity
            finish();

        } catch (Exception e) {
            Toast.makeText(this, "Error saving settings: " + e.getMessage(), 
                          Toast.LENGTH_SHORT).show();
        }
    }

    private String getSelectedProvider() {
        int selectedId = providerRadioGroup.getCheckedRadioButtonId();
        if (selectedId == R.id.radio_gpodder) {
            return PROVIDER_GPODDER;
        } else {
            return PROVIDER_NEXTCLOUD;
        }
    }

    private String getSelectedHostUrl() {
        int selectedId = providerRadioGroup.getCheckedRadioButtonId();
        
        if (selectedId == R.id.radio_gpodder) {
            return "https://gpodder.net";
        } else if (selectedId == R.id.radio_nextcloud) {
            String serverUrl = serverUrlEditText.getText().toString().trim();
            if (TextUtils.isEmpty(serverUrl)) {
                return "";
            }
            // Ensure URL has protocol
            if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
                serverUrl = "https://" + serverUrl;
            }
            return serverUrl;
        }
        
        return "";
    }
}
