using System.Net;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using AntennaPod.Core.Models;

namespace AntennaPod.Core.Services;

public class GpodderSyncService : ISyncService, IDisposable
{
    private readonly HttpClient _httpClient;
    private readonly SyncCredentials _credentials;
    private const int MaxActionsPerRequest = 30;

    public GpodderSyncService(SyncCredentials credentials, HttpClient? httpClient = null)
    {
        _credentials = credentials ?? throw new ArgumentNullException(nameof(credentials));

        var baseUrl = string.IsNullOrEmpty(credentials.BaseUrl)
            ? "https://gpodder.net"
            : credentials.BaseUrl.TrimEnd('/');

        var handler = new HttpClientHandler { CookieContainer = new CookieContainer() };
        _httpClient = httpClient ?? new HttpClient(handler);
        _httpClient.BaseAddress = new Uri(baseUrl);
        _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue(
            "Basic",
            Convert.ToBase64String(Encoding.UTF8.GetBytes($"{credentials.Username}:{credentials.Password}")));
    }

    public async Task LoginAsync()
    {
        var response = await _httpClient.PostAsync(
            $"/api/2/auth/{Uri.EscapeDataString(_credentials.Username)}/login.json", null);
        response.EnsureSuccessStatusCode();
    }

    public async Task LogoutAsync()
    {
        var response = await _httpClient.PostAsync(
            $"/api/2/auth/{Uri.EscapeDataString(_credentials.Username)}/logout.json", null);
        response.EnsureSuccessStatusCode();
    }

    public async Task<List<GpodderDevice>> GetDevicesAsync()
    {
        var response = await _httpClient.GetAsync(
            $"/api/2/devices/{Uri.EscapeDataString(_credentials.Username)}.json");
        response.EnsureSuccessStatusCode();

        var content = await response.Content.ReadAsStringAsync();
        return JsonSerializer.Deserialize<List<GpodderDevice>>(content) ?? new List<GpodderDevice>();
    }

    public async Task ConfigureDeviceAsync(string deviceId, string caption, string type)
    {
        var payload = JsonSerializer.Serialize(new { caption, type });
        var content = new StringContent(payload, Encoding.UTF8, "application/json");
        var response = await _httpClient.PostAsync(
            $"/api/2/devices/{Uri.EscapeDataString(_credentials.Username)}/{Uri.EscapeDataString(deviceId)}.json",
            content);
        response.EnsureSuccessStatusCode();
    }

    public async Task<SubscriptionChanges> GetSubscriptionChangesAsync(long lastSync)
    {
        var response = await _httpClient.GetAsync(
            $"/api/2/subscriptions/{Uri.EscapeDataString(_credentials.Username)}/{Uri.EscapeDataString(_credentials.DeviceId)}.json?since={lastSync}");
        response.EnsureSuccessStatusCode();

        var content = await response.Content.ReadAsStringAsync();
        var result = JsonSerializer.Deserialize<SubscriptionChanges>(content) ?? new SubscriptionChanges();

        // gPodder.net may escape colons in URLs unnecessarily - fix them
        result.Added = result.Added.Select(UnescapeUrl).ToList();
        result.Removed = result.Removed.Select(UnescapeUrl).ToList();

        return result;
    }

    public async Task<UploadChangesResponse> UploadSubscriptionChangesAsync(
        List<string> added, List<string> removed)
    {
        var payload = JsonSerializer.Serialize(new { add = added, remove = removed });
        var content = new StringContent(payload, Encoding.UTF8, "application/json");
        var response = await _httpClient.PostAsync(
            $"/api/2/subscriptions/{Uri.EscapeDataString(_credentials.Username)}/{Uri.EscapeDataString(_credentials.DeviceId)}.json",
            content);
        response.EnsureSuccessStatusCode();

        var responseContent = await response.Content.ReadAsStringAsync();
        return JsonSerializer.Deserialize<UploadChangesResponse>(responseContent) ?? new UploadChangesResponse();
    }

    public async Task<EpisodeActionChanges> GetEpisodeActionChangesAsync(long lastSync)
    {
        var response = await _httpClient.GetAsync(
            $"/api/2/episodes/{Uri.EscapeDataString(_credentials.Username)}.json?since={lastSync}");
        response.EnsureSuccessStatusCode();

        var content = await response.Content.ReadAsStringAsync();
        return JsonSerializer.Deserialize<EpisodeActionChanges>(content) ?? new EpisodeActionChanges();
    }

    public async Task<UploadChangesResponse> UploadEpisodeActionsAsync(List<EpisodeAction> actions)
    {
        UploadChangesResponse? lastResponse = null;

        // Split into chunks to avoid overwhelming the server
        for (int i = 0; i < actions.Count; i += MaxActionsPerRequest)
        {
            var chunk = actions.Skip(i).Take(MaxActionsPerRequest).ToList();
            var payload = JsonSerializer.Serialize(chunk);
            var content = new StringContent(payload, Encoding.UTF8, "application/json");
            var response = await _httpClient.PostAsync(
                $"/api/2/episodes/{Uri.EscapeDataString(_credentials.Username)}.json", content);
            response.EnsureSuccessStatusCode();

            var responseContent = await response.Content.ReadAsStringAsync();
            lastResponse = JsonSerializer.Deserialize<UploadChangesResponse>(responseContent);
        }

        return lastResponse ?? new UploadChangesResponse();
    }

    private static string UnescapeUrl(string url)
    {
        // gPodder.net escapes colons to %3A unnecessarily
        return url.Replace("%3A", ":", StringComparison.OrdinalIgnoreCase);
    }

    public void Dispose()
    {
        _httpClient.Dispose();
    }
}
