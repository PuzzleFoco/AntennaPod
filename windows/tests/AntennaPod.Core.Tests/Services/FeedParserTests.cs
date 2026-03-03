using Xunit;
using AntennaPod.Core.Services;

namespace AntennaPod.Core.Tests.Services;

public class FeedParserTests
{
    [Theory]
    [InlineData("1:30:45", 5445)]
    [InlineData("30:45", 1845)]
    [InlineData("3600", 3600)]
    [InlineData("0:05:30", 330)]
    [InlineData("", 0)]
    [InlineData("invalid", 0)]
    public void ParseDuration_VariousFormats(string input, int expected)
    {
        var result = FeedParser.ParseDuration(input);
        Assert.Equal(expected, result);
    }
}
