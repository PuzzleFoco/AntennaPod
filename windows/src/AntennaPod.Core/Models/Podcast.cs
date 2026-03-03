namespace AntennaPod.Core.Models;

public class Podcast
{
    public long Id { get; set; }
    public string Title { get; set; } = string.Empty;
    public string FeedUrl { get; set; } = string.Empty;
    public string? Description { get; set; }
    public string? Author { get; set; }
    public string? ImageUrl { get; set; }
    public string? Link { get; set; }
    public string? Language { get; set; }
    public DateTimeOffset? LastUpdated { get; set; }
    public DateTimeOffset SubscribedDate { get; set; } = DateTimeOffset.UtcNow;
    public List<Episode> Episodes { get; set; } = new();
}
