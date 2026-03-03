using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;

namespace AntennaPod.Windows.Views;

public sealed partial class QueuePage : Page
{
    public QueuePage()
    {
        this.InitializeComponent();
        this.Loaded += QueuePage_Loaded;
    }

    private async void QueuePage_Loaded(object sender, RoutedEventArgs e)
    {
        LoadingRing.IsActive = true;
        try
        {
            var queue = await App.Database.GetQueueAsync();
            QueueList.ItemsSource = queue;
            EmptyState.Visibility = queue.Count == 0 ? Visibility.Visible : Visibility.Collapsed;
            QueueList.Visibility = queue.Count > 0 ? Visibility.Visible : Visibility.Collapsed;
        }
        finally
        {
            LoadingRing.IsActive = false;
        }
    }
}
