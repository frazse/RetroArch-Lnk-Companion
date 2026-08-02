# RetroAchievements Companion (Passive HUD)

A high-performance Android "Passive HUD" companion for RetroArch and standalone emulators. It displays real-time telemetry, RetroAchievements progress, and career stats in a non-intrusive dashboard.

## Features

### 1. Dual Telemetry Modes
- **High-Speed UDP**: Built-in listener (Port `55432`) for seamless integration with `-lnk` forks (RetroArch, Dolphin, PPSSPP).
- **Universal Rich Presence**: Automatic HUD activation for any standalone emulator (DuckStation, AetherSX2, etc.) by syncing with RetroAchievements Rich Presence.

### 2. Shizuku-Powered Privileged Telemetry
Integrates with the **Shizuku** service to provide high-precision system stats without root or bridge scripts:
- **Zero-Setup Stats**: Get real-time **FPS**, **Power (Watts)**, and **GPU/CPU Temperatures** just by authorizing Shizuku in the app settings.
- **Rootless Performance**: Accesses `dumpsys` and system files via Shizuku's secure shell context for desktop-class telemetry accuracy.

### 3. Smart Contextual Achievement Sorting
Uses **Rich Presence** data to intelligently re-order your achievement list:
- **📍 Relevant Now**: Achievements that mention your current location (e.g., "Plains of Rohan") automatically jump to the very top of the HUD.
- **Exclusive Matching**: Smart engine extracts full location phrases and filters out "noise" (difficulty levels, character names) for 100% accuracy.
- **Menu Detection**: Automatically reverts to standard sorting when you enter a game's menu.

### 4. Battery-First Architecture
- **Deep Sleep Mode**: The entire telemetry engine (scripts, file reads, socket processing) completely shuts down when no game is active.
- **Adaptive Throttling**: Background watchers slow down when idle to minimize CPU wake-ups.
- **Hybrid Detection**: Uses a mix of Usage Stats and Shizuku for the most efficient foreground app monitoring.

### 5. RetroAchievements Integration
- **Career Dashboard**: Real-time display of Points, Rank, and Award counts (Beaten/Mastered) synced directly with your profile.
- **User Completion Progress**: Detailed history view of all played games with progress bars, filtering by system, and advanced sorting.
- **Mastery Experience**: 
    - **"Mastered!" 👑 HUD Badge**: A permanent status badge that appears at the top of the HUD when playing 100% completed games.
    - **Premium Styling**: Mastered games in your history feature a golden border, gradient background, and glow to match the site's prestige.
- **Achievement of the Week**: Real-time countdown ("Ends in X days") and unlock status (✅ indicator) synced with global AOTW.

### 6. True "Passive" HUD & Stability
- **Passive Mode**: Prevents the window from stealing focus or intercepting controller input.
- **Surgical UI Updates**: Optimized rendering engine ensures stats update in real-time without flickering or closing open dropdowns.
- **Scrollable HUD**: A unified list that supports vertical scrolling for large achievement sets while keeping the telemetry grid anchored.

## Setup & Usage

1. **Install**: Build and deploy the APK to your Android device (Odin 2, RP4Pro, etc.).
2. **Shizuku (Recommended)**: For the best experience, install and start the **Shizuku** app. Open Companion Settings and tap "Authorize Shizuku" for precise FPS/Power stats.
3. **Usage Access**: Open Settings in the app and tap the banner to grant "Usage Access" (required for automatic HUD activation).
4. **Trigger Apps**: Select your emulators in the "Trigger Apps" dropdown.

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
