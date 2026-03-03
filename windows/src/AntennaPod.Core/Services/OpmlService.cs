using System.Text;
using System.Xml;
using AntennaPod.Core.Models;

namespace AntennaPod.Core.Services;

public class OpmlService
{
    public string ExportToOpml(IEnumerable<Podcast> podcasts, string title = "AntennaPod Subscriptions")
    {
        var sb = new StringBuilder();
        using var writer = XmlWriter.Create(sb, new XmlWriterSettings
        {
            Indent = true,
            Encoding = Encoding.UTF8
        });

        writer.WriteStartDocument();
        writer.WriteStartElement("opml");
        writer.WriteAttributeString("version", "2.0");

        // Head
        writer.WriteStartElement("head");
        writer.WriteElementString("title", title);
        writer.WriteElementString("dateCreated", DateTimeOffset.UtcNow.ToString("R"));
        writer.WriteEndElement();

        // Body
        writer.WriteStartElement("body");
        foreach (var podcast in podcasts)
        {
            writer.WriteStartElement("outline");
            writer.WriteAttributeString("type", "rss");
            writer.WriteAttributeString("text", podcast.Title);
            writer.WriteAttributeString("title", podcast.Title);
            writer.WriteAttributeString("xmlUrl", podcast.FeedUrl);
            if (!string.IsNullOrEmpty(podcast.Link))
            {
                writer.WriteAttributeString("htmlUrl", podcast.Link);
            }
            writer.WriteEndElement();
        }
        writer.WriteEndElement(); // body

        writer.WriteEndElement(); // opml
        writer.WriteEndDocument();
        writer.Flush();

        return sb.ToString();
    }

    public List<OpmlEntry> ImportFromOpml(string opmlContent)
    {
        var entries = new List<OpmlEntry>();
        var doc = new XmlDocument();
        doc.LoadXml(opmlContent);

        var outlines = doc.GetElementsByTagName("outline");
        foreach (XmlNode outline in outlines)
        {
            var xmlUrl = outline.Attributes?["xmlUrl"]?.Value;
            if (!string.IsNullOrEmpty(xmlUrl))
            {
                entries.Add(new OpmlEntry
                {
                    Title = outline.Attributes?["title"]?.Value
                            ?? outline.Attributes?["text"]?.Value
                            ?? string.Empty,
                    FeedUrl = xmlUrl,
                    HtmlUrl = outline.Attributes?["htmlUrl"]?.Value
                });
            }
        }

        return entries;
    }
}

public class OpmlEntry
{
    public string Title { get; set; } = string.Empty;
    public string FeedUrl { get; set; } = string.Empty;
    public string? HtmlUrl { get; set; }
}
