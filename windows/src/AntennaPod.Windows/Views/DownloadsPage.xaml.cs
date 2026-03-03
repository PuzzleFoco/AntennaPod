using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;

namespace AntennaPod.Windows.Views;

public sealed partial class DownloadsPage : Page
{
    public DownloadsPage()
    {
        this.InitializeComponent();
        this.Loaded += DownloadsPage_Loaded;
    }

    private async void DownloadsPage_Loaded(object sender, RoutedEventArgs e)
    {
        LoadingRing.IsActive = true;
        try
        {
            var episodes = await App.Database.GetAllEpisodesAsync();
            var downloaded = episodes.Where(ep => ep.IsDownloaded).ToList();
            DownloadsList.ItemsSource = downloaded;
            EmptyState.Visibility = downloaded.Count == 0 ? Visibility.Visible : Visibility.Collapsed;
            DownloadsList.Visibility = downloaded.Count > 0 ? Visibility.Visible : Visibility.Collapsed;
        }
        finally
        {
            LoadingRing.IsActive = false;
        }
    }
}
