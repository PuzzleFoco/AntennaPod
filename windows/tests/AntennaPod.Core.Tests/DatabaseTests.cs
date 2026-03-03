using Xunit;
using AntennaPod.Core.Models;
using AntennaPod.Core.Data;

namespace AntennaPod.Core.Tests;

public class DatabaseTests : IAsyncLifetime, IDisposable
{
    private readonly string _dbPath;
    private AppDatabase _db = null!;

    public DatabaseTests()
    {
        _dbPath = Path.Combine(Path.GetTempPath(), $"antennapod_test_{Guid.NewGuid()}.db");
    }

    public async Task InitializeAsync()
    {
        _db = new AppDatabase(_dbPath);
        await _db.InitializeAsync();
    }

    public Task DisposeAsync()
    {
        _db.Dispose();
        return Task.CompletedTask;
    }

    public void Dispose()
    {
        try { File.Delete(_dbPath); } catch { }
    }

    [Fact]
    public async Task InsertAndGetPodcast()
    {
        var podcast = new Podcast
        {
            Title = "Test Podcast",
            FeedUrl = "https://example.com/feed.xml",
            Author = "Test Author",
            Description = "A test podcast"
        };

        var id = await _db.InsertPodcastAsync(podcast);
        Assert.True(id > 0);

        var retrieved = await _db.GetPodcastAsync(id);
        Assert.NotNull(retrieved);
        Assert.Equal("Test Podcast", retrieved.Title);
        Assert.Equal("https://example.com/feed.xml", retrieved.FeedUrl);
        Assert.Equal("Test Author", retrieved.Author);
    }

    [Fact]
    public async Task GetPodcastByFeedUrl()
    {
        var podcast = new Podcast
        {
            Title = "URL Test",
            FeedUrl = "https://example.com/unique-feed.xml"
        };
        await _db.InsertPodcastAsync(podcast);

        var result = await _db.GetPodcastByFeedUrlAsync("https://example.com/unique-feed.xml");
        Assert.NotNull(result);
        Assert.Equal("URL Test", result.Title);
    }

    [Fact]
    public async Task DeletePodcastAlsoDeletesEpisodes()
    {
        var podcast = new Podcast { Title = "Delete Test", FeedUrl = "https://example.com/delete.xml" };
        var podcastId = await _db.InsertPodcastAsync(podcast);

        var episode = new Episode { PodcastId = podcastId, Title = "Episode 1", Guid = "ep1" };
        await _db.InsertEpisodeAsync(episode);

        var episodes = await _db.GetEpisodesAsync(podcastId);
        Assert.Single(episodes);

        await _db.DeletePodcastAsync(podcastId);

        var deletedPodcast = await _db.GetPodcastAsync(podcastId);
        Assert.Null(deletedPodcast);

        var deletedEpisodes = await _db.GetEpisodesAsync(podcastId);
        Assert.Empty(deletedEpisodes);
    }

    [Fact]
    public async Task InsertAndGetEpisode()
    {
        var podcast = new Podcast { Title = "EP Test", FeedUrl = "https://example.com/ep.xml" };
        var podcastId = await _db.InsertPodcastAsync(podcast);

        var episode = new Episode
        {
            PodcastId = podcastId,
            Title = "Test Episode",
            Guid = "test-guid-123",
            MediaUrl = "https://example.com/ep1.mp3",
            Duration = 3600,
            PlayState = EpisodePlayState.New
        };

        var episodeId = await _db.InsertEpisodeAsync(episode);
        Assert.True(episodeId > 0);

        var retrieved = await _db.GetEpisodeAsync(episodeId);
        Assert.NotNull(retrieved);
        Assert.Equal("Test Episode", retrieved.Title);
        Assert.Equal("test-guid-123", retrieved.Guid);
        Assert.Equal(3600, retrieved.Duration);
    }

    [Fact]
    public async Task GetEpisodeByGuid()
    {
        var podcast = new Podcast { Title = "GUID Test", FeedUrl = "https://example.com/guid.xml" };
        var podcastId = await _db.InsertPodcastAsync(podcast);

        var episode = new Episode
        {
            PodcastId = podcastId,
            Title = "GUID Episode",
            Guid = "unique-guid-456"
        };
        await _db.InsertEpisodeAsync(episode);

        var result = await _db.GetEpisodeByGuidAsync("unique-guid-456");
        Assert.NotNull(result);
        Assert.Equal("GUID Episode", result.Title);
    }

