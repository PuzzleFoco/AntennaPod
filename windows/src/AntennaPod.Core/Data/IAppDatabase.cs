using AntennaPod.Core.Models;

namespace AntennaPod.Core.Data;

public interface IAppDatabase
{
    Task InitializeAsync();

    // Podcasts
    Task<List<Podcast>> GetPodcastsAsync();
    Task<Podcast?> GetPodcastAsync(long id);
    Task<Podcast?> GetPodcastByFeedUrlAsync(string feedUrl);
    Task<long> InsertPodcastAsync(Podcast podcast);
    Task UpdatePodcastAsync(Podcast podcast);
    Task DeletePodcastAsync(long id);

    // Episodes
    Task<List<Episode>> GetEpisodesAsync(long podcastId);
    Task<List<Episode>> GetAllEpisodesAsync(int limit = 100, int offset = 0);
    Task<List<Episode>> GetQueueAsync();
    Task<Episode?> GetEpisodeAsync(long id);
    Task<Episode?> GetEpisodeByGuidAsync(string guid);
    Task<Episode?> GetEpisodeByMediaUrlAsync(string mediaUrl);
    Task<long> InsertEpisodeAsync(Episode episode);
    Task UpdateEpisodeAsync(Episode episode);
    Task DeleteEpisodesForPodcastAsync(long podcastId);

    // Sync state
    Task<long> GetLastSyncTimestampAsync(string key);
    Task SetLastSyncTimestampAsync(string key, long timestamp);

    // Queued sync actions
    Task QueueSubscriptionChangeAsync(string feedUrl, bool added);
    Task<(List<string> added, List<string> removed)> GetQueuedSubscriptionChangesAsync();
    Task ClearQueuedSubscriptionChangesAsync();
    Task QueueEpisodeActionAsync(EpisodeAction action);
    Task<List<EpisodeAction>> GetQueuedEpisodeActionsAsync();
    Task ClearQueuedEpisodeActionsAsync();
}
