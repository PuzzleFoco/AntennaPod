using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using AntennaPod.Core.Models;

namespace AntennaPod.Core.Services;

public class NextcloudSyncService : ISyncService, IDisposable
{
    private readonly HttpClient _httpClient;
    private readonly SyncCredentials _credentials;
    private readonly bool _ownsHttpClient;

    public NextcloudSyncService(SyncCredentials credentials, HttpClient? httpClient = null)
    {
        _credentials = credentials ?? throw new ArgumentNullException(nameof(credentials));

        if (string.IsNullOrEmpty(credentials.BaseUrl))
        {
            throw new ArgumentException("Nextcloud server URL is required.", nameof(credentials));
        }

        _ownsHttpClient = httpClient == null;
        _httpClient = httpClient ?? new HttpClient();
        _httpClient.BaseAddress = new Uri(credentials.BaseUrl.TrimEnd('/'));
        _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue(
            "Basic",
            Convert.ToBase64String(Encoding.UTF8.GetBytes($"{credentials.Username}:{credentials.Password}")));
        _httpClient.DefaultRequestHeaders.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));
    }

    public Task LoginAsync()
    {
        // Nextcloud uses basic auth on every request, no separate login needed
        return Task.CompletedTask;
    }

    public Task LogoutAsync()
    {
        return Task.CompletedTask;
    }

    public Task<List<GpodderDevice>> GetDevicesAsync()
    {
        // Nextcloud gPodder doesn't use device management
        return Task.FromResult(new List<GpodderDevice>());
    }

    public Task ConfigureDeviceAsync(string deviceId, string caption, string type)
    {
        // Nextcloud gPodder doesn't use device management
        return Task.CompletedTask;
    }

    public async Task<SubscriptionChanges> GetSubscriptionChangesAsync(long lastSync)
    {
        var response = await _httpClient.GetAsync(
            $"/index.php/apps/gpoddersync/subscriptions?since={lastSync}");
        response.EnsureSuccessStatusCode();

        var content = await response.Content.ReadAsStringAsync();
        return JsonSerializer.Deserialize<SubscriptionChanges>(content) ?? new SubscriptionChanges();
    }

    public async Task<UploadChangesResponse> UploadSubscriptionChangesAsync(
        List<string> added, List<string> removed)
    {
        var payload = JsonSerializer.Serialize(new { add = added, remove = removed });
        var content = new StringContent(payload, Encoding.UTF8, "application/json");
        var response = await _httpClient.PostAsync(
            "/index.php/apps/gpoddersync/subscription_change/create", content);
        response.EnsureSuccessStatusCode();

        var responseContent = await response.Content.ReadAsStringAsync();
        return JsonSerializer.Deserialize<UploadChangesResponse>(responseContent) ?? new UploadChangesResponse();
    }

    public async Task<EpisodeActionChanges> GetEpisodeActionChangesAsync(long lastSync)
    {
        var response = await _httpClient.GetAsync(
            $"/index.php/apps/gpoddersync/episode_action?since={lastSync}");
        response.EnsureSuccessStatusCode();

        var content = await response.Content.ReadAsStringAsync();
        return JsonSerializer.Deserialize<EpisodeActionChanges>(content) ?? new EpisodeActionChanges();
    }

    public async Task<UploadChangesResponse> UploadEpisodeActionsAsync(List<EpisodeAction> actions)
    {
        var payload = JsonSerializer.Serialize(actions);
        var content = new StringContent(payload, Encoding.UTF8, "application/json");
        var response = await _httpClient.PostAsync(
            "/index.php/apps/gpoddersync/episode_action/create", content);
        response.EnsureSuccessStatusCode();

        var responseContent = await response.Content.ReadAsStringAsync();
        return JsonSerializer.Deserialize<UploadChangesResponse>(responseContent) ?? new UploadChangesResponse();
    }

    public void Dispose()
    {
        if (_ownsHttpClient)
        {
            _httpClient.Dispose();
        }
    }
}
