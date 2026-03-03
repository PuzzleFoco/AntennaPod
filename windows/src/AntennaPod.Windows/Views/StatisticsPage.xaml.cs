using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using AntennaPod.Core.Models;

namespace AntennaPod.Windows.Views;

public sealed partial class StatisticsPage : Page
{
    public StatisticsPage()
    {
        this.InitializeComponent();
        this.Loaded += StatisticsPage_Loaded;
    }

    private async void StatisticsPage_Loaded(object sender, RoutedEventArgs e)
    {
        var podcasts = await App.Database.GetPodcastsAsync();
        var episodes = await App.Database.GetAllEpisodesAsync(limit: 10000);

        TotalPodcasts.Text = podcasts.Count.ToString();
        TotalEpisodes.Text = episodes.Count.ToString();
        PlayedEpisodes.Text = episodes.Count(ep => ep.PlayState == EpisodePlayState.Played).ToString();
        DownloadedEpisodes.Text = episodes.Count(ep => ep.IsDownloaded).ToString();
    }
}
