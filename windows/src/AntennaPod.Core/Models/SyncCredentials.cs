namespace AntennaPod.Core.Models;

public class SyncCredentials
{
    public SyncProvider Provider { get; set; }
    public string BaseUrl { get; set; } = string.Empty;
    public string Username { get; set; } = string.Empty;
    public string Password { get; set; } = string.Empty;
    public string DeviceId { get; set; } = string.Empty;
}

public enum SyncProvider
{
    None,
    GpodderNet,
    NextcloudGpodder
}
