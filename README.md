# RetroAchievements Companion (Passive HUD)

A high-performance Android "Passive HUD" companion for RetroArch and standalone emulators. It displays real-time telemetry, RetroAchievements progress, and career stats in a non-intrusive dashboard.

## Features

### 1. Dual Telemetry Modes
- **High-Speed UDP**: Built-in listener (Port `55432`) for seamless integration with `-lnk` forks (RetroArch, Dolphin, PPSSPP).
- **Universal Rich Presence**: Automatic HUD activation for any standalone emulator (DuckStation, AetherSX2, etc.) by syncing with RetroAchievements Rich Presence.

### 2. Privileged "Rootless" Telemetry
Ported from performance tools to provide deep system stats without traditional root:
- **Global FPS & Frametime**: High-precision calculation using `dumpsys SurfaceFlinger` with native Kotlin parsing for maximum performance.
- **Power Monitoring**: Real-time power draw display in **Watts (W)**.
- **System Load**: Per-core CPU utilization, GPU load, and temperatures.

### 3. Battery-First Architecture
- **Deep Sleep Mode**: The entire telemetry engine (scripts, file reads, socket processing) completely shuts down when no game is active.
- **Adaptive Throttling**: The background app watcher slows down when idle to minimize CPU wake-ups.
- **UDP Priority Lock**: High-speed UDP streams automatically suppress slow periodic polling for 10 seconds to eliminate jitter.

### 4. RetroAchievements Integration
- **Career Dashboard**: Real-time display of Points, Rank, and Award counts (Beaten/Mastered) synced directly with your profile.
- **User Completion Progress**: Detailed history view of all played games with progress bars, filtering by system, and advanced sorting.
- **Mastery Experience**: 
    - **"Mastered!" 👑 HUD Badge**: A permanent status badge that appears at the top of the HUD when playing 100% completed games.
    - **Premium Styling**: Mastered games in your history feature a golden border, gradient background, and glow to match the site's prestige.
- **Achievement of the Week**: Real-time countdown ("Ends in X days") and unlock status (✅ indicator) synced with global AOTW.

### 5. True "Passive" HUD & Stability
- **Passive Mode**: Prevents the window from stealing focus or intercepting controller input.
- **Surgical UI Updates**: Optimized rendering engine ensures stats update in real-time without flickering or closing open dropdowns.
- **Logout & Privacy**: Secure Logout in settings with a confirmation prompt to clear all local credentials.
- **Remote Debugging**: WebView debugging enabled for Debug builds via `chrome://inspect`.

## Setup & Usage

1. **Install**: Build and deploy the APK to your Android device (Odin 2, RP4Pro, etc.).
2. **Usage Access**: Open Settings in the app and tap the red banner to grant "Usage Access" (required for automatic HUD activation).
3. **Trigger Apps**: Select your emulators in the "Trigger Apps" dropdown.
4. **RetroArch**: Configure your RetroArch Lnk setup to send UDP packets to the device IP on port `55432`.

## JSON Schema (UDP)
The app expects UDP packets in the following format:
```json
{
  "game_title": "The Legend of Zelda: A Link to the Past",
  "fps": 60.0,
  "frametime": 16.6,
  "cpu_util": 25,
  "gpu_util": 15,
  "battery": 80,
  "power_w": 2.8,
  "achievements": [...]
}
```
