using Microsoft.UI;
using Microsoft.UI.Composition.SystemBackdrops;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media;
using AntennaPod.Windows.Views;
using WinRT.Interop;

namespace AntennaPod.Windows;

public sealed partial class MainWindow : Window
{
    public MainWindow()
    {
        this.InitializeComponent();

        // Apply Mica backdrop for Windows 11 look
        TrySetMicaBackdrop();

        // Configure title bar
        ExtendsContentIntoTitleBar = true;
        SetTitleBar(AppTitleBar);

        // Set minimum window size
        var hWnd = WindowNative.GetWindowHandle(this);
        var windowId = Win32Interop.GetWindowIdFromWindow(hWnd);
        var appWindow = Microsoft.UI.Windowing.AppWindow.GetFromWindowId(windowId);
        appWindow.Resize(new global::Windows.Graphics.SizeInt32(1200, 800));
        this.Title = "AntennaPod";
    }

    private void TrySetMicaBackdrop()
    {
        if (MicaController.IsSupported())
        {
            SystemBackdrop = new MicaBackdrop { Kind = MicaKind.Base };
        }
        else if (DesktopAcrylicController.IsSupported())
        {
            SystemBackdrop = new DesktopAcrylicBackdrop();
        }
    }

    private void NavView_Loaded(object sender, RoutedEventArgs e)
    {
        // Select the first item by default
        if (NavView.MenuItems.Count > 0)
        {
            NavView.SelectedItem = NavView.MenuItems[0];
        }
    }

    private void NavView_SelectionChanged(NavigationView sender, NavigationViewSelectionChangedEventArgs args)
    {
        if (args.IsSettingsSelected)
        {
            ContentFrame.Navigate(typeof(SettingsPage));
            return;
        }

        if (args.SelectedItemContainer is NavigationViewItem item)
        {
            var tag = item.Tag?.ToString();
            NavigateToPage(tag);
        }
    }

    private void NavigateToPage(string? tag)
    {
        var pageType = tag switch
        {
            "podcasts" => typeof(PodcastsPage),
            "episodes" => typeof(EpisodesPage),
            "queue" => typeof(QueuePage),
            "discover" => typeof(DiscoverPage),
            "downloads" => typeof(DownloadsPage),
            "statistics" => typeof(StatisticsPage),
            "sync" => typeof(SyncPage),
            _ => typeof(PodcastsPage)
        };

        ContentFrame.Navigate(pageType);
    }

    public void ShowNowPlaying(string title, string podcast)
    {
        NowPlayingTitle.Text = title;
        NowPlayingPodcast.Text = podcast;
        NowPlayingBar.Visibility = Visibility.Visible;
    }
}
