using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using AntennaPod.Core.Models;
using AntennaPod.Core.Services;

namespace AntennaPod.Windows.Views;

public sealed partial class PodcastsPage : Page
{
    private List<Podcast> _podcasts = new();

    private static IntPtr GetMainWindowHandle()
    {
        return WinRT.Interop.WindowNative.GetWindowHandle(App.MainWindow);
    }

    public PodcastsPage()
    {
        this.InitializeComponent();
        this.Loaded += PodcastsPage_Loaded;
    }

    private async void PodcastsPage_Loaded(object sender, RoutedEventArgs e)
    {
        await LoadPodcastsAsync();
    }

    private async Task LoadPodcastsAsync()
    {
        LoadingRing.IsActive = true;
        try
        {
            _podcasts = await App.Database.GetPodcastsAsync();
            PodcastGrid.ItemsSource = _podcasts;
            EmptyState.Visibility = _podcasts.Count == 0 ? Visibility.Visible : Visibility.Collapsed;
            PodcastGrid.Visibility = _podcasts.Count > 0 ? Visibility.Visible : Visibility.Collapsed;
        }
        finally
        {
            LoadingRing.IsActive = false;
        }
    }

    private async void AddButton_Click(object sender, RoutedEventArgs e)
    {
        var dialog = new ContentDialog
        {
            Title = "Add Podcast",
            PrimaryButtonText = "Subscribe",
            CloseButtonText = "Cancel",
            DefaultButton = ContentDialogButton.Primary,
            XamlRoot = this.XamlRoot
        };

        var inputPanel = new StackPanel { Spacing = 8 };
        var urlBox = new TextBox
        {
            PlaceholderText = "Enter podcast feed URL (RSS/Atom)",
            Header = "Feed URL"
        };
        inputPanel.Children.Add(urlBox);
        dialog.Content = inputPanel;

        var result = await dialog.ShowAsync();
        if (result == ContentDialogResult.Primary && !string.IsNullOrWhiteSpace(urlBox.Text))
        {
            await SubscribeToPodcastAsync(urlBox.Text.Trim());
        }
    }

    private async Task SubscribeToPodcastAsync(string feedUrl)
    {
        LoadingRing.IsActive = true;
        try
        {
            // Check if already subscribed
            var existing = await App.Database.GetPodcastByFeedUrlAsync(feedUrl);
            if (existing != null)
            {
                await ShowMessageAsync("Already Subscribed", "You are already subscribed to this podcast.");
                return;
            }

            var parser = new FeedParser();
            var podcast = await parser.ParseFeedAsync(feedUrl);
            var podcastId = await App.Database.InsertPodcastAsync(podcast);

            foreach (var episode in podcast.Episodes)
            {
                episode.PodcastId = podcastId;
                await App.Database.InsertEpisodeAsync(episode);
            }

            // Queue sync action
            await App.Database.QueueSubscriptionChangeAsync(feedUrl, added: true);

            await LoadPodcastsAsync();
        }
        catch (Exception ex)
        {
            await ShowMessageAsync("Error", $"Failed to subscribe: {ex.Message}");
        }
        finally
        {
            LoadingRing.IsActive = false;
        }
    }

    private async void RefreshButton_Click(object sender, RoutedEventArgs e)
    {
        LoadingRing.IsActive = true;
        try
        {
            var parser = new FeedParser();
            foreach (var podcast in _podcasts)
            {
                try
                {
                    var updated = await parser.ParseFeedAsync(podcast.FeedUrl);
                    podcast.Title = updated.Title;
                    podcast.Description = updated.Description;
                    podcast.Author = updated.Author;
                    podcast.ImageUrl = updated.ImageUrl;
                    podcast.LastUpdated = DateTimeOffset.UtcNow;
                    await App.Database.UpdatePodcastAsync(podcast);

                    foreach (var episode in updated.Episodes)
                    {
                        episode.PodcastId = podcast.Id;
                        await App.Database.InsertEpisodeAsync(episode);
                    }
                }
                catch
                {
                    // Skip feeds that fail to refresh
                }
            }
            await LoadPodcastsAsync();
        }
        finally
        {
            LoadingRing.IsActive = false;
        }
    }

    private void PodcastGrid_ItemClick(object sender, ItemClickEventArgs e)
    {
        if (e.ClickedItem is Podcast podcast)
        {
            Frame.Navigate(typeof(EpisodesPage), podcast.Id);
        }
    }

    private async void ImportOpml_Click(object sender, RoutedEventArgs e)
    {
        var picker = new global::Windows.Storage.Pickers.FileOpenPicker();
        picker.FileTypeFilter.Add(".opml");
        picker.FileTypeFilter.Add(".xml");

        WinRT.Interop.InitializeWithWindow.Initialize(picker, GetMainWindowHandle());

        var file = await picker.PickSingleFileAsync();
        if (file != null)
        {
            LoadingRing.IsActive = true;
            try
            {
                var content = await global::Windows.Storage.FileIO.ReadTextAsync(file);
                var opmlService = new OpmlService();
                var entries = opmlService.ImportFromOpml(content);

                foreach (var entry in entries)
                {
                    await SubscribeToPodcastAsync(entry.FeedUrl);
                }
            }
            catch (Exception ex)
            {
                await ShowMessageAsync("Import Error", $"Failed to import OPML: {ex.Message}");
            }
            finally
            {
                LoadingRing.IsActive = false;
            }
        }
    }

    private async void ExportOpml_Click(object sender, RoutedEventArgs e)
    {
        var picker = new global::Windows.Storage.Pickers.FileSavePicker();
        picker.FileTypeChoices.Add("OPML", new List<string> { ".opml" });
        picker.SuggestedFileName = "AntennaPod-Subscriptions";

        WinRT.Interop.InitializeWithWindow.Initialize(picker, GetMainWindowHandle());

        var file = await picker.PickSaveFileAsync();
        if (file != null)
        {
            try
            {
                var opmlService = new OpmlService();
                var opmlContent = opmlService.ExportToOpml(_podcasts);
                await global::Windows.Storage.FileIO.WriteTextAsync(file, opmlContent);
            }
            catch (Exception ex)
            {
                await ShowMessageAsync("Export Error", $"Failed to export OPML: {ex.Message}");
            }
        }
    }

    private async Task ShowMessageAsync(string title, string message)
    {
        var dialog = new ContentDialog
        {
            Title = title,
            Content = message,
            CloseButtonText = "OK",
            XamlRoot = this.XamlRoot
        };
        await dialog.ShowAsync();
    }
}
