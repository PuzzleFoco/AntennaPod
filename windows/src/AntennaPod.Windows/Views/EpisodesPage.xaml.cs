using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Navigation;
using AntennaPod.Core.Models;

namespace AntennaPod.Windows.Views;

public sealed partial class EpisodesPage : Page
{
    private long _podcastId;
    private List<Episode> _allEpisodes = new();

    public EpisodesPage()
    {
        this.InitializeComponent();
    }

    protected override async void OnNavigatedTo(NavigationEventArgs e)
    {
        base.OnNavigatedTo(e);

        if (e.Parameter is long podcastId)
        {
            _podcastId = podcastId;
            BackButton.Visibility = Visibility.Visible;

            var podcast = await App.Database.GetPodcastAsync(podcastId);
            if (podcast != null)
            {
                PageTitle.Text = podcast.Title;
            }

            await LoadEpisodesAsync();
        }
        else
        {
            // Show all episodes across all podcasts
            _podcastId = 0;
            PageTitle.Text = "All Episodes";
            await LoadAllEpisodesAsync();
        }
    }

    private async Task LoadEpisodesAsync()
    {
        LoadingRing.IsActive = true;
        try
        {
            _allEpisodes = await App.Database.GetEpisodesAsync(_podcastId);
            ApplyFilter();
        }
        finally
        {
            LoadingRing.IsActive = false;
        }
    }

    private async Task LoadAllEpisodesAsync()
    {
        LoadingRing.IsActive = true;
        try
        {
            _allEpisodes = await App.Database.GetAllEpisodesAsync();
            ApplyFilter();
        }
        finally
        {
            LoadingRing.IsActive = false;
        }
    }

    private void ApplyFilter()
    {
        var filterIndex = FilterCombo.SelectedIndex;
        var filtered = filterIndex switch
        {
            1 => _allEpisodes.Where(e => e.PlayState == EpisodePlayState.New).ToList(),
            2 => _allEpisodes.Where(e => e.PlayState == EpisodePlayState.InProgress).ToList(),
            3 => _allEpisodes.Where(e => e.IsDownloaded).ToList(),
            _ => _allEpisodes
        };

        EpisodeList.ItemsSource = filtered;
        EmptyState.Visibility = filtered.Count == 0 ? Visibility.Visible : Visibility.Collapsed;
        EpisodeList.Visibility = filtered.Count > 0 ? Visibility.Visible : Visibility.Collapsed;
    }

    private void BackButton_Click(object sender, RoutedEventArgs e)
    {
        if (Frame.CanGoBack)
            Frame.GoBack();
    }

    private void FilterCombo_SelectionChanged(object sender, SelectionChangedEventArgs e)
    {
        if (_allEpisodes.Count > 0)
            ApplyFilter();
    }

    private void EpisodeList_ItemClick(object sender, ItemClickEventArgs e)
    {
        // Episode click - could navigate to detail or start playback
    }
}
