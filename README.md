<div align="center">

# 🎵 Music Player

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=android&logoColor=white)
![Material Design](https://img.shields.io/badge/Material%20Design%203-E52592?style=for-the-badge&logo=materialdesign&logoColor=white)
![Architecture](https://img.shields.io/badge/Architecture-MVVM-orange?style=for-the-badge)

A modern, feature-rich Android Music Player built entirely with **Kotlin** and **Jetpack Compose**, following the **MVVM Architecture**. The application provides an elegant UI, playlist management, artist browsing, favorites, local music playback, and a beautiful user experience with Material Design 3.

</div>

---

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
<img src="ScreenShots/Create Account.png" width="220"/>
<br><b>Create Account</b>
</td>
<td align="center">
<img src="ScreenShots/Equlizer.png" width="220"/>
<br><b>Equalizer</b>
</td>
</tr>
</table>

### 🎶 Library & Discovery

<table>
<tr>
<td align="center">
<img src="ScreenShots/Light Library.png" width="220"/>
<br><b>Light Library</b>
</td>
<td align="center">
<img src="ScreenShots/Dark Library.png" width="220"/>
<br><b>Dark Library</b>
</td>
<td align="center">
<img src="ScreenShots/Artists.png" width="220"/>
<br><b>Artists View</b>
</td>
</tr>
</table>

### ▶️ Playback Experience

<table>
<tr>
<td align="center">
<img src="ScreenShots/Light Player.png" width="220"/>
<br><b>Light Player</b>
</td>
<td align="center">
<img src="ScreenShots/Dark Player.png" width="220"/>
<br><b>Dark Player</b>
</td>
<td align="center">
<img src="ScreenShots/playscreen glassy (2).png" width="220"/>
<br><b>Glassy Theme</b>
</td>
</tr>
</table>

### ❤️ Favorites & Immersive UI

<table>
<tr>
<td align="center">
<img src="ScreenShots/Dark favourite.png" width="220"/>
<br><b>Favorites</b>
</td>
<td align="center">
<img src="ScreenShots/playscreen1.png" width="220"/>
<br><b>Playback Visualization</b>
</td>
<td align="center">
<img src="ScreenShots/playscreen2.png" width="220"/>
<br><b>Immersive Controls</b>
</td>
</tr>
</table>

</div>

---

# ✨ Features

### 🎼 Music Library
- Scan and display all local audio files
- Beautiful song cards with album artwork
- Sort songs by Name, Artist, and Date Added
- Instant search
- Multi-selection mode
- Rename, delete, and share songs

---

### ❤️ Favorites
- Add/remove favorite songs
- Dedicated Favorites screen
- Search favorites instantly

---

### 👨‍🎤 Artists
- Automatically groups songs by artist
- Artist artwork support
- Artist detail page
- Search artists and search songs inside each artist

---

### 🎶 Playlists
- Create unlimited playlists
- Rename and delete playlists
- Add/remove songs from playlists
- Playlist search
- Beautiful playlist artwork

---

### ▶️ Music Playback
- Play/Pause, Skip Next, Skip Previous
- Custom Built-in Equalizer
- Queue Playback
- Mini Player
- Currently Playing Indicator
- Animated Playing Visualizer
- Album Artwork Support

---

### 🌙 UI & UX
- Material Design 3
- Dynamic Light/Dark Theme & Premium Glassy Theme
- Smooth Animations
- Responsive Layout
- Beautiful Card Design
- Modern Bottom Navigation
- Search Animations

---

### 🔐 Authentication
- Login
- Create Account / Register
- Logout
- Session Management

---

## 🏗 Architecture

The project follows the **MVVM (Model-View-ViewModel)** architecture.

```text
Presentation
│
├── UI (Jetpack Compose)
├── ViewModels
│
Domain
│
├── Repository
├── Business Logic
│
Data
│
├── Room Database
├── MediaStore
├── Preferences
└── Local Storage
🛠 Built WithKotlin  Jetpack Compose  Material Design 3  MVVM Architecture  Hilt (Dependency Injection)  Kotlin Coroutines  StateFlow  Navigation Compose  Room Database  Coil  Android MediaStore  Android Storage Access Framework  ExoPlayer / Media3  DataStore  AndroidX  📂 Project StructurePlaintextapp/
│
├── data/
│   ├── model/
│   ├── repository/
│   └── local/
│
├── ui/
│   ├── screens/
│   ├── components/
│   └── theme/
│
├── viewmodel/
│
├── navigation/
│
├── service/
│
└── MainActivity.kt
🚀 Getting StartedPrerequisitesAndroid Studio Meerkat (or newer)  Android SDK 35+  Kotlin 2.x  Gradle 8+  InstallationClone the repository  Bashgit clone [https://github.com/amirkhedri/Music-Player-Android-App](https://github.com/amirkhedri/Music-Player-Android-App)
Open in Android Studio  PlaintextFile → Open
Sync Gradle  Run the application on  Android Emulator  Physical Device (Android 8.0+)  📋 PermissionsThe application requires the following permissions:  XMLREAD_MEDIA_AUDIO
READ_EXTERNAL_STORAGE
WRITE_EXTERNAL_STORAGE (Legacy)
FOREGROUND_SERVICE
POST_NOTIFICATIONS
Depending on the Android version, runtime permissions are requested automatically.  📦 Libraries UsedLibraryPurposeJetpack ComposeUINavigation ComposeNavigationHiltDependency InjectionRoomLocal DatabaseCoilImage LoadingMedia3 / ExoPlayerAudio PlaybackDataStorePreferencesCoroutinesAsync ProgrammingStateFlowUI State🎯 App WorkflowPlaintextLogin / Register
   │
   ▼
Library
   │
   ├── Favorites
   ├── Artists
   │      └── Songs
   ├── Playlists
   │      └── Playlist Songs
   └── Player
          └── Equalizer
📸 Main ScreensLogin / Create Account  Library  Favorites  Artists  Artist Details  Playlists  Playlist Details  Full Music Player  Equalizer🎨 Design PrinciplesMaterial Design 3  Responsive Layout  Modern Animations  Smooth User Experience  Minimalistic Interface  Accessibility Friendly  🔒 Data ManagementThe application stores:  Favorite songs  Custom playlists  Theme preferences  User session[cite: 11]Custom equalizer profilesAll data is stored locally on the device[cite: 11].⚡ Performance OptimizationsLazyColumn[cite: 11]LazyVerticalGrid[cite: 11]remember {}[cite: 11]StateFlow[cite: 11]Coroutines[cite: 11]Image caching with Coil[cite: 11]Efficient recomposition[cite: 11]MediaStore querying[cite: 11]🔮 Future ImprovementsLyrics Support[cite: 11]Sleep Timer[cite: 11]Recently Played[cite: 11]Most Played[cite: 11]Album Screen[cite: 11]Folder Browser[cite: 11]Shuffle All[cite: 11]Repeat Modes[cite: 11]Cloud Backup[cite: 11]Online Streaming[cite: 11]Wear OS Support[cite: 11]Android Auto Support[cite: 11]Chromecast[cite: 11]🤝 ContributingContributions are welcome![cite: 11]Fork the repository[cite: 11]Create a feature branch[cite: 11]Bashgit checkout -b feature/NewFeature
Commit changes[cite: 11]Bashgit commit -m "Add New Feature"
Push[cite: 11]Bashgit push origin feature/NewFeature
Open a Pull Request[cite: 11]👨‍💻 AuthorAmir Khedri[cite: 11]Computer Engineering Student[cite: 11]University of Isfahan[cite: 11]📄 LicenseThis project is licensed under the MIT License[cite: 11].PlaintextMIT License

Copyright (c) 2026 Amir Khedri

Permission is hereby granted, free of charge,
to any person obtaining a copy of this software...
⭐ If you like this projectGive it a ⭐ on GitHub![cite: 11]It helps support the project and motivates future development.[cite: 11]📧 ContactGitHub: https://github.com/amirkhedri[cite: 11]Email: khedria95@gmail.com[cite: 11]