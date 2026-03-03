using System.Text.Json.Serialization;

namespace AntennaPod.Core.Models;

public class SubscriptionChanges
{
    [JsonPropertyName("add")]
    public List<string> Added { get; set; } = new();

    [JsonPropertyName("remove")]
    public List<string> Removed { get; set; } = new();

    [JsonPropertyName("timestamp")]
    public long Timestamp { get; set; }
}

public class EpisodeActionChanges
{
    [JsonPropertyName("actions")]
    public List<EpisodeAction> Actions { get; set; } = new();

    [JsonPropertyName("timestamp")]
    public long Timestamp { get; set; }
}

public class UploadChangesResponse
{
    [JsonPropertyName("timestamp")]
    public long Timestamp { get; set; }

    [JsonPropertyName("update_urls")]
    public Dictionary<string, string>? UpdatedUrls { get; set; }
}

public class GpodderDevice
{
    [JsonPropertyName("id")]
    public string Id { get; set; } = string.Empty;

    [JsonPropertyName("caption")]
    public string Caption { get; set; } = string.Empty;

    [JsonPropertyName("type")]
    public string Type { get; set; } = "desktop";

    [JsonPropertyName("subscriptions")]
    public int Subscriptions { get; set; }
}
