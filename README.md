# Music Player Android App


A modern, feature-rich music player for Android, built entirely with Kotlin and Jetpack Compose. This application leverages the MVVM architecture to provide a clean, scalable, and maintainable codebase. It offers a beautiful user experience with Material Design 3, dynamic theming (Light, Dark, and Glassy), and a comprehensive set of features for local music playback.

## 📱 Screenshots

<div align="center">

### 🔑 Authentication & Settings
<table>
  <tr>
    <td align="center">
      <img src="ScreenShots/Login.png" width="220"/>
      <br><b>Login Screen</b>
    </td>
    <td align="center">
      <f:\MusicPlayer\ScreenShots\CreateAccount.jpg width="220"/>
      <br><b>Create Account</b>
    </td>
    <td align="center">
      <f:\MusicPlayer\ScreenShots\Equlizer.jpg width="220"/>
      <br><b>Equalizer</b>
    </td>
  </tr>
</table>

### 🎶 Library & Discovery
<table>
  <tr>
    <td align="center">
      <f:\MusicPlayer\ScreenShots\LightLibrary.jpg" width="220"/>
      <br><b>Light Library</b>
    </td>
    <td align="center">
      <f:\MusicPlayer\ScreenShots\DarkLibrary.jpg width="220"/>
      <br><b>Dark Library</b>
    </td>
    <td align="center">
      <f:\MusicPlayer\ScreenShots\Artists.jpgwidth="220"/>
      <br><b>Artists View</b>
    </td>
  </tr>
</table>

### ▶️ Playback Experience
<table>
  <tr>
    <td align="center">
      <f:\MusicPlayer\ScreenShots\LightPlayer.jpg width="220"/>
      <br><b>Light Player</b>
    </td>
    <td align="center">
      <f:\MusicPlayer\ScreenShots\DarkPlayer.jpg width="220"/>
      <br><b>Dark Player</b>
    </td>
    <td align="center">
      <f:\MusicPlayer\ScreenShots\playscreenglassy.jpg width="220"/>
      <br><b>Glassy Theme</b>
    </td>
  </tr>
</table>

### ❤️ Favorites & Immersive UI
<table>
  <tr>
    <td align="center">
      <f:\MusicPlayer\ScreenShots\Darkfaivourite.jpg width="220"/>
      <br><b>Favorites</b>
    </td>
    <td align="center">
      <f:\MusicPlayer\ScreenShots\playscreen1.png width="220"/>
      <br><b>Playback Visualization</b>
    </td>
    <td align="center">
      <f:\MusicPlayer\ScreenShots\playscreen2.png width="220"/>
      <br><b>Immersive Controls</b>
    </td>
  </tr>
</table>

</div>

## ✨ Features

*   **Authentication**: Secure user login and registration system with session management. Includes a "Forgot Password" feature.
*   **Music Library**: Automatically scans and displays all local audio files from the device's `MediaStore`.
*   **Sorting & Searching**: Sort your music library by Title, Artist, or Date Added. Instantly search across your library, favorites, playlists, and artists.
*   **Playback Control**: Full playback functionality including play/pause, skip next/previous, seek, and a mini-player for background control.
*   **Queue Management**: Play songs from a list, starting at any track.
*   **Favorites**: Add or remove songs from a dedicated favorites list, accessible from its own screen.
*   **Playlists**: Create, rename, and delete an unlimited number of custom playlists. Easily add or remove songs.
*   **Artists View**: Automatically groups songs by artist, displaying artist-specific views with their respective songs.
*   **Advanced Player UI**:
    *   Immersive full-screen player that dynamically adapts its color scheme based on the album art.
    *   Animated playback visualizer.
    *   Control playback speed (0.5x to 2.0x).
    *   Shuffle and Repeat (off, repeat one, repeat all) modes.
*   **Theming**:
    *   **Light Mode**: A clean, bright interface.
    *   **Dark Mode**: An elegant, eye-friendly theme.
    *   **Glassy Mode**: A premium, frosted-glass effect with dynamic colors.
*   **Built-in Equalizer**: A powerful 5-band audio equalizer with multiple presets (e.g., Pop, Rock, Jazz) and custom band level controls.
*   **File Management**: Multi-select songs to share, delete, or add to a playlist. Rename individual song titles.

## 🛠️ Tech Stack & Architecture

This project is built with a modern Android tech stack, following the **MVVM (Model-View-ViewModel)** architecture pattern.

*   **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) for building the entire UI declaratively.
*   **Architecture**: MVVM to separate UI logic from business logic.
*   **Dependency Injection**: [Hilt](https://developer.android.com/training/dependency-injection/hilt-android) for managing dependencies and scopes.
*   **Asynchronous Programming**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) and [Flow](https://kotlinlang.org/docs/flow.html) for managing background tasks and data streams.
*   **Navigation**: [Navigation Compose](https://developer.android.com/jetpack/compose/navigation) for navigating between screens.
*   **Database**: [Room](https://developer.android.com/training/data-storage/room) for persisting user data, playlists, and favorites.
*   **Data Persistence**: [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) for managing user sessions.
*   **Media Playback**: [Media3 (ExoPlayer)](https://developer.android.com/guide/topics/media/media3) for robust audio playback and background media session management.
*   **Image Loading**: [Coil](https://coil-kt.github.io/coil/) for efficiently loading and caching album art.

## 🚀 Getting Started

### Prerequisites

*   Android Studio Iguana or newer
*   Android SDK 26 (Oreo) or higher

### Installation

1.  Clone the repository:
    ```bash
    git clone https://github.com/amirkhedri/Music-Player-Android-App.git
    ```
2.  Open the project in Android Studio.
3.  Let Gradle sync the project dependencies.
4.  Run the application on an Android emulator or a physical device.

The app will request permissions to read audio files from your device on the first launch. Please grant these to populate the music library.

## 📁 Project Structure

```
.
└── app/
    ├── src/
    │   ├── main/
    │   │   ├── java/com/example/musicplayer/
    │   │   │   ├── data/
    │   │   │   │   ├── local/        # Room Database, DAOs, Entities, and DataStore SessionManager
    │   │   │   │   ├── model/        # Data models like Song
    │   │   │   │   └── repository/   # Repositories for data access (Auth, Audio, Playlist)
    │   │   │   ├── di/               # Hilt dependency injection modules
    │   │   │   ├── player/           # Media3/ExoPlayer integration and services
    │   │   │   ├── ui/
    │   │   │   │   ├── navigation/   # AppNavGraph for navigation routes
    │   │   │   │   ├── screens/      # Composable screens (Login, Library, Player)
    │   │   │   │   └── theme/        # Color schemes, typography, and theme setup
    │   │   │   └── viewmodel/        # ViewModels for each screen/feature
    │   │   ├── AndroidManifest.xml   # App permissions and services declaration
    │   │   └── res/                  # App resources (drawables, styles)
    └── build.gradle.kts              # App-level dependencies
```

## 📄 License

This project is licensed under the MIT License. See the `LICENSE` file for details.