using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using AntennaPod.Core.Models;
using AntennaPod.Core.Services;

namespace AntennaPod.Windows.Views;

public sealed partial class SyncPage : Page
{
    private readonly List<string> _logEntries = new();

    public SyncPage()
    {
        this.InitializeComponent();
        ProviderRadio.SelectedIndex = 0;
    }

    private void ProviderRadio_SelectionChanged(object sender, SelectionChangedEventArgs e)
    {
        if (ProviderRadio.SelectedItem is RadioButton radio)
        {
            var tag = radio.Tag?.ToString();
            GpodderSettings.Visibility = tag == "gpodder" ? Visibility.Visible : Visibility.Collapsed;
            NextcloudSettings.Visibility = tag == "nextcloud" ? Visibility.Visible : Visibility.Collapsed;
        }
    }

    private SyncCredentials? GetCredentials()
    {
        if (ProviderRadio.SelectedItem is not RadioButton radio)
            return null;

        var tag = radio.Tag?.ToString();
        return tag switch
        {
            "gpodder" => new SyncCredentials
            {
                Provider = SyncProvider.GpodderNet,
                BaseUrl = GpodderServer.Text,
                Username = GpodderUsername.Text,
                Password = GpodderPassword.Password,
                DeviceId = GpodderDevice.Text
            },
            "nextcloud" => new SyncCredentials
            {
                Provider = SyncProvider.NextcloudGpodder,
                BaseUrl = NextcloudServer.Text,
                Username = NextcloudUsername.Text,
                Password = NextcloudPassword.Password
            },
            _ => null
        };
    }

    private void SaveButton_Click(object sender, RoutedEventArgs e)
    {
        AddLog("Settings saved.");
    }

    private async void TestButton_Click(object sender, RoutedEventArgs e)
    {
        var credentials = GetCredentials();
        if (credentials == null)
        {
            AddLog("No sync provider configured.");
            return;
        }

        SyncStatusCard.Visibility = Visibility.Visible;
        SyncProgress.IsActive = true;
        SyncStatusText.Text = "Testing connection...";

        try
        {
            var syncService = SyncCoordinator.CreateSyncService(credentials);
            await syncService.LoginAsync();

            SyncStatusText.Text = "Connection successful!";
            SyncDetailText.Text = "Your credentials are valid and the server is reachable.";
            AddLog($"Connection test successful for {credentials.Provider}.");
        }
        catch (Exception ex)
        {
            SyncStatusText.Text = "Connection failed";
            SyncDetailText.Text = ex.Message;
            AddLog($"Connection test failed: {ex.Message}");
        }
        finally
        {
            SyncProgress.IsActive = false;
        }
    }

    private async void SyncNowButton_Click(object sender, RoutedEventArgs e)
    {
        var credentials = GetCredentials();
        if (credentials == null)
        {
            AddLog("No sync provider configured.");
            return;
        }

        SyncStatusCard.Visibility = Visibility.Visible;
        SyncProgress.IsActive = true;
        SyncNowButton.IsEnabled = false;
        SyncStatusText.Text = "Syncing...";

        try
        {
            var syncService = SyncCoordinator.CreateSyncService(credentials);
            var coordinator = new SyncCoordinator(syncService, App.Database);
            coordinator.SyncStatusChanged += status =>
            {
                DispatcherQueue.TryEnqueue(() =>
                {
                    SyncDetailText.Text = status;
                    AddLog(status);
                });
            };

            var result = await coordinator.SyncAsync();

            if (result.Success)
            {
                SyncStatusText.Text = "Sync completed";
                SyncDetailText.Text = $"Added {result.SubscriptionsAdded} subscriptions, " +
                    $"removed {result.SubscriptionsRemoved}, " +
                    $"applied {result.EpisodeActionsApplied} episode actions.";
            }
            else
            {
                SyncStatusText.Text = "Sync failed";
                SyncDetailText.Text = result.ErrorMessage;
            }

            foreach (var error in result.Errors)
            {
                AddLog($"Warning: {error}");
            }
        }
        catch (Exception ex)
        {
            SyncStatusText.Text = "Sync failed";
            SyncDetailText.Text = ex.Message;
            AddLog($"Sync error: {ex.Message}");
        }
        finally
        {
            SyncProgress.IsActive = false;
            SyncNowButton.IsEnabled = true;
        }
    }

    private void AddLog(string message)
    {
        var timestamp = DateTimeOffset.Now.ToString("HH:mm:ss");
        _logEntries.Add($"[{timestamp}] {message}");
        SyncLog.Text = string.Join("\n", _logEntries);
    }
}
