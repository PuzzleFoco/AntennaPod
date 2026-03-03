using Xunit;
using AntennaPod.Core.Models;

namespace AntennaPod.Core.Tests.Services;

public class EpisodeActionTests
{
    [Fact]
    public void CreatePlayAction_SetsCorrectFields()
    {
        var action = EpisodeAction.CreatePlayAction(
            "https://example.com/feed.xml",
            "https://example.com/ep.mp3",
            "test-guid",
            started: 100,
            position: 500,
            total: 3600);

        Assert.Equal("https://example.com/feed.xml", action.Podcast);
        Assert.Equal("https://example.com/ep.mp3", action.Episode);
        Assert.Equal("test-guid", action.Guid);
        Assert.Equal("play", action.Action);
        Assert.Equal(100, action.Started);
        Assert.Equal(500, action.Position);
        Assert.Equal(3600, action.Total);
        Assert.NotNull(action.Timestamp);
    }

    [Fact]
    public void CreateNewAction_SetsCorrectFields()
    {
        var action = EpisodeAction.CreateNewAction(
            "https://example.com/feed.xml",
            "https://example.com/ep.mp3",
            null);

        Assert.Equal("new", action.Action);
        Assert.Null(action.Guid);
        Assert.Null(action.Started);
        Assert.Null(action.Position);
    }

    [Fact]
    public void CreateDownloadAction_SetsCorrectFields()
    {
        var action = EpisodeAction.CreateDownloadAction(
            "https://example.com/feed.xml",
            "https://example.com/ep.mp3",
            "dl-guid");

        Assert.Equal("download", action.Action);
        Assert.Equal("dl-guid", action.Guid);
    }

    [Fact]
    public void CreateDeleteAction_SetsCorrectFields()
    {
        var action = EpisodeAction.CreateDeleteAction(
            "https://example.com/feed.xml",
            "https://example.com/ep.mp3",
            "del-guid");

        Assert.Equal("delete", action.Action);
    }
}
