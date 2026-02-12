# APK Build und Release Workflow

Dieses Dokument beschreibt, wie man installierbare APK-Dateien für AntennaPod erstellt und verteilt.

## Automatische Builds

Das Repository ist jetzt mit einem GitHub Actions Workflow konfiguriert, der automatisch APK-Dateien erstellt.

### Wann werden Builds ausgelöst?

1. **Bei jedem Push zu master/develop**: APKs werden automatisch gebaut
2. **Bei Pull Requests**: APKs werden gebaut (für Testing)
3. **Bei Version Tags**: APKs werden gebaut und als GitHub Release veröffentlicht
4. **Manuell**: Workflow kann manuell über GitHub Actions gestartet werden

## Gebaute APK-Varianten

Der Workflow erstellt folgende APK-Dateien:

### Haupt-App (AntennaPod):
1. **AntennaPod-play-debug.apk** - Play Store Variante (Debug)
2. **AntennaPod-play-release.apk** - Play Store Variante (Release)
3. **AntennaPod-free-release.apk** - F-Droid Variante (Release)

### Wear OS App:
4. **AntennaPod-wear-debug.apk** - Wear OS App (Debug)
5. **AntennaPod-wear-release.apk** - Wear OS App (Release)

## APKs Herunterladen

### Von GitHub Actions

1. Gehe zu **Actions** tab im Repository
2. Wähle einen Workflow-Lauf aus
3. Scrolle zu **Artifacts** am Ende der Seite
4. Lade die gewünschte APK herunter

### Von GitHub Releases (für Tags)

1. Gehe zu **Releases** im Repository
2. Wähle die gewünschte Version
3. Lade die APK aus den **Assets** herunter

## Manuellen Build Starten

1. Gehe zu **Actions** tab
2. Wähle **"Build and Release APKs"** Workflow
3. Klicke auf **"Run workflow"**
4. Wähle den Branch (master/develop)
5. Klicke auf **"Run workflow"**
6. Warte bis der Build fertig ist
7. Lade die APKs aus den Artifacts herunter

## Release Erstellen

Um ein neues Release mit APKs zu erstellen:

```bash
# Version Tag erstellen
git tag -a v3.11.1 -m "Version 3.11.1"
git push origin v3.11.1
```

Der Workflow wird automatisch:
1. Alle APKs bauen
2. Ein GitHub Release erstellen
3. Die APKs zum Release hinzufügen
4. Release Notes generieren

## Installation auf Android-Geräten

### Haupt-App Installation

1. Lade `AntennaPod-play-debug.apk` oder `AntennaPod-free-release.apk` herunter
2. Aktiviere "Installation aus unbekannten Quellen" in den Android-Einstellungen
3. Öffne die APK-Datei und installiere sie

### Wear OS App Installation

**Option 1: Via ADB**
```bash
adb install AntennaPod-wear-debug.apk
```

**Option 2: Via Wear OS Companion App**
1. Übertrage die APK auf dein Telefon
2. Installiere sie mit einer File Manager App
3. Die App wird automatisch auf die Uhr synchronisiert (wenn konfiguriert)

**Option 3: Direkt auf der Uhr**
1. Aktiviere ADB Debugging auf der Wear OS Uhr
2. Verbinde die Uhr via ADB (USB oder Wi-Fi)
3. Installiere mit `adb install`

## Technische Details

### Android Gradle Plugin (AGP)

- **Version**: 8.5.2 (stabile Version)
- **Gradle Version**: 8.13
- **Java Version**: 21

### Build-Konfiguration

Der Workflow:
- Nutzt Ubuntu Latest als Build-Umgebung
- Cached Gradle Dependencies für schnellere Builds
- Erstellt temporäre Release Keystores für unsignierte Builds
- Lädt alle APKs als Artifacts hoch
- Timeout: 60 Minuten

### Signierung

**Wichtig**: Die in GitHub Actions gebauten APKs sind:
- Debug-Builds: Mit Debug-Keystore signiert
- Release-Builds: **UNSIGNED** (müssen vor Veröffentlichung signiert werden)

Für signierte Release-Builds sollten Sie:
1. Ein echtes Keystore verwenden
2. Die Secrets in GitHub konfigurieren
3. Den Workflow anpassen um das Keystore zu nutzen

## Fehlerbehebung

### Build schlägt fehl

1. Prüfe die Build-Logs in GitHub Actions
2. Stelle sicher, dass alle Dependencies verfügbar sind
3. Prüfe ob AGP 8.5.2 mit allen Modulen kompatibel ist

### APK lässt sich nicht installieren

1. **"App nicht installiert"**: Deinstalliere alte Versionen zuerst
2. **"Unbekannte Quellen"**: Aktiviere Installation aus unbekannten Quellen
3. **"Signatur stimmt nicht überein"**: Alte Version hat anderen Keystore

### Wear OS App startet nicht

1. Prüfe ob Wear OS Version >= 2.0 ist (API 26+)
2. Stelle sicher dass alle Permissions erteilt wurden
3. Prüfe Logcat für Fehler: `adb logcat | grep AntennaPod`

## Weiterführende Links

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Android Build Variants](https://developer.android.com/studio/build/build-variants)
- [Wear OS Development](https://developer.android.com/training/wearables)
- [ADB Installation Guide](https://developer.android.com/studio/command-line/adb)
