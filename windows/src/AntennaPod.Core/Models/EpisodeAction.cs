using System.Text.Json.Serialization;

namespace AntennaPod.Core.Models;

public class EpisodeAction
{
    [JsonPropertyName("podcast")]
    public string Podcast { get; set; } = string.Empty;

    [JsonPropertyName("episode")]
    public string Episode { get; set; } = string.Empty;

    [JsonPropertyName("guid")]
    public string? Guid { get; set; }

    [JsonPropertyName("action")]
    public string Action { get; set; } = string.Empty;

    [JsonPropertyName("timestamp")]
    public string? Timestamp { get; set; }

    [JsonPropertyName("started")]
    public int? Started { get; set; }

    [JsonPropertyName("position")]
    public int? Position { get; set; }

    [JsonPropertyName("total")]
    public int? Total { get; set; }

    public static EpisodeAction CreatePlayAction(string podcastUrl, string episodeUrl, string? guid,
        int started, int position, int total)
    {
        return new EpisodeAction
        {
            Podcast = podcastUrl,
            Episode = episodeUrl,
            Guid = guid,
            Action = ActionType.Play,
            Timestamp = DateTimeOffset.UtcNow.ToString("yyyy-MM-dd'T'HH:mm:ss"),
            Started = started,
            Position = position,
            Total = total
        };
    }

    public static EpisodeAction CreateNewAction(string podcastUrl, string episodeUrl, string? guid)
    {
        return new EpisodeAction
        {
            Podcast = podcastUrl,
            Episode = episodeUrl,
            Guid = guid,
            Action = ActionType.New,
            Timestamp = DateTimeOffset.UtcNow.ToString("yyyy-MM-dd'T'HH:mm:ss")
        };
    }

    public static EpisodeAction CreateDownloadAction(string podcastUrl, string episodeUrl, string? guid)
    {
        return new EpisodeAction
        {
            Podcast = podcastUrl,
            Episode = episodeUrl,
            Guid = guid,
            Action = ActionType.Download,
            Timestamp = DateTimeOffset.UtcNow.ToString("yyyy-MM-dd'T'HH:mm:ss")
        };
    }

    public static EpisodeAction CreateDeleteAction(string podcastUrl, string episodeUrl, string? guid)
    {
        return new EpisodeAction
        {
            Podcast = podcastUrl,
            Episode = episodeUrl,
            Guid = guid,
            Action = ActionType.Delete,
            Timestamp = DateTimeOffset.UtcNow.ToString("yyyy-MM-dd'T'HH:mm:ss")
        };
    }

    public static class ActionType
    {
        public const string New = "new";
        public const string Download = "download";
        public const string Play = "play";
        public const string Delete = "delete";
    }
}
