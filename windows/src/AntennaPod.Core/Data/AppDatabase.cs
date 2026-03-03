using System.Text.Json;
using Microsoft.Data.Sqlite;
using AntennaPod.Core.Models;

namespace AntennaPod.Core.Data;

public class AppDatabase : IAppDatabase, IDisposable
{
    private readonly string _connectionString;
    private SqliteConnection? _connection;

    public AppDatabase(string databasePath)
    {
        _connectionString = $"Data Source={databasePath}";
    }

    private async Task<SqliteConnection> GetConnectionAsync()
    {
        if (_connection == null)
        {
            _connection = new SqliteConnection(_connectionString);
            await _connection.OpenAsync();
        }
        return _connection;
    }

    public async Task InitializeAsync()
    {
        var conn = await GetConnectionAsync();

        var cmd = conn.CreateCommand();
        cmd.CommandText = @"
            CREATE TABLE IF NOT EXISTS podcasts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                feed_url TEXT NOT NULL UNIQUE,
                description TEXT,
                author TEXT,
                image_url TEXT,
                link TEXT,
                language TEXT,
                last_updated TEXT,
                subscribed_date TEXT NOT NULL
            );

            CREATE TABLE IF NOT EXISTS episodes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                podcast_id INTEGER NOT NULL,
                title TEXT NOT NULL,
                guid TEXT,
                description TEXT,
                media_url TEXT,
                mime_type TEXT,
                media_size INTEGER DEFAULT 0,
                duration INTEGER DEFAULT 0,
                playback_position INTEGER DEFAULT 0,
                published_date TEXT,
                image_url TEXT,
                link TEXT,
                play_state INTEGER DEFAULT 0,
                is_downloaded INTEGER DEFAULT 0,
                local_file_path TEXT,
                is_in_queue INTEGER DEFAULT 0,
                queue_position INTEGER DEFAULT 0,
                FOREIGN KEY (podcast_id) REFERENCES podcasts(id)
            );

            CREATE TABLE IF NOT EXISTS sync_state (
                key TEXT PRIMARY KEY,
                timestamp INTEGER NOT NULL
            );

            CREATE TABLE IF NOT EXISTS queued_subscription_changes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                feed_url TEXT NOT NULL,
                added INTEGER NOT NULL
            );

            CREATE TABLE IF NOT EXISTS queued_episode_actions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                action_json TEXT NOT NULL
            );

            CREATE INDEX IF NOT EXISTS idx_episodes_podcast_id ON episodes(podcast_id);
            CREATE INDEX IF NOT EXISTS idx_episodes_guid ON episodes(guid);
            CREATE INDEX IF NOT EXISTS idx_episodes_media_url ON episodes(media_url);
            CREATE INDEX IF NOT EXISTS idx_episodes_queue ON episodes(is_in_queue, queue_position);
        ";
        await cmd.ExecuteNonQueryAsync();
    }

    public async Task<List<Podcast>> GetPodcastsAsync()
    {
        var conn = await GetConnectionAsync();
        var cmd = conn.CreateCommand();
        cmd.CommandText = "SELECT * FROM podcasts ORDER BY title";
        var podcasts = new List<Podcast>();

        using var reader = await cmd.ExecuteReaderAsync();
        while (await reader.ReadAsync())
        {
            podcasts.Add(ReadPodcast(reader));
        }
        return podcasts;
    }

    public async Task<Podcast?> GetPodcastAsync(long id)
    {
        var conn = await GetConnectionAsync();
        var cmd = conn.CreateCommand();
        cmd.CommandText = "SELECT * FROM podcasts WHERE id = @id";
        cmd.Parameters.AddWithValue("@id", id);

        using var reader = await cmd.ExecuteReaderAsync();
        return await reader.ReadAsync() ? ReadPodcast(reader) : null;
    }

    public async Task<Podcast?> GetPodcastByFeedUrlAsync(string feedUrl)
    {
        var conn = await GetConnectionAsync();
        var cmd = conn.CreateCommand();
        cmd.CommandText = "SELECT * FROM podcasts WHERE feed_url = @feedUrl";
        cmd.Parameters.AddWithValue("@feedUrl", feedUrl);

        using var reader = await cmd.ExecuteReaderAsync();
        return await reader.ReadAsync() ? ReadPodcast(reader) : null;
    }

    public async Task<long> InsertPodcastAsync(Podcast podcast)
    {
        var conn = await GetConnectionAsync();
        var cmd = conn.CreateCommand();
        cmd.CommandText = @"
            INSERT INTO podcasts (title, feed_url, description, author, image_url, link, language, last_updated, subscribed_date)
            VALUES (@title, @feedUrl, @description, @author, @imageUrl, @link, @language, @lastUpdated, @subscribedDate);
            SELECT last_insert_rowid();";
        cmd.Parameters.AddWithValue("@title", podcast.Title);
        cmd.Parameters.AddWithValue("@feedUrl", podcast.FeedUrl);
        cmd.Parameters.AddWithValue("@description", (object?)podcast.Description ?? DBNull.Value);
        cmd.Parameters.AddWithValue("@author", (object?)podcast.Author ?? DBNull.Value);
        cmd.Parameters.AddWithValue("@imageUrl", (object?)podcast.ImageUrl ?? DBNull.Value);
        cmd.Parameters.AddWithValue("@link", (object?)podcast.Link ?? DBNull.Value);
        cmd.Parameters.AddWithValue("@language", (object?)podcast.Language ?? DBNull.Value);
        cmd.Parameters.AddWithValue("@lastUpdated", podcast.LastUpdated?.ToString("O") ?? (object)DBNull.Value);
        cmd.Parameters.AddWithValue("@subscribedDate", podcast.SubscribedDate.ToString("O"));

        var result = await cmd.ExecuteScalarAsync();
        podcast.Id = Convert.ToInt64(result);
        return podcast.Id;
    }

    public async Task UpdatePodcastAsync(Podcast podcast)
    {
        var conn = await GetConnectionAsync();
        var cmd = conn.CreateCommand();
        cmd.CommandText = @"
            UPDATE podcasts SET title = @title, description = @description, author = @author,
                image_url = @imageUrl, link = @link, language = @language, last_updated = @lastUpdated
            WHERE id = @id";
        cmd.Parameters.AddWithValue("@id", podcast.Id);
        cmd.Parameters.AddWithValue("@title", podcast.Title);
        cmd.Parameters.AddWithValue("@description", (object?)podcast.Description ?? DBNull.Value);
        cmd.Parameters.AddWithValue("@author", (object?)podcast.Author ?? DBNull.Value);
        cmd.Parameters.AddWithValue("@imageUrl", (object?)podcast.ImageUrl ?? DBNull.Value);
        cmd.Parameters.AddWithValue("@link", (object?)podcast.Link ?? DBNull.Value);
        cmd.Parameters.AddWithValue("@language", (object?)podcast.Language ?? DBNull.Value);
        cmd.Parameters.AddWithValue("@lastUpdated", podcast.LastUpdated?.ToString("O") ?? (object)DBNull.Value);

        await cmd.ExecuteNonQueryAsync();
    }

    public async Task DeletePodcastAsync(long id)
    {
        var conn = await GetConnectionAsync();
        await DeleteEpisodesForPodcastAsync(id);

        var cmd = conn.CreateCommand();
        cmd.CommandText = "DELETE FROM podcasts WHERE id = @id";
        cmd.Parameters.AddWithValue("@id", id);
        await cmd.ExecuteNonQueryAsync();
    }

    public async Task<List<Episode>> GetEpisodesAsync(long podcastId)
    {
        var conn = await GetConnectionAsync();
        var cmd = conn.CreateCommand();
        cmd.CommandText = "SELECT * FROM episodes WHERE podcast_id = @podcastId ORDER BY published_date DESC";
        cmd.Parameters.AddWithValue("@podcastId", podcastId);

        var episodes = new List<Episode>();
        using var reader = await cmd.ExecuteReaderAsync();
        while (await reader.ReadAsync())
        {
            episodes.Add(ReadEpisode(reader));
        }
        return episodes;
    }

    public async Task<List<Episode>> GetAllEpisodesAsync(int limit = 100, int offset = 0)
    {
        var conn = await GetConnectionAsync();
        var cmd = conn.CreateCommand();
        cmd.CommandText = "SELECT * FROM episodes ORDER BY published_date DESC LIMIT @limit OFFSET @offset";
        cmd.Parameters.AddWithValue("@limit", limit);
        cmd.Parameters.AddWithValue("@offset", offset);

        var episodes = new List<Episode>();
        using var reader = await cmd.ExecuteReaderAsync();
        while (await reader.ReadAsync())
        {
            episodes.Add(ReadEpisode(reader));
        }
        return episodes;
    }

    public async Task<List<Episode>> GetQueueAsync()
    {
        var conn = await GetConnectionAsync();
        var cmd = conn.CreateCommand();
        cmd.CommandText = "SELECT * FROM episodes WHERE is_in_queue = 1 ORDER BY queue_position";

        var episodes = new List<Episode>();
        using var reader = await cmd.ExecuteReaderAsync();
        while (await reader.ReadAsync())
        {
            episodes.Add(ReadEpisode(reader));
        }
        return episodes;
    }

    public async Task<Episode?> GetEpisodeAsync(long id)
    {
        var conn = await GetConnectionAsync();
        var cmd = conn.CreateCommand();
        cmd.CommandText = "SELECT * FROM episodes WHERE id = @id";
        cmd.Parameters.AddWithValue("@id", id);

        using var reader = await cmd.ExecuteReaderAsync();
        return await reader.ReadAsync() ? ReadEpisode(reader) : null;
    }

    public async Task<Episode?> GetEpisodeByGuidAsync(string guid)
    {
        var conn = await GetConnectionAsync();
        var cmd = conn.CreateCommand();
        cmd.CommandText = "SELECT * FROM episodes WHERE guid = @guid";
        cmd.Parameters.AddWithValue("@guid", guid);

        using var reader = await cmd.ExecuteReaderAsync();
        return await reader.ReadAsync() ? ReadEpisode(reader) : null;
    }

    public async Task<Episode?> GetEpisodeByMediaUrlAsync(string mediaUrl)
    {
        var conn = await GetConnectionAsync();
        var cmd = conn.CreateCommand();
        cmd.CommandText = "SELECT * FROM episodes WHERE media_url = @mediaUrl";
        cmd.Parameters.AddWithValue("@mediaUrl", mediaUrl);

        using var reader = await cmd.ExecuteReaderAsync();
        return await reader.ReadAsync() ? ReadEpisode(reader) : null;
    }

    public async Task<long> InsertEpisodeAsync(Episode episode)
    {
        var conn = await GetConnectionAsync();
        var cmd = conn.CreateCommand();
        cmd.CommandText = @"
            INSERT OR IGNORE INTO episodes (podcast_id, title, guid, description, media_url, mime_type,
                media_size, duration, playback_position, published_date, image_url, link,
                play_state, is_downloaded, local_file_path, is_in_queue, queue_position)
            VALUES (@podcastId, @title, @guid, @description, @mediaUrl, @mimeType,
                @mediaSize, @duration, @playbackPosition, @publishedDate, @imageUrl, @link,
                @playState, @isDownloaded, @localFilePath, @isInQueue, @queuePosition);
            SELECT last_insert_rowid();";
        AddEpisodeParameters(cmd, episode);

        var result = await cmd.ExecuteScalarAsync();
        episode.Id = Convert.ToInt64(result);
        return episode.Id;
    }

    public async Task UpdateEpisodeAsync(Episode episode)
    {
        var conn = await GetConnectionAsync();
        var cmd = conn.CreateCommand();
        cmd.CommandText = @"
            UPDATE episodes SET title = @title, description = @description, media_url = @mediaUrl,
                mime_type = @mimeType, media_size = @mediaSize, duration = @duration,
                playback_position = @playbackPosition, image_url = @imageUrl, link = @link,
                play_state = @playState, is_downloaded = @isDownloaded, local_file_path = @localFilePath,
                is_in_queue = @isInQueue, queue_position = @queuePosition
            WHERE id = @id";
        cmd.Parameters.AddWithValue("@id", episode.Id);
        AddEpisodeParameters(cmd, episode);

        await cmd.ExecuteNonQueryAsync();
    }

    public async Task DeleteEpisodesForPodcastAsync(long podcastId)
    {
        var conn = await GetConnectionAsync();
        var cmd = conn.CreateCommand();
        cmd.CommandText = "DELETE FROM episodes WHERE podcast_id = @podcastId";
        cmd.Parameters.AddWithValue("@podcastId", podcastId);
        await cmd.ExecuteNonQueryAsync();
    }

    public async Task<long> GetLastSyncTimestampAsync(string key)
    {
        var conn = await GetConnectionAsync();
        var cmd = conn.CreateCommand();
        cmd.CommandText = "SELECT timestamp FROM sync_state WHERE key = @key";
        cmd.Parameters.AddWithValue("@key", key);

        var result = await cmd.ExecuteScalarAsync();
        return result != null ? Convert.ToInt64(result) : 0;
    }

    public async Task SetLastSyncTimestampAsync(string key, long timestamp)
    {
        var conn = await GetConnectionAsync();
        var cmd = conn.CreateCommand();
        cmd.CommandText = @"
            INSERT OR REPLACE INTO sync_state (key, timestamp) VALUES (@key, @timestamp)";
        cmd.Parameters.AddWithValue("@key", key);
        cmd.Parameters.AddWithValue("@timestamp", timestamp);
        await cmd.ExecuteNonQueryAsync();
    }

    public async Task QueueSubscriptionChangeAsync(string feedUrl, bool added)
    {
        var conn = await GetConnectionAsync();
        var cmd = conn.CreateCommand();
        cmd.CommandText = @"
            INSERT INTO queued_subscription_changes (feed_url, added) VALUES (@feedUrl, @added)";
        cmd.Parameters.AddWithValue("@feedUrl", feedUrl);
        cmd.Parameters.AddWithValue("@added", added ? 1 : 0);
        await cmd.ExecuteNonQueryAsync();
    }

    public async Task<(List<string> added, List<string> removed)> GetQueuedSubscriptionChangesAsync()
    {
        var conn = await GetConnectionAsync();
        var cmd = conn.CreateCommand();
        cmd.CommandText = "SELECT feed_url, added FROM queued_subscription_changes";

        var added = new List<string>();
        var removed = new List<string>();

        using var reader = await cmd.ExecuteReaderAsync();
        while (await reader.ReadAsync())
        {
            var feedUrl = reader.GetString(0);
            if (reader.GetInt32(1) == 1)
                added.Add(feedUrl);
            else
                removed.Add(feedUrl);
        }

        return (added, removed);
    }

    public async Task ClearQueuedSubscriptionChangesAsync()
    {
        var conn = await GetConnectionAsync();
        var cmd = conn.CreateCommand();
        cmd.CommandText = "DELETE FROM queued_subscription_changes";
        await cmd.ExecuteNonQueryAsync();
    }

    public async Task QueueEpisodeActionAsync(EpisodeAction action)
    {
        var conn = await GetConnectionAsync();
        var cmd = conn.CreateCommand();
        cmd.CommandText = @"
            INSERT INTO queued_episode_actions (action_json) VALUES (@json)";
        cmd.Parameters.AddWithValue("@json", JsonSerializer.Serialize(action));
        await cmd.ExecuteNonQueryAsync();
    }

    public async Task<List<EpisodeAction>> GetQueuedEpisodeActionsAsync()
    {
        var conn = await GetConnectionAsync();
        var cmd = conn.CreateCommand();
        cmd.CommandText = "SELECT action_json FROM queued_episode_actions";

        var actions = new List<EpisodeAction>();
        using var reader = await cmd.ExecuteReaderAsync();
        while (await reader.ReadAsync())
        {
            var json = reader.GetString(0);
            var action = JsonSerializer.Deserialize<EpisodeAction>(json);
            if (action != null)
                actions.Add(action);
        }
        return actions;
    }

    public async Task ClearQueuedEpisodeActionsAsync()
    {
        var conn = await GetConnectionAsync();
        var cmd = conn.CreateCommand();
        cmd.CommandText = "DELETE FROM queued_episode_actions";
        await cmd.ExecuteNonQueryAsync();
    }

    private static Podcast ReadPodcast(SqliteDataReader reader)
    {
        return new Podcast
        {
            Id = reader.GetInt64(reader.GetOrdinal("id")),
            Title = reader.GetString(reader.GetOrdinal("title")),
            FeedUrl = reader.GetString(reader.GetOrdinal("feed_url")),
            Description = reader.IsDBNull(reader.GetOrdinal("description")) ? null : reader.GetString(reader.GetOrdinal("description")),
            Author = reader.IsDBNull(reader.GetOrdinal("author")) ? null : reader.GetString(reader.GetOrdinal("author")),
            ImageUrl = reader.IsDBNull(reader.GetOrdinal("image_url")) ? null : reader.GetString(reader.GetOrdinal("image_url")),
            Link = reader.IsDBNull(reader.GetOrdinal("link")) ? null : reader.GetString(reader.GetOrdinal("link")),
            Language = reader.IsDBNull(reader.GetOrdinal("language")) ? null : reader.GetString(reader.GetOrdinal("language")),
            LastUpdated = reader.IsDBNull(reader.GetOrdinal("last_updated")) ? null : DateTimeOffset.Parse(reader.GetString(reader.GetOrdinal("last_updated"))),
            SubscribedDate = DateTimeOffset.Parse(reader.GetString(reader.GetOrdinal("subscribed_date")))
        };
    }

    private static Episode ReadEpisode(SqliteDataReader reader)
    {
        return new Episode
        {
            Id = reader.GetInt64(reader.GetOrdinal("id")),
            PodcastId = reader.GetInt64(reader.GetOrdinal("podcast_id")),
            Title = reader.GetString(reader.GetOrdinal("title")),
            Guid = reader.IsDBNull(reader.GetOrdinal("guid")) ? null : reader.GetString(reader.GetOrdinal("guid")),
            Description = reader.IsDBNull(reader.GetOrdinal("description")) ? null : reader.GetString(reader.GetOrdinal("description")),
            MediaUrl = reader.IsDBNull(reader.GetOrdinal("media_url")) ? null : reader.GetString(reader.GetOrdinal("media_url")),
            MimeType = reader.IsDBNull(reader.GetOrdinal("mime_type")) ? null : reader.GetString(reader.GetOrdinal("mime_type")),
            MediaSize = reader.GetInt64(reader.GetOrdinal("media_size")),
            Duration = reader.GetInt32(reader.GetOrdinal("duration")),
            PlaybackPosition = reader.GetInt32(reader.GetOrdinal("playback_position")),
            PublishedDate = reader.IsDBNull(reader.GetOrdinal("published_date")) ? null : DateTimeOffset.Parse(reader.GetString(reader.GetOrdinal("published_date"))),
            ImageUrl = reader.IsDBNull(reader.GetOrdinal("image_url")) ? null : reader.GetString(reader.GetOrdinal("image_url")),
            Link = reader.IsDBNull(reader.GetOrdinal("link")) ? null : reader.GetString(reader.GetOrdinal("link")),
            PlayState = (EpisodePlayState)reader.GetInt32(reader.GetOrdinal("play_state")),
            IsDownloaded = reader.GetInt32(reader.GetOrdinal("is_downloaded")) == 1,
            LocalFilePath = reader.IsDBNull(reader.GetOrdinal("local_file_path")) ? null : reader.GetString(reader.GetOrdinal("local_file_path")),
            IsInQueue = reader.GetInt32(reader.GetOrdinal("is_in_queue")) == 1,
            QueuePosition = reader.GetInt32(reader.GetOrdinal("queue_position"))
        };
    }

    private static void AddEpisodeParameters(SqliteCommand cmd, Episode episode)
    {
        cmd.Parameters.AddWithValue("@podcastId", episode.PodcastId);
        cmd.Parameters.AddWithValue("@title", episode.Title);
        cmd.Parameters.AddWithValue("@guid", (object?)episode.Guid ?? DBNull.Value);
        cmd.Parameters.AddWithValue("@description", (object?)episode.Description ?? DBNull.Value);
        cmd.Parameters.AddWithValue("@mediaUrl", (object?)episode.MediaUrl ?? DBNull.Value);
        cmd.Parameters.AddWithValue("@mimeType", (object?)episode.MimeType ?? DBNull.Value);
        cmd.Parameters.AddWithValue("@mediaSize", episode.MediaSize);
        cmd.Parameters.AddWithValue("@duration", episode.Duration);
        cmd.Parameters.AddWithValue("@playbackPosition", episode.PlaybackPosition);
        cmd.Parameters.AddWithValue("@publishedDate", episode.PublishedDate?.ToString("O") ?? (object)DBNull.Value);
        cmd.Parameters.AddWithValue("@imageUrl", (object?)episode.ImageUrl ?? DBNull.Value);
        cmd.Parameters.AddWithValue("@link", (object?)episode.Link ?? DBNull.Value);
        cmd.Parameters.AddWithValue("@playState", (int)episode.PlayState);
        cmd.Parameters.AddWithValue("@isDownloaded", episode.IsDownloaded ? 1 : 0);
        cmd.Parameters.AddWithValue("@localFilePath", (object?)episode.LocalFilePath ?? DBNull.Value);
        cmd.Parameters.AddWithValue("@isInQueue", episode.IsInQueue ? 1 : 0);
        cmd.Parameters.AddWithValue("@queuePosition", episode.QueuePosition);
    }

    public void Dispose()
    {
        _connection?.Close();
        _connection?.Dispose();
    }
}
