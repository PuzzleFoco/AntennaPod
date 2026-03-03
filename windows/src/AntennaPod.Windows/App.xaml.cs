using Microsoft.UI.Xaml;
using AntennaPod.Core.Data;

namespace AntennaPod.Windows;

public partial class App : Application
{
    public static IAppDatabase Database { get; private set; } = null!;
    public static string AppDataPath { get; private set; } = null!;
    public static Window? MainWindow { get; private set; }

    public App()
    {
        this.InitializeComponent();
    }

    protected override async void OnLaunched(LaunchActivatedEventArgs args)
    {
        // Set up app data directory
        AppDataPath = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "AntennaPod");
        Directory.CreateDirectory(AppDataPath);

        // Initialize database
        var dbPath = Path.Combine(AppDataPath, "antennapod.db");
        Database = new AppDatabase(dbPath);
        await Database.InitializeAsync();

        MainWindow = new MainWindow();
        MainWindow.Activate();
    }
}
