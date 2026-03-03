# AntennaPod for Windows

A native Windows 11 podcast manager built with WinUI 3 and the Windows App SDK. This app provides feature parity with the AntennaPod Android application while delivering a modern, lightweight Windows experience.

## Features

- **Podcast Management** – Subscribe to podcasts via RSS/Atom feed URLs, browse and manage your library
- **Episode Playback** – Built-in media player with playback position tracking and queue management
- **gPodder.net Sync** – Synchronize subscriptions and listening progress with [gpodder.net](https://gpodder.net)
- **Nextcloud gPodder Sync** – Sync with your self-hosted Nextcloud instance using the [gPodder Sync app](https://apps.nextcloud.com/apps/gpoddersync)
- **Podcast Discovery** – Search and discover new podcasts via iTunes Search API
- **OPML Import/Export** – Import and export your subscription list in standard OPML format
- **Download Management** – Download episodes for offline listening
- **Statistics** – View your listening statistics
- **Windows 11 Design** – Native look and feel with Mica backdrop, NavigationView, and WinUI 3 controls

## Screenshots

The app uses the Windows 11 design language with:
- Mica material backdrop
- NavigationView with icons for all sections
- Card-based layouts for settings and statistics
- Modern grid layout for podcast artwork
- Integrated now-playing bar with playback controls

## Requirements

- Windows 10 version 1903 (build 19041) or later
- Windows 11 recommended for full visual experience (Mica backdrop)
- .NET 8.0 Runtime (included in self-contained builds)

## Building

### Prerequisites

- [Visual Studio 2022](https://visualstudio.microsoft.com/) with:
  - .NET Desktop Development workload
  - Windows App SDK C# Templates
- [.NET 8.0 SDK](https://dotnet.microsoft.com/download/dotnet/8.0)

### Build from Source

```bash
# Clone the repository
git clone https://github.com/AntennaPod/AntennaPod.git
cd AntennaPod/windows

# Restore dependencies
dotnet restore AntennaPod.Windows.sln

# Build
dotnet build AntennaPod.Windows.sln -c Release

# Run tests
dotnet test tests/AntennaPod.Core.Tests/AntennaPod.Core.Tests.csproj
```

### Build Self-Contained Executable

```bash
dotnet publish src/AntennaPod.Windows/AntennaPod.Windows.csproj \
  -c Release -r win-x64 --self-contained true \
  -p:PublishSingleFile=true -o publish/
```

## CI/CD

The project includes a GitHub Actions workflow (`.github/workflows/windows-build.yml`) that automatically:

1. **Runs unit tests** on every push and PR
2. **Builds MSIX packages** for x64 and ARM64 architectures
3. **Publishes self-contained executables** that can be downloaded directly from GitHub Actions artifacts

### Installing from GitHub

1. Go to the [Actions tab](../../actions/workflows/windows-build.yml)
2. Click on the latest successful workflow run
3. Download the artifact for your architecture (x64 or ARM64)
4. Extract and run `AntennaPod.Windows.exe`

## Architecture

```
windows/
├── src/
│   ├── AntennaPod.Core/          # Platform-independent core library
│   │   ├── Models/               # Data models (Podcast, Episode, EpisodeAction, etc.)
│   │   ├── Services/             # Business logic (sync, feed parsing, OPML)
│   │   └── Data/                 # SQLite database layer
│   └── AntennaPod.Windows/       # WinUI 3 application
│       ├── Views/                # XAML pages
│       ├── ViewModels/           # MVVM view models (future)
│       ├── Converters/           # Value converters (future)
│       └── Assets/               # App icons and images
├── tests/
│   └── AntennaPod.Core.Tests/    # Unit tests
└── AntennaPod.Windows.sln        # Solution file
```

### Key Design Decisions

- **WinUI 3 / Windows App SDK** – Latest Microsoft UI framework with native Windows 11 support
- **.NET 8.0** – Long-term support runtime with excellent performance
- **SQLite** – Lightweight embedded database for local storage
- **CommunityToolkit.Mvvm** – Microsoft's recommended MVVM toolkit
- **Platform-independent Core** – The `AntennaPod.Core` library has no Windows dependencies and can be reused for other platforms

## Sync Protocol

The sync implementation follows the same protocol as the Android app:

### gPodder.net
- Uses the [gPodder.net API v2](https://gpoddernet.readthedocs.io/en/latest/api/)
- Supports device registration, subscription sync, and episode action sync
- Chunks episode action uploads to 30 items per request
- Handles URL unescaping for gPodder.net's colon encoding

### Nextcloud gPodder
- Uses the [Nextcloud gPodder Sync API](https://github.com/thrillfall/nextcloud-gpodder)
- Basic authentication with Nextcloud credentials
- Same subscription and episode action sync as gPodder.net

## License

This project follows the same license as the main AntennaPod project.
