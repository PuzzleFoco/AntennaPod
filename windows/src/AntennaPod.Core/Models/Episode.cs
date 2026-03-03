namespace AntennaPod.Core.Models;

public class Episode
{
    public long Id { get; set; }
    public long PodcastId { get; set; }
    public string Title { get; set; } = string.Empty;
    public string? Guid { get; set; }
    public string? Description { get; set; }
    public string? MediaUrl { get; set; }
    public string? MimeType { get; set; }
    public long MediaSize { get; set; }
    public int Duration { get; set; }
    public int PlaybackPosition { get; set; }
    public DateTimeOffset? PublishedDate { get; set; }
    public string? ImageUrl { get; set; }
    public string? Link { get; set; }
    public EpisodePlayState PlayState { get; set; } = EpisodePlayState.New;
    public bool IsDownloaded { get; set; }
    public string? LocalFilePath { get; set; }
    public bool IsInQueue { get; set; }
    public int QueuePosition { get; set; }
}

public enum EpisodePlayState
{
    New,
    InProgress,
    Played
}
