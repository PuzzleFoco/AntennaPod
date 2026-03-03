using System.ServiceModel.Syndication;
using System.Xml;
using AntennaPod.Core.Models;

namespace AntennaPod.Core.Services;

public class FeedParser
{
    private static readonly HttpClient SharedHttpClient = new();

    public async Task<Podcast> ParseFeedAsync(string feedUrl)
    {
        using var response = await SharedHttpClient.GetAsync(feedUrl);
        response.EnsureSuccessStatusCode();

        using var stream = await response.Content.ReadAsStreamAsync();
        using var reader = XmlReader.Create(stream);
        var feed = SyndicationFeed.Load(reader);

        var podcast = new Podcast
        {
            FeedUrl = feedUrl,
            Title = feed.Title?.Text ?? string.Empty,
            Description = feed.Description?.Text,
            Link = feed.Links.FirstOrDefault()?.Uri?.ToString(),
            Language = feed.Language,
            ImageUrl = feed.ImageUrl?.ToString(),
            LastUpdated = feed.LastUpdatedTime
        };

        // Extract author
        if (feed.Authors.Count > 0)
        {
            podcast.Author = feed.Authors[0].Name ?? feed.Authors[0].Email;
        }

        // Check for iTunes-style image in extensions
        if (string.IsNullOrEmpty(podcast.ImageUrl))
        {
            podcast.ImageUrl = GetITunesImage(feed.ElementExtensions);
        }

        // Parse episodes
        foreach (var item in feed.Items)
        {
            var episode = ParseEpisode(item);
            if (episode != null)
            {
                podcast.Episodes.Add(episode);
            }
        }

        return podcast;
    }

    private static Episode? ParseEpisode(SyndicationItem item)
    {
        var enclosure = item.Links.FirstOrDefault(l =>
            string.Equals(l.RelationshipType, "enclosure", StringComparison.OrdinalIgnoreCase));

        var episode = new Episode
        {
            Title = item.Title?.Text ?? string.Empty,
            Guid = item.Id,
            Description = item.Summary?.Text,
            PublishedDate = item.PublishDate,
            Link = item.Links.FirstOrDefault(l => l.RelationshipType != "enclosure")?.Uri?.ToString()
        };

        if (enclosure != null)
        {
            episode.MediaUrl = enclosure.Uri?.ToString();
            episode.MimeType = enclosure.MediaType;
            episode.MediaSize = enclosure.Length;
        }

        // Try to get duration from iTunes extensions
        var durationStr = GetExtensionValue(item.ElementExtensions, "duration");
        if (!string.IsNullOrEmpty(durationStr))
        {
            episode.Duration = ParseDuration(durationStr);
        }

        // Try to get episode image
        episode.ImageUrl = GetITunesImage(item.ElementExtensions);

        return episode;
    }

    private static string? GetITunesImage(SyndicationElementExtensionCollection extensions)
    {
        foreach (var ext in extensions)
        {
            if (ext.OuterName == "image" &&
                (ext.OuterNamespace == "http://www.itunes.com/dtds/podcast-1.0.dtd" ||
                 ext.OuterNamespace == ""))
            {
                try
                {
                    using var reader = ext.GetReader();
                    if (reader.MoveToAttribute("href"))
                    {
                        return reader.Value;
                    }
                }
                catch
                {
                    // Skip malformed extensions
                }
            }
        }
        return null;
    }

    private static string? GetExtensionValue(SyndicationElementExtensionCollection extensions, string name)
    {
        foreach (var ext in extensions)
        {
            if (ext.OuterName == name)
            {
                try
                {
                    using var reader = ext.GetReader();
                    reader.Read();
                    return reader.ReadContentAsString();
                }
                catch
                {
                    // Skip malformed extensions
                }
            }
        }
        return null;
    }

    internal static int ParseDuration(string duration)
    {
        if (string.IsNullOrWhiteSpace(duration))
            return 0;

        // Try HH:MM:SS or MM:SS format
        var parts = duration.Split(':');
        if (parts.Length == 3 &&
            int.TryParse(parts[0], out int hours) &&
            int.TryParse(parts[1], out int minutes) &&
            int.TryParse(parts[2], out int seconds))
        {
            return hours * 3600 + minutes * 60 + seconds;
        }

        if (parts.Length == 2 &&
            int.TryParse(parts[0], out minutes) &&
            int.TryParse(parts[1], out seconds))
        {
            return minutes * 60 + seconds;
        }

        // Try plain seconds
        if (int.TryParse(duration, out seconds))
        {
            return seconds;
        }

        return 0;
    }
}