    [Fact]
    public async Task GetEpisodeByMediaUrl()
    {
        var podcast = new Podcast { Title = "URL EP Test", FeedUrl = "https://example.com/urlep.xml" };
        var podcastId = await _db.InsertPodcastAsync(podcast);

        var episode = new Episode
        {
            PodcastId = podcastId,
            Title = "URL Episode",
            MediaUrl = "https://example.com/unique-episode.mp3"
        };
        await _db.InsertEpisodeAsync(episode);

        var result = await _db.GetEpisodeByMediaUrlAsync("https://example.com/unique-episode.mp3");
        Assert.NotNull(result);
        Assert.Equal("URL Episode", result.Title);
    }

    [Fact]
    public async Task UpdateEpisodePlayState()
    {
        var podcast = new Podcast { Title = "Play Test", FeedUrl = "https://example.com/play.xml" };
        var podcastId = await _db.InsertPodcastAsync(podcast);

        var episode = new Episode
        {
            PodcastId = podcastId,
            Title = "Play Episode",
            PlayState = EpisodePlayState.New,
            PlaybackPosition = 0
        };
        var episodeId = await _db.InsertEpisodeAsync(episode);

        episode.Id = episodeId;
        episode.PlayState = EpisodePlayState.InProgress;
        episode.PlaybackPosition = 1800;
        await _db.UpdateEpisodeAsync(episode);

        var updated = await _db.GetEpisodeAsync(episodeId);
        Assert.NotNull(updated);
        Assert.Equal(EpisodePlayState.InProgress, updated.PlayState);
        Assert.Equal(1800, updated.PlaybackPosition);
    }

    [Fact]
    public async Task QueueOperations()
    {
        var podcast = new Podcast { Title = "Queue Test", FeedUrl = "https://example.com/queue.xml" };
        var podcastId = await _db.InsertPodcastAsync(podcast);

        var ep1 = new Episode { PodcastId = podcastId, Title = "Q1", IsInQueue = true, QueuePosition = 1 };
        var ep2 = new Episode { PodcastId = podcastId, Title = "Q2", IsInQueue = true, QueuePosition = 2 };
        var ep3 = new Episode { PodcastId = podcastId, Title = "Q3", IsInQueue = false };

        await _db.InsertEpisodeAsync(ep1);
        await _db.InsertEpisodeAsync(ep2);
        await _db.InsertEpisodeAsync(ep3);

        var queue = await _db.GetQueueAsync();
        Assert.Equal(2, queue.Count);
        Assert.Equal("Q1", queue[0].Title);
        Assert.Equal("Q2", queue[1].Title);
    }

    [Fact]
    public async Task SyncTimestamps()
    {
        var timestamp = await _db.GetLastSyncTimestampAsync("subscriptions");
        Assert.Equal(0, timestamp);

        await _db.SetLastSyncTimestampAsync("subscriptions", 1234567890);
        timestamp = await _db.GetLastSyncTimestampAsync("subscriptions");
        Assert.Equal(1234567890, timestamp);

        // Update
        await _db.SetLastSyncTimestampAsync("subscriptions", 9999999999);
        timestamp = await _db.GetLastSyncTimestampAsync("subscriptions");
        Assert.Equal(9999999999, timestamp);
    }

    [Fact]
    public async Task QueuedSubscriptionChanges()
    {
        await _db.QueueSubscriptionChangeAsync("https://example.com/add1.xml", true);
        await _db.QueueSubscriptionChangeAsync("https://example.com/add2.xml", true);
        await _db.QueueSubscriptionChangeAsync("https://example.com/remove1.xml", false);

        var (added, removed) = await _db.GetQueuedSubscriptionChangesAsync();
        Assert.Equal(2, added.Count);
        Assert.Single(removed);
        Assert.Contains("https://example.com/add1.xml", added);
        Assert.Contains("https://example.com/remove1.xml", removed);

        await _db.ClearQueuedSubscriptionChangesAsync();
        (added, removed) = await _db.GetQueuedSubscriptionChangesAsync();
        Assert.Empty(added);
        Assert.Empty(removed);
    }

    [Fact]
    public async Task QueuedEpisodeActions()
    {
        var action = EpisodeAction.CreatePlayAction(
            "https://example.com/feed.xml",
            "https://example.com/ep.mp3",
            "guid-1", 0, 300, 3600);

        await _db.QueueEpisodeActionAsync(action);

        var actions = await _db.GetQueuedEpisodeActionsAsync();
        Assert.Single(actions);
        Assert.Equal("play", actions[0].Action);
        Assert.Equal(300, actions[0].Position);

        await _db.ClearQueuedEpisodeActionsAsync();
        actions = await _db.GetQueuedEpisodeActionsAsync();
        Assert.Empty(actions);
    }
}
