using Xunit;
using AntennaPod.Core.Services;
using AntennaPod.Core.Models;

namespace AntennaPod.Core.Tests.Services;

public class OpmlServiceTests
{
    private readonly OpmlService _service = new();

    [Fact]
    public void ExportToOpml_ProducesValidOpml()
    {
        var podcasts = new List<Podcast>
        {
            new() { Title = "Podcast A", FeedUrl = "https://example.com/a.xml", Link = "https://example.com/a" },
            new() { Title = "Podcast B", FeedUrl = "https://example.com/b.xml" }
        };

        var opml = _service.ExportToOpml(podcasts);

        Assert.Contains("Podcast A", opml);
        Assert.Contains("https://example.com/a.xml", opml);
        Assert.Contains("Podcast B", opml);
        Assert.Contains("https://example.com/b.xml", opml);
        Assert.Contains("<opml", opml);
        Assert.Contains("version=\"2.0\"", opml);
    }

    [Fact]
    public void ImportFromOpml_ParsesEntries()
    {
        var opml = @"<?xml version=""1.0"" encoding=""UTF-8""?>
<opml version=""2.0"">
  <head><title>Test</title></head>
  <body>
    <outline type=""rss"" text=""Pod A"" title=""Pod A"" xmlUrl=""https://example.com/a.xml"" htmlUrl=""https://example.com/a"" />
    <outline type=""rss"" text=""Pod B"" title=""Pod B"" xmlUrl=""https://example.com/b.xml"" />
  </body>
</opml>";

        var entries = _service.ImportFromOpml(opml);

        Assert.Equal(2, entries.Count);
        Assert.Equal("Pod A", entries[0].Title);
        Assert.Equal("https://example.com/a.xml", entries[0].FeedUrl);
        Assert.Equal("https://example.com/a", entries[0].HtmlUrl);
        Assert.Equal("Pod B", entries[1].Title);
        Assert.Null(entries[1].HtmlUrl);
    }

    [Fact]
    public void RoundTrip_ExportThenImport()
    {
        var original = new List<Podcast>
        {
            new() { Title = "Round Trip", FeedUrl = "https://example.com/rt.xml", Link = "https://example.com/rt" }
        };

        var opml = _service.ExportToOpml(original);
        var imported = _service.ImportFromOpml(opml);

        Assert.Single(imported);
        Assert.Equal("Round Trip", imported[0].Title);
        Assert.Equal("https://example.com/rt.xml", imported[0].FeedUrl);
    }
}
