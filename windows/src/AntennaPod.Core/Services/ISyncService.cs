using AntennaPod.Core.Models;

namespace AntennaPod.Core.Services;

public interface ISyncService
{
    Task LoginAsync();
    Task LogoutAsync();
    Task<List<GpodderDevice>> GetDevicesAsync();
    Task ConfigureDeviceAsync(string deviceId, string caption, string type);
    Task<SubscriptionChanges> GetSubscriptionChangesAsync(long lastSync);
    Task<UploadChangesResponse> UploadSubscriptionChangesAsync(List<string> added, List<string> removed);
    Task<EpisodeActionChanges> GetEpisodeActionChangesAsync(long lastSync);
    Task<UploadChangesResponse> UploadEpisodeActionsAsync(List<EpisodeAction> actions);
}
