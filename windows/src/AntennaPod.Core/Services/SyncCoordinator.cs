using AntennaPod.Core.Data;
using AntennaPod.Core.Models;

namespace AntennaPod.Core.Services;

public class SyncCoordinator
{
    private readonly ISyncService _syncService;
    private readonly IAppDatabase _database;

    public event Action<string>? SyncStatusChanged;

    public SyncCoordinator(ISyncService syncService, IAppDatabase database)
    {
        _syncService = syncService;
        _database = database;
    }

    public static ISyncService CreateSyncService(SyncCredentials credentials)
    {
        return credentials.Provider switch
        {
            SyncProvider.GpodderNet => new GpodderSyncService(credentials),
            SyncProvider.NextcloudGpodder => new NextcloudSyncService(credentials),
            _ => throw new ArgumentException($"Unsupported sync provider: {credentials.Provider}")
        };
    }

    public async Task<SyncResult> SyncAsync()
    {
        var result = new SyncResult();

        try
        {
            SyncStatusChanged?.Invoke("Logging in...");
            await _syncService.LoginAsync();

            // Phase 1: Subscription sync
            SyncStatusChanged?.Invoke("Syncing subscriptions...");
            await SyncSubscriptionsAsync(result);

            // Phase 2: Episode action sync
            SyncStatusChanged?.Invoke("Syncing episode actions...");
            await SyncEpisodeActionsAsync(result);

            SyncStatusChanged?.Invoke("Sync completed successfully.");
            result.Success = true;
        }
        catch (Exception ex)
        {
            result.Success = false;
            result.ErrorMessage = ex.Message;
            SyncStatusChanged?.Invoke($"Sync failed: {ex.Message}");
        }

        return result;
    }

    private async Task SyncSubscriptionsAsync(SyncResult result)
    {
        // Get remote changes
        var lastSync = await _database.GetLastSyncTimestampAsync("subscriptions");
        var remoteChanges = await _syncService.GetSubscriptionChangesAsync(lastSync);

        // Apply remote additions
        var feedParser = new FeedParser();
        foreach (var feedUrl in remoteChanges.Added)
        {
            var existing = await _database.GetPodcastByFeedUrlAsync(feedUrl);
            if (existing == null)
            {
                try
                {
                    var podcast = await feedParser.ParseFeedAsync(feedUrl);
                    var podcastId = await _database.InsertPodcastAsync(podcast);
                    foreach (var episode in podcast.Episodes)
                    {
                        episode.PodcastId = podcastId;
                        await _database.InsertEpisodeAsync(episode);
                    }
                    result.SubscriptionsAdded++;
                }
                catch (Exception ex)
                {
                    result.Errors.Add($"Failed to add podcast {feedUrl}: {ex.Message}");
                }
            }
        }

        // Apply remote removals
        foreach (var feedUrl in remoteChanges.Removed)
        {
            var existing = await _database.GetPodcastByFeedUrlAsync(feedUrl);
            if (existing != null)
            {
                await _database.DeletePodcastAsync(existing.Id);
                result.SubscriptionsRemoved++;
            }
        }

        // Upload local changes
        var (added, removed) = await _database.GetQueuedSubscriptionChangesAsync();
        if (added.Count > 0 || removed.Count > 0)
        {
            var response = await _syncService.UploadSubscriptionChangesAsync(added, removed);
            await _database.ClearQueuedSubscriptionChangesAsync();
            await _database.SetLastSyncTimestampAsync("subscriptions", response.Timestamp);
        }
        else
        {
            await _database.SetLastSyncTimestampAsync("subscriptions", remoteChanges.Timestamp);
        }
    }

    private async Task SyncEpisodeActionsAsync(SyncResult result)
    {
        // Get remote changes
        var lastSync = await _database.GetLastSyncTimestampAsync("episode_actions");
        var remoteChanges = await _syncService.GetEpisodeActionChangesAsync(lastSync);

        // Apply remote episode actions
        foreach (var action in remoteChanges.Actions)
        {
            try
            {
                await ApplyEpisodeActionAsync(action);
                result.EpisodeActionsApplied++;
            }
            catch (Exception ex)
            {
                result.Errors.Add($"Failed to apply episode action: {ex.Message}");
            }
        }

        // Upload local changes
        var queuedActions = await _database.GetQueuedEpisodeActionsAsync();
        if (queuedActions.Count > 0)
        {
            var response = await _syncService.UploadEpisodeActionsAsync(queuedActions);
            await _database.ClearQueuedEpisodeActionsAsync();
            await _database.SetLastSyncTimestampAsync("episode_actions", response.Timestamp);
        }
        else
        {
            await _database.SetLastSyncTimestampAsync("episode_actions", remoteChanges.Timestamp);
        }
    }

    private async Task ApplyEpisodeActionAsync(EpisodeAction action)
    {
        // Try to find episode by GUID first, then by media URL
        Episode? episode = null;
        if (!string.IsNullOrEmpty(action.Guid))
        {
            episode = await _database.GetEpisodeByGuidAsync(action.Guid);
        }
        if (episode == null && !string.IsNullOrEmpty(action.Episode))
        {
            episode = await _database.GetEpisodeByMediaUrlAsync(action.Episode);
        }

        if (episode == null)
            return;

        switch (action.Action)
        {
            case EpisodeAction.ActionType.Play:
                if (action.Position.HasValue)
                {
                    // Only update if remote position is further than local
                    if (action.Position.Value > episode.PlaybackPosition)
                    {
                        episode.PlaybackPosition = action.Position.Value;
                    }
                    if (action.Total.HasValue && action.Position.Value >= action.Total.Value - 10)
                    {
                        episode.PlayState = EpisodePlayState.Played;
                    }
                    else
                    {
                        episode.PlayState = EpisodePlayState.InProgress;
                    }
                }
                break;
            case EpisodeAction.ActionType.New:
                episode.PlayState = EpisodePlayState.New;
                episode.PlaybackPosition = 0;
                break;
        }

        await _database.UpdateEpisodeAsync(episode);
    }
}

public class SyncResult
{
    public bool Success { get; set; }
    public string? ErrorMessage { get; set; }
    public int SubscriptionsAdded { get; set; }
    public int SubscriptionsRemoved { get; set; }
    public int EpisodeActionsApplied { get; set; }
    public List<string> Errors { get; set; } = new();
}
