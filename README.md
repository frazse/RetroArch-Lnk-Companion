# RetroArch Lnk Companion

A lightweight Android dashboard for RetroArch that displays live telemetry and achievements in a non-intrusive "Passive HUD" mode.

## Compatibility

This companion app is designed to work specifically with these forks:
- [dolphin-lnk](https://github.com/frazse/dolphin-lnk)
- [retroarch-lnk](https://github.com/frazse/retroarch-lnk)

## Features

- **Live Telemetry**: Real-time display of FPS, Frametime, CPU/GPU utilization, and Battery status.
- **Achievement Tracking**: Dynamic list of achievements with progress bars and status indicators.
- **Passive HUD Mode**: The app is configured to be non-focusable and non-interactive, preventing it from intercepting controller input or taking focus away from RetroArch.
- **UDP Listener**: Listens for JSON packets on port `55432` for seamless integration with RetroArch Lnk.
- **Material Styling**: Clean, dark-themed UI that aligns perfectly across different screen sizes.

## How it Works

The app hosts a full-screen `WebView` and runs a background thread with a `DatagramSocket`. When a JSON packet is received from RetroArch, it is injected into the dashboard via JavaScript.

### Passive Mode Implementation
To ensure it acts as a true HUD, the app uses:
- `FLAG_NOT_FOCUSABLE`: Prevents the window from gaining input focus.
- Overridden `dispatchKeyEvent` and `dispatchGenericMotionEvent`: Consumes all controller/key events.

## Setup & Usage

1. **Install**: Build and deploy the APK to your Android device.
2. **Network**: Ensure your device and the host running RetroArch are on the same network.
3. **RetroArch Configuration**: Configure your RetroArch Lnk setup to send UDP packets to the IP of your Android device on port `55432`.

## Development

### Prerequisites
- Android Studio Ladybug (or newer)
- Android SDK 24+

### Build
```bash
./gradlew assembleDebug
```

## JSON Schema Example
The app expects UDP packets in the following format:
```json
{
  "game_title": "The Legend of Zelda: A Link to the Past",
  "fps": 60.0,
  "frametime": 16.6,
  "cpu_util": 25,
  "gpu_util": 15,
  "battery": 80,
  "achievements": [
    {
      "title": "Master Sword",
      "description": "Obtain the legendary blade.",
      "unlocked": true,
      "badge_url": "https://...",
      "points": 10
    }
  ]
}
```
