using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using AntennaPod.Core.Models;
using AntennaPod.Core.Services;

namespace AntennaPod.Windows.Views;

public sealed partial class DiscoverPage : Page
{
    public DiscoverPage()
    {
        this.InitializeComponent();
    }

    private async void SearchBox_QuerySubmitted(AutoSuggestBox sender, AutoSuggestBoxQuerySubmittedEventArgs args)
    {
        var query = args.QueryText?.Trim();
        if (string.IsNullOrEmpty(query))
            return;

        LoadingRing.IsActive = true;
        InitialState.Visibility = Visibility.Collapsed;

        try
        {
            // If it looks like a URL, try to parse it directly
            if (Uri.TryCreate(query, UriKind.Absolute, out var uri) &&
                (uri.Scheme == "http" || uri.Scheme == "https"))
            {
                var parser = new FeedParser();
                var podcast = await parser.ParseFeedAsync(query);
                ResultsGrid.ItemsSource = new List<Podcast> { podcast };
            }
            else
            {
                // Search using iTunes Search API
                await SearchPodcastsAsync(query);
            }
        }
        catch (Exception ex)
        {
            var dialog = new ContentDialog
            {
                Title = "Search Error",
                Content = $"Failed to search: {ex.Message}",
                CloseButtonText = "OK",
                XamlRoot = this.XamlRoot
            };
            await dialog.ShowAsync();
        }
        finally
        {
            LoadingRing.IsActive = false;
        }
    }

    private async Task SearchPodcastsAsync(string query)
    {
        using var client = new HttpClient();
        var searchUrl = $"https://itunes.apple.com/search?term={Uri.EscapeDataString(query)}&media=podcast&limit=20";
        var response = await client.GetStringAsync(searchUrl);

        var results = System.Text.Json.JsonDocument.Parse(response);
        var podcasts = new List<Podcast>();

        foreach (var result in results.RootElement.GetProperty("results").EnumerateArray())
        {
            podcasts.Add(new Podcast
            {
                Title = result.GetProperty("collectionName").GetString() ?? string.Empty,
                Author = result.GetProperty("artistName").GetString(),
                ImageUrl = result.GetProperty("artworkUrl100").GetString()?
                    .Replace("100x100", "600x600"),
                FeedUrl = result.GetProperty("feedUrl").GetString() ?? string.Empty
            });
        }

        ResultsGrid.ItemsSource = podcasts;
        ResultsGrid.Visibility = podcasts.Count > 0 ? Visibility.Visible : Visibility.Collapsed;
    }

    private async void ResultsGrid_ItemClick(object sender, ItemClickEventArgs e)
    {
        if (e.ClickedItem is Podcast podcast)
        {
            var dialog = new ContentDialog
            {
                Title = podcast.Title,
                Content = $"Subscribe to {podcast.Title}?",
                PrimaryButtonText = "Subscribe",
                CloseButtonText = "Cancel",
                DefaultButton = ContentDialogButton.Primary,
                XamlRoot = this.XamlRoot
            };

            if (await dialog.ShowAsync() == ContentDialogResult.Primary)
            {
                try
                {
                    var parser = new FeedParser();
                    var fullPodcast = await parser.ParseFeedAsync(podcast.FeedUrl);
                    var id = await App.Database.InsertPodcastAsync(fullPodcast);

                    foreach (var episode in fullPodcast.Episodes)
                    {
                        episode.PodcastId = id;
                        await App.Database.InsertEpisodeAsync(episode);
                    }

                    await App.Database.QueueSubscriptionChangeAsync(podcast.FeedUrl, true);
                }
                catch (Exception ex)
                {
                    var errorDialog = new ContentDialog
                    {
                        Title = "Error",
                        Content = $"Failed to subscribe: {ex.Message}",
                        CloseButtonText = "OK",
                        XamlRoot = this.XamlRoot
                    };
                    await errorDialog.ShowAsync();
                }
            }
        }
    }
}
